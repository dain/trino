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
package io.trino.spi.function;

import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.ptf.Argument;
import io.trino.spi.ptf.TableFunctionAnalysis;

import java.util.Map;

public interface FunctionProvider
{
    default ScalarImplementation getScalarImplementation(
            FunctionId functionId,
            BoundSignature boundSignature,
            FunctionDependencies functionDependencies,
            InvocationConvention invocationConvention)
    {
        throw new IllegalArgumentException("Unknown function " + functionId);
    }

    default AggregationImplementation getAggregationImplementation(FunctionId functionId, BoundSignature boundSignature, FunctionDependencies functionDependencies)
    {
        throw new IllegalArgumentException("Unknown function " + functionId);
    }

    default WindowFunctionSupplier getWindowFunctionSupplier(FunctionId functionId, BoundSignature boundSignature, FunctionDependencies functionDependencies)
    {
        throw new IllegalArgumentException("Unknown function " + functionId);
    }

    /**
     * This method is called by the Analyzer. Its main purposes are to:
     * 1. Determine the resulting relation type of the Table Function in case when the declared return type is GENERIC_TABLE.
     * 2. Declare the dependencies between the input descriptors and the input tables.
     * 3. Perform function-specific validation and pre-processing of the input arguments.
     * As part of function-specific validation, the Table Function's author might want to:
     * - check if the descriptors which reference input tables contain a correct number of column references
     * - check if the referenced input columns have appropriate types to fit the function's logic // TODO return request for coercions to the Analyzer in the TableFunctionAnalysis object
     * - if there is a descriptor which describes the function's output, check if it matches the shape of the actual function's output
     * - for table arguments, check the number and types of ordering columns
     * <p>
     * The actual argument values, and the pre-processing results can be stored in an ConnectorTableFunctionHandle
     * object, which will be passed along with the Table Function invocation through subsequent phases of planning.
     *
     * @param arguments actual invocation arguments, mapped by argument names
     */
    default TableFunctionAnalysis analyzeTableFunction(
            ConnectorSession session,
            ConnectorTransactionHandle transaction,
            FunctionId functionId,
            Map<String, Argument> arguments)
    {
        throw new IllegalArgumentException("Unknown function " + functionId);
    }
}
