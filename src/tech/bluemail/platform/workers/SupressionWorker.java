// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.workers;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import tech.bluemail.platform.logging.Logger;
import java.util.LinkedHashMap;
import tech.bluemail.platform.orm.Database;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.util.HashSet;
import java.util.Collection;
import tech.bluemail.platform.controllers.SuppressionManager;
import java.util.Collections;
import java.util.ArrayList;
import tech.bluemail.platform.models.admin.DataList;

public class SupressionWorker extends Thread
{
    public int proccessId;
    public int offerId;
    public DataList dataList;
    public boolean isMd5;
    public String directory;
    public int listsSize;
    
    public SupressionWorker(final int proccessId, final int offerId, final DataList dataList, final boolean isMd5, final String directory, final int listsSize) {
        this.proccessId = proccessId;
        this.offerId = offerId;
        this.dataList = dataList;
        this.isMd5 = isMd5;
        this.directory = directory;
        this.listsSize = listsSize;
    }
    
    @Override
    public void run() {
        try {
            if (this.dataList != null && this.proccessId > 0 && this.offerId > 0) {
                final List<String> suppressionEmails = new ArrayList<String>();
                String[] columns = null;
                final String schema = this.dataList.name.split("\\.")[0];
                final String table = this.dataList.name.split("\\.")[1];
                if (table.startsWith("fresh_") || table.startsWith("clean_")) {
                    columns = new String[] { "id", "email", "fname", "lname", "offers_excluded" };
                }
                else if (table.startsWith("unsubscribers_")) {
                    columns = new String[] { "id", "email", "fname", "lname", "drop_id", "action_date", "message", "offers_excluded", "verticals", "agent", "ip", "country", "region", "city", "language", "device_type", "device_name", "os", "browser_name", "browser_version" };
                }
                else {
                    columns = new String[] { "id", "email", "fname", "lname", "action_date", "offers_excluded", "verticals", "agent", "ip", "country", "region", "city", "language", "device_type", "device_name", "os", "browser_name", "browser_version" };
                }
                final List<LinkedHashMap<String, Object>> totalEmails = this.getsuppressionEmails(suppressionEmails, columns);
                if (!suppressionEmails.isEmpty() && !totalEmails.isEmpty() && columns != null) {
                    Collections.sort(suppressionEmails);
                    suppressionEmails.retainAll(SuppressionManager.MD5_EMAILS);
                    final HashSet hashset = new HashSet();
                    hashset.addAll(suppressionEmails);
                    suppressionEmails.clear();
                    suppressionEmails.addAll(hashset);
                    final String csv = this.convertEmailsToCsv(totalEmails, suppressionEmails, columns);
                    if (!"".equalsIgnoreCase(csv)) {
                        FileUtils.writeStringToFile(new File(this.directory + File.separator + this.dataList.name + ".csv"), csv);
                        String exists = "false";
                        final List<LinkedHashMap<String, Object>> res = Database.get("lists").executeQuery("SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace WHERE  n.nspname = '" + schema + "' AND c.relname = '" + table + "_suppression_copy' AND c.relkind = 'r');", null, 1);
                        if (res != null && !res.isEmpty()) {
                            exists = String.valueOf(res.get(0).get("exists"));
                        }
                        if ("true".equals(exists)) {
                            Database.get("lists").executeUpdate("DROP TABLE " + this.dataList.name + "_suppression_copy", null, 0);
                        }
                        Runtime.getRuntime().exec(new String[] { "bash", "-c", "chmod a+rw " + this.directory + File.separator + this.dataList.name + ".csv" });
                        Database.get("lists").executeUpdate("CREATE TABLE " + this.dataList.name + "_suppression_copy ( like " + this.dataList.name + " including defaults including constraints including indexes )", null, 0);
                        Database.get("lists").executeUpdate("COPY " + this.dataList.name + "_suppression_copy FROM '" + this.directory + File.separator + this.dataList.name + ".csv' WITH CSV HEADER DELIMITER AS ',' NULL AS '';", null, 0);
                        final List<LinkedHashMap<String, Object>> result = Database.get("lists").executeQuery("SELECT (SELECT COUNT(id) AS count1 FROM " + this.dataList.name + ") - (SELECT COUNT(id) AS count2 FROM " + this.dataList.name + "_suppression_copy) AS difference", null, 0);
                        boolean identical = false;
                        if (result != null && !result.isEmpty() && result.get(0).containsKey("difference")) {
                            identical = "0".equalsIgnoreCase(String.valueOf(result.get(0).get("difference")));
                        }
                        if (identical) {
                            Database.get("lists").executeUpdate("DROP TABLE " + this.dataList.name, null, 0);
                            Database.get("lists").executeUpdate("ALTER TABLE " + this.dataList.name + "_suppression_copy RENAME TO " + this.dataList.name.split("\\.")[1], null, 0);
                        }
                        SuppressionManager.updateProccess(this.proccessId, this.listsSize, suppressionEmails.size());
                    }
                }
            }
        }
        catch (Exception e) {
            Logger.error(e, SupressionWorker.class);
        }
    }
    
    public List<LinkedHashMap<String, Object>> getsuppressionEmails(final List<String> suppressionEmails, final String[] columns) {
        List<LinkedHashMap<String, Object>> emails = null;
        try {
            emails = Database.get("lists").executeQuery("SELECT " + String.join(",", (CharSequence[])columns) + ",md5(email) as md5_email FROM " + this.dataList.name, null, 1);
            for (final LinkedHashMap<String, Object> row : emails) {
                if (row != null) {
                    suppressionEmails.add(String.valueOf(row.get("md5_email")).trim());
                }
            }
        }
        catch (Exception e) {
            Logger.error(e, SupressionWorker.class);
        }
        return emails;
    }
    
    public String convertEmailsToCsv(final List<LinkedHashMap<String, Object>> totalEmails, final List<String> suppressionEmails, final String[] columns) throws SQLException {
        final StringBuilder csv = new StringBuilder();
        boolean insertOfferId = false;
        List<String> offerIds = null;
        for (int i = 0; i < columns.length; ++i) {
            csv.append("\"").append(columns[i]);
            if (i < columns.length - 1) {
                csv.append("\"").append(",");
            }
            else {
                csv.append("\"");
            }
        }
        csv.append("\n");
        for (final LinkedHashMap<String, Object> row : totalEmails) {
            insertOfferId = false;
            if (suppressionEmails.contains(String.valueOf(row.get("md5_email")).trim())) {
                insertOfferId = true;
            }
            for (int j = 0; j < columns.length; ++j) {
                csv.append("\"");
                if ("offers_excluded".equalsIgnoreCase(columns[j])) {
                    if (row.get(columns[j]) == null || "null".equalsIgnoreCase(String.valueOf(row.get(columns[j]))) || "".equalsIgnoreCase(String.valueOf(row.get(columns[j])))) {
                        if (insertOfferId) {
                            csv.append(this.offerId);
                        }
                        else {
                            csv.append("");
                        }
                    }
                    else {
                        if (insertOfferId) {
                            offerIds = new ArrayList<String>(new HashSet<String>(Arrays.asList((this.offerId + "," + String.valueOf(row.get(columns[j]))).split(","))));
                        }
                        else {
                            offerIds = new ArrayList<String>(new HashSet<String>(Arrays.asList(String.valueOf(row.get(columns[j])).split(","))));
                        }
                        for (int k = 0; k < offerIds.size(); ++k) {
                            if (!"".equalsIgnoreCase(offerIds.get(k).trim())) {
                                csv.append(offerIds.get(k));
                                if (k < offerIds.size() - 1) {
                                    csv.append(",");
                                }
                            }
                        }
                    }
                }
                else if (row.get(columns[j]) == null || "null".equalsIgnoreCase(String.valueOf(row.get(columns[j])))) {
                    csv.append("");
                }
                else {
                    csv.append(String.valueOf(row.get(columns[j])).replaceAll("\"", "\\\""));
                }
                if (j < columns.length - 1) {
                    csv.append("\"").append(",");
                }
                else {
                    csv.append("\"");
                }
            }
            csv.append("\n");
        }
        return csv.toString();
    }
}
