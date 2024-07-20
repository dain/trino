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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.services.glue.model.BatchCreatePartitionRequest;
import software.amazon.awssdk.services.glue.model.BatchCreatePartitionResponse;
import software.amazon.awssdk.services.glue.model.BatchGetPartitionRequest;
import software.amazon.awssdk.services.glue.model.BatchGetPartitionResponse;
import software.amazon.awssdk.services.glue.model.CreateDatabaseRequest;
import software.amazon.awssdk.services.glue.model.CreateDatabaseResponse;
import software.amazon.awssdk.services.glue.model.CreateTableRequest;
import software.amazon.awssdk.services.glue.model.CreateTableResponse;
import software.amazon.awssdk.services.glue.model.CreateUserDefinedFunctionRequest;
import software.amazon.awssdk.services.glue.model.CreateUserDefinedFunctionResponse;
import software.amazon.awssdk.services.glue.model.DeleteColumnStatisticsForPartitionRequest;
import software.amazon.awssdk.services.glue.model.DeleteColumnStatisticsForPartitionResponse;
import software.amazon.awssdk.services.glue.model.DeleteColumnStatisticsForTableRequest;
import software.amazon.awssdk.services.glue.model.DeleteColumnStatisticsForTableResponse;
import software.amazon.awssdk.services.glue.model.DeleteDatabaseRequest;
import software.amazon.awssdk.services.glue.model.DeleteDatabaseResponse;
import software.amazon.awssdk.services.glue.model.DeletePartitionRequest;
import software.amazon.awssdk.services.glue.model.DeletePartitionResponse;
import software.amazon.awssdk.services.glue.model.DeleteTableRequest;
import software.amazon.awssdk.services.glue.model.DeleteTableResponse;
import software.amazon.awssdk.services.glue.model.DeleteUserDefinedFunctionRequest;
import software.amazon.awssdk.services.glue.model.DeleteUserDefinedFunctionResponse;
import software.amazon.awssdk.services.glue.model.GetColumnStatisticsForPartitionRequest;
import software.amazon.awssdk.services.glue.model.GetColumnStatisticsForPartitionResponse;
import software.amazon.awssdk.services.glue.model.GetColumnStatisticsForTableRequest;
import software.amazon.awssdk.services.glue.model.GetColumnStatisticsForTableResponse;
import software.amazon.awssdk.services.glue.model.GetDatabaseRequest;
import software.amazon.awssdk.services.glue.model.GetDatabaseResponse;
import software.amazon.awssdk.services.glue.model.GetDatabasesRequest;
import software.amazon.awssdk.services.glue.model.GetDatabasesResponse;
import software.amazon.awssdk.services.glue.model.GetPartitionRequest;
import software.amazon.awssdk.services.glue.model.GetPartitionResponse;
import software.amazon.awssdk.services.glue.model.GetPartitionsRequest;
import software.amazon.awssdk.services.glue.model.GetPartitionsResponse;
import software.amazon.awssdk.services.glue.model.GetTableRequest;
import software.amazon.awssdk.services.glue.model.GetTableResponse;
import software.amazon.awssdk.services.glue.model.GetTablesRequest;
import software.amazon.awssdk.services.glue.model.GetTablesResponse;
import software.amazon.awssdk.services.glue.model.GetUserDefinedFunctionRequest;
import software.amazon.awssdk.services.glue.model.GetUserDefinedFunctionResponse;
import software.amazon.awssdk.services.glue.model.GetUserDefinedFunctionsRequest;
import software.amazon.awssdk.services.glue.model.GetUserDefinedFunctionsResponse;
import software.amazon.awssdk.services.glue.model.GlueRequest;
import software.amazon.awssdk.services.glue.model.GlueResponse;
import software.amazon.awssdk.services.glue.model.UpdateColumnStatisticsForPartitionRequest;
import software.amazon.awssdk.services.glue.model.UpdateColumnStatisticsForPartitionResponse;
import software.amazon.awssdk.services.glue.model.UpdateColumnStatisticsForTableRequest;
import software.amazon.awssdk.services.glue.model.UpdateColumnStatisticsForTableResponse;
import software.amazon.awssdk.services.glue.model.UpdateDatabaseRequest;
import software.amazon.awssdk.services.glue.model.UpdateDatabaseResponse;
import software.amazon.awssdk.services.glue.model.UpdatePartitionRequest;
import software.amazon.awssdk.services.glue.model.UpdatePartitionResponse;
import software.amazon.awssdk.services.glue.model.UpdateTableRequest;
import software.amazon.awssdk.services.glue.model.UpdateTableResponse;
import software.amazon.awssdk.utils.builder.Buildable;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.UnaryOperator.identity;

public class GlueGenerator
{
    private static final Escaper JAVA_ESCAPER = Escapers.builder()
                .addEscape('"', "\\\"")
                .addEscape('\\', "\\\\")
                .build();

    private record RpcType(String name, GlueRequest request, GlueResponse response)
    {
        public static RpcType create(GlueRequest request, GlueResponse response)
        {
            String requestName = request.getClass().getSimpleName();
            checkArgument(requestName.endsWith("Request"));
            String name = requestName.substring(0, requestName.length() - "Request".length());
            checkArgument(response.getClass().getSimpleName().endsWith(name + "Response"));
            return new RpcType(name, request, response);
        }
    }
    
    private static final List<RpcType> RPC_TYPES = List.of(
            RpcType.create(CreateDatabaseRequest.builder().build(), CreateDatabaseResponse.builder().build()),
            RpcType.create(GetDatabaseRequest.builder().build(), GetDatabaseResponse.builder().build()),
            RpcType.create(GetDatabasesRequest.builder().build(), GetDatabasesResponse.builder().build()),
            RpcType.create(UpdateDatabaseRequest.builder().build(), UpdateDatabaseResponse.builder().build()),
            RpcType.create(DeleteDatabaseRequest.builder().build(), DeleteDatabaseResponse.builder().build()),
            RpcType.create(CreateTableRequest.builder().build(), CreateTableResponse.builder().build()),
            RpcType.create(GetTableRequest.builder().build(), GetTableResponse.builder().build()),
            RpcType.create(GetTablesRequest.builder().build(), GetTablesResponse.builder().build()),
            RpcType.create(UpdateTableRequest.builder().build(), UpdateTableResponse.builder().build()),
            RpcType.create(DeleteTableRequest.builder().build(), DeleteTableResponse.builder().build()),
            RpcType.create(BatchCreatePartitionRequest.builder().build(), BatchCreatePartitionResponse.builder().build()),
            RpcType.create(GetPartitionRequest.builder().build(), GetPartitionResponse.builder().build()),
            RpcType.create(BatchGetPartitionRequest.builder().build(), BatchGetPartitionResponse.builder().build()),
            RpcType.create(GetPartitionsRequest.builder().build(), GetPartitionsResponse.builder().build()),
            RpcType.create(UpdatePartitionRequest.builder().build(), UpdatePartitionResponse.builder().build()),
            RpcType.create(DeletePartitionRequest.builder().build(), DeletePartitionResponse.builder().build()),
            RpcType.create(DeleteColumnStatisticsForPartitionRequest.builder().build(), DeleteColumnStatisticsForPartitionResponse.builder().build()),
            RpcType.create(DeleteColumnStatisticsForTableRequest.builder().build(), DeleteColumnStatisticsForTableResponse.builder().build()),
            RpcType.create(GetColumnStatisticsForPartitionRequest.builder().build(), GetColumnStatisticsForPartitionResponse.builder().build()),
            RpcType.create(GetColumnStatisticsForTableRequest.builder().build(), GetColumnStatisticsForTableResponse.builder().build()),
            RpcType.create(UpdateColumnStatisticsForPartitionRequest.builder().build(), UpdateColumnStatisticsForPartitionResponse.builder().build()),
            RpcType.create(UpdateColumnStatisticsForTableRequest.builder().build(), UpdateColumnStatisticsForTableResponse.builder().build()),
            RpcType.create(CreateUserDefinedFunctionRequest.builder().build(), CreateUserDefinedFunctionResponse.builder().build()),
            RpcType.create(GetUserDefinedFunctionRequest.builder().build(), GetUserDefinedFunctionResponse.builder().build()),
            RpcType.create(GetUserDefinedFunctionsRequest.builder().build(), GetUserDefinedFunctionsResponse.builder().build()),
            RpcType.create(DeleteUserDefinedFunctionRequest.builder().build(), DeleteUserDefinedFunctionResponse.builder().build()));

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Path cacheDir = Path.of("ignored/glue");

    public static void main(String[] args)
            throws Exception
    {
        checkState(Files.isDirectory(cacheDir), "cache directory does not exist");
        Map<Class<?>, SdkPojo> allTypes = new HashMap<>();
        for (RpcType rpcType : RPC_TYPES) {
            getDataTypes(allTypes, rpcType.request());
            getDataTypes(allTypes, rpcType.response());
        }

        System.out.println("""
                           //
                           // DATA TYPES
                           //
                           """);
        allTypes.values().stream()
                .sorted(Comparator.comparing(sdkPojo -> sdkPojo.getClass().getSimpleName()))
                .forEach(GlueGenerator::generateType);

        System.out.println("""
                           //
                           // RPC TYPES
                           //
                           """);
        RPC_TYPES.forEach(GlueGenerator::generateRpcTypes);
        RPC_TYPES.forEach(GlueGenerator::generateRpcMethod);
    }

    private static void generateType(SdkPojo type)
    {
        List<FieldProperties> properties = getFieldData(getDocHtml(type.getClass().getSimpleName())).stream()
                .peek(data -> checkState(data.kind() == FieldKind.FIELD, "unexpected field kind: %s".formatted(data)))
                .map(FieldProperties::create)
                .toList();
        generateRecord(type, properties);
    }

    private static void generateRpcTypes(RpcType rpcType)
    {
        RpcData rpcData = getRpcData(getDocHtml(rpcType.name()));

        generateRecord(rpcType.request(), rpcData.requestFields());
        generateRecord(rpcType.response(), rpcData.responseFields());
    }

    private static void generateRpcMethod(RpcType rpcType)
    {
        RpcData rpcData = getRpcData(getDocHtml(rpcType.name()));
        System.out.print("default %s %s(%s request)".formatted(
                rpcType.response().getClass().getSimpleName(),
                UPPER_CAMEL.to(LOWER_CAMEL, rpcType.name()),
                rpcType.request().getClass().getSimpleName()));
        if (!rpcData.exceptions().isEmpty()) {
            System.out.print("\n        throws %s".formatted(rpcData.exceptions().stream()
                    .map(exception -> exception.exceptionName())
                    .collect(Collectors.joining(", "))));
        }
        System.out.println();
        System.out.println("{");
        System.out.println("    throw new UnsupportedOperationException();");
        System.out.println("}");
        System.out.println();
    }

    private static void generateRecord(SdkPojo type, List<FieldProperties> properties)
    {
        Map<String, FieldProperties> propertiesByName = properties.stream()
                .collect(toImmutableMap(FieldProperties::fieldName, identity()));

        System.out.println("@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)");
        System.out.print("record %s(".formatted(type.getClass().getSimpleName()));

        Set<String> processedFields = new LinkedHashSet<>();
        boolean first = true;
        List<SdkField<?>> sdkFields = type.sdkFields();
        for (SdkField<?> sdkField : sdkFields) {
            if (!first) {
                System.out.print(",");
            }
            System.out.println();

            first = false;
            checkState(sdkField.location() == MarshallLocation.PAYLOAD, "location should be PAYLOAD");
            String memberName = sdkField.memberName();
            processedFields.add(memberName);
            FieldProperties fieldProperties = propertiesByName.get(memberName);
            if (fieldProperties == null) {
                System.out.println("    // TODO not documented");
                fieldProperties = FieldProperties.EMPTY;
            }
            checkState(sdkField.locationName().equals(memberName), "unmarshall location name should be the same as member name");
            checkState(sdkField.unmarshallLocationName().equals(memberName), "unmarshall location name should be the same as member name");
            checkState(!sdkField.ignoreDataTypeConversionFailures(), "ignoreDataTypeConversionFailures should be false");

            if (fieldProperties.required()) {
                System.out.println("    @NotNull ");
            }
            if (fieldProperties.pattern() != null) {
                System.out.println("    @Pattern(regexp = \"%s\") ".formatted(JAVA_ESCAPER.escape(fieldProperties.pattern())));
            }

            fieldProperties.lengthConstraint().ifPresent(lengthConstraint -> System.out.println("    " + toSizeAnnotation(lengthConstraint.max(), lengthConstraint.min())));
            fieldProperties.arrayItemConstraint().ifPresent(itemConstraint -> System.out.println("    " + toSizeAnnotation(itemConstraint.max(), itemConstraint.min())));
            fieldProperties.mapItemConstraint().ifPresent(itemConstraint -> System.out.println("    " + toSizeAnnotation(itemConstraint.max(), itemConstraint.min())));

            fieldProperties.rangeConstraint().ifPresent(rangeConstraint -> {
                // TODO min does not work with double
                if (rangeConstraint.min() != null && !rangeConstraint.min().equals("0.0")) {
                    System.out.println("    @Min(%s) ".formatted(rangeConstraint.min()));
                }
                if (rangeConstraint.max() != null) {
                    System.out.println("    @Max(%s) ".formatted(rangeConstraint.max()));
                }
            });

            System.out.print("    %s %s".formatted(getFieldType(sdkField, fieldProperties), UPPER_CAMEL.to(LOWER_CAMEL, memberName)));

            if (fieldProperties.validValues().isPresent()) {
                // TODO should we enforce this? It is not clear if this is enforces in Glue
            }
        }
        System.out.println(") {}");
        System.out.println();

        checkArgument(
                processedFields.containsAll(propertiesByName.keySet()),
                "missing fields: %s".formatted(propertiesByName.keySet().stream().filter(field -> !processedFields.contains(field)).toList()));
    }

    private static String toSizeAnnotation(Integer max, Integer min)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("@Size(");
        if (min != null && min != 0) {
            builder.append("min = ").append(min);
            if (max != null) {
                builder.append(", ");
            }
        }
        if (max != null) {
            builder.append("max = ").append(max);
        }
        builder.append(")");
        return builder.toString();
    }

    private enum FieldKind
    {
        REQUEST,
        RESPONSE,
        FIELD
    }

    private record FieldData(String name, String id, FieldKind kind, Map<String, String> properties) {}

    private static List<FieldData> getFieldData(String htmlFile)
    {
        org.jsoup.nodes.Document doc = Jsoup.parse(htmlFile, "UTF-8");

        ImmutableList.Builder<FieldData> fieldData = ImmutableList.builder();
        Elements dtElements = doc.select("dl > dt[id^=Glue-]");
        for (Element dt : dtElements) {
            String fieldName = dt.text().trim();
            Element dd = dt.nextElementSibling();
            if (dd != null && dd.tagName().equals("dd")) {
                List<String> propertiesList = new ArrayList<>();
                Elements pElements = dd.select("p");
                for (Element p : pElements) {
                    propertiesList.add(p.text().trim());
                }
                FieldKind fieldKind;
                if (dt.id().contains("-request-")) {
                    fieldKind = FieldKind.REQUEST;
                }
                else if (dt.id().contains("-response-")) {
                    fieldKind = FieldKind.RESPONSE;
                }
                else if (dt.id().startsWith("Glue-Type-")) {
                    fieldKind = FieldKind.FIELD;
                }
                else {
                    throw new IllegalArgumentException("Unknown field kind: %s".formatted(dt));
                }
                Map<String, String> properties = parseProperties(propertiesList);
                fieldData.add(new FieldData(fieldName, dt.id(), fieldKind, properties));
            }
        }
        return fieldData.build();
    }

    private static ImmutableList<ExceptionProperties> getExceptions(String htmlFile)
    {
        org.jsoup.nodes.Document doc = Jsoup.parse(htmlFile, "UTF-8");

        ImmutableList.Builder<ExceptionProperties> exceptions = ImmutableList.builder();
        Elements statusCodeElements = doc.select("dl > dt + dd p:matches(^HTTP Status Code)");
        for (Element exceptionElement : statusCodeElements) {
            String exceptionName = exceptionElement.parent().previousElementSibling().text();
            String statusCode = exceptionElement.text().substring("HTTP Status Code: ".length());
            exceptions.add(new ExceptionProperties(exceptionName, Integer.parseInt(statusCode)));
        }
        return exceptions.build();
    }

    private static Map<String, String> parseProperties(List<String> properties)
    {
        if (properties.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        // first property is a description and second property is the type
        String description = properties.getFirst();
        int keyValuePropertiesIndex = 1;
        while (keyValuePropertiesIndex < properties.size() &&
                !(properties.get(keyValuePropertiesIndex).startsWith("Type: ") || properties.get(keyValuePropertiesIndex).startsWith("HTTP Status Code: "))) {
            description += "\n" + properties.get(keyValuePropertiesIndex);
            keyValuePropertiesIndex++;
        }
        result.put("Description", description);
        for (String property : properties.subList(keyValuePropertiesIndex, properties.size())) {
            String[] parts = property.split(": ", 2);
            checkState(parts.length == 2, "Invalid property: '%s'".formatted(property));
            result.put(parts[0], parts[1]);
        }
        return result;
    }

    private static String getDocHtml(String entityName)
    {
        if (entityName.endsWith("Request")) {
            entityName = entityName.substring(0, entityName.length() - "Request".length());
        }
        else if (entityName.endsWith("Response")) {
            entityName = entityName.substring(0, entityName.length() - "Response".length());
        }
        try {
            if (Files.exists(cacheDir.resolve(entityName))) {
                return Files.readString(cacheDir.resolve(entityName));
            }

            System.out.println("Fetching: %s".formatted(entityName));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://docs.aws.amazon.com/glue/latest/webapi/API_%s.html".formatted(entityName)))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch the page: %s, status code: %d".formatted(request.uri(), response.statusCode()));
            }
            Files.writeString(cacheDir.resolve(entityName), response.body());
            return response.body();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void getDataTypes(Map<Class<?>, SdkPojo> allTypes, SdkPojo type)
    {
        for (SdkField<?> sdkField : type.sdkFields()) {
            getDataTypes(allTypes, sdkField);
        }
    }

    private static void getDataTypes(Map<Class<?>, SdkPojo> allTypes, SdkField<?> sdkField)
    {
        Class<?> targetClass = sdkField.marshallingType().getTargetClass();
        if (targetClass == SdkPojo.class) {
            SdkPojo sdkPojo = (SdkPojo) ((Buildable) sdkField.constructor().get()).build();
            if (allTypes.put(sdkPojo.getClass(), sdkPojo) == null) {
                getDataTypes(allTypes, sdkPojo);
            }
        }
        else if (targetClass == List.class) {
            ListTrait listTrait = sdkField.getRequiredTrait(ListTrait.class);
            getDataTypes(allTypes, listTrait.memberFieldInfo());
        }
        else if (targetClass == Map.class) {
            MapTrait mapTrait = sdkField.getRequiredTrait(MapTrait.class);
            getDataTypes(allTypes, mapTrait.valueFieldInfo());
        }
    }

    private static String getFieldType(SdkField<?> sdkField, FieldProperties fieldProperties)
    {
        Class<?> targetClass = sdkField.marshallingType().getTargetClass();
        if (targetClass == SdkPojo.class) {
            SdkPojo instance = (SdkPojo) ((Buildable) sdkField.constructor().get()).build();
            return instance.getClass().getSimpleName();
        }
        if (targetClass == List.class) {
            ListTrait listTrait = sdkField.getRequiredTrait(ListTrait.class);
            checkState(listTrait.memberLocationName() == null, "list member location name should be null");
            checkState(!listTrait.isFlattened(), "list should not be flattened");
            return "List<" + getFieldType(listTrait.memberFieldInfo(), null) + ">";
        }
        if (targetClass == Map.class) {
            MapTrait mapTrait = sdkField.getRequiredTrait(MapTrait.class);
            checkState(mapTrait.keyLocationName().equals("key"), "key location name should be 'key'");
            checkState(mapTrait.valueLocationName().equals("value"), "value location name should be 'value'");
            StringBuilder builder = new StringBuilder();
            builder.append("Map<");
            if (fieldProperties.keyPattern() != null) {
                builder.append("@Pattern(regexp = \"%s\") ".formatted(JAVA_ESCAPER.escape(fieldProperties.keyPattern())));
            }
            fieldProperties.keyLengthConstraint().ifPresent(lengthConstraint -> builder.append(toSizeAnnotation(lengthConstraint.max(), lengthConstraint.min())).append(" "));
            builder.append("String, ");

            fieldProperties.valueLengthConstraint().ifPresent(lengthConstraint -> builder.append(toSizeAnnotation(lengthConstraint.max(), lengthConstraint.min())).append(" "));
            builder.append(getFieldType(mapTrait.valueFieldInfo(), null));
            builder.append(">");
            return builder.toString();
        }
        if (targetClass == Void.class) {
            throw new IllegalArgumentException("Void is not supported");
        }
        if (targetClass == SdkBytes.class) {
            return "byte[]";
        }

        if ((targetClass == String.class ||
                targetClass == Short.class ||
                targetClass == Integer.class ||
                targetClass == Long.class ||
                targetClass == Float.class) ||
                targetClass == Double.class ||
                targetClass == BigDecimal.class ||
                targetClass == Boolean.class ||
                targetClass == Instant.class
        ) {
            return targetClass.getSimpleName();
        }

        if (targetClass == Document.class) {
            throw new IllegalArgumentException("Document is not supported");
        }
        throw new IllegalArgumentException("Unknown target class: " + targetClass);
    }

    private record FieldProperties(
            String fieldName,
            String type,
            boolean required,
            Optional<LengthConstraint> lengthConstraint,
            String pattern,
            Optional<RangeConstraint> rangeConstraint,
            Optional<List<String>> validValues,
            Optional<ItemConstraint> mapItemConstraint,
            Optional<LengthConstraint> keyLengthConstraint,
            String keyPattern,
            Optional<LengthConstraint> valueLengthConstraint,
            Optional<ItemConstraint> arrayItemConstraint)
    {
        public static final FieldProperties EMPTY = new FieldProperties(
                null,
                null,
                false,
                Optional.empty(),
                null,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                null,
                Optional.empty(),
                Optional.empty());

        public static FieldProperties create(FieldData fieldData)
        {
            return create(fieldData.name(), fieldData.properties());
        }

        public static FieldProperties create(String fieldName, Map<String, String> properties)
        {
            String type = properties.get("Type");
            boolean required = "yes".equalsIgnoreCase(properties.get("Required"));
            Optional<LengthConstraint>  lengthConstraint = Optional.ofNullable(properties.get("Length Constraints")).map(LengthConstraint::parse);
            String pattern = properties.get("Pattern");

            Optional<RangeConstraint>  rangeConstraint = Optional.ofNullable(properties.get("Valid Range")).map(RangeConstraint::parse);
            Optional<List<String>> validValues = Optional.ofNullable(properties.get("Valid Values"))
                    .map(line -> Splitter.on('|').omitEmptyStrings().trimResults().splitToList(line));

            Optional<ItemConstraint>  mapItemConstraint = Optional.ofNullable(properties.get("Map Entries")).map(ItemConstraint::parse);
            Optional<LengthConstraint>  keyLengthConstraint = Optional.ofNullable(properties.get("Key Length Constraints")).map(LengthConstraint::parse);
            String keyPattern = properties.get("Key Pattern");
            Optional<LengthConstraint>  valueLengthConstraint = Optional.ofNullable(properties.get("Value Length Constraints")).map(LengthConstraint::parse);

            Optional<ItemConstraint>  arrayItemConstraint = Optional.ofNullable(properties.get("Array Members")).map(ItemConstraint::parse);

            TreeSet<String> keys = new TreeSet<>(properties.keySet());
            keys.removeAll(List.of(
                    "Description",
                    "Type",
                    "Required",
                    "Length Constraints",
                    "Pattern",
                    "Valid Range",
                    "Valid Values",
                    "Key Length Constraints",
                    "Value Length Constraints",
                    "Key Pattern",
                    "Array Members",
                    "Map Entries"));
            if (!keys.isEmpty()) {
                throw new IllegalArgumentException("Unknown properties: " + keys);
            }

            return new FieldProperties(
                    fieldName,
                    type,
                    required,
                    lengthConstraint,
                    pattern,
                    rangeConstraint,
                    validValues,
                    mapItemConstraint,
                    keyLengthConstraint,
                    keyPattern,
                    valueLengthConstraint,
                    arrayItemConstraint);
        }
    }

    private record LengthConstraint(Integer min, Integer max)
    {
        private LengthConstraint
        {
            checkArgument(min != null || max != null, "min or max must be set");
        }

        private static final Pattern PATTERN = Pattern.compile("(?:Minimum length of (?<min>\\d+)(?:\\.|$))?(?:.*Maximum length of (?<max>\\d+)(?:\\.|$))?");

        public static LengthConstraint parse(String lengthConstraints)
        {
            if (lengthConstraints.startsWith("Fixed length of ")) {
                int length = Integer.parseInt(lengthConstraints.substring("Fixed length of ".length(), lengthConstraints.length() - 1));
                return new LengthConstraint(length, length);
            }

            Matcher matcher = PATTERN.matcher(lengthConstraints);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid length constraints: " + lengthConstraints);
            }
            Integer min = null;
            Integer max = null;
            if (matcher.group("min") != null) {
                min = Integer.parseInt(matcher.group("min"));
            }
            if (matcher.group("max") != null) {
                max = Integer.parseInt(matcher.group("max"));
            }
            return new LengthConstraint(min, max);
        }
    }

    private record RangeConstraint(String min, String max)
    {
        private RangeConstraint
        {
            checkArgument(min != null || max != null, "min or max must be set");
        }

        private static final Pattern PATTERN = Pattern.compile("(?:Minimum value of (?<min>\\d+(?:\\.\\d+)?)(?:\\.|$))?(?:.*Maximum value of (?<max>\\d+(?:\\.\\d+)?)(?:\\.|$))?");

        public static RangeConstraint parse(String rangeConstraints)
        {
            Matcher matcher = PATTERN.matcher(rangeConstraints);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid range constraints: " + rangeConstraints);
            }
            return new RangeConstraint(matcher.group("min"), matcher.group("max"));
        }
    }

    private record ItemConstraint(Integer min, Integer max)
    {
        private ItemConstraint
        {
            checkArgument(min != null || max != null, "min or max must be set");
        }

        // Minimum number of 0 items. Maximum number of 100 items.
        private static final Pattern PATTERN = Pattern.compile("(?:Minimum number of (?<min>\\d+) items?\\.)?(?:.*Maximum number of (?<max>\\d+) items?\\.)?");

        public static ItemConstraint parse(String lengthConstraints)
        {
            Matcher matcher = PATTERN.matcher(lengthConstraints);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid array item constraints: " + lengthConstraints);
            }
            Integer min = null;
            Integer max = null;
            if (matcher.group("min") != null) {
                min = Integer.parseInt(matcher.group("min"));
            }
            if (matcher.group("max") != null) {
                max = Integer.parseInt(matcher.group("max"));
            }
            return new ItemConstraint(min, max);
        }
    }

    private record ExceptionProperties(String exceptionName, int responseCode) {}

    private record RpcData(List<FieldProperties> requestFields, List<FieldProperties> responseFields, List<ExceptionProperties> exceptions) {}

    private static RpcData getRpcData(String htmlFile)
    {
        List<FieldData> fieldData = getFieldData(htmlFile);
        checkArgument(fieldData.stream().noneMatch(field -> field.kind() == FieldKind.FIELD), "unexpected field kind: %s".formatted(fieldData));
        List<FieldProperties> requestFields = fieldData.stream()
                .filter(field -> field.kind() == FieldKind.REQUEST)
                .map(FieldProperties::create)
                .toList();
        List<FieldProperties> responseFields = fieldData.stream()
                .filter(field -> field.kind() == FieldKind.RESPONSE)
                .map(FieldProperties::create)
                .toList();
        List<ExceptionProperties> exceptions = getExceptions(htmlFile);
        return new RpcData(requestFields, responseFields, exceptions);
    }
}
