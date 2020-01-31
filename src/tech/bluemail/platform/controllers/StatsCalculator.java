// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.controllers;

import java.util.Iterator;
import java.util.List;
import tech.bluemail.platform.models.production.DropIp;
import java.util.Map;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import tech.bluemail.platform.components.AccountingComponent;
import java.util.HashMap;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.remote.SSH;
import java.time.LocalDate;
import java.io.File;
import tech.bluemail.platform.orm.ActiveRecord;
import tech.bluemail.platform.parsers.TypesParser;
import tech.bluemail.platform.models.admin.Server;
import java.util.ArrayList;
import tech.bluemail.platform.interfaces.Controller;

public class StatsCalculator implements Controller
{
    public StatsCalculator() throws Exception {
    }
    
    @Override
    public void start(final String[] parameters) throws Exception {
        try {
            List<Server> servers = new ArrayList<Server>();
            if (parameters.length > 1) {
                final Integer ServerId = TypesParser.safeParseInt(parameters[1]);
                final Server serverObj = (Server)ActiveRecord.first(Server.class, "id = ? AND status_id = ?", new Object[] { ServerId, 1 });
                servers.add(serverObj);
            }
            else {
                servers = (List<Server>)ActiveRecord.all(Server.class, "status_id = ?", new Object[] { 1 });
            }
            if (servers == null || servers.isEmpty()) {
                throw new Exception("No Servers Found To Calculate Pmta Logs !");
            }
            for (final Server server : servers) {
                if (server != null) {
                    final String logsFolder = new File(System.getProperty("base.path")).getAbsolutePath() + File.separator + "tmp" + File.separator + "pmta-logs" + File.separator + "server_" + server.id;
                    final String today = LocalDate.now().toString();
                    if (!new File(logsFolder + "/" + today).exists()) {
                        new File(logsFolder).mkdirs();
                        new File(logsFolder + "/" + today + "/bounces/").mkdirs();
                        new File(logsFolder + "/" + today + "/delivered/").mkdirs();
                    }
                    final SSH ssh = SSH.SSHPassword(server.mainIp, String.valueOf(server.sshPort), server.username, server.password);
                    ssh.connect();
                    if (!ssh.isConnected()) {
                        throw new Exception("Could not connect to the server : " + server.name + " !");
                    }
                    final String[] types = { "delivered", "bounces" };
                    String result = "";
                    String[] archiveFiles = new String[0];
                    String prefix = "";
                    for (final String type : types) {
                        prefix = ("delivered".equalsIgnoreCase(type) ? "d" : "b");
                        result = ssh.cmd("awk 'FNR > 1' /etc/pmta/" + type + "/archived/*.csv > /etc/pmta/" + type + "/archived/" + today + "-clean.csv && find /etc/pmta/" + type + "/archived/" + today + "-clean.csv");
                        archiveFiles = new String[0];
                        if (result != null && !"".equals(result)) {
                            result = result.replaceAll("(?m)^[ \t]*\r?\n", "");
                            final String[] split;
                            archiveFiles = (split = result.split("\n"));
                            for (final String file : split) {
                                try {
                                    final String[] tmp = file.split("\\/");
                                    final String fileName = tmp[tmp.length - 1];
                                    ssh.downloadFile(file, logsFolder + File.separator + today + File.separator + type + File.separator + fileName);
                                }
                                catch (Exception e) {
                                    Logger.error(e, StatsCalculator.class);
                                }
                            }
                        }
                    }
                    ssh.cmd("rm -rf /etc/pmta/bounces/archived/*");
                    ssh.cmd("rm -rf /etc/pmta/delivered/archived/*");
                    ssh.disconnect();
                    final HashMap<Integer, HashMap<Integer, AccountingComponent>> stats = new HashMap<Integer, HashMap<Integer, AccountingComponent>>();
                    String[] lineParts = new String[0];
                    int dropId = 0;
                    int ipId = 0;
                    File[] bounceFiles = new File(logsFolder + File.separator + today + File.separator + "bounces").listFiles();
                    bounceFiles = (File[])ArrayUtils.addAll((Object[])bounceFiles, (Object[])new File(logsFolder + File.separator + today + File.separator + "bounces").listFiles());
                    List<String> lines = new ArrayList<String>();
                    for (final File bounceFile : bounceFiles) {
                        if (bounceFile.isFile()) {
                            lines.addAll(FileUtils.readLines(bounceFile));
                        }
                    }
                    for (final String line : lines) {
                        if (!"".equals(line)) {
                            lineParts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                            if (lineParts.length != 12 || (!"hardbnc".equalsIgnoreCase(lineParts[1]) && !"other".equalsIgnoreCase(lineParts[1])) || "".equalsIgnoreCase(lineParts[10])) {
                                continue;
                            }
                            dropId = TypesParser.safeParseInt(lineParts[10].split("_")[0]);
                            ipId = TypesParser.safeParseInt(lineParts[10].split("_")[1]);
                            if (!stats.containsKey(dropId)) {
                                stats.put(dropId, new HashMap<Integer, AccountingComponent>());
                            }
                            if (stats.get(dropId).containsKey(ipId)) {
                                final AccountingComponent accountingComponent = stats.get(dropId).get(ipId);
                                ++accountingComponent.bounced;
                            }
                            else {
                                stats.get(dropId).put(ipId, new AccountingComponent(dropId, ipId, 0, 1));
                            }
                        }
                    }
                    File[] deliveredFiles = new File(logsFolder + File.separator + today + File.separator + "delivered").listFiles();
                    deliveredFiles = (File[])ArrayUtils.addAll((Object[])deliveredFiles, (Object[])new File(logsFolder + File.separator + today + File.separator + "delivered").listFiles());
                    lines = new ArrayList<String>();
                    for (final File deliveredFile : deliveredFiles) {
                        if (deliveredFile.isFile()) {
                            lines.addAll(FileUtils.readLines(deliveredFile));
                        }
                    }
                    for (final String line2 : lines) {
                        if (!"".equals(line2)) {
                            lineParts = line2.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                            if (lineParts.length != 12 || !"success".equalsIgnoreCase(lineParts[1]) || "".equalsIgnoreCase(lineParts[10])) {
                                continue;
                            }
                            dropId = TypesParser.safeParseInt(lineParts[10].split("_")[0]);
                            ipId = TypesParser.safeParseInt(lineParts[10].split("_")[1]);
                            if (!stats.containsKey(dropId)) {
                                stats.put(dropId, new HashMap<Integer, AccountingComponent>());
                            }
                            if (stats.get(dropId).containsKey(ipId)) {
                                final AccountingComponent accountingComponent2 = stats.get(dropId).get(ipId);
                                ++accountingComponent2.delivered;
                            }
                            else {
                                stats.get(dropId).put(ipId, new AccountingComponent(dropId, ipId, 1, 0));
                            }
                        }
                    }
                    for (final Map.Entry<Integer, HashMap<Integer, AccountingComponent>> statsEntry : stats.entrySet()) {
                        dropId = statsEntry.getKey();
                        final HashMap<Integer, AccountingComponent> value = statsEntry.getValue();
                        if (dropId > 0 && value != null && !value.isEmpty()) {
                            for (final Map.Entry<Integer, AccountingComponent> accountingEntry : value.entrySet()) {
                                ipId = accountingEntry.getKey();
                                final AccountingComponent accounting = accountingEntry.getValue();
                                final DropIp dropIp = (DropIp)ActiveRecord.first(DropIp.class, "drop_id = ? AND ip_id = ?", new Object[] { dropId, ipId });
                                if (dropIp != null) {
                                    dropIp.delivered = accounting.delivered;
                                    dropIp.bounced = accounting.bounced;
                                    dropIp.update();
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e2) {
            Logger.error(e2, StatsCalculator.class);
        }
    }
}
