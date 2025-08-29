package ServerManagement.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Properties properties = new Properties();
    
    static {
        String configFilePath = "lib/config.properties";

        try (InputStream input = new FileInputStream(configFilePath)) {
            properties.load(input);
        } catch (Exception e) {
            e.printStackTrace();
            String message = "Could not load config.properties from the specified path. \n" +
                             "Please ensure the file exists at: \n" +
                             new java.io.File(configFilePath).getAbsolutePath();
            throw new RuntimeException(message, e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}