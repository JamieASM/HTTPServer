import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class PropertiesParser {
    public static Properties loadProperties() {
        try {
            // parse the properties file
            // from https://www.baeldung.com/java-properties
            String rootPath = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("")).getPath();
            String root = "HTTPServer";

            // remove anything that is after 'CS2003-C3-ticket-chief'
            int idx = rootPath.indexOf(root);

            if (idx != -1) {
                // keep up to the end of the folder name
                rootPath = rootPath.substring(0, idx + root.length());
            }

            String appConfigPath = rootPath + "/cs2003-C3.properties";

            Properties prop = new Properties();
            prop.load(new FileInputStream(appConfigPath));

            return prop;
        } catch (IOException e) {
            System.out.println("Properties file not found");
        }

        return null;
    }
}
