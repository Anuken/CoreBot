package io.anuke.corebot;

import java.io.File;

public class CoreBot {
	public static final String[] allServers = {"mindustry.us.to", "mindustry.oa.to", "batata69.zapto.org", "67.41.250.201", "thebabinator.servegame.com"};
	public static final String releasesURL = "https://api.github.com/repos/Anuken/Mindustry/releases";
	public static final long guildID = 391020510269669376L;
	public static final File prefsFile = new File("prefs.properties");

	public static Messages messages = new Messages();
	public static Commands commands = new Commands();
	public static Net net = new Net();
	public static Prefs prefs = new Prefs(prefsFile);

	public static void main(String[] args){
		new CoreBot();
	}
}
