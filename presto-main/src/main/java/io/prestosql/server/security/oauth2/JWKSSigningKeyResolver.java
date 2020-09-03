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

import io.airlift.http.client.HttpClient;
import io.airlift.units.Duration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolver;
import io.prestosql.server.security.jwt.JwkService;

import java.net.URI;
import java.security.Key;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

class JWKSSigningKeyResolver
        implements SigningKeyResolver
{
    private final JwkService jwkService;

    JWKSSigningKeyResolver(URI url, HttpClient httpClient)
    {
        requireNonNull(url, "url is null");
        requireNonNull(httpClient, "httpClient is null");
        this.jwkService = new JwkService(url, httpClient, new Duration(15, TimeUnit.MINUTES));
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims)
    {
        return getPublicKey(header.getKeyId());
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, String plaintext)
    {
        return getPublicKey(header.getKeyId());
    }

    private PublicKey getPublicKey(String keyId)
    {
        return jwkService.getKey(keyId).orElse(null);
    }
}
