package es.caib.concsv.commons.config;

import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Properties;

public class PropertyFileConfigUtil {

    public static Properties getProperties() {
        var properties = new Properties();
        var sources = ConfigProvider.getConfig().getConfigSources();
        for (var source : sources) {
            if ("PropertyFileConfigSource".equals(source.getName())) {
                for (var prop : source.getProperties().entrySet()) {
                    properties.put(prop.getKey(), prop.getValue());
                }
                break;
            }
        }
        return properties;
    }
}
