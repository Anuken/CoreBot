package io.anuke.corebot;

import java.io.File;

public class CoreBot {
	public static final String[] allServers = {"mindustry.us.to", "mindustry.oa.to", "batata69.zapto.org",
			"thebabinator.servegame.com", "mindustry.pastorhudson.com", "mindustry.indielm.com"};
	public static final String releasesURL = "https://api.github.com/repos/Anuken/Mindustry/releases";
	public static final long guildID = 391020510269669376L;
	public static final File prefsFile = new File("prefs.properties");
	public static final String bugChannelName = "secret-bot-testing";
	public static final String reportTemplate = "**Platform:** *<Android/iOS/Mac/Windows/Linux/Web>*\n" +
			"**Build:** *<Post the build number in the bottom left corner of main menu>*\n" +
			"**Issue:** *<What goes wrong. Be specific!>*\n" +
			"**Circumstances:** *<Did this bug happen when you performed some action? If so, provide details.>*";

	public static final long messageDeleteTime = 10000;

	public static Messages messages = new Messages();
	public static Commands commands = new Commands();
	public static Net net = new Net();
	public static Prefs prefs = new Prefs(prefsFile);

	public static void main(String[] args){
		new CoreBot();
	}
}
