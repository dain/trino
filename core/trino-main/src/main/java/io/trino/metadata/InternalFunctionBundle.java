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

import com.google.common.collect.ImmutableList;
import io.trino.operator.scalar.annotations.ScalarFromAnnotationsParser;
import io.trino.operator.window.SqlWindowFunction;
import io.trino.operator.window.WindowAnnotationsParser;
import io.trino.spi.function.AggregationFunction;
import io.trino.spi.function.AggregationFunctionMetadata;
import io.trino.spi.function.AggregationImplementation;
import io.trino.spi.function.BoundSignature;
import io.trino.spi.function.FunctionDependencies;
import io.trino.spi.function.FunctionDependencyDeclaration;
import io.trino.spi.function.FunctionId;
import io.trino.spi.function.FunctionMetadata;
import io.trino.spi.function.InvocationConvention;
import io.trino.spi.function.ScalarFunction;
import io.trino.spi.function.ScalarFunctionImplementation;
import io.trino.spi.function.ScalarOperator;
import io.trino.spi.function.WindowFunction;
import io.trino.spi.function.WindowFunctionSupplier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

public class InternalFunctionBundle
        implements FunctionBundle
{
    private final Map<FunctionId, SqlFunction> functions;

    public InternalFunctionBundle(SqlFunction... functions)
    {
        this(ImmutableList.copyOf(functions));
    }

    public InternalFunctionBundle(List<? extends SqlFunction> functions)
    {
        this.functions = functions.stream()
                .collect(toImmutableMap(function -> function.getFunctionMetadata().getFunctionId(), Function.identity()));
    }

    @Override
    public Collection<FunctionMetadata> getFunctions()
    {
        return functions.values().stream()
                .map(SqlFunction::getFunctionMetadata)
                .collect(toImmutableList());
    }

    @Override
    public AggregationFunctionMetadata getAggregationFunctionMetadata(FunctionId functionId)
    {
        return getSqlFunction(functionId, SqlAggregationFunction.class)
                .getAggregationMetadata();
    }

    @Override
    public FunctionDependencyDeclaration getFunctionDependencies(FunctionId functionId, BoundSignature boundSignature)
    {
        return getSqlFunction(functionId, SqlFunction.class)
                .getFunctionDependencies(boundSignature);
    }

    @Override
    public ScalarFunctionImplementation getScalarFunctionImplementation(
            FunctionId functionId,
            BoundSignature boundSignature,
            FunctionDependencies functionDependencies,
            InvocationConvention invocationConvention)
    {
        return getSqlFunction(functionId, SqlScalarFunction.class)
                .specialize(boundSignature, functionDependencies)
                .getScalarFunctionImplementation(invocationConvention);
    }

    @Override
    public AggregationImplementation getAggregationImplementation(FunctionId functionId, BoundSignature boundSignature, FunctionDependencies functionDependencies)
    {
        return getSqlFunction(functionId, SqlAggregationFunction.class)
                .specialize(boundSignature, functionDependencies);
    }

    @Override
    public WindowFunctionSupplier getWindowFunctionSupplier(FunctionId functionId, BoundSignature boundSignature, FunctionDependencies functionDependencies)
    {
        return getSqlFunction(functionId, SqlWindowFunction.class)
                .specialize(boundSignature, functionDependencies);
    }

    private <T extends SqlFunction> T getSqlFunction(FunctionId functionId, Class<T> type)
    {
        SqlFunction function = functions.get(functionId);
        checkArgument(function != null, "Unknown function implementation: " + functionId);
        checkArgument(type.isInstance(function), "%s is not a %s", function.getFunctionMetadata().getSignature(), type.getSimpleName());
        return type.cast(function);
    }

    public static InternalFunctionBundle extractFunctions(Class<?> functionClass)
    {
        return builder().functions(functionClass).build();
    }

    public static InternalFunctionBundle extractFunctions(Collection<Class<?>> functionClasses)
    {
        InternalFunctionBundleBuilder builder = builder();
        functionClasses.forEach(builder::functions);
        return builder.build();
    }

    public static InternalFunctionBundleBuilder builder()
    {
        return new InternalFunctionBundleBuilder();
    }

    public static class InternalFunctionBundleBuilder
    {
        private final List<SqlFunction> functions = new ArrayList<>();

        private InternalFunctionBundleBuilder() {}

        public InternalFunctionBundleBuilder window(Class<? extends WindowFunction> clazz)
        {
            functions.addAll(WindowAnnotationsParser.parseFunctionDefinition(clazz));
            return this;
        }

        public InternalFunctionBundleBuilder aggregates(Class<?> aggregationDefinition)
        {
            functions.addAll(SqlAggregationFunction.createFunctionsByAnnotations(aggregationDefinition));
            return this;
        }

        public InternalFunctionBundleBuilder scalar(Class<?> clazz)
        {
            functions.addAll(ScalarFromAnnotationsParser.parseFunctionDefinition(clazz));
            return this;
        }

        public InternalFunctionBundleBuilder scalars(Class<?> clazz)
        {
            functions.addAll(ScalarFromAnnotationsParser.parseFunctionDefinitions(clazz));
            return this;
        }

        public InternalFunctionBundleBuilder functions(Class<?> clazz)
        {
            if (WindowFunction.class.isAssignableFrom(clazz)) {
                @SuppressWarnings("unchecked")
                Class<? extends WindowFunction> windowClazz = (Class<? extends WindowFunction>) clazz;
                window(windowClazz);
                return this;
            }

            if (clazz.isAnnotationPresent(AggregationFunction.class)) {
                aggregates(clazz);
                return this;
            }

            if (clazz.isAnnotationPresent(ScalarFunction.class) ||
                    clazz.isAnnotationPresent(ScalarOperator.class)) {
                scalar(clazz);
                return this;
            }

            scalars(clazz);
            return this;
        }

        public InternalFunctionBundleBuilder functions(SqlFunction... sqlFunctions)
        {
            for (SqlFunction sqlFunction : sqlFunctions) {
                function(sqlFunction);
            }
            return this;
        }

        public InternalFunctionBundleBuilder function(SqlFunction sqlFunction)
        {
            requireNonNull(sqlFunction, "sqlFunction is null");
            functions.add(sqlFunction);
            return this;
        }

        public InternalFunctionBundle build()
        {
            return new InternalFunctionBundle(functions);
        }
    }
}
