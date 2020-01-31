// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.workers;

import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.utils.Strings;
import tech.bluemail.platform.parsers.TypesParser;
import java.io.File;
import tech.bluemail.platform.remote.SSH;

public class SenderWorker extends Thread
{
    public int dropId;
    public SSH ssh;
    public File pickupFile;
    
    public SenderWorker(final int dropId, final SSH ssh, final File pickupFile) {
        this.dropId = dropId;
        this.ssh = ssh;
        this.pickupFile = pickupFile;
    }
    
    @Override
    public void run() {
        try {
            if (this.ssh != null && this.pickupFile != null && this.pickupFile.exists()) {
                final int progress = TypesParser.safeParseInt(String.valueOf(this.pickupFile.getName().split("\\_")[2]));
                final String file = "/var/spool/bluemail/tmp/pickup_" + Strings.getSaltString(20, true, true, true, false) + ".txt";
                this.ssh.uploadFile(this.pickupFile.getAbsolutePath(), file);
                this.ssh.cmd("mv " + file + " /var/spool/bluemail/pickup/");
                if (this.dropId > 0) {
                    ServerWorker.updateDrop(this.dropId, progress);
                }
            }
        }
        catch (Exception e) {
            Logger.error(e, SenderWorker.class);
        }
    }
}
