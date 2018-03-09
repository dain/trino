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
package com.facebook.presto.connector.thrift.api;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

@ThriftStruct
public final class PrestoThriftFunctionStartResult
{
    private final PrestoThriftId invocationId;
    private final PrestoThriftFunctionStatus status;

    @ThriftConstructor
    public PrestoThriftFunctionStartResult(PrestoThriftId invocationId, PrestoThriftFunctionStatus status)
    {
        this.invocationId = requireNonNull(invocationId, "invocationId is null");
        this.status = requireNonNull(status, "status is null");
    }

    @ThriftField(1)
    public PrestoThriftId getInvocationId()
    {
        return invocationId;
    }

    @ThriftField(2)
    public PrestoThriftFunctionStatus getStatus()
    {
        return status;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("invocationId", invocationId)
                .add("status", status)
                .toString();
    }
}
