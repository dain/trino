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
package com.facebook.presto.connector.thrift;

import com.facebook.presto.connector.thrift.api.PrestoThriftBlock;
import com.facebook.presto.connector.thrift.api.PrestoThriftFunctionOutputResult;
import com.facebook.presto.connector.thrift.api.PrestoThriftFunctionService;
import com.facebook.presto.connector.thrift.api.PrestoThriftFunctionStartResult;
import com.facebook.presto.connector.thrift.api.PrestoThriftFunctionStatus;
import com.facebook.presto.connector.thrift.api.PrestoThriftId;
import com.facebook.presto.connector.thrift.api.PrestoThriftPageResult;
import com.facebook.presto.spi.ConnectorPageSource;
import com.facebook.presto.spi.Page;
import com.facebook.presto.spi.type.Type;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.facebook.presto.connector.thrift.api.PrestoThriftBlock.fromBlock;
import static com.facebook.presto.connector.thrift.api.PrestoThriftFunctionStatus.BLOCKED;
import static com.facebook.presto.connector.thrift.api.PrestoThriftFunctionStatus.FINISHED;
import static com.facebook.presto.connector.thrift.api.PrestoThriftFunctionStatus.HAS_OUTPUT;
import static com.facebook.presto.connector.thrift.api.PrestoThriftFunctionStatus.NEEDS_INPUT;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static io.airlift.concurrent.MoreFutures.toCompletableFuture;
import static io.airlift.concurrent.MoreFutures.tryGetFutureValue;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public final class ThriftTableFunctionPageSource
        implements ConnectorPageSource
{
    private final PrestoThriftFunctionService service;
    private final ConnectorPageSource input;
    private final List<Type> inputTypes;
    private final List<Type> outputTypes;
    private final PrestoThriftId invocationId;

    private boolean finished;
    private ListenableFuture<PrestoThriftFunctionStatus> future;
    private CompletableFuture<PrestoThriftFunctionStatus> completableFuture;

    public ThriftTableFunctionPageSource(
            PrestoThriftFunctionService service,
            String functionName,
            List<Type> inputTypes,
            List<Type> outputTypes,
            ConnectorPageSource input)
    {
        this.service = requireNonNull(service, "service is null");
        this.input = requireNonNull(input, "input is null");
        this.inputTypes = ImmutableList.copyOf(requireNonNull(inputTypes, "inputTypes is null"));
        this.outputTypes = ImmutableList.copyOf(requireNonNull(outputTypes, "outputTypes is null"));

        PrestoThriftFunctionStartResult result = service.startInvocation(
                functionName,
                inputTypes.stream().map(Type::getDisplayName).collect(toList()),
                outputTypes.stream().map(Type::getDisplayName).collect(toList()));

        this.invocationId = result.getInvocationId();
        setStatus(result.getStatus());
    }

    @Override
    public long getCompletedBytes()
    {
        return 0;
    }

    @Override
    public long getReadTimeNanos()
    {
        return 0;
    }

    @Override
    public long getSystemMemoryUsage()
    {
        return 0;
    }

    @Override
    public CompletableFuture<?> isBlocked()
    {
        Optional<PrestoThriftFunctionStatus> status = tryGetFutureValue(future);
        if (!status.isPresent()) {
            return completableFuture;
        }

        if (status.get() == NEEDS_INPUT) {
            CompletableFuture<?> blocked = input.isBlocked();
            if (!blocked.isDone()) {
                return blocked;
            }

            Page page = input.getNextPage();
            if (page != null) {
                setStatus(service.addInput(invocationId, toThriftPage(page, inputTypes)));
            }

            if (input.isFinished()) {
                setStatus(service.noMoreInput(invocationId));
            }

            return completableFuture;
        }

        if (status.get() == BLOCKED) {
            setStatus(service.getStatus(invocationId));
        }

        return completableFuture;
    }

    @Override
    public boolean isFinished()
    {
        Optional<PrestoThriftFunctionStatus> status = tryGetFutureValue(future);
        if (status.isPresent() && (status.get() == FINISHED)) {
            finished = true;
        }
        return finished;
    }

    @Override
    public Page getNextPage()
    {
        if (input.isFinished()) {
            setStatus(service.noMoreInput(invocationId));
        }

        Optional<PrestoThriftFunctionStatus> status = tryGetFutureValue(future);
        if (!status.isPresent() || (status.get() != HAS_OUTPUT)) {
            return null;
        }

        PrestoThriftFunctionOutputResult result = service.getOutput(invocationId);
        setStatus(result.getStatus());
        return result.toPage(outputTypes);
    }

    @Override
    public void close()
    {
        service.endInvocation(invocationId);
    }

    private void setStatus(PrestoThriftFunctionStatus status)
    {
        setStatus(immediateFuture(status));
    }

    private void setStatus(ListenableFuture<PrestoThriftFunctionStatus> status)
    {
        future = status;
        completableFuture = toCompletableFuture(future);
    }

    private static PrestoThriftPageResult toThriftPage(Page page, List<Type> types)
    {
        if (page == null) {
            return new PrestoThriftPageResult(ImmutableList.of(), 0, null);
        }
        checkArgument(page.getChannelCount() == types.size(), "page channels and types do not match");
        List<PrestoThriftBlock> columnBlocks = new ArrayList<>(types.size());
        for (int i = 0; i < types.size(); i++) {
            columnBlocks.add(fromBlock(page.getBlock(i), types.get(i)));
        }
        return new PrestoThriftPageResult(columnBlocks, page.getPositionCount(), null);
    }
}
