package corebot;

import java.awt.*;
import java.io.File;

public class CoreBot{
    public static final String releasesURL = "https://api.github.com/repos/Anuken/Mindustry/releases";
    public static final long guildID = 391020510269669376L;
    public static final File prefsFile = new File("prefs.properties");
    public static final long pluginChannelID = 617833229973717032L;
    public static final long crashReportChannelID = 467033526018113546L;
    public static final long announcementsChannelID = 391020997098340352L;
    public static final long screenshotsChannelID = 553071673587400705L;
    public static final long artChannelID = 754011833928515664L;
    public static final long mapsChannelID = 416719902641225732L;
    public static final long moderationChannelID = 488049830275579906L;
    public static final long schematicsChannelID = 640604827344306207L;
    public static final long baseSchematicsChannelID = 718536034127839252L;
    public static final long logChannelID = 568416809964011531L;
    public static final long joinChannelID = 832688792338038844L;
    public static final long streamsChannelID = 833420066238103604L;
    public static final long videosChannelID = 833826797048692747L;
    public static final long testingChannelID = 432984286099144706L;

    public static final Color normalColor = Color.decode("#FAB462");
    public static final Color errorColor = Color.decode("#ff3838");

    public static final long messageDeleteTime = 20000;
    public static final int warnExpireDays = 20;

    public static ContentHandler contentHandler = new ContentHandler();
    public static Messages messages = new Messages();
    public static Commands commands = new Commands();
    public static Net net = new Net();
    public static Prefs prefs = new Prefs(prefsFile);
    public static StreamScanner streams = new StreamScanner();
    public static VideoScanner videos = new VideoScanner();

    //crash reporting disabled basically forever
    //public static Reports reports = new Reports();

    public static void main(String[] args){
        new CoreBot();
    }
}
