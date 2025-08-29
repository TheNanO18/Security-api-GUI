package ServerManagement.dbmanage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPass;

    public DatabaseManager(String url, String user, String password) {
        this.dbUrl  = url;
        this.dbUser = user;
        this.dbPass = password;
        
        // Ensure the JDBC driver is loaded
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("PostgreSQL JDBC Driver not found. Please add it to your project's classpath.", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(this.dbUrl, this.dbUser, this.dbPass);
    }
}