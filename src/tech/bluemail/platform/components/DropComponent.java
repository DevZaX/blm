// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.components;

import tech.bluemail.platform.models.admin.Offer;
import tech.bluemail.platform.models.admin.Sponsor;
import tech.bluemail.platform.models.lists.Fresh;
import java.util.HashMap;
import tech.bluemail.platform.models.admin.Isp;
import tech.bluemail.platform.models.admin.OfferSubject;
import tech.bluemail.platform.models.admin.OfferName;
import tech.bluemail.platform.models.admin.Vmta;
import tech.bluemail.platform.models.admin.Server;
import java.util.List;
import java.io.Serializable;

public class DropComponent implements Serializable
{
    public int id;
    public boolean isSend;
    public String content;
    public String[] randomTags;
    public int mailerId;
    public boolean isNewDrop;
    public boolean isStoped;
    public String[] serversIds;
    public List<Server> servers;
    public String[] vmtasIds;
    public List<Vmta> vmtas;
    public String vmtasEmailsProcces;
    public int numberOfEmails;
    public int emailsPeriodValue;
    public String emailsPeriodType;
    public int batch;
    public long delay;
    public int vmtasRotation;
    public int fromNameId;
    public String fromName;
    public OfferName fromNameObject;
    public int subjectId;
    public String subject;
    public OfferSubject subjectObject;
    public int headersRotation;
    public String[] headers;
    public String bounceEmail;
    public String fromEmail;
    public String returnPath;
    public String replyTo;
    public String received;
    public String to;
    public boolean hasPlaceholders;
    public int placeholdersRotation;
    public String[] placeholders;
    public boolean uploadImages;
    public String charset;
    public String contentTransferEncoding;
    public String contentType;
    public String body;
    public String redirectFileName;
    public String optoutFileName;
    public boolean trackOpens;
    public String staticDomain;
    public int ispId;
    public Isp isp;
    public String emailsSplitType;
    public int testFrequency;
    public String[] testEmails;
    public String rcptfrom;
    public int dataStart;
    public int dataCount;
    public int emailsCount;
    public int emailsPerSeeds;
    public HashMap<Integer, String> lists;
    public int listsCount;
    public List<Fresh> emails;
    public int sponsorId;
    public Sponsor sponsor;
    public int offerId;
    public Offer offer;
    public int creativeId;
    public boolean isAutoResponse;
    public boolean randomCaseAutoResponse;
    public int autoResponseRotation;
    public String[] autoReplyEmails;
    public volatile String pickupsFolder;
    public volatile int emailsCounter;
    public volatile RotatorComponent vmtasRotator;
    
    public DropComponent() {
        this.id = 0;
        this.isNewDrop = true;
        this.isStoped = false;
        this.emailsCounter = 0;
    }
    
    public synchronized int updateCounter() {
        return this.emailsCounter++;
    }
    
    public Vmta getCurrentVmta() {
        return (Vmta)this.vmtasRotator.getCurrentThenRotate();
    }
}
