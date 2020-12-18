package corebot;

import java.io.File;

public class CoreBot{
    public static final String releasesURL = "https://api.github.com/repos/Anuken/Mindustry/releases";
    public static final long guildID = 391020510269669376L;
    public static final File prefsFile = new File("prefs.properties");
    public static final long modChannelID = 663227511987240980L;
    public static final long pluginChannelID = 617833229973717032L;
    public static final long crashReportChannelID = 467033526018113546L;
    public static final long announcementsChannelID = 391020997098340352L;
    public static final long screenshotsChannelID = 553071673587400705L;
    public static final long artChannelID = 754011833928515664L;
    public static final long mapsChannelID = 416719902641225732L;
    public static final long moderationChannelID = 488049830275579906L;
    public static final long schematicsChannelID = 640604827344306207L;
    public static final long baseSchematicsChannelID = 718536034127839252L;

    public static final long messageDeleteTime = 20000;

    public static ContentHandler contentHandler = new ContentHandler();
    public static Messages messages = new Messages();
    public static Commands commands = new Commands();
    public static Net net = new Net();
    public static Prefs prefs = new Prefs(prefsFile);

    //crash reporting disabled until V6 is out
    //public static Reports reports = new Reports();

    public static void main(String[] args){
        new CoreBot();
    }
}
