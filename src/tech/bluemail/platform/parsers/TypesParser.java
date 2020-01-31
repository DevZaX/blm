// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.parsers;

public class TypesParser
{
    public static int safeParseInt(final Object numericValue) {
        try {
            return Integer.parseInt(String.valueOf(numericValue));
        }
        catch (NumberFormatException ex) {
            return 0;
        }
    }
}
