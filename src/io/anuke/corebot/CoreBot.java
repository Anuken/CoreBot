package io.anuke.corebot;

public class CoreBot {
	public static Messages messages = new Messages();
	public static Commands commands = new Commands();
	public static Net net = new Net();

	public static final String[] allServers = {"mindustry.us.to", "mindustry.oa.to", "batata69.zapto.org", "67.41.250.201"};

	public static void main(String[] args){
		new CoreBot();
	}
}
