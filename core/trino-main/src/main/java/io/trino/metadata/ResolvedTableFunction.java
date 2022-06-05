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

import io.trino.spi.ptf.TableFunctionMetadata;

import static java.util.Objects.requireNonNull;

public class ResolvedTableFunction
{
    private final CatalogSchemaFunctionName functionName;
    private final TableFunctionMetadata function;

    public ResolvedTableFunction(CatalogSchemaFunctionName functionName, TableFunctionMetadata function)
    {
        this.functionName = requireNonNull(functionName, "functionName is null");
        this.function = requireNonNull(function, "function is null");
    }

    public CatalogSchemaFunctionName getFunctionName()
    {
        return functionName;
    }

    public TableFunctionMetadata getFunction()
    {
        return function;
    }
}
