/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.metadata;

import io.trino.FeaturesConfig;
import io.trino.operator.aggregation.AggregationMetadata;
import io.trino.operator.window.WindowFunctionSupplier;
import io.trino.spi.TrinoException;
import io.trino.spi.block.Block;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.function.InOut;
import io.trino.spi.function.InvocationConvention;
import io.trino.spi.function.InvocationConvention.InvocationArgumentConvention;
import io.trino.spi.type.Type;
import io.trino.spi.type.TypeOperators;
import io.trino.type.BlockTypeOperators;

import javax.inject.Inject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.primitives.Primitives.wrap;
import static io.trino.client.NodeVersion.UNKNOWN;
import static io.trino.spi.StandardErrorCode.FUNCTION_IMPLEMENTATION_ERROR;
import static io.trino.type.InternalTypeManager.TESTING_TYPE_MANAGER;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class FunctionManager
{
    private final GlobalFunctionCatalog globalFunctionCatalog;

    @Inject
    public FunctionManager(GlobalFunctionCatalog globalFunctionCatalog)
    {
        this.globalFunctionCatalog = requireNonNull(globalFunctionCatalog, "globalFunctionCatalog is null");
    }

    public FunctionInvoker getScalarFunctionInvoker(ResolvedFunction resolvedFunction, InvocationConvention invocationConvention)
    {
        FunctionDependencies functionDependencies = getFunctionDependencies(resolvedFunction);
        FunctionInvoker functionInvoker = globalFunctionCatalog.getScalarFunctionInvoker(
                resolvedFunction.getFunctionId(),
                resolvedFunction.getSignature(),
                functionDependencies,
                invocationConvention);
        verifyMethodHandleSignature(resolvedFunction.getSignature(), functionInvoker, invocationConvention);
        return functionInvoker;
    }

    public AggregationMetadata getAggregateFunctionImplementation(ResolvedFunction resolvedFunction)
    {
        FunctionDependencies functionDependencies = getFunctionDependencies(resolvedFunction);
        return globalFunctionCatalog.getAggregateFunctionImplementation(
                resolvedFunction.getFunctionId(),
                resolvedFunction.getSignature(),
                functionDependencies);
    }

    public WindowFunctionSupplier getWindowFunctionImplementation(ResolvedFunction resolvedFunction)
    {
        FunctionDependencies functionDependencies = getFunctionDependencies(resolvedFunction);
        return globalFunctionCatalog.getWindowFunctionImplementation(
                resolvedFunction.getFunctionId(),
                resolvedFunction.getSignature(),
                functionDependencies);
    }

    private FunctionDependencies getFunctionDependencies(ResolvedFunction resolvedFunction)
    {
        return new FunctionDependencies(this::getScalarFunctionInvoker, resolvedFunction.getTypeDependencies(), resolvedFunction.getFunctionDependencies());
    }

    private static void verifyMethodHandleSignature(BoundSignature boundSignature, FunctionInvoker functionInvoker, InvocationConvention convention)
    {
        MethodHandle methodHandle = functionInvoker.getMethodHandle();
        MethodType methodType = methodHandle.type();

        checkArgument(convention.getArgumentConventions().size() == boundSignature.getArgumentTypes().size(),
                "Expected %s arguments, but got %s", boundSignature.getArgumentTypes().size(), convention.getArgumentConventions().size());

        int expectedParameterCount = convention.getArgumentConventions().stream()
                .mapToInt(InvocationArgumentConvention::getParameterCount)
                .sum();
        expectedParameterCount += methodType.parameterList().stream().filter(ConnectorSession.class::equals).count();
        if (functionInvoker.getInstanceFactory().isPresent()) {
            expectedParameterCount++;
        }
        checkArgument(expectedParameterCount == methodType.parameterCount(),
                "Expected %s method parameters, but got %s", expectedParameterCount, methodType.parameterCount());

        int parameterIndex = 0;
        if (functionInvoker.getInstanceFactory().isPresent()) {
            verifyFunctionSignature(convention.supportsInstanceFactory(), "Method requires instance factory, but calling convention does not support an instance factory");
            MethodHandle factoryMethod = functionInvoker.getInstanceFactory().orElseThrow();
            verifyFunctionSignature(methodType.parameterType(parameterIndex).equals(factoryMethod.type().returnType()), "Invalid return type");
            parameterIndex++;
        }

        int lambdaArgumentIndex = 0;
        for (int argumentIndex = 0; argumentIndex < boundSignature.getArgumentTypes().size(); argumentIndex++) {
            // skip session parameters
            while (methodType.parameterType(parameterIndex).equals(ConnectorSession.class)) {
                verifyFunctionSignature(convention.supportsSession(), "Method requires session, but calling convention does not support session");
                parameterIndex++;
            }

            Class<?> parameterType = methodType.parameterType(parameterIndex);
            Type argumentType = boundSignature.getArgumentTypes().get(argumentIndex);
            InvocationArgumentConvention argumentConvention = convention.getArgumentConvention(argumentIndex);
            switch (argumentConvention) {
                case NEVER_NULL:
                    verifyFunctionSignature(parameterType.isAssignableFrom(argumentType.getJavaType()),
                            "Expected argument type to be %s, but is %s", argumentType, parameterType);
                    break;
                case NULL_FLAG:
                    verifyFunctionSignature(parameterType.isAssignableFrom(argumentType.getJavaType()),
                            "Expected argument type to be %s, but is %s", argumentType.getJavaType(), parameterType);
                    verifyFunctionSignature(methodType.parameterType(parameterIndex + 1).equals(boolean.class),
                            "Expected null flag parameter to be followed by a boolean parameter");
                    break;
                case BOXED_NULLABLE:
                    verifyFunctionSignature(parameterType.isAssignableFrom(wrap(argumentType.getJavaType())),
                            "Expected argument type to be %s, but is %s", wrap(argumentType.getJavaType()), parameterType);
                    break;
                case BLOCK_POSITION:
                    verifyFunctionSignature(parameterType.equals(Block.class) && methodType.parameterType(parameterIndex + 1).equals(int.class),
                            "Expected BLOCK_POSITION argument types to be Block and int");
                    break;
                case IN_OUT:
                    verifyFunctionSignature(parameterType.equals(InOut.class), "Expected IN_OUT argument type to be InOut");
                    break;
                case FUNCTION:
                    Class<?> lambdaInterface = functionInvoker.getLambdaInterfaces().get(lambdaArgumentIndex);
                    verifyFunctionSignature(parameterType.equals(lambdaInterface),
                            "Expected function interface to be %s, but is %s", lambdaInterface, parameterType);
                    lambdaArgumentIndex++;
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown argument convention: " + argumentConvention);
            }
            parameterIndex += argumentConvention.getParameterCount();
        }

        Type returnType = boundSignature.getReturnType();
        switch (convention.getReturnConvention()) {
            case FAIL_ON_NULL:
                verifyFunctionSignature(methodType.returnType().isAssignableFrom(returnType.getJavaType()),
                        "Expected return type to be %s, but is %s", returnType.getJavaType(), methodType.returnType());
                break;
            case NULLABLE_RETURN:
                verifyFunctionSignature(methodType.returnType().isAssignableFrom(wrap(returnType.getJavaType())),
                        "Expected return type to be %s, but is %s", returnType.getJavaType(), wrap(methodType.returnType()));
                break;
            default:
                throw new UnsupportedOperationException("Unknown return convention: " + convention.getReturnConvention());
        }
    }

    private static void verifyFunctionSignature(boolean check, String message, Object... args)
    {
        if (!check) {
            throw new TrinoException(FUNCTION_IMPLEMENTATION_ERROR, format(message, args));
        }
    }

    public static FunctionManager createTestingFunctionManager()
    {
        TypeOperators typeOperators = new TypeOperators();
        GlobalFunctionCatalog functionCatalog = new GlobalFunctionCatalog();
        functionCatalog.addFunctions(SystemFunctionBundle.create(new FeaturesConfig(), typeOperators, new BlockTypeOperators(typeOperators), UNKNOWN));
        functionCatalog.addFunctions(new InternalFunctionBundle(new LiteralFunction(new InternalBlockEncodingSerde(new BlockEncodingManager(), TESTING_TYPE_MANAGER))));
        return new FunctionManager(functionCatalog);
    }
}
