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
package io.prestosql.server.ui;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import io.airlift.configuration.AbstractConfigurationAwareModule;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import static com.google.inject.Scopes.SINGLETON;
import static io.airlift.jaxrs.JaxrsBinder.jaxrsBinder;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

public class TestUiModule
        extends AbstractConfigurationAwareModule
{
    @Override
    protected void setup(Binder binder)
    {
        binder.bind(RewriteUiRedirectionResponseFilter.class).in(SINGLETON);
        jaxrsBinder(binder).bind(RewriteUiRedirectionResponseFilter.class);
    }

    @Priority(Priorities.AUTHENTICATION - 1)
    private static class RewriteUiRedirectionResponseFilter
            implements ContainerResponseFilter
    {
        @Override
        public void filter(ContainerRequestContext request, ContainerResponseContext response)
        {
            if (response.getStatus() == SEE_OTHER.getStatusCode()) {
                if (getLocation(request, "/ui/").equals(response.getHeaderString(LOCATION))) {
                    response.getHeaders().replace(LOCATION, ImmutableList.of(getLocation(request, "/ui/test.html")));
                }
            }
        }

        private static String getLocation(ContainerRequestContext request, String path)
        {
            return request.getUriInfo().getBaseUriBuilder().path(path).build().toString();
        }
    }
}
