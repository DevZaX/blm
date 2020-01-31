// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.bootstrap;

import org.apache.commons.io.FileUtils;
import tech.bluemail.platform.security.Crypto;
import tech.bluemail.platform.interfaces.Controller;
import tech.bluemail.platform.controllers.SuppressionManager;
import tech.bluemail.platform.controllers.BounceCleaner;
import tech.bluemail.platform.controllers.StatsCalculator;
import tech.bluemail.platform.controllers.DropsSender;
import org.apache.commons.codec.binary.Base64;
import tech.bluemail.platform.orm.Database;
import tech.bluemail.platform.logging.Logger;
import java.io.File;

public class Start
{
    public static void main(final String[] args) {
        final long startTime = System.currentTimeMillis();
        try {
            check();
            System.setProperty("base.path", new File(Start.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getParentFile().getParentFile().getParentFile().getAbsolutePath().replaceAll("%20", " "));
            Logger.initlog4Java();
            Database.init();
            if (args.length == 0) {
                throw new Exception("No Parameters Passed !");
            }
            Controller controller = null;
            final String s = new String(Base64.encodeBase64(args[0].getBytes()));
            switch (s) {
                case "c2VuZF9wcm9jY2Vzcw==": {
                    controller = new DropsSender();
                    break;
                }
                case "c2VuZF9zdGF0cw==": {
                    controller = new StatsCalculator();
                    break;
                }
                case "Ym91bmNlX2NsZWFu": {
                    controller = new BounceCleaner();
                    break;
                }
                case "c3VwcHJlc3Npb25fcHJvY2Nlc3M=": {
                    controller = new SuppressionManager();
                    break;
                }
                default: {
                    throw new Exception("Unsupported Action !");
                }
            }
            if (controller != null) {
                controller.start(args);
            }
        }
        catch (Exception e) {
            Logger.error(e, Start.class);
        }
        finally {
            final long end = System.currentTimeMillis();
            System.out.println("Job Completed in : " + (end - startTime) + " miliseconds");
            System.exit(0);
        }
    }
    
    public static void check() throws Exception {
        String ip = "63.141.227.202";
        
        ip = Crypto.md5(ip);
        for (int i = 0; i < 5; ++i) {
            ip = java.util.Base64.getEncoder().encodeToString(ip.getBytes());
        }
        final File fb = new File("/var/strong/applications/bluemail/jobs/bluemail.txt");
        if (!fb.exists()) {
        	System.out.println("file not exist");
            System.exit(0);
        }
        final String license = FileUtils.readFileToString(fb);
        if (license == null || "".equalsIgnoreCase(license) || !license.trim().equals(ip)) {
        	System.out.println("licence not correct");
            System.exit(0);
        }
        System.out.println("licence correct");
    }
}
