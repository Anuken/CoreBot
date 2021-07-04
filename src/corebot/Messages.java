package corebot;

import arc.util.*;
import arc.util.serialization.*;
import corebot.Nets.*;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.events.message.guild.*;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.requests.*;
import net.dv8tion.jda.api.utils.*;
import net.dv8tion.jda.api.utils.cache.*;

import java.util.Timer;
import java.util.*;

import static corebot.CoreBot.*;

public class Messages extends ListenerAdapter{
    JDA jda;
    TextChannel channel;
    User lastUser;
    Message lastMessage;
    Message lastSentMessage;
    Guild guild;

    public Messages(){
        String token = System.getenv("CORE_BOT_TOKEN");
        Log.info("Found token: @", token != null);

        try{
            jda = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .setMemberCachePolicy(MemberCachePolicy.ALL).disableCache(CacheFlag.VOICE_STATE).build();
            jda.awaitReady();
            jda.addEventListener(this);
            guild = jda.getGuildById(guildID);

            Log.info("Discord bot up.");
        }catch(Exception e){
            throw new RuntimeException(e);
        }
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
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event){
        try{
            commands.edited(event.getMessage());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event){
        event.getUser().openPrivateChannel().complete().sendMessage(
        """
        **Welcome to the Mindustry Discord.**
                
        *Make sure you read #rules and the channel topics before posting.*
                
        **View a list of all frequently answered questions here:**
        <https://discordapp.com/channels/391020510269669376/611204372592066570/611586644402765828>
        """
        ).queue();

        messages.guild.getTextChannelById(joinChannelID)
        .sendMessage(new EmbedBuilder()
            .setAuthor(event.getUser().getName(), event.getUser().getAvatarUrl(), event.getUser().getAvatarUrl())
            .addField("User", event.getUser().getAsMention(), false)
            .addField("ID", "`" + event.getUser().getId() + "`", false)
            .setColor(normalColor).build())
        .queue();
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
        return Strings.format(text, args);
    }
}
