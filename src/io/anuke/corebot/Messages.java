package io.anuke.corebot;

import com.badlogic.gdx.utils.JsonValue;
import io.anuke.corebot.Net.PingResult;
import io.anuke.corebot.Net.VersionInfo;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Strings;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.obj.Guild;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.anuke.corebot.CoreBot.messages;
import static io.anuke.corebot.CoreBot.net;
import static io.anuke.corebot.CoreBot.prefs;

public class Messages {
    IDiscordClient client;
    IChannel channel;
    IUser lastUser;
    IMessage lastMessage;
    IMessage lastSentMessage;
    Color normalColor = Color.decode("#FAB462");
    Color errorColor = Color.decode("#ff3838");

    public Messages(){
        String token = System.getProperty("token");
        Log.info("Found token: {0}", token);

        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.withToken(token);

        client = clientBuilder.login();

        EventDispatcher event = client.getDispatcher();
        event.registerListener(this);

        Log.info("Discord bot up.");

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            List<PingResult> results = new CopyOnWriteArrayList<>();

            for(String server : prefs.getArray("servers")){
                net.pingServer(server, result -> {
                    if(result.valid) results.add(result);
                });
            }

            net.run(Net.timeout, () -> {
                String result;
                if(results.isEmpty()){
                    result = "All known servers are offline.";
                }else{
                    StringBuilder s = new StringBuilder();
                    for(PingResult r : results){
                        s.append("**").append(r.ip).append("** **/** ").append(r.players).append(" players ").append(" **/** ").append(r.version).append(" `[").append(r.ping).append("ms]`\n");
                    }
                    result = s.toString();
                }

                 client.getGuilds().stream().filter(g -> g.getName().equals("Mindustry")).findFirst().orElseThrow(() -> new RuntimeException("Guild not found"))
                            .getChannelByID(CoreBot.multiplayerChannelID).changeTopic("**Server List:**\n\n" + result);

            });
        }, 120, 60, TimeUnit.SECONDS);
    }

    @EventSubscriber
    public void onMessageReceivedEvent(MessageReceivedEvent event){
        IMessage m = event.getMessage();
        CoreBot.commands.handle(m);
    }

    @EventSubscriber
    public void onUserJoinEvent(UserJoinEvent event){
        event.getGuild().getChannelsByName("general").get(0)
                .sendMessage("*Welcome* " + event.getUser().mention() + " *to the Mindustry Discord!*", false);

        event.getUser().getOrCreatePMChannel().sendMessage(
            "**Welcome to the Mindustry Discord.**" +
            "\n\n*Make sure you read #rules and the channel topics before posting.*\n\n" +
            "**For a list of public servers**, see `!servers` in #bots.\n" +
            "**For info on how to play with friends**, see `!info multiplayer` in #bots.\n" +
            "**If you need info on the dedicated server**, see `!info server` in #bots.\n"
        );
    }

    public IGuild getGuild(){
        return client.getGuilds().stream().filter(guild -> guild.getName().equals("Mindustry")).findAny().orElseThrow(() -> new RuntimeException("No Mindustry guild!"));
    }

    public void sendUpdate(VersionInfo info){
        client.getGuildByID(CoreBot.guildID)
                .getChannelsByName("announcements").get(0)
                .sendMessage(new EmbedBuilder()
                        .withColor(normalColor).withTitle(info.name)
                        .appendDesc(info.description).build());
    }

    public void deleteMessages(){
        IMessage last = lastMessage, lastSent = lastSentMessage;

        new Timer().schedule(
            new TimerTask() {
                @Override
                public void run() {
                    last.delete();
                    lastSent.delete();
                }
            }, CoreBot.messageDeleteTime
        );
    }

    public void deleteMessage(){
        IMessage last = lastSentMessage;

        new Timer().schedule(
            new TimerTask() {
                @Override
                public void run() {
                    last.delete();
                }
            }, CoreBot.messageDeleteTime
        );
    }

    public void sendCrash(JsonValue value){

        StringBuilder builder = new StringBuilder();
        value = value.child;
        while(value != null){
            builder.append("**");
            builder.append(value.name);
            builder.append("**");
            builder.append(": ");
            if(value.name.equals("trace")){
                builder.append("```xl\n"); //xl formatting looks nice
                builder.append(value.asString().replace("\\n", "\n").replace("\t", "  "));
                builder.append("```");
            }else{
                builder.append(value.asString());
            }
            builder.append("\n");
            value = value.next;
        }
        getGuild().getChannelByID(CoreBot.crashReportChannelID).sendMessage(builder.toString());
    }

    public void text(String text, Object... args){
        lastSentMessage = channel.sendMessage(format(text, args), false);
    }

    public void info(String title, String text, Object... args){
        EmbedObject object = new EmbedBuilder()
                .appendField(title, format(text, args), true).withColor(normalColor).build();
        lastSentMessage = channel.sendMessage(object);
    }

    public void err(String text, Object... args){
        err("Error", text, args);
    }

    public void err(String title, String text, Object... args){
        EmbedObject object = new EmbedBuilder()
                .appendField(title, format(text, args), true).withColor(errorColor).build();
        lastSentMessage = channel.sendMessage(object);
    }

    private String format(String text, Object... args){
        for(int i = 0; i < args.length; i ++){
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return text;
    }
}
