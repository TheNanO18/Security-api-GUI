package ServerManagement.dbmanage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ServerManagement.dto.RegisterRequest;
import ServerManagement.dto.User;
import ServerManagement.pwdhash.Bcrypt;

public class UserDAO {

    /**
     * Creates a new user in the en_user table.
     * @param conn The database connection.
     * @param userData The user data from the form.
     * @return true if the user was created successfully, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean createUser(Connection conn, RegisterRequest userData) throws SQLException {
        // ❗ SECURITY: Always hash passwords before storing them.
        String hashedPassword = Bcrypt.hashPassword(userData.getPassword());

        // Assumes your table is 'en_user' and has these columns.
        // Adjust the SQL query to match your actual table structure.
        String sql = "INSERT INTO en_user (id, password, ip, port, database) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userData.getId());
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, userData.getIp());
            pstmt.setString(4, userData.getPort());
            pstmt.setString(5, userData.getDatabase());

            int affectedRows = pstmt.executeUpdate();
            
            // executeUpdate() returns the number of rows affected. 
            // If it's greater than 0, the insert was successful.
            return affectedRows > 0;
        }
    }
    
    public List<User> getAllUsers(Connection conn) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, password, ip, port, database FROM en_user";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getString("id"));
                user.setPassword(rs.getString("password"));
                user.setIp(rs.getString("ip"));
                user.setPort(rs.getString("port"));
                user.setDatabase(rs.getString("database"));
                users.add(user);
            }
        }
        return users;
    }

    public boolean updateUser(Connection conn, User user) throws SQLException {
        // Note: This example does not update the password.
        // A separate "change password" feature is more secure.
        String sql = "UPDATE en_user SET ip = ?, port = ?, database = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getIp());
            pstmt.setString(2, user.getPort());
            pstmt.setString(3, user.getDatabase());
            pstmt.setString(4, user.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteUser(Connection conn, String userId) throws SQLException {
        String sql = "DELETE FROM en_user WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }
}