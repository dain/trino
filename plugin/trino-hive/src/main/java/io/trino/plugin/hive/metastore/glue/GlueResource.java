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

import software.amazon.awssdk.services.glue.model.CreateDatabaseRequest;
import software.amazon.awssdk.services.glue.model.CreateDatabaseResponse;
import software.amazon.awssdk.services.glue.model.DeleteDatabaseRequest;
import software.amazon.awssdk.services.glue.model.DeleteDatabaseResponse;
import software.amazon.awssdk.services.glue.model.GetDatabaseRequest;
import software.amazon.awssdk.services.glue.model.GetDatabaseResponse;
import software.amazon.awssdk.services.glue.model.UpdateDatabaseRequest;
import software.amazon.awssdk.services.glue.model.UpdateDatabaseResponse;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class GlueResource
{
    private final Function<String, GlueFrontDoor> glueFrontDoorProvider;

    public GlueResource(Function<String, GlueFrontDoor> glueFrontDoorProvider)
    {
        this.glueFrontDoorProvider = requireNonNull(glueFrontDoorProvider, "glueFrontDoorProvider is null");
    }

    // TODO:
    //  - Many of hte names need to be lowered.  This is documented on the AWS Glue API documentation, but not implemented on the client side.
    //  - We likely need to do lots of validation :(
    //

    // Actions:
    //  createDatabase
    //  getDatabase
    //  getDatabases
    //  updateDatabase
    //  deleteDatabase
    //  createTable
    //  getTable
    //  getTables
    //  updateTable
    //  deleteTable
    //  getPartition
    //  batchGetPartition
    //  getPartitions
    //  updatePartition
    //  deletePartition
    //  batchCreatePartition
    //  createUserDefinedFunction
    //  getUserDefinedFunction
    //  getUserDefinedFunctions
    //  getUserDefinedFunctions
    //  deleteUserDefinedFunction
    //  updateUserDefinedFunction
    //  deleteColumnStatisticsForPartition
    //  deleteColumnStatisticsForTable
    //  getColumnStatisticsForPartition
    //  getColumnStatisticsForTable
    //  updateColumnStatisticsForPartition
    //  updateColumnStatisticsForTable

    // Summary of Actions by entity:
    //  Database:
    //    createDatabase
    //    getDatabase
    //    getDatabases
    //    updateDatabase
    //    deleteDatabase
    //  Table:
    //    createTable
    //    getTable
    //    getTables
    //    updateTable
    //    deleteTable
    //  Partition:
    //    batchCreatePartition
    //    getPartition
    //    batchGetPartition
    //    getPartitions
    //    updatePartition
    //    deletePartition
    //  ColumnStatistics:
    //    deleteColumnStatisticsForPartition
    //    deleteColumnStatisticsForTable
    //    getColumnStatisticsForPartition
    //    getColumnStatisticsForTable
    //    updateColumnStatisticsForPartition
    //    updateColumnStatisticsForTable
    //  UserDefinedFunction:
    //    createUserDefinedFunction
    //    getUserDefinedFunction
    //    getUserDefinedFunctions
    //    deleteUserDefinedFunction
    //    updateUserDefinedFunction

    public GetDatabaseResponse getDatabase(GetDatabaseRequest getDatabaseRequest)
    {
        String catalogId = getDatabaseRequest.catalogId();
        return GetDatabaseResponse.builder()
//                .database(GlueConverter.toGlueDatabase(glueFrontDoorProvider.apply(catalogId).getDatabase(getDatabaseRequest.name())))
                .build();
    }

    public CreateDatabaseResponse createDatabase(CreateDatabaseRequest createDatabaseRequest)
    {
        String catalogId = createDatabaseRequest.catalogId();
        glueFrontDoorProvider.apply(catalogId).createDatabase(createDatabaseRequest.databaseInput());
        return CreateDatabaseResponse.builder().build();
    }

    public UpdateDatabaseResponse updateDatabase(UpdateDatabaseRequest updateDatabaseRequest)
    {
        String catalogId = updateDatabaseRequest.catalogId();
        glueFrontDoorProvider.apply(catalogId).updateDatabase(updateDatabaseRequest.name(), updateDatabaseRequest.databaseInput());
        return UpdateDatabaseResponse.builder().build();
    }
    
    public DeleteDatabaseResponse deleteDatabase(DeleteDatabaseRequest deleteDatabaseRequest)
    {
        String catalogId = deleteDatabaseRequest.catalogId();
        glueFrontDoorProvider.apply(catalogId).deleteDatabase(deleteDatabaseRequest.name());
        return DeleteDatabaseResponse.builder().build();
    }
}
