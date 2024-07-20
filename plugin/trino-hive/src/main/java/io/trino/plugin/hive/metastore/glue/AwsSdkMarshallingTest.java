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
package io.trino.plugin.hive.metastore.glue;

import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.services.glue.model.CreateDatabaseRequest;
import software.amazon.awssdk.services.glue.model.DatabaseInput;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.awssdk.protocols.json.AwsJsonProtocol.AWS_JSON;

public class AwsSdkMarshallingTest
{
    public static void main(String[] args)
            throws Exception
    {
        DatabaseInput database = DatabaseInput.builder()
                .name("name-test")
                .parameters(ImmutableMap.of("a", "b"))
                .description("description-test")
                .locationUri("location-test")
                .build();

        CreateDatabaseRequest createDatabaseRequest = CreateDatabaseRequest.builder()
                .databaseInput(database)
                .catalogId("catalog-id")
                .build();

        String x = serialize(createDatabaseRequest);
        System.out.println(x);
        CreateDatabaseRequest deserialize = deserialize(x, CreateDatabaseRequest::builder);
        System.out.println(deserialize);
    }

    private static final OperationInfo OPERATION_INFO =
            OperationInfo.builder().hasPayloadMembers(true).httpMethod(SdkHttpMethod.POST).build();

    private static String serialize(SdkPojo sdkPojo)
    {
        AwsJsonProtocolFactory awsJsonProtocolFactory = getAwsJsonProtocolFactory();

        ProtocolMarshaller<SdkHttpFullRequest> protocolMarshaller = awsJsonProtocolFactory.createProtocolMarshaller(OPERATION_INFO);

        return protocolMarshaller.marshall(sdkPojo).contentStreamProvider()
                .map(streamProvider -> {
                    try (InputStream inputStream = streamProvider.newStream()) {
                        return new String(inputStream.readAllBytes(), UTF_8);
                    }
                    catch (IOException e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    private static <T extends SdkPojo> T deserialize(String json, Supplier<SdkPojo> builder)
            throws Exception
    {
        AwsJsonProtocolFactory protocolFactory = getAwsJsonProtocolFactory();

        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<T> responseHandler = protocolFactory.createResponseHandler(operationMetadata, builder);

        SdkHttpFullResponse x = SdkHttpFullResponse.builder()
                .statusCode(200)
                .content(AbortableInputStream.create(new ByteArrayInputStream(json.getBytes(UTF_8))))
                .build();
        return responseHandler.handle(x, ExecutionAttributes.builder().build());
    }

    private static AwsJsonProtocolFactory getAwsJsonProtocolFactory()
    {
        AwsJsonProtocolFactory awsJsonProtocolFactory = AwsJsonProtocolFactory.builder()
                .clientConfiguration(SdkClientConfiguration.builder()
                        // AwsJsonProtocolFactory requires any URI to be present
                        .option(SdkClientOption.ENDPOINT, URI.create("http://empty"))
                        .build())
                .protocol(AWS_JSON)
                .build();
        return awsJsonProtocolFactory;
    }
}
