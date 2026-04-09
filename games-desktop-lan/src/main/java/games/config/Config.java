package games.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Config {

    private static final Logger log = LogManager.getLogger(Config.class);

    private static final Properties PROPS = new Properties();

    static {
        String resourceName = "games/config.properties";

        try (InputStream in = Config.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                PROPS.load(in);
            } else {
                log.error("Config: resource {} not found, using defaults.", resourceName);
            }
        } catch (IOException e) {
            log.error("Config: failed to load {}, using defaults.", resourceName, e);
        }
    }

    private Config() {
    }

    private static String get(String key, String defaultValue) {
        String value = PROPS.getProperty(key);

        return (value != null) ? value : defaultValue;
    }

    public static String getNetworkHost() {
        return get("network.host", "localhost");
    }

    public static String getNetworkTransport() {
        String value = PROPS.getProperty("network.transport");

        if (value != null) {
            String trimmed = value.trim().toLowerCase();
            if ("tcp".equals(trimmed) || "rest".equals(trimmed)) {
                return trimmed;
            }
            log.error("Config: invalid network.transport \"{}\", using default tcp", value);
        }

        return "tcp";
    }

    public static int getTextPort() {
        String value = PROPS.getProperty("network.port.text");

        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.error("Config: invalid network.port.text \"{}\", using default 5000", value);
            }
        }

        // Fallback for older configs that only had network.port
        return getLegacyNetworkPortOrDefault(5000);
    }

    public static int getJsonPort() {
        String value = PROPS.getProperty("network.port.json");

        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.error("Config: invalid network.port.json \"{}\", using default 5001", value);
            }
        }

        // Fallback for older configs that only had network.port
        return getLegacyNetworkPortOrDefault(5001);
    }

    public static int getRestPort() {
        String value = PROPS.getProperty("network.rest.port");

        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.error("Config: invalid network.rest.port \"{}\", using default 5002", value);
            }
        }

        return 5002;
    }

    private static int getLegacyNetworkPortOrDefault(int defaultPort) {
        String value = PROPS.getProperty("network.port");

        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.error("Config: invalid legacy network.port \"{}\", using default {}", value, defaultPort);
            }
        }

        return defaultPort;
    }

    public static String getClientProtocol() {
        String value = PROPS.getProperty("client.protocol");

        if (value != null) {
            String trimmed = value.trim().toLowerCase();

            if ("text".equals(trimmed) || "json".equals(trimmed)) {
                return trimmed;
            }

            log.error("Config: invalid client.protocol \"{}\", using default text", value);
        }

        return "text";
    }

    public static int getClientPort() {
        String value = PROPS.getProperty("client.port");

        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.error("Config: invalid client.port \"{}\", falling back to protocol-specific default", value);
            }
        }

        String transport = getClientTransport();
        if ("rest".equals(transport)) {
            return getRestPort();
        }

        // Default per protocol (TCP)
        String protocol = getClientProtocol();
        return "json".equals(protocol) ? getJsonPort() : getTextPort();
    }

    public static String getClientTransport() {
        String value = PROPS.getProperty("client.transport");

        if (value != null) {
            String trimmed = value.trim().toLowerCase();
            if ("tcp".equals(trimmed) || "rest".equals(trimmed)) {
                return trimmed;
            }
            log.error("Config: invalid client.transport \"{}\", using default tcp", value);
        }

        return "tcp";
    }

    public static long getRobotDelayMillis() {
        String value = PROPS.getProperty("robot.delay.ms");

        if (value != null) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                log.error("Config: invalid robot.delay.ms \"{}\", using default 0", value);
            }
        }

        return 0L;
    }
}

