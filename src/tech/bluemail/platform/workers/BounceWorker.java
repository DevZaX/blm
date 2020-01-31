// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.workers;

import java.util.Iterator;
import java.util.List;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.controllers.BounceCleaner;
import java.util.LinkedHashMap;
import tech.bluemail.platform.orm.Database;
import tech.bluemail.platform.parsers.TypesParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import tech.bluemail.platform.orm.ActiveRecord;
import tech.bluemail.platform.models.admin.DataList;
import tech.bluemail.platform.models.lists.Clean;
import tech.bluemail.platform.models.lists.HardBounce;
import java.util.ArrayList;
import java.io.File;
import tech.bluemail.platform.models.admin.Server;

public class BounceWorker extends Thread
{
    public int proccessId;
    public String listName;
    public Server server;
    public int mailerId;
    public int ispId;
    
    public BounceWorker(final int proccessId, final String listName, final Server server, final int mailerId, final int ispId) {
        this.proccessId = proccessId;
        this.listName = listName;
        this.server = server;
        this.mailerId = mailerId;
        this.ispId = ispId;
    }
    
    @Override
    public void run() {
        try {
            final String logsFolder = new File(System.getProperty("base.path")).getAbsolutePath() + File.separator + "tmp" + File.separator + "pmta-logs" + File.separator + "server_" + this.server.id;
            if (!new File(logsFolder).exists()) {
                throw new Exception("Pmta Logs Folder Does Not Exists For Server : " + this.server.name);
            }
            final String schema = this.listName.split("\\.")[0];
            final String table = this.listName.split("\\.")[1];
            final String type = table.split("_")[0];
            final String flag = table.split("_")[1];
            final String tablePrefix = table.replaceAll(flag + "_", "").replaceAll(type + "_", "");
            final List<HardBounce> bounceEmails = new ArrayList<HardBounce>();
            final List<Clean> cleanEmails = new ArrayList<Clean>();
            final DataList list = (DataList)ActiveRecord.first(DataList.class, "name = ?", new Object[] { this.listName });
            if (list == null || list.id == 0) {
                throw new Exception("Data List : " + this.listName + " Does not Exists !");
            }
            final File[] logsDirs = new File(logsFolder).listFiles();
            File[] bounceFiles = new File[0];
            File[] deliveredFiles = new File[0];
            if (logsDirs != null && logsDirs.length > 0) {
                for (final File logsDir : logsDirs) {
                    if (logsDir.isDirectory()) {
                        bounceFiles = (File[])ArrayUtils.addAll((Object[])bounceFiles, (Object[])new File(logsDir.getAbsolutePath() + File.separator + "bounces").listFiles());
                        deliveredFiles = (File[])ArrayUtils.addAll((Object[])bounceFiles, (Object[])new File(logsDir.getAbsolutePath() + File.separator + "delivered").listFiles());
                    }
                }
            }
            List<String> lines = null;
            String[] lineParts = new String[0];
            int clientId = 0;
            int listId = 0;
            final List<String> checkBounce = new ArrayList<String>();
            if (bounceFiles != null && bounceFiles.length > 0) {
                for (final File bounceFile : bounceFiles) {
                    if (!bounceFile.isDirectory()) {
                        lines = (List<String>)FileUtils.readLines(bounceFile);
                        if (lines != null && !lines.isEmpty()) {
                            for (final String line : lines) {
                                if (!"".equals(line)) {
                                    lineParts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                                    if (lineParts.length != 12 || !"hardbnc".equalsIgnoreCase(lineParts[1]) || "".equalsIgnoreCase(lineParts[10])) {
                                        continue;
                                    }
                                    clientId = TypesParser.safeParseInt(lineParts[10].split("_")[2]);
                                    listId = TypesParser.safeParseInt(lineParts[10].split("_")[3]);
                                    final List<LinkedHashMap<String, Object>> result = Database.get("lists").executeQuery("SELECT * FROM " + this.listName + " WHERE id =" + clientId, null, 0);
                                    if (result.isEmpty()) {
                                        continue;
                                    }
                                    lineParts[5] = (String) result.get(0).get("email");
                                    if (listId != list.id || checkBounce.contains(lineParts[5])) {
                                        continue;
                                    }
                                    final HardBounce bounceEmail = new HardBounce();
                                    bounceEmail.id = clientId;
                                    bounceEmail.setSchema(schema);
                                    bounceEmail.setTable(table);
                                    bounceEmail.load();
                                    bounceEmail.setTable("hard_bounce");
                                    bounceEmails.add(bounceEmail);
                                    checkBounce.add(lineParts[5]);
                                }
                            }
                        }
                    }
                }
            }
            lines = null;
            lineParts = new String[0];
            clientId = 0;
            listId = 0;
            final List<String> checkClean = new ArrayList<String>();
            final String cleanTable = "clean_" + flag + "_" + tablePrefix;
            if (deliveredFiles != null && deliveredFiles.length > 0) {
                for (final File deliveredFile : deliveredFiles) {
                    if (!deliveredFile.isDirectory()) {
                        lines = (List<String>)FileUtils.readLines(deliveredFile);
                        if (lines != null && !lines.isEmpty()) {
                            for (final String line2 : lines) {
                                if (!"".equals(line2)) {
                                    lineParts = line2.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                                    if (lineParts.length != 12 || !"success".equalsIgnoreCase(lineParts[1]) || "".equalsIgnoreCase(lineParts[10])) {
                                        continue;
                                    }
                                    clientId = TypesParser.safeParseInt(lineParts[10].split("_")[2]);
                                    listId = TypesParser.safeParseInt(lineParts[10].split("_")[3]);
                                    final List<LinkedHashMap<String, Object>> result2 = Database.get("lists").executeQuery("SELECT * FROM " + this.listName + " WHERE id =" + clientId, null, 0);
                                    if (result2.isEmpty()) {
                                        continue;
                                    }
                                    lineParts[5] = (String) result2.get(0).get("email");
                                    if (listId != list.id || checkClean.contains(lineParts[5])) {
                                        continue;
                                    }
                                    final Clean cleanEmail = new Clean();
                                    cleanEmail.id = clientId;
                                    cleanEmail.setSchema(schema);
                                    cleanEmail.setTable(table);
                                    cleanEmail.load();
                                    cleanEmail.setTable(cleanTable);
                                    cleanEmails.add(cleanEmail);
                                    checkClean.add(lineParts[5]);
                                }
                            }
                        }
                    }
                }
            }
            BounceCleaner.updateCount(bounceEmails.size() + cleanEmails.size());
            if (!bounceEmails.isEmpty()) {
                HardBounce email = null;
                if (!bounceEmails.get(0).tableExists()) {
                    bounceEmails.get(0).sync();
                }
                for (final HardBounce bounce : bounceEmails) {
                    email = new HardBounce();
                    email.setSchema(bounce.getSchema());
                    email.setTable(bounce.getTable());
                    email.email = bounce.email;
                    Database.get("lists").executeUpdate("DELETE FROM " + this.listName + " WHERE id = ?", new Object[] { bounce.id }, 0);
                    email.insert();
                    BounceCleaner.updateProccess(this.proccessId, "bounce");
                }
            }
            if (!cleanEmails.isEmpty()) {
                Clean email2 = null;
                if (!cleanEmails.get(0).tableExists()) {
                    cleanEmails.get(0).sync();
                    final DataList listObject = new DataList();
                    listObject.name = cleanEmails.get(0).getSchema() + "." + cleanEmails.get(0).getTable();
                    listObject.ispId = list.ispId;
                    listObject.authorizedUsers = list.authorizedUsers;
                    listObject.flag = list.flag;
                    listObject.statusId = list.statusId;
                    listObject.createdAt = list.createdAt;
                    listObject.createdBy = list.createdBy;
                    listObject.lastUpdatedAt = list.lastUpdatedAt;
                    listObject.lastUpdatedBy = list.lastUpdatedBy;
                    listObject.insert();
                }
                for (final Clean clean : cleanEmails) {
                    email2 = new Clean();
                    email2.setSchema(clean.getSchema());
                    email2.setTable(clean.getTable());
                    email2.email = clean.email;
                    email2.fname = clean.email.split("\\@")[0];
                    email2.lname = clean.fname;
                    email2.offersExcluded = ((clean.offersExcluded == null) ? "" : clean.offersExcluded);
                    Database.get("lists").executeUpdate("DELETE FROM " + this.listName + " WHERE id = ?", new Object[] { clean.id }, 0);
                    email2.insert();
                    BounceCleaner.updateProccess(this.proccessId, "clean");
                }
            }
            final int count = Database.get("lists").query().from(this.listName, new String[] { "id" }).count();
            if (count == 0) {
                Database.get("lists").executeUpdate("DROP TABLE " + this.listName, new Object[0], 0);
                Database.get("lists").executeUpdate("DROP SEQUENCE " + schema + ".seq_id_" + table, new Object[0], 0);
                list.delete();
            }
        }
        catch (Exception e) {
            Logger.error(e, BounceWorker.class);
        }
    }
}
