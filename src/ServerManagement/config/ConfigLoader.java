package ServerManagement.config;

// ConfigLoader.java (in your GUI project)
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Properties properties = new Properties();
    static {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(input);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not load config.properties");
        }
    }
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}