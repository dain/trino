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

import com.facebook.swift.service.ThriftException;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.util.List;

@ThriftService
public interface PrestoThriftFunctionService
        extends Closeable
{
    @ThriftMethod("prestoFunctionStartInvocation")
    PrestoThriftFunctionStartResult startInvocation(String functionName, List<String> inputTypes, List<String> outputTypes)
            throws PrestoThriftServiceException;

    @ThriftMethod("prestoFunctionEndInvocation")
    void endInvocation(PrestoThriftId invocationId)
            throws PrestoThriftServiceException;

    @ThriftMethod(value = "prestoFunctionGetStatus",
            exception = @ThriftException(type = PrestoThriftServiceException.class, id = 1))
    ListenableFuture<PrestoThriftFunctionStatus> getStatus(PrestoThriftId invocationId);

    @ThriftMethod("prestoFunctionNoMoreInput")
    PrestoThriftFunctionStatus noMoreInput(PrestoThriftId invocationId)
            throws PrestoThriftServiceException;

    @ThriftMethod("prestoFunctionAddInput")
    PrestoThriftFunctionStatus addInput(PrestoThriftId invocationId, PrestoThriftPageResult input)
            throws PrestoThriftServiceException;

    @ThriftMethod("prestoFunctionGetOutput")
    PrestoThriftFunctionOutputResult getOutput(PrestoThriftId invocationId)
            throws PrestoThriftServiceException;

    @Override
    void close();
}
