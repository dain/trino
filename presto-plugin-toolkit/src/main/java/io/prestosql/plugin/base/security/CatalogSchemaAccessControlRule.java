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
import io.prestosql.spi.connector.CatalogSchemaName;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class CatalogSchemaAccessControlRule
{
    public static final CatalogSchemaAccessControlRule ALLOW_ALL;

    static {
        ALLOW_ALL = new CatalogSchemaAccessControlRule(Optional.empty());
        ALLOW_ALL.setSchemaAccessControlRule(SchemaAccessControlRule.ALLOW_ALL);
    }

    private final Optional<Pattern> catalogRegex;
    private SchemaAccessControlRule schemaAccessControlRule;

    @JsonCreator
    public CatalogSchemaAccessControlRule(
            @JsonProperty("catalog") Optional<Pattern> catalogRegex)
    {
        this.catalogRegex = requireNonNull(catalogRegex, "catalogRegex is null");
    }

    // Jackson does not support JsonUnwrapped constructor arguments
    @JsonUnwrapped
    private void setSchemaAccessControlRule(SchemaAccessControlRule schemaAccessControlRule)
    {
        checkState(this.schemaAccessControlRule == null, "schemaAccessControlRule already set");
        requireNonNull(schemaAccessControlRule, "schemaAccessControlRule is null");
        this.schemaAccessControlRule = schemaAccessControlRule;
    }

    public Optional<Boolean> match(String user, Set<String> groups, CatalogSchemaName schema)
    {
        if (!catalogRegex.map(regex -> regex.matcher(schema.getCatalogName()).matches()).orElse(true)) {
            return Optional.empty();
        }
        return schemaAccessControlRule.match(user, groups, schema.getSchemaName());
    }
}
