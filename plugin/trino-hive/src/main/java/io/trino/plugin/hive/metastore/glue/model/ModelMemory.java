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

import com.google.common.collect.ImmutableList;
import software.amazon.awssdk.services.glue.model.AlreadyExistsException;
import software.amazon.awssdk.services.glue.model.ConcurrentModificationException;
import software.amazon.awssdk.services.glue.model.EntityNotFoundException;
import software.amazon.awssdk.services.glue.model.FederatedResourceAlreadyExistsException;
import software.amazon.awssdk.services.glue.model.FederationSourceException;
import software.amazon.awssdk.services.glue.model.GlueEncryptionException;
import software.amazon.awssdk.services.glue.model.InternalServiceException;
import software.amazon.awssdk.services.glue.model.InvalidInputException;
import software.amazon.awssdk.services.glue.model.OperationTimeoutException;
import software.amazon.awssdk.services.glue.model.ResourceNumberLimitExceededException;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ModelMemory implements Model
{
    private final ConcurrentMap<String, Database> databases = new ConcurrentHashMap<>();
    private int maxDatabasesQuota;

    @Override
    public CreateDatabaseResponse createDatabase(CreateDatabaseRequest request)
            throws AlreadyExistsException, ConcurrentModificationException, FederatedResourceAlreadyExistsException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException, ResourceNumberLimitExceededException
    {
        maxDatabasesQuota = 1000;
        if (databases.size() >= maxDatabasesQuota) {
            throw ResourceNumberLimitExceededException.builder().message("Number of databases exceeds the limit: %s".formatted(maxDatabasesQuota)).build();
        }
        Database database = toDatabase(request.databaseInput());
        Database existing = databases.putIfAbsent(database.name(), database);
        if (existing != null) {
            throw AlreadyExistsException.builder().message("Database already exists").build();
        }
        return new CreateDatabaseResponse();
    }

    private static Database toDatabase(DatabaseInput databaseInput)
    {
        Database database = new Database(
                databaseInput.name(),
                databaseInput.locationUri(),
                databaseInput.description(),
                databaseInput.parameters(),
                Instant.now(),
                ImmutableList.of(),
                null,
                null,
                null);
        return database;
    }

    @Override
    public GetDatabaseResponse getDatabase(GetDatabaseRequest request)
            throws EntityNotFoundException, FederationSourceException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        Database database = databases.get(request.name());
        if (database == null) {
            throw EntityNotFoundException.builder().message("Database not found").build();
        }
        return new GetDatabaseResponse(database);
    }

    @Override
    public GetDatabasesResponse getDatabases(GetDatabasesRequest request)
            throws GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        return new GetDatabasesResponse(ImmutableList.copyOf(databases.values()), null);
    }

    @Override
    public UpdateDatabaseResponse updateDatabase(UpdateDatabaseRequest request)
            throws ConcurrentModificationException, EntityNotFoundException, GlueEncryptionException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        String databaseToUpdate = request.name();
        Database database = toDatabase(request.databaseInput());
        if (databaseToUpdate.equals(database.name())) {
            Database replaced = databases.replace(databaseToUpdate, database);
            if (replaced == null) {
                throw EntityNotFoundException.builder().message("Database not found").build();
            }
            return new UpdateDatabaseResponse();
        }

        // rename and update
        // todo verify this is even supported
        Database added = databases.putIfAbsent(database.name(), database);
        if (added != null) {
            throw ConcurrentModificationException.builder().message("Target database already exists").build();
        }
        Database removed = databases.remove(databaseToUpdate);
        if (removed == null) {
            databases.remove(database.name(), database);
            throw ConcurrentModificationException.builder().message("Database not found").build();
        }
        return new UpdateDatabaseResponse();
    }

    @Override
    public DeleteDatabaseResponse deleteDatabase(DeleteDatabaseRequest request)
            throws ConcurrentModificationException, EntityNotFoundException, InternalServiceException, InvalidInputException, OperationTimeoutException
    {
        Database removed = databases.remove(request.name());
        if (removed == null) {
            throw EntityNotFoundException.builder().message("Database not found").build();
        }
        return new DeleteDatabaseResponse();
    }
}
