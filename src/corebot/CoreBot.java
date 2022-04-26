package corebot;

import java.awt.*;
import java.io.*;

public class CoreBot{
    public static final String releasesURL = "https://api.github.com/repos/Anuken/Mindustry/releases";
    public static final File prefsFile = new File("prefs.properties");

    public static final Color normalColor = Color.decode("#FAB462");
    public static final Color errorColor = Color.decode("#ff3838");

    public static final long messageDeleteTime = 20000; //milliseconds
    public static final int warnExpireDays = 15;

    public static ContentHandler contentHandler = new ContentHandler();
    public static Messages messages = new Messages();
    public static Net net = new Net();
    public static Prefs prefs = new Prefs(prefsFile);
    public static StreamScanner streams = new StreamScanner();
    public static VideoScanner videos = new VideoScanner();
    //public static Reports reports = new Reports();

    public static void main(String[] args){
    }
}
