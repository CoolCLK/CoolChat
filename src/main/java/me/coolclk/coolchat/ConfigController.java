package me.coolclk.coolchat;

import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

public class ConfigController {
    public static Object getValue(String key) {
        try {
            Map<String, Object> yaml = new Yaml().load(new FileReader(System.getProperty("user.dir") + "\\config\\coolchat.yml"));
            Object value = yaml;
            String[] keys = key.split("\\.");
            if (keys.length > 0) {
                for (String subkey : keys) {
                    value = ((Map) value).get(subkey);
                }
            }
            else {
                value = yaml.get(key);
            }
            return value;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
