// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.security;

import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;

public class Crypto
{
    public static String md5(final String str) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(str.getBytes());
        final byte[] digest = md.digest();
        return DatatypeConverter.printHexBinary(digest).toLowerCase();
    }
}
