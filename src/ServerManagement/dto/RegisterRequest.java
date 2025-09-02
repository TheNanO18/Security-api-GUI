package ServerManagement.dto;

public class RegisterRequest {
    private String id;
    private String password;
    private String ip;
    private String port;
    private String database;
    private String refreshToken;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    
    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }
    
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    
    public String getRefreshToken() { return refreshToken; }	
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}