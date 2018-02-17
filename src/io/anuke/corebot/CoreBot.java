package io.anuke.corebot;

public class CoreBot {
	public static Messages messages = new Messages();
	public static Commands commands = new Commands();
	public static Net net = new Net();

	public static void main(String[] args){
		new CoreBot();
	}
}
