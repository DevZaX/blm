// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.models.lists;

import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.meta.annotations.Column;
import java.io.Serializable;
import tech.bluemail.platform.orm.ActiveRecord;

public class HardBounce extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "email", type = "text", nullable = false, length = 100)
    public String email;
    
    public HardBounce() throws DatabaseException {
        this.setDatabase("lists");
        this.setSchema("");
        this.setTable("hard_bounce");
    }
    
    public HardBounce(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("lists");
        this.setSchema("");
        this.setTable("hard_bounce");
        this.load();
    }
}
