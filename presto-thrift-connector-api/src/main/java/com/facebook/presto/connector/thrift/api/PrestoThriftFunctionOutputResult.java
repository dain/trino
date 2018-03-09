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

import com.facebook.presto.spi.Page;
import com.facebook.presto.spi.type.Type;
import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import javax.annotation.Nullable;

import java.util.List;

import static com.facebook.presto.connector.thrift.api.PrestoThriftPageResult.checkAllColumnsAreOfExpectedSize;
import static com.facebook.presto.connector.thrift.api.PrestoThriftPageResult.createPage;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@ThriftStruct
public final class PrestoThriftFunctionOutputResult
{
    private final List<PrestoThriftBlock> columnBlocks;
    private final int rowCount;
    private final PrestoThriftFunctionStatus status;

    @ThriftConstructor
    public PrestoThriftFunctionOutputResult(List<PrestoThriftBlock> columnBlocks, int rowCount, PrestoThriftFunctionStatus status)
    {
        this.columnBlocks = requireNonNull(columnBlocks, "columnBlocks is null");
        checkArgument(rowCount >= 0, "rowCount is negative");
        checkAllColumnsAreOfExpectedSize(columnBlocks, rowCount);
        this.rowCount = rowCount;
        this.status = requireNonNull(status, "status is null");
    }

    @ThriftField(1)
    public List<PrestoThriftBlock> getColumnBlocks()
    {
        return columnBlocks;
    }

    @ThriftField(2)
    public int getRowCount()
    {
        return rowCount;
    }

    @ThriftField(3)
    public PrestoThriftFunctionStatus getStatus()
    {
        return status;
    }

    @Nullable
    public Page toPage(List<Type> columnTypes)
    {
        return createPage(columnTypes, columnBlocks, rowCount);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("rowCount", rowCount)
                .add("status", status)
                .toString();
    }
}
