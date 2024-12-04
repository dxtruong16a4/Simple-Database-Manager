package helloswing;

import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class QueryBuilder {

    private static String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }
    
    public static String escapeTableName(String table) {
        return "`" + table.replace("`", "``") + "`";
    }

    public static String escapeColumnName(String column) {
        return "`" + column.replace("`", "``") + "`";
    }
    
    public static String buildInsertQuery(String table, Map<String, Object> data) {
        if (table == null || data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Table name and data cannot be null or empty");
        }
        StringJoiner columns = new StringJoiner(", ", "(", ")");
        StringJoiner values = new StringJoiner(", ", "(", ")");
        data.forEach((key, value) -> {
            columns.add(escapeColumnName(key));
            values.add("?");
        });
        String query = String.format("INSERT INTO %s %s VALUES %s", escapeTableName(table), columns.toString(), values.toString());
        return query;
    }
    
    public static String buildUpdateQuery(String table, Map<String,Object> data, Map<String, Object> conditions) {
        if (table == null || data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Table name and data cannot be null or empty");
        }
        String setClause = data.keySet().stream().map(key -> String.format("%s = ?", escapeColumnName(key))).collect(Collectors.joining(", "));
        StringBuilder query = new StringBuilder().append("UPDATE ").append(escapeTableName(table)).append(" SET ").append(setClause);
        if (conditions != null && !conditions.isEmpty()) {
            String whereClause = conditions.keySet().stream().map(key -> String.format("%s = ?", escapeColumnName(key))).collect(Collectors.joining(" AND "));
            query.append(" WHERE ").append(whereClause);
        }
        return query.toString();
    }

    public static String buildSelectQuery(String table, String columns, Map<String, Object> conditions) {
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
        StringBuilder query = new StringBuilder("SELECT ");
        String[] columnArray = columns.split(",");
        String escapedColumns = Arrays.stream(columnArray).map(String::trim).map(QueryBuilder::escapeColumnName).collect(Collectors.joining(", "));        
        query.append(escapedColumns).append(" FROM ").append(escapeTableName(table));        
        if (conditions != null && !conditions.isEmpty()) {
            String whereClause = conditions.entrySet().stream().map(e -> String.format("%s = %s", escapeColumnName(e.getKey()), formatValue(e.getValue()))).collect(Collectors.joining(" AND "));
            query.append(" WHERE ").append(whereClause);
        }
        return query.toString();
    }

    public static String buildDeleteQuery(String table, String where) {
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
        StringBuilder query = new StringBuilder().append("DELETE FROM ").append(escapeTableName(table));
        if (where != null && !where.trim().isEmpty()) {
            query.append(" WHERE ").append(where);
        }
        return query.toString();
    }

    public static String buildCreateTableQuery(String tableName, String columns) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
        if (columns == null || columns.trim().isEmpty()) {
            throw new IllegalArgumentException("Columns cannot be null or empty");
        }
        return String.format("CREATE TABLE %s (%s)", escapeTableName(tableName), columns);
    }

    public static String buildDropTableQuery(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
        return String.format("DROP TABLE %s", escapeTableName(tableName));
    }
}