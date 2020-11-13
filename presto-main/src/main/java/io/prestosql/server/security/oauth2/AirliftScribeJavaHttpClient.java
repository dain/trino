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

import com.github.scribejava.core.httpclient.multipart.MultipartPayload;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.OAuthRequest.ResponseConverter;
import com.github.scribejava.core.model.Verb;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import io.airlift.http.client.BodyGenerator;
import io.airlift.http.client.FileBodyGenerator;
import io.airlift.http.client.HttpClient;
import io.airlift.http.client.Request;
import io.airlift.http.client.Response;
import io.airlift.http.client.ResponseHandler;
import io.airlift.http.client.StaticBodyGenerator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

import static com.google.common.net.HttpHeaders.USER_AGENT;
import static io.airlift.http.client.StaticBodyGenerator.createStaticBodyGenerator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableMap;

public class AirliftScribeJavaHttpClient
        implements com.github.scribejava.core.httpclient.HttpClient
{
    private final HttpClient client;

    public AirliftScribeJavaHttpClient(HttpClient client)
    {
        this.client = requireNonNull(client, "client is null");
    }

    @Override
    public void close() {}

    @Override
    public <T> Future<T> executeAsync(
            String userAgent,
            Map<String, String> headers,
            Verb httpVerb,
            String completeUrl,
            byte[] bodyContents,
            OAuthAsyncRequestCallback<T> callback,
            OAuthRequest.ResponseConverter<T> converter)
    {
        return doExecuteAsync(
                httpVerb,
                completeUrl,
                userAgent,
                headers,
                Optional.ofNullable(bodyContents).map(StaticBodyGenerator::createStaticBodyGenerator),
                converter,
                callback);
    }

    @Override
    public <T> Future<T> executeAsync(
            String userAgent,
            Map<String, String> headers,
            Verb httpVerb,
            String completeUrl,
            MultipartPayload bodyContents,
            OAuthAsyncRequestCallback<T> callback,
            OAuthRequest.ResponseConverter<T> converter)
    {
        throw new UnsupportedOperationException("Multipart payload not supported");
    }

    @Override
    public <T> Future<T> executeAsync(
            String userAgent,
            Map<String, String> headers,
            Verb httpVerb,
            String completeUrl,
            String bodyContents,
            OAuthAsyncRequestCallback<T> callback,
            OAuthRequest.ResponseConverter<T> converter)
    {
        return doExecuteAsync(
                httpVerb,
                completeUrl,
                userAgent,
                headers,
                Optional.ofNullable(bodyContents).map(contents -> createStaticBodyGenerator(contents, UTF_8)),
                converter,
                callback);
    }

    @Override
    public <T> Future<T> executeAsync(
            String userAgent,
            Map<String, String> headers,
            Verb httpVerb,
            String completeUrl,
            File bodyContents,
            OAuthAsyncRequestCallback<T> callback,
            OAuthRequest.ResponseConverter<T> converter)
    {
        return doExecuteAsync(
                httpVerb,
                completeUrl,
                userAgent,
                headers,
                Optional.ofNullable(bodyContents).map(File::toPath).map(FileBodyGenerator::new),
                converter,
                callback);
    }

    private <T> Future<T> doExecuteAsync(
            Verb httpVerb,
            String completeUrl,
            String userAgent,
            Map<String, String> headers,
            Optional<BodyGenerator> bodyGenerator,
            ResponseConverter<T> converter,
            OAuthAsyncRequestCallback<T> callback)
    {
        Request request = createRequest(httpVerb, completeUrl, userAgent, headers, bodyGenerator);
        return client.executeAsync(request, new ResponseHandler<>()
        {
            @Override
            public T handleException(Request request, Exception exception)
                    throws Exception
            {
                if (callback != null) {
                    callback.onThrowable(exception);
                }
                throw exception;
            }

            @Override
            public T handle(Request request, Response response)
                    throws Exception
            {
                try {
                    T value = converter.convert(convertResponse(response));
                    if (callback != null) {
                        callback.onCompleted(value);
                    }
                    return value;
                }
                catch (Exception e) {
                    if (callback != null) {
                        callback.onThrowable(e);
                    }
                    throw e;
                }
            }
        });
    }

    @Override
    public com.github.scribejava.core.model.Response execute(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl, byte[] bodyContents)
            throws IOException
    {
        return doExecute(httpVerb, completeUrl, userAgent, headers, Optional.ofNullable(bodyContents).map(StaticBodyGenerator::createStaticBodyGenerator));
    }

    @Override
    public com.github.scribejava.core.model.Response execute(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl, MultipartPayload bodyContents)
    {
        throw new UnsupportedOperationException("Multipart payload not supported");
    }

    @Override
    public com.github.scribejava.core.model.Response execute(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl, String bodyContents)
            throws IOException
    {
        return doExecute(httpVerb, completeUrl, userAgent, headers, Optional.ofNullable(bodyContents).map(contents -> createStaticBodyGenerator(contents, UTF_8)));
    }

    @Override
    public com.github.scribejava.core.model.Response execute(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl, File bodyContents)
            throws IOException
    {
        return doExecute(httpVerb, completeUrl, userAgent, headers, Optional.ofNullable(bodyContents).map(File::toPath).map(FileBodyGenerator::new));
    }

    private com.github.scribejava.core.model.Response doExecute(Verb httpVerb,
            String completeUrl, String userAgent,
            Map<String, String> headers,
            Optional<BodyGenerator> bodyGenerator)
            throws IOException
    {
        Request request = createRequest(httpVerb, completeUrl, userAgent, headers, bodyGenerator);
        return client.execute(request, new ResponseHandler<com.github.scribejava.core.model.Response, IOException>()
        {
            @Override
            public com.github.scribejava.core.model.Response handleException(Request request, Exception exception)
                    throws IOException
            {
                Throwables.throwIfInstanceOf(exception, IOException.class);
                Throwables.throwIfUnchecked(exception);
                throw new IOException(exception);
            }

            @Override
            public com.github.scribejava.core.model.Response handle(Request request, Response response)
                    throws IOException
            {
                return convertResponse(response);
            }
        });
    }

    private static Request createRequest(
            Verb httpVerb,
            String completeUrl,
            String userAgent,
            Map<String, String> headers,
            Optional<BodyGenerator> bodyGenerator)
    {
        Request.Builder requestBuilder = Request.builder();

        requestBuilder.setMethod(httpVerb.name());
        requestBuilder.setUri(URI.create(completeUrl));

        for (Map.Entry<String, String> header : headers.entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }
        if (userAgent != null) {
            requestBuilder.setHeader(USER_AGENT, userAgent);
        }

        if (bodyGenerator.isPresent()) {
            if (!headers.containsKey(CONTENT_TYPE)) {
                requestBuilder.setHeader(CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
            }
            requestBuilder.setBodyGenerator(bodyGenerator.get());
        }

        return requestBuilder.build();
    }

    private static com.github.scribejava.core.model.Response convertResponse(Response response)
            throws IOException
    {
        Map<String, String> headers = response.getHeaders().asMap().entrySet().stream()
                .collect(toUnmodifiableMap(entry -> entry.getKey().toString(), entry -> Iterables.getLast(entry.getValue())));
        return new com.github.scribejava.core.model.Response(response.getStatusCode(), null, headers, response.getInputStream());
    }
}
