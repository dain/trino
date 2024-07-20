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

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import software.amazon.awssdk.services.glue.model.AlreadyExistsException;
import software.amazon.awssdk.services.glue.model.ConcurrentModificationException;
import software.amazon.awssdk.services.glue.model.EntityNotFoundException;
import software.amazon.awssdk.services.glue.model.FederatedResourceAlreadyExistsException;
import software.amazon.awssdk.services.glue.model.FederationSourceException;
import software.amazon.awssdk.services.glue.model.FederationSourceRetryableException;
import software.amazon.awssdk.services.glue.model.GlueEncryptionException;
import software.amazon.awssdk.services.glue.model.InternalServiceException;
import software.amazon.awssdk.services.glue.model.InvalidInputException;
import software.amazon.awssdk.services.glue.model.InvalidStateException;
import software.amazon.awssdk.services.glue.model.OperationTimeoutException;
import software.amazon.awssdk.services.glue.model.ResourceNotReadyException;
import software.amazon.awssdk.services.glue.model.ResourceNumberLimitExceededException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused"})
public interface Model
{
    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record BinaryColumnStatisticsData(
        @NotNull
        @Min(0)
        Long maximumLength,
        @NotNull
        Double averageLength,
        @NotNull
        @Min(0)
        Long numberOfNulls) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record BooleanColumnStatisticsData(
        @NotNull
        @Min(0)
        Long numberOfTrues,
        @NotNull
        @Min(0)
        Long numberOfFalses,
        @NotNull
        @Min(0)
        Long numberOfNulls) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record Column(
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String name,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(max = 131072)
        String type,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(max = 255)
        String comment,
        Map<@Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*") @Size(min = 1, max = 255) String, @Size(max = 512000) String> parameters) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record ColumnError(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String columnName,
        ErrorDetail error) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record ColumnStatistics(
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String columnName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(max = 20000)
        String columnType,
        @NotNull
        Instant analyzedTime,
        @NotNull
        ColumnStatisticsData statisticsData) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record ColumnStatisticsData(
        @NotNull
        String type,
        BooleanColumnStatisticsData booleanColumnStatisticsData,
        DateColumnStatisticsData dateColumnStatisticsData,
        DecimalColumnStatisticsData decimalColumnStatisticsData,
        DoubleColumnStatisticsData doubleColumnStatisticsData,
        LongColumnStatisticsData longColumnStatisticsData,
        StringColumnStatisticsData stringColumnStatisticsData,
        BinaryColumnStatisticsData binaryColumnStatisticsData) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record ColumnStatisticsError(
        ColumnStatistics columnStatistics,
        ErrorDetail error) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DataLakePrincipal(
        @Size(min = 1, max = 255)
        String dataLakePrincipalIdentifier) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record Database(
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String name,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\r\\n\\t]*")
        @Size(max = 2048)
        String description,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\r\\n\\t]*")
        @Size(min = 1, max = 1024)
        String locationUri,
        Map<@Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*") @Size(min = 1, max = 255) String, @Size(max = 512000) String> parameters,
        Instant createTime,
        List<PrincipalPermissions> createTableDefaultPermissions,
        DatabaseIdentifier targetDatabase,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        FederatedDatabase federatedDatabase) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DatabaseIdentifier(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String region) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DatabaseInput(
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String name,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\r\\n\\t]*")
        @Size(max = 2048)
        String description,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\r\\n\\t]*")
        @Size(min = 1, max = 1024)
        String locationUri,
        Map<@Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*") @Size(min = 1, max = 255) String, @Size(max = 512000) String> parameters,
        List<PrincipalPermissions> createTableDefaultPermissions,
        DatabaseIdentifier targetDatabase,
        FederatedDatabase federatedDatabase) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DateColumnStatisticsData(
        Instant minimumValue,
        Instant maximumValue,
        @NotNull
        @Min(0)
        Long numberOfNulls,
        @NotNull
        @Min(0)
        Long numberOfDistinctValues) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DecimalColumnStatisticsData(
        DecimalNumber minimumValue,
        DecimalNumber maximumValue,
        @NotNull
        @Min(0)
        Long numberOfNulls,
        @NotNull
        @Min(0)
        Long numberOfDistinctValues) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DecimalNumber(
        @NotNull
        byte[] unscaledValue,
        @NotNull
        Integer scale) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DoubleColumnStatisticsData(
        Double minimumValue,
        Double maximumValue,
        @NotNull
        @Min(0)
        Long numberOfNulls,
        @NotNull
        @Min(0)
        Long numberOfDistinctValues) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record ErrorDetail(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String errorCode,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\r\\n\\t]*")
        @Size(max = 2048)
        String errorMessage) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record FederatedDatabase(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 512)
        String identifier,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String connectionName) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record FederatedTable(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 512)
        String identifier,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 512)
        String databaseIdentifier,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String connectionName) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record IcebergInput(
        @NotNull
        String metadataOperation,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String version) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record LongColumnStatisticsData(
        Long minimumValue,
        Long maximumValue,
        @NotNull
        @Min(0)
        Long numberOfNulls,
        @NotNull
        @Min(0)
        Long numberOfDistinctValues) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record OpenTableFormatInput(
        IcebergInput icebergInput) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record Order(
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String column,
        @NotNull
        @Min(0)
        @Max(1)
        Integer sortOrder) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record Partition(
        @Size(max = 1024)
        List<String> values,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        Instant creationTime,
        Instant lastAccessTime,
        StorageDescriptor storageDescriptor,
        Map<@Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*") @Size(min = 1, max = 255) String, @Size(max = 512000) String> parameters,
        Instant lastAnalyzedTime,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record PartitionError(
        @Size(max = 1024)
        List<String> partitionValues,
        ErrorDetail errorDetail) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record PartitionIndex(
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        @Size(min = 1)
        List<String> keys,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String indexName) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record PartitionInput(
        @Size(max = 1024)
        List<String> values,
        Instant lastAccessTime,
        StorageDescriptor storageDescriptor,
        Map<@Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*") @Size(min = 1, max = 255) String, @Size(max = 512000) String> parameters,
        Instant lastAnalyzedTime) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record PartitionValueList(
        @NotNull
        @Size(max = 1024)
        List<String> values) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record PrincipalPermissions(
        DataLakePrincipal principal,
        List<String> permissions) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record ResourceUri(
        String resourceType,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\r\\n\\t]*")
        @Size(min = 1, max = 1024)
        String uri) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record SchemaId(
        @Pattern(regexp = "arn:(aws|aws-us-gov|aws-cn):glue:.*")
        @Size(min = 1, max = 10240)
        String schemaArn,
        @Pattern(regexp = "[a-zA-Z0-9-_$#.]+")
        @Size(min = 1, max = 255)
        String schemaName,
        @Pattern(regexp = "[a-zA-Z0-9-_$#.]+")
        @Size(min = 1, max = 255)
        String registryName) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record SchemaReference(
        SchemaId schemaId,
        @Pattern(regexp = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
        @Size(min = 36, max = 36)
        String schemaVersionId,
        @Min(1)
        @Max(100000)
        Long schemaVersionNumber) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record Segment(
        @NotNull
        @Min(0)
        Integer segmentNumber,
        @NotNull
        @Min(1)
        @Max(10)
        Integer totalSegments) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record SerDeInfo(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String name,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String serializationLibrary,
        Map<@Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*") @Size(min = 1, max = 255) String, @Size(max = 512000) String> parameters) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record SkewedInfo(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        List<String> skewedColumnNames,
        List<String> skewedColumnValues,
        Map<String, String> skewedColumnValueLocationMaps) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record StorageDescriptor(
        List<Column> columns,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\r\\n\\t]*")
        @Size(max = 2056)
        String location,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\r\\n\\t]*")
        @Size(max = 2056)
        List<String> additionalLocations,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(max = 128)
        String inputFormat,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(max = 128)
        String outputFormat,
        Boolean compressed,
        Integer numberOfBuckets,
        SerDeInfo serdeInfo,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        List<String> bucketColumns,
        List<Order> sortColumns,
        Map<@Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*") @Size(min = 1, max = 255) String, @Size(max = 512000) String> parameters,
        SkewedInfo skewedInfo,
        Boolean storedAsSubDirectories,
        SchemaReference schemaReference) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record StringColumnStatisticsData(
        @NotNull
        @Min(0)
        Long maximumLength,
        @NotNull
        Double averageLength,
        @NotNull
        @Min(0)
        Long numberOfNulls,
        @NotNull
        @Min(0)
        Long numberOfDistinctValues) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record Table(
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String name,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\r\\n\\t]*")
        @Size(max = 2048)
        String description,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String owner,
        Instant createTime,
        Instant updateTime,
        Instant lastAccessTime,
        Instant lastAnalyzedTime,
        @Min(0)
        Integer retention,
        StorageDescriptor storageDescriptor,
        List<Column> partitionKeys,
        @Size(max = 409600)
        String viewOriginalText,
        @Size(max = 409600)
        String viewExpandedText,
        @Size(max = 255)
        String tableType,
        Map<@Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*") @Size(min = 1, max = 255) String, @Size(max = 512000) String> parameters,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String createdBy,
        Boolean isRegisteredWithLakeFormation,
        TableIdentifier targetTable,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String versionId,
        FederatedTable federatedTable,
        ViewDefinition viewDefinition,
        Boolean isMultiDialectView) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record TableIdentifier(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String name,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String region) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record TableInput(
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String name,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\r\\n\\t]*")
        @Size(max = 2048)
        String description,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String owner,
        Instant lastAccessTime,
        Instant lastAnalyzedTime,
        @Min(0)
        Integer retention,
        StorageDescriptor storageDescriptor,
        List<Column> partitionKeys,
        @Size(max = 409600)
        String viewOriginalText,
        @Size(max = 409600)
        String viewExpandedText,
        @Size(max = 255)
        String tableType,
        Map<@Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*") @Size(min = 1, max = 255) String, @Size(max = 512000) String> parameters,
        TableIdentifier targetTable,
        ViewDefinitionInput viewDefinition) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record UserDefinedFunction(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String functionName,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String className,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String ownerName,
        String ownerType,
        Instant createTime,
        @Size(max = 1000)
        List<ResourceUri> resourceUris,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record UserDefinedFunctionInput(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String functionName,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String className,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String ownerName,
        String ownerType,
        @Size(max = 1000)
        List<ResourceUri> resourceUris) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record ViewDefinition(
        Boolean isProtected,
        @Size(min = 20, max = 2048)
        String definer,
        @Size(min = 20, max = 2048)
        @Size(max = 10)
        List<String> subObjects,
        @Size(min = 1, max = 1000)
        List<ViewRepresentation> representations) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record ViewDefinitionInput(
        Boolean isProtected,
        @Size(min = 20, max = 2048)
        String definer,
        @Size(min = 1, max = 10)
        List<ViewRepresentationInput> representations,
        @Size(min = 20, max = 2048)
        @Size(max = 10)
        List<String> subObjects) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record ViewRepresentation(
        String dialect,
        @Size(min = 1, max = 255)
        String dialectVersion,
        @Size(max = 409600)
        String viewOriginalText,
        @Size(max = 409600)
        String viewExpandedText,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String validationConnection,
        Boolean isStale) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record ViewRepresentationInput(
        String dialect,
        @Size(min = 1, max = 255)
        String dialectVersion,
        @Size(max = 409600)
        String viewOriginalText,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String validationConnection,
        @Size(max = 409600)
        String viewExpandedText) {}

    //
    // RPC TYPES
    //

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record CreateDatabaseRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        DatabaseInput databaseInput,
        @Size(max = 50)
        Map<@Size(min = 1, max = 128) String, @Size(max = 256) String> tags) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record CreateDatabaseResponse() {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetDatabaseRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String name) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetDatabaseResponse(
        Database database) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetDatabasesRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        String nextToken,
        @Min(1)
        @Max(100)
        Integer maxResults,
        String resourceShareType,
        // TODO not documented
        List<String> attributesToGet) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetDatabasesResponse(
        List<Database> databaseList,
        String nextToken) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record UpdateDatabaseRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String name,
        @NotNull
        DatabaseInput databaseInput) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record UpdateDatabaseResponse() {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DeleteDatabaseRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String name) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DeleteDatabaseResponse() {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record CreateTableRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        TableInput tableInput,
        @Size(max = 3)
        List<PartitionIndex> partitionIndexes,
        @Pattern(regexp = "[\\p{L}\\p{N}\\p{P}]*")
        @Size(min = 1, max = 255)
        String transactionId,
        OpenTableFormatInput openTableFormatInput) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record CreateTableResponse() {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetTableRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String name,
        @Pattern(regexp = "[\\p{L}\\p{N}\\p{P}]*")
        @Size(min = 1, max = 255)
        String transactionId,
        Instant queryAsOfTime) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetTableResponse(
        Table table) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetTablesRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(max = 2048)
        String expression,
        String nextToken,
        @Min(1)
        @Max(100)
        Integer maxResults,
        @Pattern(regexp = "[\\p{L}\\p{N}\\p{P}]*")
        @Size(min = 1, max = 255)
        String transactionId,
        Instant queryAsOfTime) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetTablesResponse(
        List<Table> tableList,
        String nextToken) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record UpdateTableRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        TableInput tableInput,
        Boolean skipArchive,
        @Pattern(regexp = "[\\p{L}\\p{N}\\p{P}]*")
        @Size(min = 1, max = 255)
        String transactionId,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String versionId,
        String viewUpdateAction,
        Boolean force) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record UpdateTableResponse() {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DeleteTableRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String name,
        @Pattern(regexp = "[\\p{L}\\p{N}\\p{P}]*")
        @Size(min = 1, max = 255)
        String transactionId) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DeleteTableResponse() {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record BatchCreatePartitionRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        @NotNull
        @Size(max = 100)
        List<PartitionInput> partitionInputList) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record BatchCreatePartitionResponse(
        List<PartitionError> errors) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetPartitionRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        @NotNull
        @Size(max = 1024)
        List<String> partitionValues) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetPartitionResponse(
        Partition partition) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record BatchGetPartitionRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        @NotNull
        @Size(max = 1000)
        List<PartitionValueList> partitionsToGet) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record BatchGetPartitionResponse(
        List<Partition> partitions,
        @Size(max = 1000)
        List<PartitionValueList> unprocessedKeys) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetPartitionsRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\r\\n\\t]*")
        @Size(max = 2048)
        String expression,
        String nextToken,
        Segment segment,
        @Min(1)
        @Max(1000)
        Integer maxResults,
        Boolean excludeColumnSchema,
        @Pattern(regexp = "[\\p{L}\\p{N}\\p{P}]*")
        @Size(min = 1, max = 255)
        String transactionId,
        Instant queryAsOfTime) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetPartitionsResponse(
        List<Partition> partitions,
        String nextToken) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record UpdatePartitionRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        @NotNull
        @Size(max = 1024)
        @Size(max = 100)
        List<String> partitionValueList,
        @NotNull
        PartitionInput partitionInput) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record UpdatePartitionResponse() {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DeletePartitionRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        @NotNull
        @Size(max = 1024)
        List<String> partitionValues) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DeletePartitionResponse() {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DeleteColumnStatisticsForPartitionRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        @NotNull
        @Size(max = 1024)
        List<String> partitionValues,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String columnName) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DeleteColumnStatisticsForPartitionResponse() {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DeleteColumnStatisticsForTableRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String columnName) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DeleteColumnStatisticsForTableResponse() {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetColumnStatisticsForPartitionRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        @NotNull
        @Size(max = 1024)
        List<String> partitionValues,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        @Size(max = 100)
        List<String> columnNames) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetColumnStatisticsForPartitionResponse(
        List<ColumnStatistics> columnStatisticsList,
        List<ColumnError> errors) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetColumnStatisticsForTableRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        @Size(max = 100)
        List<String> columnNames) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetColumnStatisticsForTableResponse(
        List<ColumnStatistics> columnStatisticsList,
        List<ColumnError> errors) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record UpdateColumnStatisticsForPartitionRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        @NotNull
        @Size(max = 1024)
        List<String> partitionValues,
        @NotNull
        @Size(max = 25)
        List<ColumnStatistics> columnStatisticsList) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record UpdateColumnStatisticsForPartitionResponse(
        List<ColumnStatisticsError> errors) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record UpdateColumnStatisticsForTableRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String tableName,
        @NotNull
        @Size(max = 25)
        List<ColumnStatistics> columnStatisticsList) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record UpdateColumnStatisticsForTableResponse(
        List<ColumnStatisticsError> errors) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record CreateUserDefinedFunctionRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        UserDefinedFunctionInput functionInput) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record CreateUserDefinedFunctionResponse() {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetUserDefinedFunctionRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String functionName) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetUserDefinedFunctionResponse(
        UserDefinedFunction userDefinedFunction) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetUserDefinedFunctionsRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String pattern,
        String nextToken,
        @Min(1)
        @Max(100)
        Integer maxResults) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record GetUserDefinedFunctionsResponse(
        List<UserDefinedFunction> userDefinedFunctions,
        String nextToken) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DeleteUserDefinedFunctionRequest(
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String catalogId,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String databaseName,
        @NotNull
        @Pattern(regexp = "[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\t]*")
        @Size(min = 1, max = 255)
        String functionName) {}

    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record DeleteUserDefinedFunctionResponse() {}

    default CreateDatabaseResponse createDatabase(CreateDatabaseRequest request)
            throws AlreadyExistsException, ConcurrentModificationException, FederatedResourceAlreadyExistsException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException, ResourceNumberLimitExceededException
    {
        throw new UnsupportedOperationException();
    }

    default GetDatabaseResponse getDatabase(GetDatabaseRequest request)
            throws EntityNotFoundException, FederationSourceException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default GetDatabasesResponse getDatabases(GetDatabasesRequest request)
            throws GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default UpdateDatabaseResponse updateDatabase(UpdateDatabaseRequest request)
            throws ConcurrentModificationException, EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default DeleteDatabaseResponse deleteDatabase(DeleteDatabaseRequest request)
            throws ConcurrentModificationException, EntityNotFoundException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default CreateTableResponse createTable(CreateTableRequest request)
            throws AlreadyExistsException, ConcurrentModificationException, EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException, ResourceNotReadyException, ResourceNumberLimitExceededException
    {
        throw new UnsupportedOperationException();
    }

    default GetTableResponse getTable(GetTableRequest request)
            throws EntityNotFoundException, FederationSourceException, FederationSourceRetryableException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException, ResourceNotReadyException
    {
        throw new UnsupportedOperationException();
    }

    default GetTablesResponse getTables(GetTablesRequest request)
            throws EntityNotFoundException, FederationSourceException, FederationSourceRetryableException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default UpdateTableResponse updateTable(UpdateTableRequest request)
            throws ConcurrentModificationException, EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException, ResourceNotReadyException, ResourceNumberLimitExceededException
    {
        throw new UnsupportedOperationException();
    }

    default DeleteTableResponse deleteTable(DeleteTableRequest request)
            throws ConcurrentModificationException, EntityNotFoundException, InternalServiceException, InvalidInputException, OperationTimeoutException, ResourceNotReadyException
    {
        throw new UnsupportedOperationException();
    }

    default BatchCreatePartitionResponse batchCreatePartition(BatchCreatePartitionRequest request)
            throws AlreadyExistsException, EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException, ResourceNumberLimitExceededException
    {
        throw new UnsupportedOperationException();
    }

    default GetPartitionResponse getPartition(GetPartitionRequest request)
            throws EntityNotFoundException, FederationSourceException, FederationSourceRetryableException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default BatchGetPartitionResponse batchGetPartition(BatchGetPartitionRequest request)
            throws EntityNotFoundException, FederationSourceException, FederationSourceRetryableException, GlueEncryptionException, InternalServiceException, InvalidInputException, InvalidStateException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default GetPartitionsResponse getPartitions(GetPartitionsRequest request)
            throws EntityNotFoundException, FederationSourceException, FederationSourceRetryableException, GlueEncryptionException, InternalServiceException, InvalidInputException, InvalidStateException, OperationTimeoutException, ResourceNotReadyException
    {
        throw new UnsupportedOperationException();
    }

    default UpdatePartitionResponse updatePartition(UpdatePartitionRequest request)
            throws EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default DeletePartitionResponse deletePartition(DeletePartitionRequest request)
            throws EntityNotFoundException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default DeleteColumnStatisticsForPartitionResponse deleteColumnStatisticsForPartition(DeleteColumnStatisticsForPartitionRequest request)
            throws EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default DeleteColumnStatisticsForTableResponse deleteColumnStatisticsForTable(DeleteColumnStatisticsForTableRequest request)
            throws EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default GetColumnStatisticsForPartitionResponse getColumnStatisticsForPartition(GetColumnStatisticsForPartitionRequest request)
            throws EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default GetColumnStatisticsForTableResponse getColumnStatisticsForTable(GetColumnStatisticsForTableRequest request)
            throws EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default UpdateColumnStatisticsForPartitionResponse updateColumnStatisticsForPartition(UpdateColumnStatisticsForPartitionRequest request)
            throws EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default UpdateColumnStatisticsForTableResponse updateColumnStatisticsForTable(UpdateColumnStatisticsForTableRequest request)
            throws EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default CreateUserDefinedFunctionResponse createUserDefinedFunction(CreateUserDefinedFunctionRequest request)
            throws AlreadyExistsException, EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException, ResourceNumberLimitExceededException
    {
        throw new UnsupportedOperationException();
    }

    default GetUserDefinedFunctionResponse getUserDefinedFunction(GetUserDefinedFunctionRequest request)
            throws EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default GetUserDefinedFunctionsResponse getUserDefinedFunctions(GetUserDefinedFunctionsRequest request)
            throws EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }

    default DeleteUserDefinedFunctionResponse deleteUserDefinedFunction(DeleteUserDefinedFunctionRequest request)
            throws EntityNotFoundException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        throw new UnsupportedOperationException();
    }
}
