// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.utils;

import java.util.Base64;

public class Encoder
{
    public static String encodeToBase64(final String str) {
        return new String(Base64.getEncoder().encode(str.getBytes()));
    }
    
    public static String decodeFromBase64(final String str) {
        return new String(Base64.getDecoder().decode(str));
    }
}
