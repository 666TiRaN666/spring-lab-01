package kz.iitu.springlab.bank;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;
import java.util.Set;

/**
 * Tables created by Flyway migrations. Hibernate create/create-drop (dev profile of lab 8) must not
 * create or drop them; validate still checks them against the entities.
 */
public class FlywayOwnedTables implements SchemaFilterProvider {
    private static final Set<String> OWNED = Set.of("accounts", "audit_log", "flyway_schema_history");

    private static final SchemaFilter SKIP_OWNED = new SchemaFilter() {
        @Override public boolean includeNamespace(Namespace namespace) { return true; }
        @Override public boolean includeTable(Table table) { return !OWNED.contains(table.getName().toLowerCase()); }
        @Override public boolean includeSequence(Sequence sequence) { return true; }
    };
    private static final SchemaFilter ALL = new SchemaFilter() {
        @Override public boolean includeNamespace(Namespace namespace) { return true; }
        @Override public boolean includeTable(Table table) { return true; }
        @Override public boolean includeSequence(Sequence sequence) { return true; }
    };

    @Override public SchemaFilter getCreateFilter() { return SKIP_OWNED; }
    @Override public SchemaFilter getDropFilter() { return SKIP_OWNED; }
    @Override public SchemaFilter getTruncatorFilter() { return SKIP_OWNED; }
    @Override public SchemaFilter getMigrateFilter() { return SKIP_OWNED; }
    @Override public SchemaFilter getValidateFilter() { return ALL; }
}
