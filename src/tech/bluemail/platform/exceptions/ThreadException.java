// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.exceptions;

import tech.bluemail.platform.logging.Logger;

public class ThreadException implements Thread.UncaughtExceptionHandler
{
    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        Logger.error(e, t.getClass());
    }
}
