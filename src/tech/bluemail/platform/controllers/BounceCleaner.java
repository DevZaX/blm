// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.controllers;

import java.sql.Timestamp;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.orm.Database;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.List;
import tech.bluemail.platform.logging.Logger;
import java.util.concurrent.TimeUnit;
import tech.bluemail.platform.exceptions.ThreadException;
import tech.bluemail.platform.workers.BounceWorker;
import java.util.concurrent.Executors;
import tech.bluemail.platform.orm.ActiveRecord;
import tech.bluemail.platform.models.admin.Server;
import java.util.ArrayList;
import tech.bluemail.platform.parsers.TypesParser;
import tech.bluemail.platform.interfaces.Controller;

public class BounceCleaner implements Controller
{
    public static volatile int COUNT;
    public static volatile int INDEX;
    
    public BounceCleaner() throws Exception {
    }
    
    @Override
    public void start(final String[] parameters) throws Exception {
        final int proccessId = TypesParser.safeParseInt(parameters[1]);
        boolean errorOccured = false;
        try {
            final String listName = parameters[2];
            final int ispId = TypesParser.safeParseInt(parameters[3]);
            final int userId = TypesParser.safeParseInt(parameters[4]);
            System.out.println("CL");
            List<Server> servers = new ArrayList<Server>();
            if (proccessId == 0) {
                throw new Exception("No Proccess Id Found !");
            }
            if (listName == null || "".equals(listName)) {
                throw new Exception("No List Name Found !");
            }
            if (parameters.length > 4) {
                final int serverId = TypesParser.safeParseInt(parameters[5]);
                final Controller controllerCalcul = new StatsCalculator();
                final String[] args = { "send_stats", parameters[5] };
                controllerCalcul.start(args);
                final Server serverObj = (Server)ActiveRecord.first(Server.class, "id = ? AND status_id = ?", new Object[] { serverId, 1 });
                servers.add(serverObj);
            }
            else {
                final Controller controllerCalcul2 = new StatsCalculator();
                final String[] args2 = { "send_stats" };
                if (controllerCalcul2 != null) {
                    controllerCalcul2.start(parameters);
                }
                servers = (List<Server>)ActiveRecord.all(Server.class, "status_id = ?", new Object[] { 1 });
            }
            if (servers == null || servers.isEmpty()) {
                throw new Exception("No Servers Found To Clean Bounce From !");
            }
            final ExecutorService serversExecutor = Executors.newFixedThreadPool((servers.size() > 15) ? 15 : servers.size());
            BounceWorker worker = null;
            for (final Server server : servers) {
                if (server != null) {
                    System.out.println("Start Cleaninf for server -> " + server.name);
                    worker = new BounceWorker(proccessId, listName, server, userId, ispId);
                    worker.setUncaughtExceptionHandler(new ThreadException());
                    serversExecutor.submit(worker);
                }
            }
            serversExecutor.shutdown();
            serversExecutor.awaitTermination(10L, TimeUnit.DAYS);
        }
        catch (Exception e) {
            this.interruptProccess(proccessId);
            Logger.error(e, BounceCleaner.class);
            errorOccured = true;
        }
        finally {
            if (!errorOccured) {
                this.finishProccess(proccessId);
            }
        }
    }
    
    public static synchronized void updateProccess(final int proccessId, final String type) throws DatabaseException {
        final int progress = (int)(getIndex() / (double)getCount() * 100.0);
        final String update = "bounce".equalsIgnoreCase(type) ? " , hard_bounce = hard_bounce + 1 " : " , clean = clean + 1 ";
        Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET progress = '" + progress + "%' " + update + " WHERE Id = ?", new Object[] { proccessId }, 0);
        updateIndex();
    }
    
    public static synchronized int getIndex() {
        return BounceCleaner.INDEX;
    }
    
    public static synchronized void updateIndex() {
        ++BounceCleaner.INDEX;
    }
    
    public static synchronized void updateCount(final int size) {
        BounceCleaner.COUNT += size;
    }
    
    public static synchronized int getCount() {
        return BounceCleaner.COUNT;
    }
    
    public void interruptProccess(final int proccessId) {
        try {
            Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET status = 'error' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(System.currentTimeMillis()), proccessId }, 0);
        }
        catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }
    
    public void finishProccess(final int proccessId) {
        try {
            Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET status = 'completed' , progress = '100%' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(System.currentTimeMillis()), proccessId }, 0);
        }
        catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }
    
    static {
        BounceCleaner.COUNT = 0;
        BounceCleaner.INDEX = 1;
    }
}
