package helloswing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectSQL {
    public static Connection getConnection(String user, char[] password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/";
            String passwordStr = new String(password);
            return DriverManager.getConnection(url, user, passwordStr);
        } catch (ClassNotFoundException e) {
            // e.printStackTrace();
            return null;
        } catch (SQLException e) {
            // e.printStackTrace();
            return null;
        }
    }
}