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
package io.prestosql.plugin.base.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.prestosql.plugin.base.security.TableAccessControlRule.TablePrivilege;
import io.prestosql.spi.connector.CatalogSchemaTableName;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class CatalogTableAccessControlRule
{
    public static final CatalogTableAccessControlRule ALLOW_ALL;

    static {
        ALLOW_ALL = new CatalogTableAccessControlRule(Optional.empty());
        ALLOW_ALL.setTableAccessControlRule(TableAccessControlRule.ALLOW_ALL);
    }

    private final Optional<Pattern> catalogRegex;
    private TableAccessControlRule tableAccessControlRule;

    @JsonCreator
    public CatalogTableAccessControlRule(
            @JsonProperty("catalog") Optional<Pattern> catalogRegex)
    {
        this.catalogRegex = requireNonNull(catalogRegex, "catalogRegex is null");
    }

    // Jackson does not support JsonUnwrapped constructor arguments
    @JsonUnwrapped
    private void setTableAccessControlRule(TableAccessControlRule tableAccessControlRule)
    {
        checkState(this.tableAccessControlRule == null, "tableAccessControlRule already set");
        requireNonNull(tableAccessControlRule, "tableAccessControlRule is null");
        this.tableAccessControlRule = tableAccessControlRule;
    }

    public Optional<Set<TablePrivilege>> match(String user, Set<String> groups, CatalogSchemaTableName table)
    {
        if (!catalogRegex.map(regex -> regex.matcher(table.getCatalogName()).matches()).orElse(true)) {
            return Optional.empty();
        }
        return tableAccessControlRule.match(user, groups, table.getSchemaTableName());
    }

    public Optional<AnyCatalogPermissionsRule> toAnyCatalogPermissionsRule()
    {
        if (tableAccessControlRule.getPrivileges().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AnyCatalogPermissionsRule(
                tableAccessControlRule.getUserRegex(),
                tableAccessControlRule.getGroupRegex(),
                catalogRegex));
    }

    public Optional<AnyCatalogSchemaPermissionsRule> toAnyCatalogSchemaPermissionsRule()
    {
        if (tableAccessControlRule.getPrivileges().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AnyCatalogSchemaPermissionsRule(
                tableAccessControlRule.getUserRegex(),
                tableAccessControlRule.getGroupRegex(),
                catalogRegex,
                tableAccessControlRule.getSchemaRegex()));
    }
}
