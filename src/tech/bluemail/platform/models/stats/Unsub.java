// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.models.stats;

import tech.bluemail.platform.exceptions.DatabaseException;
import java.sql.Timestamp;
import tech.bluemail.platform.meta.annotations.Column;
import java.io.Serializable;
import tech.bluemail.platform.orm.ActiveRecord;

public class Unsub extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "drop_id", type = "integer", nullable = false)
    public int dropId;
    @Column(name = "email", type = "text", nullable = false, length = 100)
    public String email;
    @Column(name = "type", type = "text", nullable = false, length = 20)
    public String type;
    @Column(name = "action_date", type = "timestamp", nullable = false)
    public Timestamp actionDate;
    @Column(name = "list", type = "text", nullable = false, length = 100)
    public String list;
    @Column(name = "message", type = "text", nullable = true)
    public String message;
    @Column(name = "ip", type = "text", nullable = true, length = 20)
    public String ip;
    @Column(name = "country", type = "text", nullable = true)
    public String country;
    @Column(name = "region", type = "text", nullable = true)
    public String region;
    @Column(name = "city", type = "text", nullable = true)
    public String city;
    @Column(name = "language", type = "text", nullable = true, length = 2)
    public String language;
    @Column(name = "device_type", type = "text", nullable = true)
    public String deviceType;
    @Column(name = "device_name", type = "text", nullable = true, length = 100)
    public String deviceName;
    @Column(name = "os", type = "text", nullable = true)
    public String os;
    @Column(name = "browser_name", type = "text", nullable = true)
    public String browserName;
    @Column(name = "browser_version", type = "text", nullable = true, length = 100)
    public String browserVersion;
    @Column(name = "action_occurences", type = "integer", nullable = true)
    public int actionOccurences;
    
    public Unsub() throws DatabaseException {
        this.setDatabase("master");
        this.setSchema("stats");
        this.setTable("unsubs");
    }
    
    public Unsub(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("stats");
        this.setTable("unsubs");
        this.load();
    }
}
