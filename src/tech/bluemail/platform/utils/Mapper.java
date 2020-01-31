// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.utils;

import java.io.InputStream;
import java.io.IOException;
import tech.bluemail.platform.logging.Logger;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.util.Properties;
import java.util.HashMap;
import java.util.TreeMap;

public class Mapper
{
    public static Object getMapValue(final TreeMap map, final String key, final Object defaultValue) {
        if (map != null && !map.isEmpty() && map.containsKey(key)) {
            final Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }
    
    public static Object getMapValue(final HashMap map, final String key, final Object defaultValue) {
        if (map != null && !map.isEmpty() && map.containsKey(key)) {
            final Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }
    
    public static HashMap<String, String> readProperties(final String filePath) {
        final HashMap<String, String> results = new HashMap<String, String>();
        final Properties properties = new Properties();
        try {
            final InputStream in = FileUtils.openInputStream(new File(filePath));
            properties.load(in);
            final Properties properties2 = null;
            final String[] value = {"a"};
            final HashMap<String, String> hashMap = null;
            properties.stringPropertyNames().forEach(key -> {
                value[0] = properties.getProperty(key);
                results.put(key, value[0]);
                return;
            });
        }
        catch (IOException e) {
            Logger.error(e, Mapper.class);
        }
        return results;
    }
}
