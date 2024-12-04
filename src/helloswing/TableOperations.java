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

    public ResultSet executeQuery(Connection connection, String query) throws SQLException {
        validateConnection(connection);
        validateInput(query, "Query");        
        Statement stmt = connection.createStatement(
            ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY
        );
        return stmt.executeQuery(query);
    }
        
    public void insertRecord(Connection connection, String table, Map<String, Object> data) throws SQLException {
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

    public void insertRecordWithPrompt(Connection connection, String database, String table, Component parent) throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("DESCRIBE " + database + "." + table);
        Map<String, Object> data = new HashMap<>();
        while (rs.next()) {
            String columnName = rs.getString("Field");
            String columnType = rs.getString("Type");
            String newValue = JOptionPane.showInputDialog(parent,
                "Insert value for " + columnName + " (" + columnType + ")");
            if (newValue != null && !newValue.trim().isEmpty()) {
                data.put(columnName, newValue);
            }
        }
        if (!data.isEmpty()) {
            String query = QueryBuilder.buildInsertQuery(table, data);
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                int paramIndex = 1;
                for (Object value : data.values()) {
                    pstmt.setObject(paramIndex++, value);
                }
                pstmt.executeUpdate();
            }
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
            whereClause.append("`").append(columnName).append("`=").append("'");
            if (columnType.contains("char") || columnType.contains("text") || columnType.contains("date")) {
                whereClause.append(value).append("'");
            } else {
                whereClause.append(value).append("'");
            }
            firstWhere = false;
        }
        int confirm = JOptionPane.showConfirmDialog(parent, "Are you sure you want to delete this record?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String query = "DELETE FROM " + table + " WHERE " + whereClause;
            stmt.executeUpdate(query);
        }
    }

    public void updateRecordWithPrompt(Connection connection, String database, String table, JTable tblDB, int selectedRow, Component parent) throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("DESCRIBE " + database + "." + table);
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> conditions = new HashMap<>();
        while (rs.next()) {
            String columnName = rs.getString("Field");
            String columnType = rs.getString("Type");
            int columnIndex = tblDB.getColumnModel().getColumnIndex(columnName);
            String oldValue = tblDB.getValueAt(selectedRow, columnIndex).toString();
            String newValue = JOptionPane.showInputDialog(parent,
                "Update value for " + columnName + " (" + columnType + ")", oldValue);
            if (newValue != null && !newValue.trim().isEmpty()) {
                data.put(columnName, newValue);
            }
            conditions.put(columnName, oldValue);
        }
        if (!data.isEmpty()) {
            String query = QueryBuilder.buildUpdateQuery(table, data, conditions);
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                int paramIndex = 1;
                for (Object value : data.values()) {
                    pstmt.setObject(paramIndex++, value);
                }
                for (Object value : conditions.values()) {
                    pstmt.setObject(paramIndex++, value);
                }
                pstmt.executeUpdate();
            }
        }
    }

    public void dropTable(String database, String table) throws SQLException {
        Statement stmt = connection.createStatement();
        String query = "DROP TABLE " + database + "." + table;
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

    public ResultSet getTableData(Connection connection, String database, String table) throws SQLException {
        validateConnection(connection);
        validateInput(database, "Database");
        validateInput(table, "Table");
        String query = QueryBuilder.buildSelectQuery(table, "*", null);
        return executeQuery(connection, query);
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
}