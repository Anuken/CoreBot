package io.anuke.corebot;

import com.badlogic.gdx.utils.Array;
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

import java.io.File;
import java.io.PrintWriter;
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
            if(messages.lastMessage.getChannel().getName().equalsIgnoreCase("multiplayer")){
                messages.err("Use this command in #bots.");
                messages.deleteMessages();
                return;
            }

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
            if(!messages.lastMessage.getChannel().getName().equalsIgnoreCase("bots")){
                messages.err("Use this command in #bots.");
                messages.deleteMessages();
                return;
            }

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
                messages.deleteMessages();
            }
        });

        handler.register("servers", "Displays all known online servers.", args -> {
            if(!messages.lastMessage.getChannel().getName().equalsIgnoreCase("bots")){
                messages.err("Use this command in #bots.");
                messages.deleteMessages();
                return;
            }

            List<PingResult> results = new CopyOnWriteArrayList<>();

            for(String server : prefs.getArray("servers")){
                net.pingServer(server, result -> {
                    if(result.valid) results.add(result);
                });
            }

            net.run(Net.timeout, () -> {
                if(results.isEmpty()){
                    messages.err("No servers found.", "All known servers are offline.");
                }else{
                    StringBuilder s = new StringBuilder();
                    for(PingResult r : results){
                        s.append("**").append(r.ip).append("** **/** ").append(r.players).append(" players ").append(" **/** ").append(r.version).append(" `[").append(r.ping).append("ms]`\n");
                    }
                    messages.info("Online Servers", s.toString());
                }
            });
        });

        handler.register("postmap", "<mapname> [description...]", "Post a map to the #maps channel.", args -> {
            IMessage message = messages.lastMessage;

            if(message.getAttachments().size() != 1){
                messages.err("You must post an image in the same message as the command!");
                messages.deleteMessages();
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
                messages.deleteMessages();
            }
        });

        handler.register("google", "<phrase...>", "Let me google that for you.", args -> {
            try {
                messages.text("http://lmgtfy.com/?q={0}", URLEncoder.encode(args[0], "UTF-8"));
            }catch (UnsupportedEncodingException e){
                e.printStackTrace();
            }
        });

        adminHandler.register("listservers", "List servers pinged automatically.", args -> {
            messages.text("**Servers:** {0}", prefs.getArray("servers").toString().replace("[", "").replace("]", ""));
        });

        adminHandler.register("addserver", "<IP>", "Add server to list.", args -> {
            Array<String> servers = prefs.getArray("servers");
            servers.add(args[0]);
            prefs.putArray("servers", servers);
            messages.text("*Server added.*");
        });

        adminHandler.register("removeserver", "<IP>", "Remove server from list.", args -> {
            Array<String> servers = prefs.getArray("servers");
            boolean removed = servers.removeValue(args[0], false);
            prefs.putArray("servers", servers);
            if(removed){
                messages.text("*Server removed.*");
            }else{
                messages.err("Server not found!");
            }
        });

        adminHandler.register("warn", "<@user> [reason...]", "Warn a user.", args -> {
            String author = args[0].substring(2, args[0].length()-1);
            try{
                long l = Long.parseLong(author);
                IUser user = messages.client.getUserByID(l);
                int warnings =  prefs.getInt("warnings-" + l, 0) + 1;
                messages.text("**{0}**, you've been warned *{1}*.", user.mention(), warningStrings[Mathf.clamp(warnings-1, 0, warningStrings.length-1)]);
                prefs.put("warnings-" + l, warnings + "");
                if(warnings >= 3){
                    messages.lastMessage.getGuild().getChannelsByName("moderation").get(0)
                            .sendMessage("User "+user.mention()+" has been warned 3 or more times!");
                }
            }catch (Exception e){
                e.printStackTrace();
                messages.err("Incorrect name format.");
                messages.deleteMessages();
            }
        });

        adminHandler.register("warnings", "<@user>", "Get number of warnings a user has.", args -> {
            String author = args[0].substring(2, args[0].length()-1);
            try{
                long l = Long.parseLong(author);
                IUser user = messages.client.getUserByID(l);
                int warnings =  prefs.getInt("warnings-" + l, 0);
                messages.text("User '{0}' has **{1}** {2}.", user.getDisplayName(messages.channel.getGuild()), warnings, warnings == 1 ? "warning" : "warnings");
            }catch (Exception e){
                e.printStackTrace();
                messages.err("Incorrect name format.");
                messages.deleteMessages();
            }
        });

        adminHandler.register("clearwarnings", "<@user>", "Clear number of warnings for a person.", args -> {
            String author = args[0].substring(2, args[0].length()-1);
            try{
                long l = Long.parseLong(author);
                IUser user = messages.client.getUserByID(l);
                prefs.put("warnings-" + l, 0 + "");
                messages.text("Cleared warnings for user '{0}'.", user.getDisplayName(messages.channel.getGuild()));
            }catch (Exception e){
                e.printStackTrace();
                messages.err("Incorrect name format.");
                messages.deleteMessages();
            }
        });
    }

    boolean isAdmin(IUser user){
        try {
            return user.getRolesForGuild(messages.channel.getGuild()).stream()
                    .anyMatch(role -> role.getName().equals("Developer") || role.getName().equals("Moderator"));
        }catch (Exception e){
            return false; //I don't care enough to fix this
        }
    }

    void sendReportTemplate(IMessage message){
        messages.err("**Do not send messages here unless you are reporting a crash or issue!**\nTo report an issue, follow the template provided in **!info issues**.\nTo report a crash, send the crash report text file.");
        messages.deleteMessages();
    }

    void checkForReport(IMessage message){
        if(!message.getAttachments().isEmpty()){
            Attachment at = message.getAttachments().get(0);

            if(message.getAttachments().size() > 1){
                messages.err("Please do not send multiple crash reports in one message.");
                messages.deleteMessages();
                return;
            }

            //is crash report
            if(at.getFilename().startsWith("crash-report") && at.getFilename().endsWith("txt")){
                String text = net.getText(at.getUrl());
                CrashReport report = new CrashReport(text);
                if(!report.valid){
                    messages.err("Invalid crash report.");
                    messages.deleteMessages();
                }else{
                    try{
                        int build = Integer.parseInt(report.values.get("build"));

                        if(build == -1){
                            messages.err("Do not report crashes for custom builds. No support is provided.");
                            messages.deleteMessages();
                        }else if(build != 0 && build < net.getLastBuild()){
                            messages.err("Outdated game: You are using build {0}, while the latest is build {1}.\n**Update your game.**", build, net.getLastBuild());
                            messages.deleteMessages();
                        }else{
                            try {
                                messages.info("Info", "Crash report submitted successfully.");
                                File file = File.createTempFile("crash", "txt");
                                PrintWriter out = new PrintWriter(file);
                                out.print(text);
                                out.close();

                                String extraText = message.getContent() == null || message.getContent().isEmpty() ? "" :
                                        "\"" + message.getContent() + "\"";
                                messages.deleteMessages();
                                message.getChannel().getGuild().getChannelsByName("crashes").get(0).sendFile("v**" + report.values.get("build")+"**\n" + extraText +
                                        "\n*Submitted by " + message.getAuthor().mention() + ".*", file);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                    }catch (NumberFormatException | NullPointerException e){
                        messages.err("Outdated game: You are using an old version of Mindustry\n**Update your game.**");
                        messages.deleteMessages();
                        e.printStackTrace();
                    }
                }
            }else if(emptyText(message)){
                messages.err("Please do not send images or other unrelated files in this channel.\nCrash reports should be sent as un-renamed **text files.**");
                messages.deleteMessages();
            }else{
                checkForIssue(message);
            }
        }else if(emptyText(message)){
            sendReportTemplate(message);
        }else{
            checkForIssue(message);
        }
    }

    void checkForIssue(IMessage message){
        String text = message.getContent();
        String[] required = {"Platform:", "Build:", "Issue:", "Circumstances:"};
        String[] split = text.split("\n");

        if(split.length == 0){
            sendReportTemplate(message);
            return;
        }

        //get used entries
        Array<String> arr = Array.with(required);
        for(String s : split){
            for(String req : required){
                if(s.toLowerCase().startsWith(req.toLowerCase()) && s.length() > req.length()){
                    arr.removeValue(req, false);

                    //special case: don't let people report a build as a version such as 4.0/3.5
                    if(req.equals("Build:")){
                        String buildText = req.substring("Build:".length());
                        if(buildText.contains("4.") || buildText.contains("3.")){
                            messages.err("The build you specified is incorrect!\nWrite **only the build number**, not the version. *(for example, build 47, not 4.0)*.\n*Copy and re-send your message with a corrected report.*");
                            messages.deleteMessages();
                            return;
                        }
                    }
                }
            }
        }

        //validate entries
        if(arr.size == required.length){
            sendReportTemplate(message);
            return;
        }

        //validate all entries present
        if(arr.size != 0){
            messages.err("Your issue report is incomplete. Make sure you've followed the issue template correctly!\n*Copy and re-send your message with a corrected report.*");
            messages.deleteMessages();
            return;
        }

        //validate template text
        if(text.contains("<Android/iOS/Mac/Windows/Linux/Web>") || text.contains("<Post the build number in the bottom left corner of main menu>")
                || text.contains("<What goes wrong. Be specific!>") || text.contains("<Provide details on what you were doing when this bug occurred, as well as any other helpful information.>")){
            messages.err("You have not filled in your issue report! Make sure you've replaced all template text properly.\n*Copy and re-send your message with a corrected report.*");
            messages.deleteMessages();
            return;
        }

        messages.text("*Issue reported successfully.*");
        messages.deleteMessage();
    }

    boolean emptyText(IMessage message){
        return message.getContent() == null || message.getContent().isEmpty();
    }

    void handle(IMessage message){
        if(message.getChannel().getLongID() == bugReportChannelID && !message.isSystemMessage()
                && message.getAuthor().getRolesForGuild(message.getGuild()).stream().noneMatch(role -> role.getName().equals("Developer"))) {
            messages.channel = message.getChannel();
            messages.lastUser = message.getAuthor();
            messages.lastMessage = message;
            checkForReport(message);
            return;
        }

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
            if(logUnknown){
                messages.err("Unknown command. Type !help for a list of commands.");
                messages.deleteMessages();
            }
            return false;
        }else if(response.type == ResponseType.manyArguments || response.type == ResponseType.fewArguments){
            if(response.command.params.length == 0){
                messages.err("Invalid arguments.", "Usage: {0}{1}", prefix, response.command.text);
                messages.deleteMessages();
            }else {
                messages.err("Invalid arguments.", "Usage: {0}{1} *{2}*", prefix, response.command.text, response.command.paramText);
                messages.deleteMessages();
            }
            return false;
        }
        return true;
    }

}
