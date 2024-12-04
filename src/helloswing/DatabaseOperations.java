package helloswing;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

public class DatabaseOperations {
    private final Connection connection;

    public DatabaseOperations(Connection connection) {
        this.connection = connection;
    }

    public DefaultTableModel executeSelectQuery(String query) throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
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

    public DefaultTableModel selectRecords(String table, String columns, Map<String, Object> conditions) throws SQLException {
        String query = QueryBuilder.buildSelectQuery(table, columns, conditions);
        return executeSelectQuery(query);
    }

    public List<String> getDatabases() throws SQLException {
        List<String> databases = new ArrayList<>();
        try (Statement stmt = this.connection.createStatement();
                ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
            while (rs.next()) {
                databases.add(rs.getString(1));
            }
        }
        return databases;
    }

    public List<String> getTables(String database) throws SQLException {
        List<String> tables = new ArrayList<>();
        String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setString(1, database);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
        }
        return tables;
    }

    public ResultSet getTableData(Connection connection, String database, String tableName) throws SQLException {
        String query = "SELECT * FROM " + database + "." + tableName;
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(query);
    }

    public void createDatabase(String name) throws SQLException {
        try (Statement stmt = this.connection.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE " + name);
        }
    }

    public void createTable(String name, String columns) throws SQLException {
        try (Statement stmt = this.connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE " + name + " (" + columns + ")");
        }
    }

    public void dropTable(String database, String table) throws SQLException {
        String query = "DROP TABLE " + database + "." + table;
        try (Statement stmt = this.connection.createStatement()) {
            stmt.executeUpdate(query);
        }
    }

    public void showTables(String database, StyledDocument doc) throws SQLException, BadLocationException {
        Statement stmt = connection.createStatement();
        stmt.execute("USE " + database);
        doc.insertString(doc.getLength(), "Show tables of " + database + "\n", null);
        ResultSet rs = stmt.executeQuery("SHOW TABLES");
        while (rs.next()) {
            String tableName = rs.getString(1);
            doc.insertString(doc.getLength(), "Table: " + tableName + "\n", null);
        }
    }

    public void closeDatabase(StyledDocument doc) throws SQLException, BadLocationException {
        Statement stmt = connection.createStatement();
        stmt.execute("USE mysql");
        doc.insertString(doc.getLength(), "Database closed\n", null);
    }

    public void insertRecord(String table, Map<String, Object> data) throws SQLException {
        String query = QueryBuilder.buildInsertQuery(table, data);
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            int paramIndex = 1;
            for (Object value : data.values()) {
                pstmt.setObject(paramIndex++, value);
            }
            pstmt.executeUpdate();
        }
    }

    public void updateRecord(String table, Map<String, Object> data, Map<String, Object> conditions) throws SQLException {
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

    public void deleteRecord(String table, String where) throws SQLException {
        String query = QueryBuilder.buildDeleteQuery(table, where);
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(query);
        }
    }
}