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
package io.trino.plugin.hive.metastore.glue.model;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.inject.Injector;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.discovery.client.Announcer;
import io.airlift.discovery.client.DiscoveryModule;
import io.airlift.event.client.EventModule;
import io.airlift.http.server.HttpServerModule;
import io.airlift.jaxrs.JaxrsModule;
import io.airlift.jaxrs.JsonMapperParsingException;
import io.airlift.json.JsonModule;
import io.airlift.log.Logger;
import io.airlift.node.NodeModule;
import io.trino.plugin.hive.metastore.glue.model.Model.CreateDatabaseRequest;
import io.trino.plugin.hive.metastore.glue.model.Model.DeleteDatabaseRequest;
import io.trino.plugin.hive.metastore.glue.model.Model.GetDatabaseRequest;
import io.trino.plugin.hive.metastore.glue.model.Model.GetDatabaseResponse;
import io.trino.plugin.hive.metastore.glue.model.Model.GetDatabasesRequest;
import io.trino.plugin.hive.metastore.glue.model.Model.GetDatabasesResponse;
import io.trino.plugin.hive.metastore.glue.model.Model.UpdateDatabaseRequest;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import junit.framework.TestCase;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.GlueClientBuilder;
import software.amazon.awssdk.services.glue.model.GlueException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static io.airlift.jaxrs.JaxrsBinder.jaxrsBinder;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class TestModel extends TestCase
{
    @Path("/")
    public static class ModelResource
    {
        private final Model model;

        @Inject
        public ModelResource(Model model)
        {
            this.model = model;
        }

        @POST
        @Consumes("application/x-amz-json-1.1")
        @Produces("application/x-amz-json-1.1")
        public Object doIt(@Context Request request, @HeaderParam("x-amz-target") String target, Object glueRequest)
        {
            try {
                return switch (target) {
                    // Down cast directly to the correct request type.
                    // This only works because the AWS json mapper handles creating the correct type.
                    case "AWSGlue.CreateDatabase" -> model.createDatabase((CreateDatabaseRequest) glueRequest);
                    case "AWSGlue.GetDatabase" -> model.getDatabase((GetDatabaseRequest) glueRequest);
                    case "AWSGlue.GetDatabases" -> model.getDatabases((GetDatabasesRequest) glueRequest);
                    case "AWSGlue.UpdateDatabase" -> model.updateDatabase((UpdateDatabaseRequest) glueRequest);
                    case "AWSGlue.DeleteDatabase" -> model.deleteDatabase((DeleteDatabaseRequest) glueRequest);
                    default -> Response.status(BAD_REQUEST).build();
                };
            }
            catch (GlueException e) {
                return Response.status(BAD_REQUEST)
                        .entity("{ \"message\": \"%s\" }".formatted("WHAT?"))
                        .build();
            }
        }
    }

    @Provider
    @Consumes("application/x-amz-json-1.1")
    @Produces("application/x-amz-json-1.1")
    public static class AwsJsonMapper
            implements MessageBodyReader<Object>, MessageBodyWriter<Object>
    {
        private final Logger log = Logger.get(getClass());
        private final ObjectMapper objectMapper;

        @Inject
        public AwsJsonMapper(ObjectMapper objectMapper)
        {
            objectMapper = objectMapper.copy();

            SimpleModule module = new SimpleModule();
            module.addSerializer(Instant.class, new UnixTimestampMillsInstantSerializer());
            module.addDeserializer(Instant.class, new UnixTimestampMillsInstantDeserializer());
            objectMapper.registerModule(module);

            this.objectMapper = objectMapper;
        }

        private boolean isPrettyPrintRequested()
        {
            return true;
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
            return true;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
        {
            return true;
        }

        @Override
        public Object readFrom(Class<Object> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders,
                InputStream inputStream)
                throws IOException
        {
            Class<?> targetType = switch (httpHeaders.getFirst("x-amz-target")) {
                case "AWSGlue.CreateDatabase" -> CreateDatabaseRequest.class;
                case "AWSGlue.GetDatabase" -> GetDatabaseRequest.class;
                case "AWSGlue.GetDatabases" -> GetDatabasesRequest.class;
                case "AWSGlue.UpdateDatabase" -> UpdateDatabaseRequest.class;
                case "AWSGlue.DeleteDatabase" -> DeleteDatabaseRequest.class;
                default -> throw new BadRequestException("Invalid target");
            };

            try {
                JsonParser jsonParser = objectMapper.getFactory().createParser(inputStream);

                // Do not close underlying stream after mapping
                jsonParser.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

                return objectMapper.readValue(jsonParser, objectMapper.getTypeFactory().constructType(targetType));
            }
            catch (Exception e) {
                // We want to return a 400 for bad JSON but not for a real IO exception
                if (e instanceof IOException && !(e instanceof JsonProcessingException) && !(e instanceof EOFException)) {
                    throw e;
                }

                // log the exception at debug so it can be viewed during development
                // Note: we are not logging at a higher level because this could cause a denial of service
                log.debug(e, "Invalid JSON for Java type: %s", type);

                // Invalid JSON request. Throwing exception so the response code can be overridden using a mapper.
                throw new JsonMapperParsingException(type, e);
            }
        }

        @Override
        public void writeTo(Object value,
                Class<?> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream outputStream)
                throws IOException
        {
            JsonFactory jsonFactory = objectMapper.getFactory();
            JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream, JsonEncoding.UTF8);

            // Do not close underlying stream after mapping
            jsonGenerator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

            // Pretty print?
            if (isPrettyPrintRequested()) {
                jsonGenerator.useDefaultPrettyPrinter();
            }

            ObjectWriter writer = objectMapper.writerFor(type);

            try {
                writer.writeValue(jsonGenerator, value);

                // add a newline so when you use curl it looks nice
                outputStream.write('\n');
            }
            catch (EOFException e) {
                // ignore EOFException
                // This happens when the client terminates the connection when data
                // is being written.  If the exception is allowed to propagate,
                // the exception will be logged, but this error is not important.
                // This is safe since the output stream is already closed.
            }
        }
    }

    public static class MyObject {

//        @JsonSerialize(using = UnixTimestampMillsInstantSerializer.class)
//        @JsonDeserialize(using = UnixTimestampMillsInstantDeserializer.class)
        private Instant timestamp;

        // Getters and setters

        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
    }
    
    public static void main(String[] args)
            throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        // Register custom serializers and deserializers
        SimpleModule module = new SimpleModule();
        module.addSerializer(Instant.class, new UnixTimestampMillsInstantSerializer());
        module.addDeserializer(Instant.class, new UnixTimestampMillsInstantDeserializer());
        mapper.registerModule(module);

        // Create an instance of MyObject
        MyObject obj = new MyObject();
        obj.setTimestamp(Instant.now());

        // Serialize the object
        String json = mapper.writeValueAsString(obj);
        System.out.println("Serialized: " + json);

        // Deserialize the JSON string back to the object
        MyObject deserializedObj = mapper.readValue(json, MyObject.class);
        System.out.println("Deserialized: " + deserializedObj.getTimestamp());

        ModelMemory modelMemory = new ModelMemory();
        modelMemory.createDatabase(new CreateDatabaseRequest(null, new Model.DatabaseInput("db", "s3://bucket", "desc", Map.of(), List.of(), null, null), Map.of()));
        GetDatabaseRequest getDatabaseRequest = new GetDatabaseRequest(null, "db");
        GetDatabaseResponse getDatabaseResponse = modelMemory.getDatabase(getDatabaseRequest);
        System.out.println(getDatabaseResponse.database());
        GetDatabasesRequest getDatabasesRequest = new GetDatabasesRequest(null, null, 1000, null, List.of());
        GetDatabasesResponse getDatabasesResponse = modelMemory.getDatabases(getDatabasesRequest);
        System.out.println(getDatabasesResponse.databaseList());
        UpdateDatabaseRequest updateDatabaseRequest = new UpdateDatabaseRequest(null, "db", new Model.DatabaseInput("db", "s3://bucket", "desc", Map.of(), List.of(), null, null));
        modelMemory.updateDatabase(updateDatabaseRequest);
        DeleteDatabaseRequest deleteDatabaseRequest = new DeleteDatabaseRequest(null, "db");
        modelMemory.deleteDatabase(deleteDatabaseRequest);
        modelMemory.createDatabase(new CreateDatabaseRequest(null, new Model.DatabaseInput("dba", "s3://bucket/A", "desc-a", Map.of(), List.of(), null, null), Map.of()));
        modelMemory.createDatabase(new CreateDatabaseRequest(null, new Model.DatabaseInput("dbb", "s3://bucket/B", "desc-b", Map.of(), List.of(), null, null), Map.of()));

        Bootstrap app = new Bootstrap(
                new NodeModule(),
                new DiscoveryModule(),
                new HttpServerModule(),
                new JsonModule(),
                new JaxrsModule(),
                new EventModule(),
                binder -> {
                    binder.bind(Model.class).toInstance(modelMemory);
                    jaxrsBinder(binder).bind(ModelResource.class);
                    jaxrsBinder(binder).bind(AwsJsonMapper.class);
                    newSetBinder(binder, Filter.class).addBinding()
                            .toInstance(new HttpFilter() {
                                @Override
                                protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                                        throws IOException, ServletException
                                {
                                    super.doFilter(req, res, chain);
                                }
                            });
                });

        Injector injector = app
                .setRequiredConfigurationProperties(Map.of("node.environment", "test", "http-server.http.port", "10064"))
                .initialize();
        injector.getInstance(Announcer.class).start();

        try (GlueClient glueClient = createGlueClient()) {
            System.out.println(glueClient.getDatabase(builder -> builder.name("dba")));
        }
    }

    public static GlueClient createGlueClient() // GlueHiveMetastoreConfig config, OpenTelemetry openTelemetry)
    {
        GlueClientBuilder glue = GlueClient.builder();

//        glue.overrideConfiguration(builder -> builder
//                .addExecutionInterceptor(AwsSdkTelemetry.builder(openTelemetry)
//                        .setCaptureExperimentalSpanAttributes(true)
//                        .setRecordIndividualHttpError(true)
//                        .build().newExecutionInterceptor())
//                .retryPolicy(retry -> retry
//                        .numRetries(config.getMaxGlueErrorRetries())));

//        Optional<StaticCredentialsProvider> staticCredentialsProvider = getStaticCredentialsProvider(config);
//
//        if (config.isUseWebIdentityTokenCredentialsProvider()) {
//            glue.credentialsProvider(StsWebIdentityTokenFileCredentialsProvider.builder()
//                    .stsClient(getStsClient(config, staticCredentialsProvider))
//                    .asyncCredentialUpdateEnabled(true)
//                    .build());
//        }
//        else if (config.getIamRole().isPresent()) {
//            glue.credentialsProvider(StsAssumeRoleCredentialsProvider.builder()
//                    .refreshRequest(request -> request
//                            .roleArn(config.getIamRole().get())
//                            .roleSessionName("trino-session")
//                            .externalId(config.getExternalId().orElse(null)))
//                    .stsClient(getStsClient(config, staticCredentialsProvider))
//                    .asyncCredentialUpdateEnabled(true)
//                    .build());
//        }
//        else {
//            staticCredentialsProvider.ifPresent(glue::credentialsProvider);
//        }
        glue.endpointProvider(endpointParams -> CompletableFuture.completedFuture(Endpoint.builder().url(URI.create("http://localhost:10064")).build()));
        ApacheHttpClient.Builder httpClient = ApacheHttpClient.builder();
//                .maxConnections(config.getMaxGlueConnections());

//        if (config.getGlueEndpointUrl().isPresent()) {
//            checkArgument(config.getGlueRegion().isPresent(), "Glue region must be set when Glue endpoint URL is set");
//            glue.region(Region.of(config.getGlueRegion().get()));
//            httpClient.proxyConfiguration(ProxyConfiguration.builder()
//                    .endpoint(URI.create("http://localhost:10064"))
//                            .scheme("http")
//                    .build());
//        }
//        else if (config.getGlueRegion().isPresent()) {
//            glue.region(Region.of(config.getGlueRegion().get()));
//        }
//        else if (config.getPinGlueClientToCurrentRegion()) {
//            glue.region(DefaultAwsRegionProviderChain.builder().build().getRegion());
//        }

        glue.httpClientBuilder(httpClient);

        return glue.build();
    }

    public static class UnixTimestampMillsInstantSerializer
            extends StdSerializer<Instant>
    {
        public UnixTimestampMillsInstantSerializer()
        {
            super(Instant.class);
        }

        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider)
                throws IOException
        {
            long seconds = value.getEpochSecond();
            long millis = value.getNano() / 1_000_000;
            gen.writeString(String.format("%d.%03d", seconds, millis));
        }
    }

    public static class UnixTimestampMillsInstantDeserializer
            extends StdDeserializer<Instant>
    {
        public UnixTimestampMillsInstantDeserializer()
        {
            super(Instant.class);
        }

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext context)
                throws IOException
        {
            String timestamp = p.getText();
            int split = timestamp.indexOf('.');
            if (split < 0) {
                return Instant.ofEpochSecond(Long.parseLong(timestamp));
            }
            long seconds = Long.parseLong(timestamp.substring(0, split));
            long millis = Long.parseLong(timestamp.substring(split + 1));
            return Instant.ofEpochSecond(seconds).plusMillis(millis);
        }
    }
}