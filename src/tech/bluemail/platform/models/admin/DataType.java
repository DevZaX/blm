// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.models.admin;

import tech.bluemail.platform.exceptions.DatabaseException;
import java.sql.Date;
import tech.bluemail.platform.meta.annotations.Column;
import java.io.Serializable;
import tech.bluemail.platform.orm.ActiveRecord;

public class DataType extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "status_id", type = "integer", nullable = false)
    public int statusId;
    @Column(name = "name", type = "text", nullable = false, length = 100)
    public String name;
    @Column(name = "created_by", type = "integer", nullable = false)
    public int createdBy;
    @Column(name = "last_updated_by", type = "integer", nullable = true)
    public int lastUpdatedBy;
    @Column(name = "created_at", type = "date", nullable = false)
    public Date createdAt;
    @Column(name = "last_updated_at", type = "date", nullable = true)
    public Date lastUpdatedAt;
    
    public DataType() throws DatabaseException {
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("data_types");
    }
    
    public DataType(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("data_types");
        this.load();
    }
}
