package io.anuke.corebot;

import io.anuke.arc.collection.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.CommandHandler.*;
import io.anuke.corebot.Maps.Map;
import org.apache.commons.io.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.handle.obj.IMessage.*;
import sx.blah.discord.util.*;

import javax.imageio.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import static io.anuke.corebot.CoreBot.*;

public class Commands{
    private final String prefix = "!";
    private CommandHandler handler = new CommandHandler(prefix);
    private CommandHandler adminHandler = new CommandHandler(prefix);
    private String[] warningStrings = {"once", "twice", "thrice", "too many times"};
    private Pattern invitePattern = Pattern.compile("(discord\\.gg/\\w|discordapp\\.com/invite/\\w)");

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
                e.printStackTrace();
                messages.err("Error", "Invalid topic '{0}'.\nValid topics: *{1}*", args[0], Arrays.toString(Info.values()));
                messages.deleteMessages();
            }
        });

        handler.register("postplugin", "<github-url>", "Post a plugin via Github repository URL.", args -> {
            if(!args[0].startsWith("https") || !args[0].contains("github")){
                messages.err("That's not a valid Github URL.");
            }else{
                try{
                    Document doc = Jsoup.connect(args[0]).get();

                    EmbedBuilder builder = new EmbedBuilder().withColor(messages.normalColor).
                    withColor(messages.normalColor)
                    .withAuthorName(messages.lastUser.getName()).withTitle(doc.select("strong[itemprop=name]").text())
                    .withAuthorIcon(messages.lastUser.getAvatarURL());

                    Elements elem = doc.select("span[itemprop=about]");
                    if(!elem.isEmpty()){
                        builder.appendField("About", elem.text(), false);
                    }

                    builder.appendField("Link", args[0], false);

                    builder.appendField("Downloads", args[0] + (args[0].endsWith("/") ? "" : "/") + "releases", false);

                    messages.channel.getGuild().getChannelsByName("plugins").get(0)
                    .sendMessage(builder.build());

                    messages.text("*Plugin posted.*");
                }catch(IOException e){
                    e.printStackTrace();
                    messages.err("Failed to fetch plugin info from URL.");
                }
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

        handler.register("postmap", "Post a .msav file to the #maps channel.", args -> {
            IMessage message = messages.lastMessage;

            if(message.getAttachments().size() != 1 || !message.getAttachments().get(0).getFilename().endsWith(".msav")){
                messages.err("You must have one .msav file in the same message as the command!");
                messages.deleteMessages();
                return;
            }

            Attachment a = message.getAttachments().get(0);

            try{
                Map map = maps.parseMap(net.download(a.getUrl()));
                new File("maps/").mkdir();
                File mapFile = new File("maps/" + a.getFilename());
                File imageFile = new File("maps/image_" + a.getFilename().replace(".msav", ".png"));
                IOUtils.copy(net.download(a.getUrl()), new FileOutputStream(mapFile));
                ImageIO.write(map.image, "png", imageFile);

                EmbedBuilder builder = new EmbedBuilder().withColor(messages.normalColor).withColor(messages.normalColor)
                .withImage("attachment://" + imageFile.getName())
                .withAuthorName(messages.lastUser.getName()).withTitle(map.name == null ? a.getFilename().replace(".msav", "") : map.name)
                .withAuthorIcon(messages.lastUser.getAvatarURL());

                if(map.description != null) builder.withFooterText(map.description);

                messages.channel.getGuild().getChannelsByName("maps").get(0)
                .sendFiles(builder.build(), mapFile, imageFile);

                messages.text("*Map posted successfully.*");
            }catch(Exception e){
                e.printStackTrace();
                messages.err("Error parsing map.\n```{0}```", Strings.parseException(e, true));
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

        adminHandler.register("delete", "<amount>", "Delete some messages.", args -> {
            try{
                int number = Integer.parseInt(args[0]) + 1;
                MessageHistory hist = messages.channel.getMessageHistory(number);
                hist.bulkDelete();
                Log.info("Deleted {0} messages.", number);
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
            Log.info(args[0]);
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
            return user.getRolesForGuild(messages.client.getGuildByID(guildID)).stream()
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
                    if(s.toLowerCase().startsWith("build:")){
                        String buildText = s.substring("Build:".length());
                        String test = buildText.toLowerCase().trim();
                        String errormessage = null;
                        if(buildText.contains("4.") || buildText.contains("3.") || buildText.toLowerCase().contains("latest")){
                            errormessage = "The build you specified is incorrect!\nWrite **only the build/commit number in the bottom left corner of the menu**, not the version. *(for example, build 47, not 4.0)*.\n*Copy and re-send your message with a corrected report.*";
                        }else if(test.equals("be") || test.equals("bleeding edge") || test.equals("bleedingedge")){
                            errormessage = "Invalid bleeding edge version!\n**Only write the bleeding edge commit number displayed in the bottom left corner of the menu (or the or Jenkins build number).**\n*Copy and re-send your message with a corrected report.*";
                        }else if(Strings.canParseInt(test) && Integer.parseInt(test) > CoreBot.net.getLastBuild()){
                            errormessage = "Build " + test + " doesn't exist.\nFor bleeding edge builds, report the commit number, not the build number.";
                        }

                        if(errormessage != null){
                            messages.err(errormessage);
                            messages.deleteMessages();
                            return;
                        }
                    }else if(s.toLowerCase().startsWith("platform:")){
                        String platformText = s.substring("Platform:".length()).toLowerCase().trim();
                        if(!(platformText.contains("windows") || platformText.contains("mac") || platformText.contains("osx") || platformText.contains("linux") || platformText.contains("android") || platformText.contains("ios") || platformText.contains("iphone"))){
                            messages.err("**Invalid platform: '" + platformText + "'**.\nPlatform must be one of the following: `windows/linux/mac/osx/android/ios`.\n*Copy and re-send your message with a corrected report.*");
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

    boolean checkInvite(IMessage message){
        if(message.getContent() != null && invitePattern.matcher(message.getContent()).find() && !isAdmin(message.getAuthor())){
            Log.warn("User {0} just sent a discord invite in {1}.", message.getAuthor().getName(), message.getChannel().getName());
            message.delete();
            message.getAuthor().getOrCreatePMChannel().sendMessage("Do not send invite links in the Mindustry Discord server! Read the rules.");
            return true;
        }
        return false;
    }

    void edited(IMessage message, IMessage previous){
        if(message.getAuthor() == null || message.getContent() == null || previous == null) return;

        messages.logTo("------\n**{0}#{1}** just edited a message.\n\n*From*: \"{2}\"\n*To*: \"{3}\"", message.getAuthor().getName(), message.getAuthor().getDiscriminator(), previous.getContent(), message.getContent());
        checkInvite(message);
    }

    void deleted(IMessage message){
        if(message == null || message.getAuthor() == null) return;
        messages.logTo("------\n**{0}#{1}** just deleted a message.\n *Text:* \"{2}\"", message.getAuthor().getName(), message.getAuthor().getDiscriminator(), message.getContent());
    }

    void handleBugReact(ReactionAddEvent event){
        EmbedBuilder builder = new EmbedBuilder().withColor(messages.normalColor);
        String url = Strings.format("https://discordapp.com/channels/{0}/{1}/{2}",
            event.getGuild().getStringID(), event.getChannel().getStringID(), event.getMessage().getStringID());

        String emoji = event.getReaction().getEmoji().getName();
        Log.info("Recieved react emoji -> {0}, message {1}", emoji, url);
        boolean valid = true;
        if(emoji.equals("✅")){
            Log.info("| Solved.");
            builder.withColor(Color.decode("#87FF4B"));
            builder.appendDesc("[Your bug report](" + url + ") in the Mindustry Discord has been marked as solved.");
        }else if(emoji.equals("❌")){
            Log.info("| Not a bug.");
            builder.withColor(messages.errorColor);
            builder.appendDesc("[Your bug report]("+url+") in the Mindustry Discord has been marked as **not a bug** (intentional or unfixable behavior).");
        }else if(emoji.equals("\uD83C\uDDE9")){
            Log.info("| Duplicate.");
            builder.withColor(messages.errorColor);
            builder.appendDesc("Your bug report in the Mindustry Discord has been marked as a **duplicate**: Someone has reported this issue before.\nYour report has been removed to clean up the channel.\n\nReport deleted: ```" + event.getMessage().getContent() + "```");
            event.getMessage().delete();
        }else{
            Log.info("| Invalid.");
            valid = false;
        }

        if(valid){
            event.getMessage().getAuthor().getOrCreatePMChannel()
            .sendMessage(builder.build());
        }
    }

    void handle(IMessage message){
        if(checkInvite(message)){
            return;
        }

        if(isAdmin(message.getAuthor()) && message.getChannel().getLongID() == commandChannelID){
            server.send(message.getContent());
            return;
        }

        if(message.getChannel().getLongID() == bugReportChannelID && !message.isSystemMessage() && !isAdmin(message.getAuthor())){
            messages.channel = message.getChannel();
            messages.lastUser = message.getAuthor();
            messages.lastMessage = message;
            checkForReport(message);
            return;
        }

        if(message.getChannel().getLongID() == screenshotsChannelID && message.getAttachments().isEmpty()){
            message.delete();
            try{
                message.getAuthor().getOrCreatePMChannel().sendMessage("Don't send messages without images in the #screenshots channel.");
            }catch(Exception e){
                e.printStackTrace();
            }
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

    boolean handleResponse(CommandResponse response, boolean logUnknown){
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
