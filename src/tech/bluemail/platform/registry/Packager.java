// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.registry;

import java.util.HashMap;

public class Packager
{
    private HashMap<String, Object> registry;
    private static Packager instance;
    
    public static Packager getInstance() {
        if (Packager.instance == null) {
            Packager.instance = new Packager();
        }
        return Packager.instance;
    }
    
    private Packager() {
        this.registry = new HashMap<String, Object>();
    }
    
    public HashMap<String, Object> getRegistry() {
        return this.registry;
    }
    
    public void setRegistry(final HashMap<String, Object> registry) {
        this.registry = registry;
    }
}
