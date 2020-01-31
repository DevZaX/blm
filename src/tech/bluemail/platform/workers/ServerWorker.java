// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.workers;

import java.util.Map;
import org.apache.commons.io.FileUtils;
import java.sql.Timestamp;
import tech.bluemail.platform.exceptions.DatabaseException;
import java.util.concurrent.ExecutorService;
import java.util.Iterator;
import tech.bluemail.platform.logging.Logger;
import java.util.Arrays;
import tech.bluemail.platform.parsers.TypesParser;
import java.util.concurrent.TimeUnit;
import tech.bluemail.platform.exceptions.ThreadException;
import java.util.concurrent.Executors;
import java.util.LinkedHashMap;
import tech.bluemail.platform.orm.Database;
import tech.bluemail.platform.helpers.DropsHelper;
import tech.bluemail.platform.remote.SSH;
import java.io.File;
import tech.bluemail.platform.utils.Strings;
import tech.bluemail.platform.components.RotatorComponent;
import java.util.ArrayList;
import tech.bluemail.platform.models.admin.Vmta;
import java.util.List;
import tech.bluemail.platform.models.admin.Server;
import tech.bluemail.platform.components.DropComponent;

public class ServerWorker extends Thread
{
    public DropComponent drop;
    public Server server;
    public List<Vmta> vmtas;
    public int offset;
    public int limit;
    public String query;
    public List<PickupWorker> pickupWorkers;
    public List<SenderWorker> sendersWorkers;
    
    public ServerWorker(final DropComponent drop, final Server server, final List<Vmta> vmtas, final int offset, final int limit) {
        this.pickupWorkers = new ArrayList<PickupWorker>();
        this.sendersWorkers = new ArrayList<SenderWorker>();
        this.drop = drop;
        this.server = server;
        this.vmtas = vmtas;
        this.offset = offset;
        this.limit = limit;
        if (this.drop.isSend) {
            this.query = "SELECT * FROM (";
            String[] string = {"a"};
            final StringBuilder sb =  new StringBuilder();
            this.drop.lists.entrySet().stream().map(en -> {
                this.query = this.query + "SELECT id,'" + String.valueOf(en.getValue()) + "' AS table,'" + String.valueOf(en.getKey()) + "' AS list_id,fname,lname,email";
                return en;
            }).map(en -> {
                new StringBuilder().append(this.query);
                if (String.valueOf(en.getValue()).contains("seeds")) {
                    string[0] = ",generate_series(1," + drop.emailsPerSeeds + ") AS serie";
                }
                else {
                    string[0] = ",id AS serie";
                }
                this.query = sb.append(string).toString();
                return en;
            }).forEachOrdered(en -> this.query = this.query + " FROM " + String.valueOf(en.getValue()) + " UNION ALL ");
            this.query = this.query.substring(0, this.query.length() - 10) + " WHERE (offers_excluded IS NULL OR offers_excluded = '' OR NOT ('" + this.drop.offerId + "' = ANY(string_to_array(offers_excluded,',')))) ORDER BY id OFFSET " + this.drop.dataStart + " LIMIT " + this.drop.dataCount + ") As Sub OFFSET " + this.offset + " LIMIT " + this.limit;
        }
        if (!this.vmtas.isEmpty()) {
            final int rotation = this.drop.isSend ? this.drop.vmtasRotation : this.drop.testEmails.length;
            this.drop.vmtasRotator = new RotatorComponent(this.vmtas, rotation);
        }
        this.drop.pickupsFolder = System.getProperty("base.path") + "/tmp/pickups/server_" + this.server.id + "_" + Strings.getSaltString(20, true, true, true, false);
        new File(this.drop.pickupsFolder).mkdirs();
    }
    
    @Override
    public void run() {
        SSH ssh = null;
        boolean errorOccured = false;
        boolean isStopped = false;
        try {
            if (this.server != null && this.server.id > 0 && !this.vmtas.isEmpty()) {
                ssh = SSH.SSHPassword(this.server.mainIp, String.valueOf(this.server.sshPort), this.server.username, this.server.password);
                ssh.connect();
                if (this.drop.uploadImages) {
                    DropsHelper.uploadImage(this.drop, ssh);
                }
                if (this.vmtas.isEmpty()) {
                    throw new Exception("No Vmtas Found !");
                }
                List<LinkedHashMap<String, Object>> result = null;
                if (this.drop.isSend) {
                	System.out.println(this.query);
                    result = Database.get("lists").executeQuery(this.query, null, 1);
                    this.drop.emailsCount = result.size();
                }
                else {
                    result = new ArrayList<LinkedHashMap<String, Object>>();
                    if (this.drop.testEmails != null && this.drop.testEmails.length > 0) {
                        for (final Vmta vmta : this.vmtas) {
                            if (vmta != null) {
                                for (final String testEmail : this.drop.testEmails) {
                                    final LinkedHashMap<String, Object> tmp = new LinkedHashMap<String, Object>();
                                    tmp.put("id", 0);
                                    tmp.put("email", testEmail.trim());
                                    tmp.put("table", "");
                                    tmp.put("list_id", 0);
                                    result.add(tmp);
                                }
                            }
                        }
                    }
                }
                if (this.drop.isSend && this.drop.isNewDrop) {
                    DropsHelper.saveDrop(this.drop, this.server);
                    if (this.drop.id > 0) {
                        if (this.vmtas.isEmpty()) {
                            throw new Exception("No Vmtas Found !");
                        }
                        final int vmtasTotal = (int)Math.ceil(this.drop.emailsCount / this.vmtas.size());
                        final int vmtasRest = this.drop.emailsCount - vmtasTotal * this.vmtas.size();
                        int index = 0;
                        if (!this.vmtas.isEmpty()) {
                            for (final Vmta vmta2 : this.vmtas) {
                                if (index < vmtasRest) {
                                    DropsHelper.saveDropVmta(this.drop, vmta2, vmtasTotal + 1);
                                }
                                else {
                                    DropsHelper.saveDropVmta(this.drop, vmta2, vmtasTotal);
                                }
                                ++index;
                            }
                        }
                    }
                }
                if (this.drop.isSend) {
                    DropsHelper.writeThreadStatusFile(this.server.id, this.drop.pickupsFolder);
                }
                if (!this.drop.isSend) {
                    this.drop.emailsCount = this.drop.testEmails.length * this.vmtas.size();
                }
                this.drop.batch = ((this.drop.batch > this.drop.emailsCount) ? this.drop.emailsCount : this.drop.batch);
                this.drop.batch = ((this.drop.batch == 0) ? 1 : this.drop.batch);
                final ExecutorService pickupsExecutor = Executors.newFixedThreadPool(100);
                if (this.drop.batch == 0) {
                    throw new Exception("Batch should be greather than 0 !");
                }
                final int pickupsNumber = (this.drop.emailsCount % this.drop.batch == 0) ? ((int)Math.ceil(this.drop.emailsCount / this.drop.batch)) : ((int)Math.ceil(this.drop.emailsCount / this.drop.batch) + 1);
                int start = 0;
                int finish = this.drop.batch;
                Vmta periodVmta = null;
                PickupWorker worker = null;
                for (int i = 0; i < pickupsNumber; ++i) {
                    if (this.drop != null && this.drop.isSend && this.drop.id > 0) {
                        final String status = this.DropStatus();
                        if (DropsHelper.hasToStopDrop(this.server.id, this.drop.pickupsFolder) || status == "interrupted") {
                            this.interrupt();
                            pickupsExecutor.shutdownNow();
                            this.interruptDrop();
                            isStopped = true;
                            this.drop.isStoped = true;
                            break;
                        }
                    }
                    if (isStopped || this.isInterrupted() || this.drop.isStoped) {
                        pickupsExecutor.shutdownNow();
                        this.interrupt();
                        this.pickupWorkers.forEach(previousWorker -> {
                            if (previousWorker.isAlive()) {
                                previousWorker.interrupt();
                            }
                            return;
                        });
                        break;
                    }
                    periodVmta = ("emails-per-period".equalsIgnoreCase(this.drop.vmtasEmailsProcces) ? this.drop.getCurrentVmta() : null);
                    worker = new PickupWorker(i, this.drop, this.server, result.subList(start, finish), periodVmta);
                    worker.setUncaughtExceptionHandler(new ThreadException());
                    pickupsExecutor.submit(worker);
                    this.pickupWorkers.add(worker);
                    start += this.drop.batch;
                    finish += this.drop.batch;
                    if (finish > result.size()) {
                        finish = result.size();
                    }
                    if (start >= result.size()) {
                        break;
                    }
                }
                pickupsExecutor.shutdown();
                pickupsExecutor.awaitTermination(1L, TimeUnit.DAYS);
                if (!isStopped && !this.drop.isStoped) {
                    File[] pickupsFiles = new File(this.drop.pickupsFolder).listFiles();
                    if (pickupsFiles != null && pickupsFiles.length > 0 && ssh.isConnected() && !this.drop.isStoped) {
                        final File[] tmp2 = this.drop.isSend ? new File[pickupsFiles.length - 1] : new File[pickupsFiles.length];
                        int idx = 0;
                        for (final File pickupsFile : pickupsFiles) {
                            if (pickupsFile.getName().startsWith("pickup_")) {
                                tmp2[idx] = pickupsFile;
                                ++idx;
                            }
                        }
                        pickupsFiles = tmp2;
                        Arrays.sort(pickupsFiles, (f1, f2) -> new Integer(TypesParser.safeParseInt(f1.getName().split("_")[1])).compareTo(TypesParser.safeParseInt(f2.getName().split("_")[1])));
                        final ExecutorService senderExecutor = Executors.newFixedThreadPool(100);
                        SenderWorker senderWorker = null;
                        for (final File pickupsFile2 : pickupsFiles) {
                            if (this.drop != null && this.drop.isSend && this.drop.id > 0) {
                                final String status2 = this.DropStatus();
                                if (DropsHelper.hasToStopDrop(this.server.id, this.drop.pickupsFolder) || status2 == "interrupted") {
                                    senderExecutor.shutdownNow();
                                    this.interruptDrop();
                                    isStopped = true;
                                    break;
                                }
                            }
                            if (isStopped || this.drop.isStoped) {
                                senderExecutor.shutdownNow();
                                this.interrupt();
                                this.sendersWorkers.forEach(previousWorker -> {
                                    if (previousWorker.isAlive()) {
                                        previousWorker.interrupt();
                                    }
                                    return;
                                });
                                break;
                            }
                            senderWorker = new SenderWorker(this.drop.id, ssh, pickupsFile2);
                            senderWorker.setUncaughtExceptionHandler(new ThreadException());
                            senderExecutor.submit(senderWorker);
                            this.sendersWorkers.add(senderWorker);
                            if (this.drop.delay > 0L) {
                                Thread.sleep(this.drop.delay);
                            }
                        }
                        senderExecutor.shutdown();
                        senderExecutor.awaitTermination(1L, TimeUnit.DAYS);
                    }
                }
            }
        }
        catch (Exception e) {
            if (this.drop != null && this.drop.isSend && this.drop.id > 0) {
                this.errorDrop();
            }
            Logger.error(e, ServerWorker.class);
            errorOccured = true;
        }
        finally {
            this.finishProccess(ssh, errorOccured, isStopped);
        }
    }
    
    public static synchronized void updateDrop(final int dropId, final int progress) throws DatabaseException {
        Database.get("master").executeUpdate("UPDATE production.drops SET sent_progress = sent_progress + '" + progress + "'  WHERE id = ?", new Object[] { dropId }, 0);
    }
    
    public void finishProccess(final SSH ssh, final boolean errorOccured, final boolean isStopped) {
        try {
            if (ssh != null && ssh.isConnected()) {
                ssh.disconnect();
            }
            if (this.drop != null) {
                if (this.drop.id > 0 && !errorOccured && !isStopped) {
                    int progress = 0;
                    final List<LinkedHashMap<String, Object>> result = Database.get("master").executeQuery("SELECT sent_progress FROM production.drops WHERE id =" + this.drop.id, null, 0);
                    if (!result.isEmpty()) {
                        progress = (int) result.get(0).get("sent_progress");
                        if (progress == this.drop.emailsCount) {
                            Database.get("master").executeUpdate("UPDATE production.drops SET status = 'completed' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(System.currentTimeMillis()), this.drop.id }, 0);
                        }
                    }
                }
                FileUtils.deleteDirectory(new File(this.drop.pickupsFolder));
            }
        }
        catch (Exception e) {
            Logger.error(e, ServerWorker.class);
        }
    }
    
    public void errorDrop() {
        try {
            Database.get("master").executeUpdate("UPDATE production.drops SET status = 'error' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(System.currentTimeMillis()), this.drop.id }, 0);
            if (this.drop != null) {
                FileUtils.deleteDirectory(new File(this.drop.pickupsFolder));
            }
        }
        catch (Exception e) {
            Logger.error(e, ServerWorker.class);
        }
    }
    
    public void interruptDrop() {
        try {
            Database.get("master").executeUpdate("UPDATE production.drops SET status = 'interrupted' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(System.currentTimeMillis()), this.drop.id }, 0);
            if (this.drop != null) {
                FileUtils.deleteDirectory(new File(this.drop.pickupsFolder));
            }
        }
        catch (Exception e) {
            Logger.error(e, ServerWorker.class);
        }
    }
    
    public String DropStatus() {
        String status = "";
        try {
            final List<LinkedHashMap<String, Object>> result = Database.get("master").executeQuery("SELECT status FROM production.drops WHERE id =" + this.drop.id, null, 0);
            if (!result.isEmpty()) {
                status = (String) result.get(0).get("status");
                return status;
            }
        }
        catch (Exception e) {
            Logger.error(e, ServerWorker.class);
        }
        return status;
    }
}
