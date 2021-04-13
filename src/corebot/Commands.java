package corebot;

import arc.files.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.CommandHandler.*;
import arc.util.io.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import corebot.ContentHandler.Map;
import mindustry.*;
import mindustry.game.*;
import mindustry.type.*;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import java.util.zip.*;

import static corebot.CoreBot.*;

public class Commands{
    private final String prefix = "!";
    private static final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
    private CommandHandler handler = new CommandHandler(prefix);
    private CommandHandler adminHandler = new CommandHandler(prefix);
    private String[] warningStrings = {"once", "twice", "thrice", "too many times"};
    private Pattern invitePattern = Pattern.compile("(discord\\.gg/\\w|discordapp\\.com/invite/\\w|discord\\.com/invite/\\w)");

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
                if(result.name != null){
                    messages.info("Server Online", "Host: @\nPlayers: @\nMap: @\nWave: @\nVersion: @\nPing: @ms",
                    Strings.stripColors(result.name), result.players, Strings.stripColors(result.mapname), result.wave, result.version, result.ping);
                }else{
                    messages.err("Server Offline", "Timed out.");
                }
            });
        });

        handler.register("info", "<topic>", "Displays information about a topic.", args -> {
            try{
                Info info = Info.valueOf(args[0]);
                messages.info(info.title, info.text);
            }catch(IllegalArgumentException e){
                messages.err("Error", "Invalid topic '@'.\nValid topics: *@*", args[0], Arrays.toString(Info.values()));
                messages.deleteMessages();
            }
        });


        handler.register("postplugin", "<github-url>", "Post a plugin via Github repository URL.", args -> {
            if(!args[0].startsWith("https") || !args[0].contains("github")){
                messages.err("That's not a valid Github URL.");
            }else{
                try{
                    Document doc = Jsoup.connect(args[0]).get();

                    EmbedBuilder builder = new EmbedBuilder().setColor(messages.normalColor).
                    setColor(messages.normalColor)
                    .setAuthor(messages.lastUser.getName(), messages.lastUser.getAvatarUrl(), messages.lastUser.getAvatarUrl()).setTitle(doc.select("strong[itemprop=name]").text());

                    Elements elem = doc.select("span[itemprop=about]");
                    if(!elem.isEmpty()){
                        builder.addField("About", elem.text(), false);
                    }

                    builder.addField("Link", args[0], false);

                    builder.addField("Downloads", args[0] + (args[0].endsWith("/") ? "" : "/") + "releases", false);

                    messages.channel.getGuild().getTextChannelById(pluginChannelID).sendMessage(builder.build()).queue();

                    messages.text("*Plugin posted.*");
                }catch(IOException e){
                    e.printStackTrace();
                    messages.err("Failed to fetch plugin info from URL.");
                }
            }
        });

        handler.register("postmap", "Post a .msav file to the #maps channel.", args -> {
            Message message = messages.lastMessage;

            if(message.getAttachments().size() != 1 || !message.getAttachments().get(0).getFileName().endsWith(".msav")){
                messages.err("You must have one .msav file in the same message as the command!");
                messages.deleteMessages();
                return;
            }

            Attachment a = message.getAttachments().get(0);

            try{
                Map map = contentHandler.readMap(net.download(a.getUrl()));
                new File("cache/").mkdir();
                File mapFile = new File("cache/" + a.getFileName());
                Fi imageFile = Fi.get("cache/image_" + a.getFileName().replace(".msav", ".png"));
                Streams.copy(net.download(a.getUrl()), new FileOutputStream(mapFile));
                ImageIO.write(map.image, "png", imageFile.file());

                EmbedBuilder builder = new EmbedBuilder().setColor(messages.normalColor).setColor(messages.normalColor)
                .setImage("attachment://" + imageFile.name())

                .setAuthor(messages.lastUser.getName(), messages.lastUser.getAvatarUrl(), messages.lastUser.getAvatarUrl()).setTitle(map.name == null ? a.getFileName().replace(".msav", "") : map.name);

                if(map.description != null) builder.setFooter(map.description);

                messages.channel.getGuild().getTextChannelById(mapsChannelID).sendFile(mapFile).addFile(imageFile.file()).embed(builder.build()).queue();

                messages.text("*Map posted successfully.*");
            }catch(Exception e){
                String err = Strings.neatError(e, true);
                int max = 900;
                e.printStackTrace();
                messages.err("Error parsing map.", err.length() < max ? err : err.substring(0, max));
                messages.deleteMessages();
            }
        });

        handler.register("google", "<phrase...>", "Let me google that for you.", args -> {
            try{
                messages.text("http://lmgtfy.com/?q=@", URLEncoder.encode(args[0], "UTF-8"));
            }catch(UnsupportedEncodingException e){
                e.printStackTrace();
            }
        });

        handler.register("cleanmod", "Clean up a modded zip archive. Changes json into hjson and formats code.", args -> {
            Message message = messages.lastMessage;

            if(message.getAttachments().size() != 1 || !message.getAttachments().get(0).getFileName().endsWith(".zip")){
                messages.err("You must have one .zip file in the same message as the command!");
                messages.deleteMessages();
                return;
            }

            Attachment a = message.getAttachments().get(0);

            if(a.getSize() > 1024 * 1024 * 6){
                messages.err("Zip files may be no more than 6 MB.");
                messages.deleteMessages();
            }

            try{
                new File("cache/").mkdir();
                File baseFile = new File("cache/" + a.getFileName());
                Fi destFolder = new Fi("cache/dest_mod" + a.getFileName());
                Fi destFile = new Fi("cache/" + new Fi(baseFile).nameWithoutExtension() + "-cleaned.zip");

                if(destFolder.exists()) destFolder.deleteDirectory();
                if(destFile.exists()) destFile.delete();

                Streams.copy(net.download(a.getUrl()), new FileOutputStream(baseFile));
                ZipFi zip = new ZipFi(new Fi(baseFile.getPath()));
                zip.walk(file -> {
                    Fi output = destFolder.child(file.extension().equals("json") ? file.pathWithoutExtension() + ".hjson" : file.path());
                    output.parent().mkdirs();

                    if(file.extension().equals("json") || file.extension().equals("hjson")){
                        output.writeString(fixJval(Jval.read(file.readString())).toString(Jformat.hjson));
                    }else{
                        file.copyTo(output);
                    }
                });

                try(OutputStream fos = destFile.write(false, 2048); ZipOutputStream zos = new ZipOutputStream(fos)){
                    for(Fi add : destFolder.findAll(f -> true)){
                        if(add.isDirectory()) continue;
                        zos.putNextEntry(new ZipEntry(add.path().substring(destFolder.path().length())));
                        Streams.copy(add.read(), zos);
                        zos.closeEntry();
                    }

                }

                messages.channel.sendFile(destFile.file()).queue();

                messages.text("*Mod converted successfully.*");
            }catch(Throwable e){
                e.printStackTrace();
                messages.err("Error parsing mod.", Strings.neatError(e, false));
                messages.deleteMessages();
            }
        });

        adminHandler.register("userinfo", "<@user>", "Get user info.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = messages.jda.retrieveUserById(l).complete();

                if(user == null){
                    messages.err("That user (ID @) is not in the cache. How did this happen?", l);
                }else{
                    Member member = messages.guild.retrieveMember(user).complete();

                    messages.info("Info for " + member.getEffectiveName(),
                        "Nickname: @\nUsername: @\nID: @\nStatus: @\nRoles: @\nIs Admin: @\nTime Joined: @",
                        member.getNickname(),
                        user.getName(),
                        member.getIdLong(),
                        member.getOnlineStatus(),
                        member.getRoles().stream().map(Role::getName).collect(Collectors.toList()),
                        isAdmin(user),
                        member.getTimeJoined()
                    );
                }
            }catch(Exception e){
                e.printStackTrace();
                messages.err("Incorrect name format or missing user.");
                messages.deleteMessages();
            }
        });

        adminHandler.register("warnings", "<@user>", "Get number of warnings a user has.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = messages.jda.retrieveUserById(l).complete();
                var list = getWarnings(user);
                messages.text("User '@' has **@** @.\n@", user.getName(), list.size, list.size == 1 ? "warning" : "warnings",
                list.map(s -> {
                    String[] split = s.split(":::");
                    long time = Long.parseLong(split[0]);
                    String warner = split.length > 1 ? split[1] : null, reason = split.length > 2 ? split[2] : null;
                    return "- `" + fmt.format(new Date(time)) + "`: Expires in " + (30-Duration.ofMillis((System.currentTimeMillis() - time)).toDays()) + " days" +
                    (warner == null ? "" : "\n  ↳ *From:* " + warner) +
                    (reason == null ? "" : "\n  ↳ *Reason:* " + reason);
                }).toString("\n"));
            }catch(Exception e){
                e.printStackTrace();
                messages.err("Incorrect name format.");
                messages.deleteMessages();
            }
        });

        adminHandler.register("delete", "<amount>", "Delete some messages.", args -> {
            try{
                int number = Integer.parseInt(args[0]) + 1;
                MessageHistory hist = messages.channel.getHistoryBefore(messages.lastMessage, number).complete();
                messages.channel.deleteMessages(hist.getRetrievedHistory()).queue();
                Log.info("Deleted @ messages.", number);
            }catch(NumberFormatException e){
                messages.err("Invalid number.");
            }
        });

        adminHandler.register("warn", "<@user> [reason...]", "Warn a user.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = messages.jda.retrieveUserById(l).complete();
                var list = getWarnings(user);
                list.add(System.currentTimeMillis() + ":::" + messages.lastUser.getName() + (args.length > 1 ? ":::" + args[1] : ""));
                messages.text("**@**, you've been warned *@*.", user.getAsMention(), warningStrings[Mathf.clamp(list.size - 1, 0, warningStrings.length - 1)]);
                prefs.putArray("warning-list-" + user.getIdLong(), list);
                if(list.size >= 3){
                    messages.lastMessage.getGuild().getTextChannelById(moderationChannelID)
                    .sendMessage("User " + user.getAsMention() + " has been warned 3 or more times!").queue();
                }
            }catch(Exception e){
                e.printStackTrace();
                messages.err("Incorrect name format.");
                messages.deleteMessages();
            }
        });

        adminHandler.register("clearwarnings", "<@user>", "Clear number of warnings for a person.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = messages.jda.retrieveUserById(l).complete();
                prefs.putArray("warning-list-" + user.getIdLong(), new Seq<>());
                messages.text("Cleared warnings for user '@'.", user.getName());
            }catch(Exception e){
                e.printStackTrace();
                messages.err("Incorrect name format.");
                messages.deleteMessages();
            }
        });
    }

    private Seq<String> getWarnings(User user){
        var list = prefs.getArray("warning-list-" + user.getIdLong());
        //remove invalid warnings
        list.removeAll(s -> {
            String[] split = s.split(":::");
            return Duration.ofMillis((System.currentTimeMillis() - Long.parseLong(split[0]))).toDays() >= 30;
        });

        return list;
    }

    private Jval fixJval(Jval val){
        if(val.isArray()){
            Seq<Jval> list = val.asArray().copy();
            for(Jval child : list){
                if(child.isObject() && (child.has("item")) && child.has("amount")){
                    val.asArray().remove(child);
                    val.asArray().add(Jval.valueOf(child.getString("item", child.getString("liquid", "")) + "/" + child.getInt("amount", 0)));
                }else{
                    fixJval(child);
                }
            }
        }else if(val.isObject()){
            Seq<String> keys = val.asObject().keys().toArray();

            for(String key : keys){
                Jval child = val.get(key);
                if(child.isObject() && (child.has("item")) && child.has("amount")){
                    val.remove(key);
                    val.add(key, Jval.valueOf(child.getString("item", child.getString("liquid", "")) + "/" + child.getInt("amount", 0)));
                }else{
                    fixJval(child);
                }
            }
        }

        return val;
    }

    boolean isAdmin(User user){
        var member = messages.guild.retrieveMember(user).complete();
        return member != null && member.getRoles().stream().anyMatch(role -> role.getName().equals("Developer") || role.getName().equals("Moderator") || role.getName().equals("\uD83D\uDD28 \uD83D\uDD75️\u200D♂️"));
    }

    boolean checkInvite(Message message){
        if(invitePattern.matcher(message.getContentRaw()).find() && !isAdmin(message.getAuthor()) && message.getChannel().getType() != ChannelType.PRIVATE){
            Log.warn("User @ just sent a discord invite in @.", message.getAuthor().getName(), message.getChannel().getName());
            message.delete().queue();
            message.getAuthor().openPrivateChannel().complete().sendMessage("Do not send invite links in the Mindustry Discord server! Read the rules.").queue();
            return true;
        }
        return false;
    }

    void checkContents(Message message){
        if(isAdmin(message.getAuthor())) return;

        if(checkInvite(message)){
            return;
        }

        if((message.getChannel().getIdLong() == screenshotsChannelID || message.getChannel().getIdLong() == artChannelID) && message.getAttachments().isEmpty()){
            message.delete().queue();
            try{
                message.getAuthor().openPrivateChannel().complete().sendMessage("Don't send messages without images in that channel.").queue();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    void edited(Message message){
        checkContents(message);
    }

    void handle(Message message){
        if(message.getAuthor().isBot() || message.getChannel().getType() != ChannelType.TEXT) return;

        messages.guild.getTextChannelById(logChannelID)
            .sendMessage((isAdmin(message.getAuthor()) ? message.getAuthor().getName() + "//" + message.getAuthor().getId() : message.getAuthor().getAsMention()) +
            " *in* " + message.getTextChannel().getAsMention() + ":\n\n" + message.getContentDisplay() + "\n").queue();

        checkContents(message);

        String text = message.getContentRaw();

        if(message.getContentRaw().startsWith(prefix)){
            messages.channel = message.getTextChannel();
            messages.lastUser = message.getAuthor();
            messages.lastMessage = message;
        }

        //schematic preview
        if((message.getContentRaw().startsWith(ContentHandler.schemHeader) && message.getAttachments().isEmpty()) ||
        (message.getAttachments().size() == 1 && message.getAttachments().get(0).getFileExtension() != null && message.getAttachments().get(0).getFileExtension().equals(Vars.schematicExtension))){
            try{
                Schematic schem = message.getAttachments().size() == 1 ? contentHandler.parseSchematicURL(message.getAttachments().get(0).getUrl()) : contentHandler.parseSchematic(message.getContentRaw());
                BufferedImage preview = contentHandler.previewSchematic(schem);
                String sname = schem.name().replace("/", "_").replace(" ", "_");
                if(sname.isEmpty()) sname = "empty";

                new File("cache").mkdir();
                File previewFile = new File("cache/img_" + UUID.randomUUID() + ".png");
                File schemFile = new File("cache/" + sname + "." + Vars.schematicExtension);
                Schematics.write(schem, new Fi(schemFile));
                ImageIO.write(preview, "png", previewFile);

                EmbedBuilder builder = new EmbedBuilder().setColor(messages.normalColor).setColor(messages.normalColor)
                .setImage("attachment://" + previewFile.getName())
                .setAuthor(message.getAuthor().getName(), message.getAuthor().getAvatarUrl(), message.getAuthor().getAvatarUrl()).setTitle(schem.name());

                if(!schem.description().isEmpty()) builder.setFooter(schem.description());

                StringBuilder field = new StringBuilder();

                for(ItemStack stack : schem.requirements()){
                    List<Emote> emotes = messages.guild.getEmotesByName(stack.item.name.replace("-", ""), true);
                    Emote result = emotes.isEmpty() ? messages.guild.getEmotesByName("ohno", true).get(0) : emotes.get(0);

                    field.append(result.getAsMention()).append(stack.amount).append("  ");
                }
                builder.addField("Requirements", field.toString(), false);

                message.getChannel().sendFile(schemFile).addFile(previewFile).embed(builder.build()).queue();
                message.delete().queue();
            }catch(Throwable e){
                if(message.getTextChannel().getIdLong() == schematicsChannelID || message.getTextChannel().getIdLong() == baseSchematicsChannelID){
                    message.delete().queue();
                    try{
                        message.getAuthor().openPrivateChannel().complete().sendMessage("Invalid schematic: " + e.getClass().getSimpleName() + (e.getMessage() == null ? "" : " (" + e.getMessage() + ")")).queue();
                    }catch(Exception e2){
                        e2.printStackTrace();
                    }
                }

                Log.err("Failed to parse schematic, skipping.");
                Log.err(e);
            }
        }else if((message.getTextChannel().getIdLong() == schematicsChannelID || message.getTextChannel().getIdLong() == schematicsChannelID) && !isAdmin(message.getAuthor())){
            message.delete().queue();
            try{
                message.getAuthor().openPrivateChannel().complete().sendMessage("Only send valid schematics in the #schematics channel. You may send them either as clipboard text or as a schematic file.").queue();
            }catch(Exception e){
                e.printStackTrace();
            }
            return;
        }

        if(!text.trim().equals("!")){
            if(isAdmin(message.getAuthor())){
                boolean unknown = handleResponse(adminHandler.handleMessage(text), false);
                handleResponse(handler.handleMessage(text), !unknown);
            }else{
                handleResponse(handler.handleMessage(text), true);
            }
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
                messages.err("Invalid arguments.", "Usage: @@", prefix, response.command.text);
                messages.deleteMessages();
            }else{
                messages.err("Invalid arguments.", "Usage: @@ *@*", prefix, response.command.text, response.command.paramText);
                messages.deleteMessages();
            }
            return false;
        }
        return true;
    }

}
