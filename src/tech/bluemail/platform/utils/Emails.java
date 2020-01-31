// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Emails
{
    public static boolean isValidEmail(final String email) {
        final String emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        final Pattern pattern = Pattern.compile(emailPattern);
        final Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}
