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
package io.trino.operator.project;

import com.google.common.collect.ImmutableMap;
import io.airlift.units.Duration;
import io.trino.plugin.base.metrics.DurationTiming;
import io.trino.plugin.base.metrics.LongCount;
import io.trino.spi.metrics.Metric;
import io.trino.spi.metrics.Metrics;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class PageProcessorMetrics
{
    private static final String FILTER_TIME = "Filter CPU time";
    private static final String PROJECTION_TIME = "Projection CPU time";
    public static final String DYNAMIC_FILTER_TIME = "Dynamic Filter CPU time";
    public static final String DYNAMIC_FILTER_OUTPUT_POSITIONS = "Dynamic Filter output positions";

    private long filterTimeNanos;
    private boolean hasFilter;
    private long projectionTimeNanos;
    private boolean hasProjection;
    private long dynamicFilterTimeNanos;
    private long dynamicFilterOutputPositions;

    public void recordFilterTime(long filterTimeNanos)
    {
        this.filterTimeNanos += filterTimeNanos;
        hasFilter = true;
    }

    public void recordDynamicFilterMetrics(long filterTimeNanos, long outputPositions)
    {
        dynamicFilterTimeNanos += filterTimeNanos;
        dynamicFilterOutputPositions += outputPositions;
    }

    public void recordProjectionTime(long projectionTimeNanos)
    {
        this.projectionTimeNanos += projectionTimeNanos;
        hasProjection = true;
    }

    public Metrics getMetrics()
    {
        ImmutableMap.Builder<String, Metric<?>> builder = ImmutableMap.builder();
        if (hasFilter) {
            builder.put(FILTER_TIME, new DurationTiming(new Duration(filterTimeNanos, NANOSECONDS)));
        }
        if (dynamicFilterOutputPositions > 0) {
            builder.put(DYNAMIC_FILTER_TIME, new DurationTiming(new Duration(dynamicFilterTimeNanos, NANOSECONDS)));
            builder.put(DYNAMIC_FILTER_OUTPUT_POSITIONS, new LongCount(dynamicFilterOutputPositions));
        }
        if (hasProjection) {
            builder.put(PROJECTION_TIME, new DurationTiming(new Duration(projectionTimeNanos, NANOSECONDS)));
        }
        return new Metrics(builder.buildOrThrow());
    }
}
