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

import com.google.common.collect.Ordering;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolver;
import io.prestosql.server.security.oauth2.OAuth2Client.AccessToken;

import javax.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.base.Verify.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class OAuth2Service
{
    private static final String STATE_AUDIENCE = "presto_oauth";
    private static final String FAILURE_REPLACEMENT_TEXT = "<!-- ERROR_MESSAGE -->";

    private final OAuth2Client client;
    private final JwtParser jwtParser;

    private final String successHtml;
    private final String failureHtml;

    private final long challengeTimeoutMillis;
    private final byte[] stateHmac;

    @Inject
    public OAuth2Service(OAuth2Client client, @ForOAuth2 SigningKeyResolver signingKeyResolver, OAuth2Config oauth2Config)
            throws IOException
    {
        this.client = requireNonNull(client, "client is null");
        this.jwtParser = Jwts.parser().setSigningKeyResolver(signingKeyResolver);

        this.successHtml = Resources.toString(Resources.getResource(getClass(), "/oauth2/success.html"), UTF_8);
        this.failureHtml = Resources.toString(Resources.getResource(getClass(), "/oauth2/failure.html"), UTF_8);
        verify(failureHtml.contains(FAILURE_REPLACEMENT_TEXT), "login.html does not contain the replacement text");

        requireNonNull(oauth2Config, "oauth2Config is null");
        this.challengeTimeoutMillis = oauth2Config.getChallengeTimeout().toMillis();
        if (oauth2Config.getStateKey().isPresent()) {
            stateHmac = Hashing.sha256().hashString(oauth2Config.getStateKey().get(), UTF_8).asBytes();
        }
        else {
            stateHmac = new byte[32];
            new SecureRandom().nextBytes(stateHmac);
        }
    }

    public URI startChallenge(URI callbackUri)
    {
        String state = Jwts.builder()
                .signWith(SignatureAlgorithm.HS256, stateHmac)
                .setAudience(STATE_AUDIENCE)
                .setExpiration(new Date(System.currentTimeMillis() + challengeTimeoutMillis))
                .setExpiration(Date.from(ZonedDateTime.now().plusMinutes(5).toInstant()))
                .compact();

        return client.getAuthorizationUri(state, callbackUri);
    }

    public OAuthResult finishChallenge(String state, String code, URI callbackUri)
            throws ChallengeFailedException
    {
        requireNonNull(callbackUri, "callbackUri is null");
        requireNonNull(state, "state is null");
        requireNonNull(code, "code is null");

        Claims stateClaims = parseState(state);
        if (!STATE_AUDIENCE.equals(stateClaims.getAudience())) {
            // this is very unlikely, but is a good safety check
            throw new ChallengeFailedException("Unexpected state audience");
        }

        // fetch access token
        AccessToken accessToken = client.getAccessToken(code, callbackUri);

        // validate access token is trusted by this server
        Claims parsedToken = jwtParser.parseClaimsJws(accessToken.getAccessToken()).getBody();

        // determine expiration
        Instant validUntil = accessToken.getValidUntil()
                .map(instant -> Ordering.natural().min(instant, parsedToken.getExpiration().toInstant()))
                .orElse(parsedToken.getExpiration().toInstant());

        return new OAuthResult(accessToken.getAccessToken(), validUntil);
    }

    private Claims parseState(String state)
            throws ChallengeFailedException
    {
        Claims stateClaims;
        try {
            stateClaims = Jwts.parser()
                    .setSigningKey(stateHmac)
                    .parseClaimsJws(state)
                    .getBody();
        }
        catch (RuntimeException e) {
            throw new ChallengeFailedException("State validation failed", e);
        }
        return stateClaims;
    }

    public Jws<Claims> parseClaimsJws(String token)
    {
        return jwtParser.parseClaimsJws(token);
    }

    public String getSuccessHtml()
    {
        return successHtml;
    }

    public String getCallbackErrorHtml(String errorCode)
    {
        String message = "";
        switch (errorCode) {
            case "access_denied":
                message = "OAuth2 server denied the login";
                break;
            case "unauthorized_client":
                message = "OAuth2 server does not allow request from this Presto server";
                break;
            case "server_error":
                message = "OAuth2 server had a failure";
                break;
            case "temporarily_unavailable":
                message = "OAuth2 server is temporarily unavailable";
                break;
        }
        return failureHtml.replace(FAILURE_REPLACEMENT_TEXT, message);
    }

    public String getInternalFailureHtml(String errorMessage)
    {
        return failureHtml.replace(FAILURE_REPLACEMENT_TEXT, nullToEmpty(errorMessage));
    }

    public static class OAuthResult
    {
        private final String accessToken;
        private final Instant tokenExpiration;

        public OAuthResult(String accessToken, Instant tokenExpiration)
        {
            this.accessToken = requireNonNull(accessToken, "accessToken is null");
            this.tokenExpiration = requireNonNull(tokenExpiration, "tokenExpiration is null");
        }

        public String getAccessToken()
        {
            return accessToken;
        }

        public Instant getTokenExpiration()
        {
            return tokenExpiration;
        }
    }
}
