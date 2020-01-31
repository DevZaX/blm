// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.utils;

import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.exceptions.SystemException;
import tech.bluemail.platform.meta.annotations.Column;
import java.lang.reflect.Field;
import org.apache.commons.lang.ArrayUtils;

public class Inspector
{
    public static String[] classFields(final Object object) {
        String[] fields = new String[0];
        if (object != null) {
            for (final Field field : object.getClass().getDeclaredFields()) {
                fields = (String[])ArrayUtils.add((Object[])fields, (Object)field.getName());
            }
        }
        return fields;
    }
    
    public static Column columnMeta(final Object object, final String columnName) {
        if (object != null) {
            try {
                final Field field = object.getClass().getDeclaredField(columnName);
                final Column[] annotations = field.getAnnotationsByType(Column.class);
                if (annotations.length > 0) {
                    return annotations[0];
                }
            }
            catch (Exception e) {
                Logger.error(new SystemException(e), Inspector.class);
            }
        }
        return null;
    }
}
