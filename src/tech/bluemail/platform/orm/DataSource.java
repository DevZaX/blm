// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.orm;

import java.sql.Connection;
import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.io.IOException;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DataSource
{
    private ComboPooledDataSource cpds;
    
    public DataSource(final String driver, final String url, final String username, final String password) throws IOException, SQLException, PropertyVetoException {
        (this.cpds = new ComboPooledDataSource()).setDriverClass(driver);
        this.cpds.setJdbcUrl(url);
        this.cpds.setUser(username);
        this.cpds.setPassword(password);
        this.cpds.setMinPoolSize(1);
        this.cpds.setAcquireIncrement(1);
        this.cpds.setMaxPoolSize(300);
        this.cpds.setMaxStatements(100);
    }
    
    public Connection getConnection() throws SQLException {
        return this.cpds.getConnection();
    }
}
