// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.orm;

import java.sql.SQLException;
import tech.bluemail.platform.logging.Logger;
import java.sql.ResultSetMetaData;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import tech.bluemail.platform.exceptions.DatabaseException;

public class Connector
{
    private DataSource dataSource;
    private String key;
    private String databaseName;
    private String host;
    private int port;
    private String username;
    private String password;
    private String driver;
    private String charset;
    private String engine;
    private String[] supportedDrivers;
    private String lastErrorMessage;
    private int lastInsertedId;
    private int affectedRowsCount;
    public static final int FETCH_FIRST = 0;
    public static final int FETCH_ALL = 1;
    public static final int FETCH_LAST = 3;
    public static final int AFFECTED_ROWS = 0;
    public static final int LAST_INSERTED_ID = 1;
    public static final int BEGIN_TRANSACTION = 0;
    public static final int COMMIT_TRANSACTION = 1;
    public static final int ROLLBACK_TRANSACTION = 2;
    
    public Connector() {
        this.driver = "mysql";
        this.charset = "utf8";
        this.engine = "InnoDB";
        this.supportedDrivers = new String[] { "mysql", "pgsql" };
        this.lastErrorMessage = "";
        this.lastInsertedId = 0;
        this.affectedRowsCount = 0;
    }
    
    public synchronized void iniDataSource() throws Exception {
        final String driver = this.getDriver();
        switch (driver) {
            case "mysql": {
                this.dataSource = new DataSource("com.mysql.jdbc.Driver", "jdbc:mysql://" + this.getHost() + ":" + this.getPort() + "/" + this.getName(), this.getUsername(), this.getPassword());
                break;
            }
            case "pgsql": {
                this.dataSource = new DataSource("org.postgresql.Driver", "jdbc:postgresql://" + this.getHost() + ":" + this.getPort() + "/" + this.getName(), this.getUsername(), this.getPassword());
                break;
            }
            default: {
                throw new DatabaseException("Database Not Supported !");
            }
        }
    }
    
    public synchronized List<LinkedHashMap<String, Object>> executeQuery(final String query, final Object[] data, final int returnType) throws DatabaseException {
        final List results = new ArrayList();
        try (final Connection connection = this.dataSource.getConnection();
             final PreparedStatement pr = connection.prepareStatement(query, 1005, 1008)) {
            if (data != null && data.length > 0) {
                int index = 1;
                int type = 0;
                for (final Object object : data) {
                    if (object != null) {
                        final String name = object.getClass().getName();
                        switch (name) {
                            case "java.lang.String": {
                                type = 12;
                                break;
                            }
                            case "java.lang.Double": {
                                type = 3;
                                break;
                            }
                            case "java.lang.Integer": {
                                type = 4;
                                break;
                            }
                            case "java.sql.Date": {
                                type = 91;
                                break;
                            }
                            case "java.sql.Timestamp": {
                                type = 93;
                                break;
                            }
                            case "java.lang.Boolean": {
                                type = 16;
                                break;
                            }
                        }
                        pr.setObject(index, object, type);
                        ++index;
                    }
                }
            }
            try (final ResultSet result = pr.executeQuery()) {
                if (result.isBeforeFirst()) {
                    final ResultSetMetaData meta = result.getMetaData();
                    int count = 0;
                    switch (returnType) {
                        case 1: {
                            while (result.next()) {
                                final LinkedHashMap<String, Object> row = new LinkedHashMap<String, Object>();
                                for (int i = 1; i <= meta.getColumnCount(); ++i) {
                                    row.put(meta.getColumnName(i), result.getObject(i));
                                }
                                results.add(row);
                                ++count;
                            }
                            break;
                        }
                        case 0: {
                            result.first();
                            final LinkedHashMap<String, Object> row = new LinkedHashMap<String, Object>();
                            for (int i = 1; i <= meta.getColumnCount(); ++i) {
                                row.put(meta.getColumnName(i), result.getObject(i));
                            }
                            results.add(row);
                            ++count;
                            break;
                        }
                    }
                    this.affectedRowsCount = count;
                }
            }
        }
        catch (Exception e) {
            this.lastErrorMessage = e.getMessage();
            throw new DatabaseException(e);
        }
        return (List<LinkedHashMap<String, Object>>)results;
    }
    
    public synchronized int executeUpdate(final String query, final Object[] data, final int returnType) throws DatabaseException {
        int result = 0;
        try (final Connection connection = this.dataSource.getConnection();
             final PreparedStatement pr = (returnType == 1) ? connection.prepareStatement(query, 1) : connection.prepareStatement(query)) {
            if (data != null && data.length > 0) {
                int index = 1;
                int type = 0;
                for (final Object object : data) {
                    if (object != null) {
                        final String name = object.getClass().getName();
                        switch (name) {
                            case "java.lang.String": {
                                type = 12;
                                break;
                            }
                            case "java.lang.Double": {
                                type = 3;
                                break;
                            }
                            case "java.lang.Integer": {
                                type = 4;
                                break;
                            }
                            case "java.sql.Date": {
                                type = 91;
                                break;
                            }
                            case "java.sql.Timestamp": {
                                type = 93;
                                break;
                            }
                            case "java.lang.Boolean": {
                                type = 16;
                                break;
                            }
                        }
                    }
                    pr.setObject(index, object, type);
                    ++index;
                }
            }
            result = pr.executeUpdate();
            if (returnType == 1) {
                final ResultSet rs = pr.getGeneratedKeys();
                if (rs.next()) {
                    this.lastInsertedId = rs.getInt(1);
                    result = this.lastInsertedId;
                }
                this.closeResultset(rs);
            }
            else {
                this.affectedRowsCount = result;
            }
        }
        catch (Exception e) {
            this.lastErrorMessage = e.getMessage();
            throw new DatabaseException(e);
        }
        return result;
    }
    
    public synchronized List<String> availableTables(final String schema) throws Exception {
        final List<String> tables = new ArrayList<String>();
        String sql = "";
        final String condition = (schema != null && !"".equals(schema)) ? ("WHERE schemaname = '" + schema + "'") : "";
        final String columns = (schema != null && !"".equals(schema)) ? "relname" : "schemaname || '.' || relname";
        if ("pgsql".equalsIgnoreCase(this.driver)) {
            sql = "SELECT " + columns + " AS name FROM pg_stat_user_tables " + condition + " ORDER BY name ASC";
        }
        else if ("mysql".equalsIgnoreCase(this.driver)) {
            sql = "SHOW tables FROM " + this.databaseName;
        }
        final List<LinkedHashMap<String, Object>> result = this.executeQuery(sql, null, 1);
        result.forEach(row -> tables.add(String.valueOf(row.get("name"))));
        return tables;
    }
    
    public synchronized void transaction(final int type) {
        this.lastErrorMessage = "";
        try {
            switch (type) {
                case 0: {
                    this.dataSource.getConnection().setSavepoint();
                    break;
                }
                case 1: {
                    this.dataSource.getConnection().commit();
                    break;
                }
                case 2: {
                    this.dataSource.getConnection().rollback();
                    break;
                }
                default: {
                    this.lastErrorMessage = "The passed transaction type is wrong!";
                    throw new DatabaseException(this.lastErrorMessage);
                }
            }
        }
        catch (Exception e) {
            this.lastErrorMessage = e.getMessage();
            Logger.error(e, Connector.class);
        }
    }
    
    public synchronized void closePreparedStatement(final PreparedStatement pr) {
        try {
            if (pr != null) {
                pr.close();
            }
        }
        catch (SQLException e) {
            this.lastErrorMessage = e.getMessage();
            Logger.error(e, Connector.class);
        }
    }
    
    public synchronized void closeResultset(final ResultSet result) {
        try {
            if (result != null) {
                result.close();
            }
        }
        catch (SQLException e) {
            this.lastErrorMessage = e.getMessage();
            Logger.error(e, Connector.class);
        }
    }
    
    public Query query() {
        return new Query(this.key);
    }
    
    public String getKey() {
        return this.key;
    }
    
    public void setKey(final String key) {
        this.key = key;
    }
    
    public String getName() {
        return this.databaseName;
    }
    
    public void setName(final String name) {
        this.databaseName = name;
    }
    
    public String getHost() {
        return this.host;
    }
    
    public void setHost(final String host) {
        this.host = host;
    }
    
    public int getPort() {
        return this.port;
    }
    
    public void setPort(final int port) {
        this.port = port;
    }
    
    public String getUsername() {
        return this.username;
    }
    
    public void setUsername(final String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return this.password;
    }
    
    public void setPassword(final String password) {
        this.password = password;
    }
    
    public String getDriver() {
        return this.driver;
    }
    
    public void setDriver(final String driver) {
        this.driver = driver;
    }
    
    public String getCharset() {
        return this.charset;
    }
    
    public void setCharset(final String charset) {
        this.charset = charset;
    }
    
    public String getEngine() {
        return this.engine;
    }
    
    public void setEngine(final String engine) {
        this.engine = engine;
    }
    
    public String[] getSupportedDrivers() {
        return this.supportedDrivers;
    }
    
    public void setSupportedDrivers(final String[] supportedDrivers) {
        this.supportedDrivers = supportedDrivers;
    }
    
    public String getLastErrorMessage() {
        return this.lastErrorMessage;
    }
    
    public void setLastErrorMessage(final String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }
    
    public int getLastInsertedId() {
        return this.lastInsertedId;
    }
    
    public void setLastInsertedId(final int lastInsertedId) {
        this.lastInsertedId = lastInsertedId;
    }
    
    public int getAffectedRowsCount() {
        return this.affectedRowsCount;
    }
    
    public void setAffectedRowsCount(final int affectedRowsCount) {
        this.affectedRowsCount = affectedRowsCount;
    }
    
    public DataSource getDataSource() {
        return this.dataSource;
    }
    
    public void setDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public String getDatabaseName() {
        return this.databaseName;
    }
    
    public void setDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
    }
}
