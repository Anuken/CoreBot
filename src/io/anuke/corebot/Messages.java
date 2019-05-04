package io.anuke.corebot;

import io.anuke.arc.util.Log;
import io.anuke.arc.util.Strings;
import io.anuke.arc.util.serialization.JsonValue;
import io.anuke.corebot.Net.PingResult;
import io.anuke.corebot.Net.VersionInfo;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.*;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

import static io.anuke.corebot.CoreBot.*;

public class Messages{
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
        clientBuilder.set5xxRetryCount(99999999); //retry more or less forever
        clientBuilder.withToken(token);

        client = clientBuilder.login();

        EventDispatcher event = client.getDispatcher();
        event.registerListener(this);

        Log.info("Discord bot up.");

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            List<PingResult> results = new CopyOnWriteArrayList<>();

            for(String server : prefs.getArray("servers")){
                net.pingServer(server, results::add);
            }

            net.run(Net.timeout, () -> {
                //not loaded yet
                if(client.getGuildByID(guildID) == null){
                    return;
                }

                results.sort((a, b) -> a.valid && !b.valid ? 1 : !a.valid && b.valid ? -1 : a.ip.compareTo(b.ip));

                IMessage[] arr = client.getGuildByID(guildID).getChannelByID(serverChannelID).getFullMessageHistory().asArray();
                messages.channel = client.getGuildByID(guildID).getChannelByID(serverChannelID);

                StringBuilder builder = new StringBuilder();

                builder.append(Strings.format("*Last Updated: {0}*\n\n", DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss ZZZZ").format(ZonedDateTime.now())));

                //send new messages
                for(PingResult result : results){
                    if(!result.valid){
                        builder.append(Strings.format("```diff\n{0}\n- offline```", result.ip));
                    }else{
                        builder.append(Strings.format("```http\n{0}\n\nPlayers: {1}\nMap: {2}\nWave: {3}\nVersion: {4}\nPing: {5}ms```",
                        result.ip, result.players, result.map, result.wave, result.version, result.ping));
                    }

                    builder.append("\n");
                }

                if(arr.length == 0){
                    messages.text(builder.toString());
                }else{
                    arr[0].edit(builder.toString());
                }

            });
        }, 10, 60, TimeUnit.SECONDS);

        StringBuilder messageBuilder = new StringBuilder();

        server.connect(input -> {
            if(messageBuilder.length() > 1000){
                String text = messageBuilder.toString();
                messageBuilder.setLength(0);
                client.getGuildByID(CoreBot.guildID).getChannelByID(commandChannelID).sendMessage(text);
            }else if(messageBuilder.length() == 0){
                messageBuilder.append(input);
                new Timer().schedule(new TimerTask(){
                    @Override
                    public void run(){
                        if(messageBuilder.length() == 0) return;
                        String text = messageBuilder.toString();
                        messageBuilder.setLength(0);
                        client.getGuildByID(CoreBot.guildID).getChannelByID(commandChannelID).sendMessage(text);
                    }
                }, 60L);
            }else{
                messageBuilder.append("\n").append(input);
            }
        });
    }

    @EventSubscriber
    public void onMessageReceivedEvent(MessageReceivedEvent event){
        commands.handle(event.getMessage());
    }

    @EventSubscriber
    public void onMessageEdited(MessageUpdateEvent event){
        commands.edited(event.getNewMessage(), event.getOldMessage());
    }

    @EventSubscriber
    public void onMessageDeleted(MessageDeleteEvent event){
        commands.deleted(event.getMessage());
    }

    @EventSubscriber
    public void onUserJoinEvent(UserJoinEvent event){
        if(CoreBot.sendWelcomeMessages){
            event.getGuild().getChannelsByName("general").get(0)
            .sendMessage("*Welcome* " + event.getUser().mention() + " *to the Mindustry Discord!*", false);
        }

        event.getUser().getOrCreatePMChannel().sendMessage(
        "**Welcome to the Mindustry Discord.**" +
        "\n\n*Make sure you read #rules and the channel topics before posting.*\n\n" +
        "**For a list of public servers**, see `!servers` in #bots.\n" +
        "**For info on how to play with friends**, see `!info multiplayer` in #bots.\n" +
        "**If you need info on the dedicated server**, see `!info server` in #bots.\n"
        );
    }

    public void sendUpdate(VersionInfo info){
        String text = info.description;
        int maxLength = 2000;
        while(true){
            String current = text.substring(0, Math.min(maxLength, text.length()));
            client.getGuildByID(CoreBot.guildID)
            .getChannelByID(announcementsChannelID)
            .sendMessage(new EmbedBuilder()
            .withColor(normalColor).withTitle(info.name)
            .appendDesc(current).build());

            if(text.length() < maxLength){
                return;
            }

            text = text.substring(maxLength);
        }
    }

    public void deleteMessages(){
        IMessage last = lastMessage, lastSent = lastSentMessage;

        new Timer().schedule(new TimerTask(){
            @Override
            public void run(){
                last.delete();
                lastSent.delete();
            }
        }, CoreBot.messageDeleteTime);
    }

    public void deleteMessage(){
        IMessage last = lastSentMessage;

        new Timer().schedule(new TimerTask(){
            @Override
            public void run(){
                last.delete();
            }
        }, CoreBot.messageDeleteTime);
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
        client.getGuildByID(CoreBot.guildID).getChannelByID(CoreBot.crashReportChannelID).sendMessage(builder.toString());
    }

    public void logTo(String text, Object... args){
        client.getGuildByID(guildID).getChannelByID(logChannelID).sendMessage(Strings.format(text, args));
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
        for(int i = 0; i < args.length; i++){
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return text;
    }
}
