package io.anuke.corebot;

import io.anuke.corebot.Net.PingResult;
import io.anuke.ucore.util.CommandHandler;
import io.anuke.ucore.util.CommandHandler.Command;
import io.anuke.ucore.util.CommandHandler.Response;
import io.anuke.ucore.util.CommandHandler.ResponseType;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IMessage.Attachment;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.anuke.corebot.CoreBot.*;

public class Commands {
    private final String prefix = "!";
    private CommandHandler handler = new CommandHandler(prefix);
    private CommandHandler adminHandler = new CommandHandler(prefix);
    private String[] warningStrings = {"once", "twice", "thrice", "more than thrice"};

    Commands(){
        handler.register("help", "Displays all bot commands.", args -> {
            StringBuilder builder = new StringBuilder();
            for(Command command : handler.getCommandList()){
                builder.append(prefix);
                builder.append("**");
                builder.append(command.text);
                builder.append("**");
                if(command.params.length > 0) {
                    builder.append(" *");
                    builder.append(command.paramText);
                    builder.append("*");
                }
                builder.append(" - ");
                builder.append(command.description);
                builder.append("\n");
            }

            messages.info("Commands", builder.toString());
        });

        handler.register("ping", "<ip>", "Pings a server.", args -> {
            net.pingServer(args[0], result -> {
                if(result.valid){
                    messages.info("Server Online", "Host: {0}\nPlayers: {1}\nMap: {2}\nWave: {3}\nVersion: {4}\nPing: {5}ms",
                            result.host, result.players, result.map, result.wave, result.version, result.ping);
                }else{
                    messages.err("Server Offline", "Reason: {0}", result.error);
                }
            });
        });

        handler.register("info", "<topic>", "Displays information about a topic.", args -> {
            try {
                Info info = Info.valueOf(args[0]);
                messages.info(info.title, info.text);
            }catch (IllegalArgumentException e){
                messages.err("Error", "Invalid topic '{0}'.\nValid topics: *{1}*", args[0], Arrays.toString(Info.values()));
            }
        });

        handler.register("servers", "Displays all known online servers.", args -> {
            List<PingResult> results = new CopyOnWriteArrayList<>();

            for(String server : allServers){
                net.pingServer(server, result -> {
                    if(result.valid) results.add(result);
                });
            }

            net.run(Net.timeout, () -> {
                if(results.isEmpty()){
                    messages.err("No servers found.", "All known servers are offline.");
                }else{
                    String s = "";
                    for(PingResult r : results){
                        s += "**" + r.ip + "** **/** " + r.players + " players `[" + r.ping + "ms]`\n";
                    }
                    messages.info("Online Servers", s);
                }
            });
        });

        handler.register("postmap", "<mapname> [description...]", "Post a map to the #maps channel.", args -> {
            IMessage message = messages.lastMessage;

            if(message.getAttachments().size() != 1){
                messages.err("You must post an image in the same message as the command!");
                return;
            }

            Attachment a = message.getAttachments().get(0);

            String name = args[0];
            String desc = args.length < 2 ? "" : args[1];

            try{
                EmbedBuilder builder = new EmbedBuilder().withColor(messages.normalColor).withColor(messages.normalColor)
                        .withImage(a.getUrl()).withAuthorName(messages.lastUser.getName()).withTitle(name)
                        .withAuthorIcon(messages.lastUser.getAvatarURL());

                if(!desc.isEmpty()) builder.withFooterText(desc);

                messages.channel.getGuild().getChannelsByName("maps").get(0)
                        .sendMessage(builder.build());

                messages.text("*Map posted successfully.*");
            }catch (Exception e){
                messages.err("Invalid username format.");
            }
        });

        handler.register("google", "<phrase...>", "Let me google that for you.", args -> {
            try {
                messages.text("http://lmgtfy.com/?q={0}", URLEncoder.encode(args[0], "UTF-8"));
            }catch (UnsupportedEncodingException e){
                e.printStackTrace();
            }
        });

        adminHandler.register("warn", "<@user>", "Warn a user.", args -> {
            String author = args[0].substring(2, args[0].length()-1);
            try{
                long l = Long.parseLong(author);
                IUser user = messages.client.getUserByID(l);
                int warnings =  prefs.getInt("warnings-" + l, 0) + 1;
                messages.info("**{0}**, you've been warned *{2}*.", user.mention(), warningStrings[Mathf.clamp(warnings-1, 0, warningStrings.length-1)]);
                prefs.put("warnings-" + l, warnings + "");
                if(warnings > 3){
                    messages.lastMessage.getGuild().getChannelsByName("moderation").get(0)
                            .sendMessage("User "+user.mention()+" has been warned 3 or more times!");
                }
            }catch (Exception e){
                e.printStackTrace();
                messages.err("Incorrect name format.");
            }
        });

        adminHandler.register("warnings", "<@user>", "Get number of warnings a user has.", args -> {
            String author = args[0].substring(2, args[0].length()-1);
            try{
                long l = Long.parseLong(author);
                IUser user = messages.client.getUserByID(l);
                int warnings =  prefs.getInt("warnings-" + l, 0) + 1;
                messages.info("User '{0}' has **{1}** {2}.", user.getDisplayName(messages.channel.getGuild()), warnings, warnings == 1 ? "warning" : "warnings");
            }catch (Exception e){
                e.printStackTrace();
                messages.err("Incorrect name format.");
            }
        });
    }

    boolean isAdmin(IUser user){
        return user.getRolesForGuild(messages.channel.getGuild()).stream()
                .anyMatch(role -> role.getName().equals("Developer") || role.getName().equals("Moderator"));
    }

    void handle(IMessage message){
        if(message.getContent() == null) return;

        String text = message.getContent();

        if(message.getContent() != null && message.getContent().startsWith(prefix)){
            messages.channel = message.getChannel();
            messages.lastUser = message.getAuthor();
            messages.lastMessage = message;
        }

        if(isAdmin(message.getAuthor())){
            boolean unknown = handleResponse(adminHandler.handleMessage(text), false);
            handleResponse(handler.handleMessage(text), !unknown);
        }else{
            handleResponse(handler.handleMessage(text), true);
        }
    }

    boolean handleResponse(Response response, boolean logUnknown){
        if(response.type == ResponseType.unknownCommand){
            if(logUnknown) messages.err("Unknown command. Type !help for a list of commands.");
            return false;
        }else if(response.type == ResponseType.manyArguments || response.type == ResponseType.fewArguments){
            if(response.command.params.length == 0){
                messages.err("Invalid arguments.", "Usage: {0}{1}", prefix, response.command.text);
            }else {
                messages.err("Invalid arguments.", "Usage: {0}{1} *{2}*", prefix, response.command.text, response.command.paramText);
            }
            return false;
        }
        return true;
    }

}
