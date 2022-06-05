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
package io.trino.plugin.jdbc;

import io.trino.plugin.jdbc.ptf.TableFunction;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.function.FunctionId;
import io.trino.spi.function.FunctionProvider;
import io.trino.spi.ptf.Argument;
import io.trino.spi.ptf.TableFunctionAnalysis;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

public class JdbcFunctionProvider
        implements FunctionProvider
{
    private final Map<FunctionId, TableFunction> tableFunctions;

    public JdbcFunctionProvider(Collection<TableFunction> tableFunctions)
    {
        this.tableFunctions = requireNonNull(tableFunctions, "tableFunctions is null").stream()
                .collect(toImmutableMap(function -> function.getMetadata().getFunctionId(), Function.identity()));
    }

    @Override
    public TableFunctionAnalysis analyzeTableFunction(ConnectorSession session, ConnectorTransactionHandle transaction, FunctionId functionId, Map<String, Argument> arguments)
    {
        TableFunction tableFunction = tableFunctions.get(functionId);
        if (tableFunction == null) {
            throw new IllegalArgumentException("Unknown function " + functionId);
        }
        return tableFunction.analyze(session, transaction, arguments);
    }
}
