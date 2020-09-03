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

import com.github.scribejava.core.oauth2.OAuth2Error;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import io.airlift.configuration.AbstractConfigurationAwareModule;
import io.prestosql.server.ui.WebUiAuthenticationFilter;

import static io.airlift.configuration.ConfigBinder.configBinder;
import static io.airlift.http.client.HttpClientBinder.httpClientBinder;
import static io.airlift.jaxrs.JaxrsBinder.jaxrsBinder;
import static io.airlift.json.JsonCodecBinder.jsonCodecBinder;

public class OAuth2AuthenticatorModule
        extends AbstractConfigurationAwareModule
{
    @Override
    protected void setup(Binder binder)
    {
        configBinder(binder).bindConfig(OAuth2Config.class);
        binder.bind(OAuth2Service.class).in(Scopes.SINGLETON);
        jaxrsBinder(binder).bind(OAuth2Resource.class);
        jsonCodecBinder(binder).bindJsonCodec(OAuth2Error.class);
        binder.bind(WebUiAuthenticationFilter.class).to(OAuth2WebUiAuthenticationFilter.class).in(Scopes.SINGLETON);
        httpClientBinder(binder).bindHttpClient("oauth2-jwk", ForOAuth2.class);
    }

    @Override
    public int hashCode()
    {
        return OAuth2AuthenticatorModule.class.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof OAuth2AuthenticatorModule;
    }
}
