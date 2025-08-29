package ServerManagement.dbmanage;

import ServerManagement.dto.RegisterRequest;
import ServerManagement.pwdhash.Bcrypt; // Assuming you have a Bcrypt class
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
    
    // You would also have other methods here, for example:
    // public boolean validateUser(Connection conn, String id, String password) { ... }
}