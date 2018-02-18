package io.anuke.corebot;

import io.anuke.ucore.util.Log;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.*;

public class Messages {
    IDiscordClient client;
    IChannel channel;

    public Messages(){
        String token = System.getProperty("token");
        Log.info("Found token: {0}", token);

        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.withToken(token);

        client = clientBuilder.login();

        EventDispatcher event = client.getDispatcher();
        event.registerListener(this);

        Log.info("Discord bot up.");
    }

    @EventSubscriber
    public void onMessageReceivedEvent(MessageReceivedEvent event){
        IMessage m = event.getMessage();
        channel = m.getChannel();
        CoreBot.commands.handle(m);
    }

    @EventSubscriber
    public void onUserJoinEvent(UserJoinEvent event){
        event.getGuild().getChannelsByName("general").get(0)
                .sendMessage("*Welcome* " + event.getUser().mention() + " *to the Mindustry Discord!*", true);
        /*
        event.getGuild().getChannelsByName("general").get(0)
                .sendMessage(new EmbedBuilder()
                        .appendField("Welcome!", event.getUser().mention() + " has joined the server.", true)
                        .withColor(Color.decode("#FAB462")).build());*/
    }

    public void text(String text, Object... args){
        channel.sendMessage(format(text, args));
    }

    public void info(String title, String text, Object... args){
        EmbedObject object = new EmbedBuilder()
                .appendField(title, format(text, args), true).withColor(Color.decode("#FAB462")).build();
        channel.sendMessage(object);
    }

    public void err(String text, Object... args){
        err("Error", text, args);
    }

    public void err(String title, String text, Object... args){
        EmbedObject object = new EmbedBuilder()
                .appendField(title, format(text, args), true).withColor(Color.decode("#ff3838")).build();
        channel.sendMessage(object);
    }

    private String format(String text, Object... args){
        for(int i = 0; i < args.length; i ++){
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return text;
    }
}
