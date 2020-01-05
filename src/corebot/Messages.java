package corebot;

import arc.*;
import arc.Net.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import corebot.Net.*;
import mindustry.mod.*;
import mindustry.net.*;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.events.message.guild.*;
import net.dv8tion.jda.api.events.message.react.*;
import net.dv8tion.jda.api.hooks.*;

import java.awt.*;
import java.time.*;
import java.time.format.*;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.*;

import static corebot.CoreBot.*;

public class Messages extends ListenerAdapter{
    JDA jda;
    TextChannel channel;
    User lastUser;
    Message lastMessage;
    Message lastSentMessage;
    Guild guild;
    Color normalColor = Color.decode("#FAB462");
    Color errorColor = Color.decode("#ff3838");
    Json json = new Json();

    public Messages(){
        String token = System.getProperty("token");
        Log.info("Found token: {0}", token);

        try{
            jda = new JDABuilder(token).build();
            jda.awaitReady();
            jda.addEventListener(this);
            guild = jda.getGuildById(guildID);

            Log.info("Discord bot up.");
            Core.net = new arc.Net();

            //server updater
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                List<Host> results = new CopyOnWriteArrayList<>();

                for(String server : prefs.getArray("servers")){
                    net.pingServer(server, results::add);
                }

                net.run(Net.timeout, () -> {
                    results.sort((a, b) -> a.name != null && b.name == null ? 1 : a.name == null && b.name != null ? -1 : Integer.compare(a.players, b.players));

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(normalColor);

                    //send new messages
                    for(Host result : results){
                        if(result.name != null){
                            embed.addField(result.address,
                            Strings.format("*{0}*\nPlayers: {1}\nMap: {2}\nWave: {3}\nVersion: {4}\nMode: {5}\nPing: {6}ms\n_\n_\n",
                            result.name.replace("\\", "\\\\").replace("_", "\\_").replace("*", "\\*").replace("`", "\\`"),
                            (result.playerLimit > 0 ? result.players + "/" + result.playerLimit : result.players),
                            result.mapname.replace("\\", "\\\\").replace("_", "\\_").replace("*", "\\*").replace("`", "\\`").replaceAll("\\[.*?\\]", ""),
                            result.wave,
                            result.version,
                            Strings.capitalize(result.mode.name()),
                            result.ping), false);
                        }
                    }

                    embed.setFooter(Strings.format("Last Updated: {0}", DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss ZZZZ").format(ZonedDateTime.now())));

                    guild.getTextChannelById(serverChannelID).editMessageById(578594853991088148L, embed.build()).queue();
                });
            }, 10, 60, TimeUnit.SECONDS);

            StringBuilder messageBuilder = new StringBuilder();

            //server output, if applicable (currently doesn't work)
            server.connect(input -> {
                if(messageBuilder.length() > 1000){
                    String text = messageBuilder.toString();
                    messageBuilder.setLength(0);
                    guild.getTextChannelById(commandChannelID).sendMessage(text).queue();
                }else if(messageBuilder.length() == 0){
                    messageBuilder.append(input);
                    new Timer().schedule(new TimerTask(){
                        @Override
                        public void run(){
                            if(messageBuilder.length() == 0) return;
                            String text = messageBuilder.toString();
                            messageBuilder.setLength(0);
                            guild.getTextChannelById(commandChannelID).sendMessage(text).queue();
                        }
                    }, 60L);
                }else{
                    messageBuilder.append("\n").append(input);
                }
            });

            //mod list updater
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                Core.net.httpGet("https://raw.githubusercontent.com/Anuken/MindustryMods/master/mods.json", response -> {
                    if(response.getStatus() != HttpStatus.OK){
                        return;
                    }

                    Array<ModListing> listings = json.fromJson(Array.class, ModListing.class, response.getResultAsString());
                    listings.removeAll(l -> l.stars == 0);
                    listings.sort(Structs.comparing(list -> Date.from(Instant.parse(list.lastUpdated))));
                    listings.reverse();
                    listings.truncate(25);
                    listings.reverse();

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(normalColor);
                    embed.setTitle("Last 25 Updated Mods");
                    embed.setFooter(Strings.format("Last Updated: {0}", DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss ZZZZ").format(ZonedDateTime.now())));
                    for(ModListing listing : listings){
                        embed.addField(listing.repo + "  " + listing.stars + "★",
                        Strings.format("**[{0}]({1})**\n{2}\n*Updated {3} ago*\n_\n_\n_\n_",
                            Strings.stripColors(listing.name),
                            "https://github.com/" + listing.repo,
                            Strings.stripColors(listing.description),
                            durFormat(Duration.between(Instant.parse(listing.lastUpdated), Instant.now()))), false);
                    }

                    guild.getTextChannelById(modChannelID).editMessageById(663246057660219413L, embed.build()).queue();
                }, Throwable::printStackTrace);
            }, 0, 20, TimeUnit.MINUTES);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private static String durFormat(Duration duration){
        if(duration.toDays() > 0){
            return duration.toDays() + "d";
        }
        return duration.toHours() + "h";
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        try{
            commands.handle(event.getMessage());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event){
        //commands.edited(event.getMessage(), event.getOldMessage());
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event){
        //commands.deleted(event.getMessage());
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event){

        if(event.getChannel().getIdLong() == bugReportChannelID && commands.isAdmin(event.getUser())){
            commands.handleBugReact(event);
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event){
        event.getUser().openPrivateChannel().complete().sendMessage(
        "**Welcome to the Mindustry Discord.**" +
        "\n\n*Make sure you read #rules and the channel topics before posting.*\n\n" +
        "**For a list of public servers**, see the #servers channel.\n" +
        "**Make sure you check out the #faq channel here:**\n<https://discordapp.com/channels/391020510269669376/611204372592066570/611586644402765828>"
        ).queue();
    }

    public void sendUpdate(VersionInfo info){
        String text = info.description;
        int maxLength = 2000;
        while(true){
            String current = text.substring(0, Math.min(maxLength, text.length()));
            guild
            .getTextChannelById(announcementsChannelID)
            .sendMessage(new EmbedBuilder()
            .setColor(normalColor).setTitle(info.name)
            .setDescription(current).build()).queue();

            if(text.length() < maxLength){
                return;
            }

            text = text.substring(maxLength);
        }
    }

    public void deleteMessages(){
        Message last = lastMessage, lastSent = lastSentMessage;

        new Timer().schedule(new TimerTask(){
            @Override
            public void run(){
                last.delete().queue();
                lastSent.delete().queue();
            }
        }, CoreBot.messageDeleteTime);
    }

    public void deleteMessage(){
        Message last = lastSentMessage;

        new Timer().schedule(new TimerTask(){
            @Override
            public void run(){
                last.delete().queue();
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
        guild.getTextChannelById(CoreBot.crashReportChannelID).sendMessage(builder.toString()).queue();
    }

    public void logTo(String text, Object... args){
        guild.getTextChannelById(logChannelID).sendMessage(Strings.format(text, args)).queue();
    }

    public void text(String text, Object... args){
        lastSentMessage = channel.sendMessage(format(text, args)).complete();
    }

    public void info(String title, String text, Object... args){
        MessageEmbed object = new EmbedBuilder()
        .addField(title, format(text, args), true).setColor(normalColor).build();

        lastSentMessage = channel.sendMessage(object).complete();
    }

    public void err(String text, Object... args){
        err("Error", text, args);
    }

    public void err(String title, String text, Object... args){
        MessageEmbed e = new EmbedBuilder()
        .addField(title, format(text, args), true).setColor(errorColor).build();
        lastSentMessage = channel.sendMessage(e).complete();
    }

    private String format(String text, Object... args){
        for(int i = 0; i < args.length; i++){
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return text;
    }
}
