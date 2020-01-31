// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.utils;

public class Domains
{
    public static String getDomainName(final String url) {
        try {
            final String[] parts = url.split("\\.");
            if (parts.length > 2) {
                String domain = "";
                if (parts.length > 3) {
                    domain = parts[parts.length - 3] + ".";
                }
                return domain + parts[parts.length - 2] + "." + parts[parts.length - 1];
            }
        }
        catch (Exception e) {
            return url;
        }
        return url;
    }
}
