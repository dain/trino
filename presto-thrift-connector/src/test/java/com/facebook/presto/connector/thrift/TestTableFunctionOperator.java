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

import com.facebook.presto.operator.DriverContext;
import com.facebook.presto.operator.OperatorFactory;
import com.facebook.presto.operator.TableFunctionOperator.TableFunctionOperatorFactory;
import com.facebook.presto.spi.ColumnMetadata;
import com.facebook.presto.spi.Page;
import com.facebook.presto.spi.TableFunction;
import com.facebook.presto.spi.function.PolymorphicTableFunction;
import com.facebook.presto.spi.function.TableFunctionImplementation;
import com.facebook.presto.sql.planner.plan.PlanNodeId;
import com.facebook.presto.type.TypeRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static com.facebook.presto.RowPagesBuilder.rowPagesBuilder;
import static com.facebook.presto.SessionTestUtils.TEST_SESSION;
import static com.facebook.presto.operator.OperatorAssertion.assertOperatorEquals;
import static com.facebook.presto.spi.type.VarcharType.VARCHAR;
import static com.facebook.presto.testing.TestingTaskContext.createTaskContext;
import static com.google.common.collect.Iterables.getOnlyElement;
import static io.airlift.concurrent.Threads.daemonThreadsNamed;
import static io.airlift.slice.Slices.utf8Slice;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newScheduledThreadPool;

@Test(singleThreaded = true)
public class TestTableFunctionOperator
{
    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutor;
    private DriverContext driverContext;

    @BeforeMethod
    public void setUp()
    {
        executor = newCachedThreadPool(daemonThreadsNamed("test-executor-%s"));
        scheduledExecutor = newScheduledThreadPool(2, daemonThreadsNamed("test-scheduledExecutor-%s"));
        driverContext = createTaskContext(executor, scheduledExecutor, TEST_SESSION)
                .addPipelineContext(0, true, true)
                .addDriverContext();
    }

    @AfterMethod
    public void tearDown()
    {
        executor.shutdownNow();
        scheduledExecutor.shutdownNow();
    }

    @Test
    public void testFunction()
    {
        Set<PolymorphicTableFunction> factories = new ThriftPlugin().getPolymorphicTableFunctionFactories(new TypeRegistry());
        PolymorphicTableFunction factory = getOnlyElement(factories);

        TableFunction descriptor = factory.specialize(ImmutableMap.<String, Object>builder()
                .put("name", utf8Slice("reverse"))
                .put("address", utf8Slice("localhost:7779"))
                .put("input", ImmutableList.of(new ColumnMetadata("x", VARCHAR)))
                .put("output", ImmutableList.of(new ColumnMetadata("x", VARCHAR)))
                .build());

        TableFunctionImplementation function = factory.getInstance(descriptor.getHandle());

        OperatorFactory operatorFactory = new TableFunctionOperatorFactory(0, new PlanNodeId("test"), ImmutableList.of(VARCHAR), function);

        List<Page> input = rowPagesBuilder(VARCHAR)
                .row("hello")
                .row("world")
                .pageBreak()
                .row("apple")
                .row("orange")
                .pageBreak()
                .row("grape")
                .build();

        List<Page> expected = rowPagesBuilder(VARCHAR)
                .row("olleh")
                .row("dlrow")
                .pageBreak()
                .row("elppa")
                .row("egnaro")
                .pageBreak()
                .row("eparg")
                .build();

        assertOperatorEquals(operatorFactory, driverContext, input, expected);
    }
}
