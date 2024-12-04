package helloswing;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;

public class TableOperations {
    private Connection connection;

    public TableOperations(Connection connection) {
        this.connection = connection;
    }

    public ResultSet executeQuery(Connection connection, String query) throws SQLException {
        validateConnection(connection);
        validateInput(query, "Query");        
        Statement stmt = connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY
        );
        return stmt.executeQuery(query);
    }
    
    public ResultSet getTableData(Connection connection, String database, String table) throws SQLException {
        validateConnection(connection);
        validateInput(database, "Database");
        validateInput(table, "Table");        
        String query = QueryBuilder.buildSelectQuery(table, "*", null);
        return executeQuery(connection, query);
    }
    
    public void insertRecord(Connection connection, String table, Map<String,Object> data) throws SQLException {
        validateConnection(connection);
        validateInput(table, "Table");
        validateInput(data, "Data");        
        String query = QueryBuilder.buildInsertQuery(table, data);        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            int paramIndex = 1;
            for (Object value : data.values()) {
                pstmt.setObject(paramIndex++, value);
            }
            pstmt.executeUpdate();
        }
    }
    
    public void updateRecord(Connection connection, String table, Map<String,Object> data, String whereClause) throws SQLException {
        validateConnection(connection);
        validateInput(table, "Table");
        validateInput(data, "Data");
        validateInput(whereClause, "Where clause");        
        String query = QueryBuilder.buildUpdateQuery(table, data, whereClause);        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            int paramIndex = 1;
            for (Object value : data.values()) {
                pstmt.setObject(paramIndex++, value);
            }
            pstmt.executeUpdate();
        }
    }
    
    public void deleteRecord(Connection connection, String table, String whereClause) throws SQLException {
        validateConnection(connection);
        validateInput(table, "Table");
        validateInput(whereClause, "Where clause");        
        String query = QueryBuilder.buildDeleteQuery(table, whereClause);        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.executeUpdate();
        }
    }
    
    private void validateConnection(Connection connection) throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Invalid or closed database connection");
        }
    }
    
    private void validateInput(Object input, String fieldName) throws SQLException {
        if (input == null || (input instanceof String && ((String)input).trim().isEmpty())) {
            throw new SQLException(fieldName + " cannot be null or empty");
        }
    }

    public DefaultTableModel createTableModel(ResultSet rs) throws SQLException {
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        String[] columnNames = new String[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            columnNames[i - 1] = metaData.getColumnName(i);
        }
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                row[i - 1] = rs.getObject(i);
            }
            model.addRow(row);
        }
        return model;
    }

    public void updateRecordWithPrompt(Connection connection, String database, String table, JTable tblDB, int selectedRow, Component parent) throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("DESCRIBE " + database + "." + table);
        Map<String, Object> data = new HashMap<>();
        StringBuilder whereClause = new StringBuilder();
        boolean firstWhere = true;
        while (rs.next()) {
            String columnName = rs.getString("Field");
            String columnType = rs.getString("Type");
            int columnIndex = tblDB.getColumnModel().getColumnIndex(columnName);
            String oldValue = tblDB.getValueAt(selectedRow, columnIndex).toString();
            String newValue = JOptionPane.showInputDialog(parent,
                "Update " + columnName + " (" + columnType + ")\nCurrent value: " + oldValue,
                oldValue);
            if (newValue != null && !newValue.equals(oldValue)) {
                data.put(columnName, newValue);
            }
            if (!firstWhere) {
                whereClause.append(" AND ");
            }
            whereClause.append(columnName).append("=");
            if (columnType.contains("char") || columnType.contains("text") || columnType.contains("date")) {
                whereClause.append("'").append(oldValue).append("'");
            } else {
                whereClause.append(oldValue);
            }
            firstWhere = false;
        }
        if (!data.isEmpty()) {
            String query = QueryBuilder.buildUpdateQuery(table, data, whereClause.toString());
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                int paramIndex = 1;
                for (Object value : data.values()) {
                    pstmt.setObject(paramIndex++, value);
                }
                pstmt.executeUpdate();
            }
        }
    }
    
    public void insertRecordWithPrompt(String database, String table, Component parent) throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("DESCRIBE " + database + "." + table);
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        boolean first = true;
        while (rs.next()) {
            String columnName = rs.getString("Field");
            String columnType = rs.getString("Type");
            String value = JOptionPane.showInputDialog(parent,
                "Enter value for " + columnName + " (" + columnType + "):");
            if (value != null) {
                if (!first) {
                    columns.append(", ");
                    values.append(", ");
                }
                columns.append(columnName);
                if (columnType.contains("char") || columnType.contains("text") || columnType.contains("date")) {
                    values.append("'").append(value).append("'");
                } else {
                    values.append(value);
                }
                first = false;
            }
        }
        String query = "INSERT INTO " + table + " (" + columns + ") VALUES (" + values + ")";
        stmt.executeUpdate(query);
    }

    public void createTable(String database, String tableName, String columns) throws SQLException {
        validateConnection(connection);
        validateInput(database, "Database");
        validateInput(tableName, "Table");
        validateInput(columns, "Columns");
        String query = QueryBuilder.buildCreateTableQuery(tableName, columns);        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(query);
        }
    }

    public void deleteRecordWithPrompt(String database, String table, JTable tblDB, int selectedRow, Component parent) throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("DESCRIBE " + database + "." + table);
        StringBuilder whereClause = new StringBuilder();
        boolean firstWhere = true;
        while (rs.next()) {
            String columnName = rs.getString("Field");
            String columnType = rs.getString("Type");
            int columnIndex = tblDB.getColumnModel().getColumnIndex(columnName);
            String value = tblDB.getValueAt(selectedRow, columnIndex).toString();
            if (!firstWhere) {
                whereClause.append(" AND ");
            }
            whereClause.append(columnName).append("=");
            if (columnType.contains("char") || columnType.contains("text") || columnType.contains("date")) {
                whereClause.append("'").append(value).append("'");
            } else {
                whereClause.append(value);
            }
            firstWhere = false;
        }
        int confirm = JOptionPane.showConfirmDialog(parent, "Are you sure you want to delete this record?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String query = "DELETE FROM " + table + " WHERE " + whereClause;
            stmt.executeUpdate(query);
        }
    }

    public void dropTable(String database, String table) throws SQLException {
        Statement stmt = connection.createStatement();
        String query = "DROP TABLE " + database + "." + table;
        stmt.executeUpdate(query);
    }
}