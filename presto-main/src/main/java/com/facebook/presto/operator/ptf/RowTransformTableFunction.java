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
package com.facebook.presto.operator.ptf;

import com.facebook.presto.spi.ConnectorPageSource;
import com.facebook.presto.spi.Page;
import com.facebook.presto.spi.function.TableFunctionImplementation;

import java.io.IOException;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class RowTransformTableFunction
        implements TableFunctionImplementation
{
    private final Function<Page, Page> transform;

    public RowTransformTableFunction(Function<Page, Page> transform)
    {
        this.transform = requireNonNull(transform, "transform is null");
    }

    @Override
    public ConnectorPageSource create(ConnectorPageSource inputPageSource)
    {
        return new TransformPageSource(inputPageSource, transform);
    }

    private static class TransformPageSource
            implements ConnectorPageSource
    {
        private final ConnectorPageSource inputPageSource;
        private final Function<Page, Page> transform;

        public TransformPageSource(ConnectorPageSource inputPageSource, Function<Page, Page> transform)
        {
            this.inputPageSource = requireNonNull(inputPageSource, "inputPageSource is null");
            this.transform = requireNonNull(transform, "transform is null");
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
        public boolean isFinished()
        {
            return inputPageSource.isFinished();
        }

        @Override
        public Page getNextPage()
        {
            Page page = inputPageSource.getNextPage();
            if (page != null) {
                page = transform.apply(page);
            }
            return page;
        }

        @Override
        public long getSystemMemoryUsage()
        {
            return 0;
        }

        @Override
        public void close()
                throws IOException
        {
            inputPageSource.close();
        }
    }
}
