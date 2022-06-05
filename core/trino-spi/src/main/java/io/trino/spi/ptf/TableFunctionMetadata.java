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
package io.trino.spi.ptf;

import io.trino.spi.function.FunctionId;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public class TableFunctionMetadata
{
    private final FunctionId functionId;
    private final List<ArgumentSpecification> arguments;
    private final ReturnTypeSpecification returnTypeSpecification;

    private TableFunctionMetadata(FunctionId functionId, List<ArgumentSpecification> arguments, ReturnTypeSpecification returnTypeSpecification)
    {
        this.functionId = requireNonNull(functionId, "functionId is null");
        this.arguments = requireNonNull(List.copyOf(arguments), "arguments is null");
        this.returnTypeSpecification = requireNonNull(returnTypeSpecification, "returnTypeSpecification is null");
    }

    public FunctionId getFunctionId()
    {
        return functionId;
    }

    public List<ArgumentSpecification> getArguments()
    {
        return arguments;
    }

    public ReturnTypeSpecification getReturnTypeSpecification()
    {
        return returnTypeSpecification;
    }

    @Override
    public String toString()
    {
        return new StringJoiner(", ", TableFunctionMetadata.class.getSimpleName() + "[", "]")
                .add("functionId=" + functionId)
                .add("arguments=" + arguments)
                .add("returnTypeSpecification=" + returnTypeSpecification)
                .toString();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private FunctionId functionId;
        private List<ArgumentSpecification> arguments = new ArrayList<>();
        private ReturnTypeSpecification returnTypeSpecification;

        private Builder() {}

        public Builder functionId(FunctionId functionId)
        {
            this.functionId = functionId;
            return this;
        }

        public Builder argument(ArgumentSpecification argument)
        {
            this.arguments.add(argument);
            return this;
        }

        public Builder arguments(List<ArgumentSpecification> arguments)
        {
            this.arguments = new ArrayList<>(arguments);
            return this;
        }

        public Builder returnTypeSpecification(ReturnTypeSpecification returnTypeSpecification)
        {
            this.returnTypeSpecification = returnTypeSpecification;
            return this;
        }

        public TableFunctionMetadata builder()
        {
            return new TableFunctionMetadata(functionId, arguments, returnTypeSpecification);
        }
    }
}
