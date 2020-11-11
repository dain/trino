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
package io.prestosql.server.security.oauth2;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import io.airlift.units.Duration;
import io.prestosql.dispatcher.DispatchExecutor;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class OAuth2TokenExchange
{
    public static final Duration MAX_POLL_TIME = new Duration(30, SECONDS);

    private static final Response AUTHENTICATION_TIMED_OUT = Response.status(Status.REQUEST_TIMEOUT).entity("Authentication timed out").build();

    private final ListeningExecutorService responseExecutor;
    private final LoadingCache<UUID, SettableFuture<Response>> cache;

    @Inject
    public OAuth2TokenExchange(OAuth2Config config, DispatchExecutor executor)
    {
        this.responseExecutor = requireNonNull(executor, "responseExecutor is null").getExecutor();

        long challengeTimeout = config.getChallengeTimeout().toMillis();
        ListeningScheduledExecutorService scheduledExecutor = executor.getScheduledExecutor();
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite((challengeTimeout + MAX_POLL_TIME.toMillis()) * 2, MILLISECONDS)
                .<UUID, SettableFuture<Response>>removalListener(notification -> notification.getValue().set(AUTHENTICATION_TIMED_OUT))
                .build(new CacheLoader<>()
                {
                    @Override
                    public SettableFuture<Response> load(UUID authId)
                    {
                        SettableFuture<Response> future = SettableFuture.create();
                        ListenableScheduledFuture<?> timout = scheduledExecutor.schedule(() -> future.set(AUTHENTICATION_TIMED_OUT), challengeTimeout, MILLISECONDS);
                        future.addListener(() -> timout.cancel(true), responseExecutor);
                        return future;
                    }
                });
    }

    public void setAccessToken(UUID authId, String accessToken)
    {
        cache.getUnchecked(authId).set(Response.ok(accessToken).build());
    }

    public void setErrorResponse(UUID authId, String message)
    {
        cache.getUnchecked(authId).set(Response.status(Status.UNAUTHORIZED).entity(message).build());
    }

    public SettableFuture<Response> getResponse(UUID authId)
    {
        return cache.getUnchecked(authId);
    }

    public void dropResponse(UUID authId)
    {
        cache.invalidate(authId);
    }
}
