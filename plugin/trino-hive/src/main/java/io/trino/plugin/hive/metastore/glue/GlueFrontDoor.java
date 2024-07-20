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

import io.trino.metastore.Database;
import io.trino.metastore.HiveMetastore;
import software.amazon.awssdk.services.glue.model.DatabaseInput;

import java.util.Optional;

public class GlueFrontDoor
{
    private final HiveMetastore metastore;

    public GlueFrontDoor(HiveMetastore metastore)
    {
        this.metastore = metastore;
    }

    public Optional<software.amazon.awssdk.services.glue.model.Database> getDatabase(String name)
    {
        return metastore.getDatabase(name).map(GlueConverter::toGlueDatabase);
    }

    public void createDatabase(DatabaseInput databaseInput)
    {
        Database database = GlueConverter.fromGlueDatabaseInput(databaseInput);
        metastore.createDatabase(database);
    }

    public void updateDatabase(String name, DatabaseInput databaseInput)
    {
        Database database = GlueConverter.fromGlueDatabaseInput(databaseInput);
        // todo
        // metastore.data(database);
    }

    public void deleteDatabase(String name)
    {
        // todo delete data?
        metastore.dropDatabase(name, false);
    }
}
