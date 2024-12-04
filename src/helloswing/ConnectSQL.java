package helloswing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ConnectSQL {
    private static final Logger LOGGER = Logger.getLogger(ConnectSQL.class.getName());
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final int CONNECTION_TIMEOUT = 5000;
    
    private Connection connection;
    private static volatile ConnectSQL instance;
    private String host;
    private String port;
    private String username;
    private char[] password;
    
    public ConnectSQL() {
        this(DEFAULT_HOST, DEFAULT_PORT, null, null);
    }
    
    public ConnectSQL(String host, String port, String username, char[] password) {
        this.host = host != null ? host : DEFAULT_HOST;
        this.port = port != null ? port : DEFAULT_PORT;
        this.username = username;
        this.password = password != null ? password.clone() : null;
    }
    
    public static synchronized ConnectSQL getInstance() {
        if (instance == null) {
            instance = new ConnectSQL();
        }
        return instance;
    }

    public Connection getConnection(String user, char[] password) throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return connection;
        }

        try {
            Class.forName(DRIVER);
            String url = String.format("jdbc:mysql://%s:%s/?connectTimeout=%d", 
                host, port, CONNECTION_TIMEOUT);
            String passwordStr = new String(password);
            connection = DriverManager.getConnection(url, user, passwordStr);
            return connection;
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found", e);
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return getConnection(username, password);
    }
    
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
                if (password != null) {
                    java.util.Arrays.fill(password, '0');
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing connection", e);
        }
    }
    
    public void switchDatabase(String dbName) throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.setCatalog(dbName);
        }
    }
    
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void setHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        this.host = host;
    }

    public void setPort(String port) {
        if (port == null || !port.matches("\\d+")) {
            throw new IllegalArgumentException("Invalid port number");
        }
        this.port = port;
    }

    private boolean tryReconnect() throws SQLException {
        if (!isConnected()) {
            connection = getConnection(username, password);
            return true;
        }
        return false;
    }

    public boolean testConnection() {
        try (Connection testConn = getConnection()) {
            return testConn != null && !testConn.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Connection test failed", e);
            return false;
        }
    }

    public static void resetInstance() {
        instance = null;
    }
}