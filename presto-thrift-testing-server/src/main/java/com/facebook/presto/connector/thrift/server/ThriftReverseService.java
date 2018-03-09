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
package com.facebook.presto.connector.thrift.server;

import com.facebook.presto.connector.thrift.api.PrestoThriftBlock;
import com.facebook.presto.connector.thrift.api.PrestoThriftFunctionOutputResult;
import com.facebook.presto.connector.thrift.api.PrestoThriftFunctionService;
import com.facebook.presto.connector.thrift.api.PrestoThriftFunctionStartResult;
import com.facebook.presto.connector.thrift.api.PrestoThriftFunctionStatus;
import com.facebook.presto.connector.thrift.api.PrestoThriftId;
import com.facebook.presto.connector.thrift.api.PrestoThriftPageResult;
import com.facebook.presto.connector.thrift.api.PrestoThriftServiceException;
import com.facebook.presto.connector.thrift.api.datatypes.PrestoThriftVarchar;
import com.facebook.presto.spi.block.Block;
import com.facebook.presto.spi.block.BlockBuilder;
import com.facebook.presto.spi.block.BlockBuilderStatus;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import io.airlift.log.Logger;
import io.airlift.slice.Slice;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static com.facebook.presto.connector.thrift.api.PrestoThriftFunctionStatus.FINISHED;
import static com.facebook.presto.connector.thrift.api.PrestoThriftFunctionStatus.HAS_OUTPUT;
import static com.facebook.presto.connector.thrift.api.PrestoThriftFunctionStatus.NEEDS_INPUT;
import static com.facebook.presto.spi.type.VarcharType.VARCHAR;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static io.airlift.slice.SliceUtf8.reverse;

@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class ThriftReverseService
        implements PrestoThriftFunctionService
{
    private static final Logger log = Logger.get(ThriftReverseService.class);
    private static final Random RANDOM = new SecureRandom();

    private final Map<PrestoThriftId, Invocation> invocations = new ConcurrentHashMap<>();

    @Override
    public PrestoThriftFunctionStartResult startInvocation(String functionName, List<String> inputTypes, List<String> outputTypes)
            throws PrestoThriftServiceException
    {
        if (!functionName.equals("reverse")) {
            throw new PrestoThriftServiceException("Function name must be 'reverse'", false);
        }
        if (!inputTypes.equals(ImmutableList.of("varchar"))) {
            throw new PrestoThriftServiceException("Input types must be [varchar]", false);
        }
        if (!outputTypes.equals(ImmutableList.of("varchar"))) {
            throw new PrestoThriftServiceException("Output types must be [varchar]", false);
        }

        PrestoThriftId invocationId = createInvocationId();
        invocations.put(invocationId, new Invocation());
        log.info("startInvocation(%s)", invocationId);
        return new PrestoThriftFunctionStartResult(invocationId, NEEDS_INPUT);
    }

    @Override
    public void endInvocation(PrestoThriftId invocationId)
            throws PrestoThriftServiceException
    {
        log.info("endInvocation(%s)", invocationId);
        getInvocation(invocationId);
        invocations.remove(invocationId);
    }

    @Override
    public ListenableFuture<PrestoThriftFunctionStatus> getStatus(PrestoThriftId invocationId)
    {
        Invocation invocation = getInvocation(invocationId);
        PrestoThriftFunctionStatus status;
        synchronized (invocation) {
            if (invocation.getInput() != null) {
                status = HAS_OUTPUT;
            }
            else if (invocation.isNoMoreInput()) {
                status = FINISHED;
            }
            else {
                status = NEEDS_INPUT;
            }
        }
        log.info("getStatus(%s): %s", invocationId, status);
        return immediateFuture(status);
    }

    @Override
    public PrestoThriftFunctionStatus noMoreInput(PrestoThriftId invocationId)
            throws PrestoThriftServiceException
    {
        log.info("noMoreInput(%s)", invocationId);
        Invocation invocation = getInvocation(invocationId);
        synchronized (invocation) {
            invocation.noMoreInput();
        }
        return FINISHED;
    }

    @Override
    public PrestoThriftFunctionStatus addInput(PrestoThriftId invocationId, PrestoThriftPageResult input)
            throws PrestoThriftServiceException
    {
        log.info("addInput(%s): rows=%s", invocationId, input.getRowCount());
        Invocation invocation = getInvocation(invocationId);
        synchronized (invocation) {
            if (invocation.getInput() != null) {
                throw new PrestoThriftServiceException("Invocation does not need input", false);
            }
            invocation.setInput(input);
            return HAS_OUTPUT;
        }
    }

    @Override
    public PrestoThriftFunctionOutputResult getOutput(PrestoThriftId invocationId)
            throws PrestoThriftServiceException
    {
        log.info("getOutput(%s)", invocationId);
        Invocation invocation = getInvocation(invocationId);
        synchronized (invocation) {
            PrestoThriftPageResult input = invocation.getInput();
            if (input == null) {
                throw new PrestoThriftServiceException("Invocation does not have input", false);
            }
            invocation.setInput(null);

            Block block = input.getColumnBlocks().get(0).getVarcharData().toBlock(VARCHAR);
            BlockBuilder builder = VARCHAR.createBlockBuilder(new BlockBuilderStatus(), block.getPositionCount());

            for (int i = 0; i < input.getRowCount(); i++) {
                if (block.isNull(i)) {
                    builder.appendNull();
                }
                else {
                    Slice slice = VARCHAR.getSlice(block, i);
                    slice = reverse(slice);
                    VARCHAR.writeSlice(builder, slice);
                }
            }

            PrestoThriftBlock column = PrestoThriftVarchar.fromBlock(builder.build(), VARCHAR);

            return new PrestoThriftFunctionOutputResult(
                    ImmutableList.of(column),
                    column.numberOfRecords(),
                    invocation.isNoMoreInput() ? FINISHED : NEEDS_INPUT);
        }
    }

    @Override
    public void close() {}

    private Invocation getInvocation(PrestoThriftId invocationId)
    {
        Invocation invocation = invocations.get(invocationId);
        if (invocation == null) {
            throw new PrestoThriftServiceException("Invocation not found", false);
        }
        return invocation;
    }

    private static PrestoThriftId createInvocationId()
    {
        byte[] id = new byte[16];
        RANDOM.nextBytes(id);
        return new PrestoThriftId(id);
    }

    private static class Invocation
    {
        private boolean noMoreInput;
        private PrestoThriftPageResult input;

        public boolean isNoMoreInput()
        {
            return noMoreInput;
        }

        public void noMoreInput()
        {
            this.noMoreInput = true;
        }

        public PrestoThriftPageResult getInput()
        {
            return input;
        }

        public void setInput(PrestoThriftPageResult input)
        {
            this.input = input;
        }
    }
}
