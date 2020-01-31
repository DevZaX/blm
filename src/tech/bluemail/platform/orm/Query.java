// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.orm;

import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.exceptions.DatabaseException;
import org.apache.commons.lang.ArrayUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Query
{
    private String database;
    private String from;
    private String[] fields;
    private int offset;
    private int limit;
    private String[] order;
    private String direction;
    private String[] group;
    private String[] join;
    private String[] where;
    private Object[] whereParameters;
    private Object[] parameters;
    private String query;
    public static final int SELECT = 0;
    public static final int INSERT = 1;
    public static final int UPDATE = 2;
    public static final int DELETE = 3;
    public static final int ONLY_BUILD_QUERY = 0;
    public static final int EXECUTE_QUERY = 1;
    public static final String ASC = "ASC";
    public static final String DESC = "DESC";
    public static final String LEFT_JOIN = "LEFT JOIN";
    public static final String RIGHT_JOIN = "RIGHT JOIN";
    public static final String INNER_JOIN = "INNER JOIN";
    public static final String FULL_OUTER_JOIN = "FULL OUTER JOIN";
    
    public List<LinkedHashMap<String, Object>> all() {
        List<LinkedHashMap<String, Object>> results = new ArrayList<LinkedHashMap<String, Object>>();
        try {
            if (this.whereParameters != null && this.whereParameters.length > 0) {
                this.parameters = ArrayUtils.addAll(this.parameters, this.whereParameters);
            }
            if ("".equalsIgnoreCase(this.query)) {
                this.build(0);
            }
            results = Database.get(this.database).executeQuery(this.query, this.parameters, 1);
            this.reset();
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), Query.class);
        }
        return results;
    }
    
    public LinkedHashMap<String, Object> first() {
        LinkedHashMap<String, Object> row = new LinkedHashMap<String, Object>();
        try {
            if (this.whereParameters != null && this.whereParameters.length > 0) {
                this.parameters = ArrayUtils.addAll(this.parameters, this.whereParameters);
            }
            if ("".equalsIgnoreCase(this.query)) {
                this.build(0);
            }
            final List<LinkedHashMap<String, Object>> results = Database.get(this.database).executeQuery(this.query, this.parameters, 0);
            row = (results.isEmpty() ? row : results.get(0));
            this.reset();
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), Query.class);
        }
        return row;
    }
    
    public int count() {
        int count = 0;
        try {
            if (this.whereParameters != null && this.whereParameters.length > 0) {
                this.parameters = ArrayUtils.addAll(this.parameters, this.whereParameters);
            }
            if ("".equalsIgnoreCase(this.query)) {
                this.build(0);
            }
            count = Database.get(this.database).executeQuery(this.query, this.parameters, 1).size();
            this.reset();
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), Query.class);
        }
        return count;
    }
    
    public int insert(final Object[] parameters) {
        int result = 0;
        try {
            this.parameters = parameters;
            if ("".equalsIgnoreCase(this.query)) {
                this.build(1);
            }
            result = Database.get(this.database).executeUpdate(this.query, this.parameters, 1);
            this.reset();
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), Query.class);
        }
        return result;
    }
    
    public int update(final Object[] parameters) {
        int result = 0;
        try {
            this.parameters = parameters;
            if (this.whereParameters != null && this.whereParameters.length > 0) {
                this.parameters = ArrayUtils.addAll(this.parameters, this.whereParameters);
            }
            if ("".equalsIgnoreCase(this.query)) {
                this.build(2);
            }
            result = Database.get(this.database).executeUpdate(this.query, this.parameters, 0);
            this.reset();
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), Query.class);
        }
        return result;
    }
    
    public int delete() {
        int result = 0;
        try {
            if (this.whereParameters != null && this.whereParameters.length > 0) {
                this.parameters = this.whereParameters;
            }
            if ("".equalsIgnoreCase(this.query)) {
                this.build(3);
            }
            result = Database.get(this.database).executeUpdate(this.query, this.parameters, 0);
            this.reset();
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), Query.class);
        }
        return result;
    }
    
    public Query from(final String from, String[] fields) {
        this.from = from;
        if (fields == null || 0 == fields.length) {
            fields = new String[] { "*" };
        }
        this.fields = (String[])ArrayUtils.addAll((Object[])this.fields, (Object[])fields);
        return this;
    }
    
    public Query where(final String condition, final Object[] parameters, String concat) {
        concat = (("and".equalsIgnoreCase(concat) || "or".equalsIgnoreCase(concat) || "nand".equalsIgnoreCase(concat) || "nor".equalsIgnoreCase(concat)) ? (concat + " ") : "");
        this.where = (String[])ArrayUtils.add((Object[])this.where, (Object)(concat + condition));
        this.whereParameters = ArrayUtils.addAll(this.whereParameters, parameters);
        return this;
    }
    
    public Query order(final String[] columns, final String direction) {
        this.order = (String[])ArrayUtils.addAll((Object[])this.order, (Object[])columns);
        this.direction = direction;
        return this;
    }
    
    public Query limit(final int offset, final int limit) {
        this.offset = offset;
        this.limit = limit;
        return this;
    }
    
    public Query group(final String[] columns) {
        this.group = (String[])ArrayUtils.addAll((Object[])this.group, (Object[])columns);
        return this;
    }
    
    public Query join(final String join, final String on, String[] fields, String type) {
        type = ((type == null || "".equalsIgnoreCase(type)) ? "LEFT JOIN" : type);
        if (fields == null) {
            fields = new String[0];
        }
        if (0 == fields.length) {
            fields[0] = "*";
        }
        this.fields = (String[])ArrayUtils.addAll((Object[])this.fields, (Object[])fields);
        this.join = (String[])ArrayUtils.add((Object[])this.join, (Object)(type + " " + join + " ON " + on));
        return this;
    }
    
    public Query build(final int type) throws DatabaseException {
        switch (type) {
            case 0: {
                final String template = "SELECT %s FROM %s %s %s %s %s %s";
                String fields = "";
                String wheres = "";
                String orders = "";
                String limit = "";
                String joins = "";
                String groups = "";
                for (int i = 0; i < this.fields.length; ++i) {
                    fields += this.fields[i];
                    if (i != this.fields.length - 1) {
                        fields += ",";
                    }
                }
                if (this.join != null && this.join.length > 0) {
                    for (int i = 0; i < this.join.length; ++i) {
                        joins += this.join[i];
                        if (i != this.join.length - 1) {
                            joins = joins + this.join[i] + " ";
                        }
                    }
                }
                if (this.where != null && this.where.length > 0) {
                    wheres = "WHERE ";
                    for (int i = 0; i < this.where.length; ++i) {
                        wheres += this.where[i];
                        if (i != this.where.length - 1) {
                            wheres += " ";
                        }
                    }
                }
                if (this.group != null && this.group.length > 0) {
                    groups = "GROUP BY ";
                    for (int i = 0; i < this.group.length; ++i) {
                        groups += this.group[i];
                        if (i != this.group.length - 1) {
                            groups += ",";
                        }
                    }
                }
                if (this.order != null && this.order.length > 0) {
                    orders = "ORDER BY ";
                    for (int i = 0; i < this.order.length; ++i) {
                        orders += this.order[i];
                        if (i != this.order.length - 1) {
                            orders += ",";
                        }
                    }
                    orders = orders + " " + this.direction;
                }
                if (this.limit > 0) {
                    if (this.offset > 0) {
                        if ("mysql".equalsIgnoreCase(Database.get(this.database).getDriver())) {
                            limit = "LIMIT " + this.offset + "," + this.limit;
                        }
                        else {
                            limit = "OFFSET " + this.offset + " LIMIT " + this.limit;
                        }
                    }
                    else {
                        limit = "LIMIT " + this.limit;
                    }
                }
                this.query = String.format(template, fields, this.from, joins, wheres, groups, orders, limit);
                break;
            }
            case 1: {
                final String template = "INSERT INTO %s (%s) VALUES (%s)";
                String fields = "";
                String values = "";
                int[] removeIndexes = new int[0];
                for (int j = 0; j < this.fields.length; ++j) {
                    fields += this.fields[j];
                    if (j != this.fields.length - 1) {
                        fields += ",";
                    }
                }
                for (int j = 0; j < this.fields.length; ++j) {
                    if (this.parameters[j] == null) {
                        values += "NULL";
                        removeIndexes = ArrayUtils.add(removeIndexes, j);
                    }
                    else {
                        values += "?";
                    }
                    if (j != this.fields.length - 1) {
                        values += ",";
                    }
                }
                for (final int removeIndex : removeIndexes) {
                    this.parameters = ArrayUtils.remove(this.parameters, removeIndex);
                }
                this.query = String.format(template, this.from, fields, values);
                break;
            }
            case 2: {
                final String template = "UPDATE %s SET %s %s";
                String fields = "";
                String wheres = "";
                int[] removeIndexes = new int[0];
                for (int j = 0; j < this.fields.length; ++j) {
                    if (this.parameters[j] == null) {
                        fields = fields + this.fields[j] + " = NULL";
                        removeIndexes = ArrayUtils.add(removeIndexes, j);
                    }
                    else {
                        fields = fields + this.fields[j] + " = ?";
                    }
                    if (j != this.fields.length - 1) {
                        fields += ",";
                    }
                }
                if (this.where != null && this.where.length > 0) {
                    wheres = "WHERE ";
                    for (int j = 0; j < this.where.length; ++j) {
                        wheres += this.where[j];
                        if (j != this.where.length - 1) {
                            wheres += " ";
                        }
                    }
                }
                for (final int removeIndex : removeIndexes) {
                    this.parameters = ArrayUtils.remove(this.parameters, removeIndex);
                }
                this.query = String.format(template, this.from, fields, wheres);
                break;
            }
            case 3: {
                final String template = "DELETE FROM %s %s";
                String wheres2 = "";
                if (this.where != null && this.where.length > 0) {
                    wheres2 = "WHERE ";
                    for (int k = 0; k < this.where.length; ++k) {
                        wheres2 += this.where[k];
                        if (k != this.where.length - 1) {
                            wheres2 += " ";
                        }
                    }
                }
                this.query = String.format(template, this.from, wheres2);
                break;
            }
            default: {
                throw new DatabaseException("Unsupported query type !");
            }
        }
        return this;
    }
    
    private void reset() {
        this.from = "";
        this.fields = new String[0];
        this.offset = 0;
        this.limit = 0;
        this.order = new String[0];
        this.direction = "ASC";
        this.group = new String[0];
        this.join = new String[0];
        this.where = new String[0];
        this.parameters = new Object[0];
        this.query = "";
    }
    
    public Query(final String database) {
        this.database = Database.getDefault().getKey();
        this.from = "";
        this.fields = new String[0];
        this.offset = 0;
        this.limit = 0;
        this.order = new String[0];
        this.direction = "ASC";
        this.group = new String[0];
        this.join = new String[0];
        this.where = new String[0];
        this.whereParameters = new Object[0];
        this.parameters = new Object[0];
        this.query = "";
        this.database = database;
    }
    
    public String getDatabase() {
        return this.database;
    }
    
    public void setDatabase(final String database) {
        this.database = database;
    }
    
    public Object[] getWhereParameters() {
        return this.whereParameters;
    }
    
    public void setWhereParameters(final Object[] whereParameters) {
        this.whereParameters = whereParameters;
    }
    
    public String getFrom() {
        return this.from;
    }
    
    public void setFrom(final String from) {
        this.from = from;
    }
    
    public String[] getFields() {
        return this.fields;
    }
    
    public void setFields(final String[] fields) {
        this.fields = fields;
    }
    
    public int getOffset() {
        return this.offset;
    }
    
    public void setOffset(final int offset) {
        this.offset = offset;
    }
    
    public int getLimit() {
        return this.limit;
    }
    
    public void setLimit(final int limit) {
        this.limit = limit;
    }
    
    public String[] getOrder() {
        return this.order;
    }
    
    public void setOrder(final String[] order) {
        this.order = order;
    }
    
    public String getDirection() {
        return this.direction;
    }
    
    public void setDirection(final String direction) {
        this.direction = direction;
    }
    
    public String[] getGroup() {
        return this.group;
    }
    
    public void setGroup(final String[] group) {
        this.group = group;
    }
    
    public String[] getJoin() {
        return this.join;
    }
    
    public void setJoin(final String[] join) {
        this.join = join;
    }
    
    public String[] getWhere() {
        return this.where;
    }
    
    public void setWhere(final String[] where) {
        this.where = where;
    }
    
    public Object[] getParameters() {
        return this.parameters;
    }
    
    public void setParameters(final Object[] parameters) {
        this.parameters = parameters;
    }
    
    public String getQuery() {
        return this.query;
    }
    
    public void setQuery(final String query) {
        this.query = query;
    }
}
