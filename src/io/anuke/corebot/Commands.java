package io.anuke.corebot;

import io.anuke.arc.collection.Array;
import io.anuke.arc.util.CommandHandler;
import io.anuke.arc.util.CommandHandler.Command;
import io.anuke.arc.util.CommandHandler.Response;
import io.anuke.arc.util.CommandHandler.ResponseType;
import io.anuke.arc.math.Mathf;
import io.anuke.corebot.Maps.Map;
import org.apache.commons.io.IOUtils;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IMessage.Attachment;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageHistory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

import static io.anuke.corebot.CoreBot.*;

public class Commands{
    private final String prefix = "!";
    private CommandHandler handler = new CommandHandler(prefix);
    private CommandHandler adminHandler = new CommandHandler(prefix);
    private String[] warningStrings = {"once", "twice", "thrice", "too many times"};

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
                if(command.params.length > 0){
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
            try{
                Info info = Info.valueOf(args[0]);
                messages.info(info.title, info.text);
            }catch(IllegalArgumentException e){
                messages.err("Error", "Invalid topic '{0}'.\nValid topics: *{1}*", args[0], Arrays.toString(Info.values()));
                messages.deleteMessages();
            }
        });

        handler.register("postimagemap", "<mapname> [description...]", "Post an image (3.5) map to the #maps channel.", args -> {
            IMessage message = messages.lastMessage;

            if(message.getAttachments().size() != 1 || !message.getAttachments().get(0).getFilename().endsWith(".png")){
                messages.err("You must post a .png image in the same message as the command!");
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
            }catch(Exception e){
                messages.err("Invalid username format.");
                messages.deleteMessages();
            }
        });

        handler.register("postmap", "Post a mmap file to the #maps channel.", args -> {
            IMessage message = messages.lastMessage;

            if(message.getAttachments().size() != 1 || !message.getAttachments().get(0).getFilename().endsWith(".mmap")){
                messages.err("You must have one .mmap file in the same message as the command!");
                messages.deleteMessages();
                return;
            }

            Attachment a = message.getAttachments().get(0);

            try{
                Map map = maps.parseMap(net.download(a.getUrl()));
                new File("maps/").mkdir();
                File mapFile = new File("maps/" + a.getFilename());
                File imageFile = new File("maps/image_" + a.getFilename().replace(".mmap", ".png"));
                IOUtils.copy(net.download(a.getUrl()), new FileOutputStream(mapFile));
                ImageIO.write(map.image, "png", imageFile);

                EmbedBuilder builder = new EmbedBuilder().withColor(messages.normalColor).withColor(messages.normalColor)
                .withImage("attachment://" + imageFile.getName())
                .withAuthorName(messages.lastUser.getName()).withTitle(map.name == null ? a.getFilename().replace(".mmap", "") : map.name)
                .withAuthorIcon(messages.lastUser.getAvatarURL());

                if(map.description != null) builder.withFooterText(map.description);

                messages.channel.getGuild().getChannelsByName("maps").get(0)
                .sendFiles(builder.build(), mapFile, imageFile);

                messages.text("*Map posted successfully.*");
            }catch(Exception e){
                e.printStackTrace();
                messages.err("Error parsing map.");
                messages.deleteMessages();
            }
        });

        handler.register("google", "<phrase...>", "Let me google that for you.", args -> {
            try{
                messages.text("http://lmgtfy.com/?q={0}", URLEncoder.encode(args[0], "UTF-8"));
            }catch(UnsupportedEncodingException e){
                e.printStackTrace();
            }
        });

        adminHandler.register("delete", "<amount>", args -> {
            try{
                int number = Integer.parseInt(args[0]) + 1;
                MessageHistory hist = messages.channel.getMessageHistory(number);
                hist.bulkDelete();
            }catch(NumberFormatException e){
                messages.err("Invalid number.");
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
            String author = args[0].substring(2, args[0].length() - 1);
            try{
                long l = Long.parseLong(author);
                IUser user = messages.client.getUserByID(l);
                int warnings = prefs.getInt("warnings-" + l, 0) + 1;
                messages.text("**{0}**, you've been warned *{1}*.", user.mention(), warningStrings[Mathf.clamp(warnings - 1, 0, warningStrings.length - 1)]);
                prefs.put("warnings-" + l, warnings + "");
                if(warnings >= 3){
                    messages.lastMessage.getGuild().getChannelsByName("moderation").get(0)
                    .sendMessage("User " + user.mention() + " has been warned 3 or more times!");
                }
            }catch(Exception e){
                e.printStackTrace();
                messages.err("Incorrect name format.");
                messages.deleteMessages();
            }
        });

        adminHandler.register("warnings", "<@user>", "Get number of warnings a user has.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            try{
                long l = Long.parseLong(author);
                IUser user = messages.client.getUserByID(l);
                int warnings = prefs.getInt("warnings-" + l, 0);
                messages.text("User '{0}' has **{1}** {2}.", user.getDisplayName(messages.channel.getGuild()), warnings, warnings == 1 ? "warning" : "warnings");
            }catch(Exception e){
                e.printStackTrace();
                messages.err("Incorrect name format.");
                messages.deleteMessages();
            }
        });

        adminHandler.register("clearwarnings", "<@user>", "Clear number of warnings for a person.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            try{
                long l = Long.parseLong(author);
                IUser user = messages.client.getUserByID(l);
                prefs.put("warnings-" + l, 0 + "");
                messages.text("Cleared warnings for user '{0}'.", user.getDisplayName(messages.channel.getGuild()));
            }catch(Exception e){
                e.printStackTrace();
                messages.err("Incorrect name format.");
                messages.deleteMessages();
            }
        });
    }

    boolean isAdmin(IUser user){
        try{
            return user.getRolesForGuild(messages.channel.getGuild()).stream()
            .anyMatch(role -> role.getName().equals("Developer") || role.getName().equals("Moderator"));
        }catch(Exception e){
            return false; //I don't care enough to fix this
        }
    }

    void sendReportTemplate(IMessage message){
        messages.err("**Do not send messages here unless you are reporting an issue!**\nTo report an issue, follow the template provided in **!info bugs**.\nTo report a crash, send the crash report text file.");
        messages.deleteMessages();
    }

    void checkForReport(IMessage message){
        if(!message.getAttachments().isEmpty()){

            if(emptyText(message)){
                messages.err("Please do not send images or other unrelated files in this channel.");
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
        String[] required = {"Platform:", "Build:", "Issue:"};
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
                    if(req.startsWith("Build:")){
                        String buildText = req.substring("Build:".length());
                        if(buildText.contains("4.") || buildText.contains("3.") || buildText.toLowerCase().contains("latest")){
                            messages.err("The build you specified is incorrect!\nWrite **only the build/commit number in the bottom left corner of the menu**, not the version. *(for example, build 47, not 4.0)*.\n*Copy and re-send your message with a corrected report.*");
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
            messages.err("Error", "Your issue report is incomplete. You have not provided: *{0}*.\n*Copy and re-send your message with a corrected report.*", arr.toString());
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
        && message.getAuthor().getRolesForGuild(message.getGuild()).stream().noneMatch(role -> role.getName().equals("Developer"))){
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
            }else{
                messages.err("Invalid arguments.", "Usage: {0}{1} *{2}*", prefix, response.command.text, response.command.paramText);
                messages.deleteMessages();
            }
            return false;
        }
        return true;
    }

}
