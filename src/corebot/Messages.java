package corebot;

import arc.files.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.CommandHandler.*;
import arc.util.io.Streams;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.type.*;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.*;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.events.message.guild.*;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.requests.*;
import net.dv8tion.jda.api.utils.*;
import net.dv8tion.jda.api.utils.cache.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.*;
import java.util.zip.*;

import static corebot.CoreBot.*;

public class Messages extends ListenerAdapter{
    private static final String prefix = "!";
    private static final int scamAutobanLimit = 4;
    private static final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
    private static final String[] warningStrings = {"once", "twice", "thrice", "too many times"};
    private static final Pattern invitePattern = Pattern.compile("(discord\\.gg/\\w|discordapp\\.com/invite/\\w|discord\\.com/invite/\\w)");
    private static final Pattern scamPattern = Pattern.compile(String.join("|",
        "stea.*co.*\\.ru",
        "http.*stea.*c.*\\..*trad",
        "csgo.*kni[fv]e",
        "cs.?go.*inventory",
        "cs.?go.*cheat",
        "cheat.*cs.?go",
        "cs.?go.*skins",
        "skins.*cs.?go",
        "stea.*com.*partner",
        "скин.*partner",
        "steamcommutiny",
        "di.*\\.gift.*nitro",
        "http.*disc.*gift.*\\.",
        "free.*nitro.*http",
        "http.*free.*nitro.*",
        "nitro.*free.*http",
        "discord.*nitro.*free",
        "free.*discord.*nitro",
        "<@&391020510269669376>",
        "discordgivenitro",
        "http.*gift.*nitro",
        "http.*nitro.*gift",
        "http.*n.*gift",
        "бесплат.*нитро.*http",
        "нитро.*бесплат.*http",
        "nitro.*http.*disc.*nitro",
        "http.*click.*nitro",
        "http.*st.*nitro",
        "http.*nitro",
        "stea.*give.*nitro",
        "discord.*nitro.*steam.*get"
    ));

    private final ObjectIntMap<String> scamMessagesSent = new ObjectIntMap<>();
    private final CommandHandler handler = new CommandHandler(prefix);
    private final CommandHandler adminHandler = new CommandHandler(prefix);
    private final JDA jda;

    public Guild guild;
    public TextChannel
    pluginChannel, crashReportChannel, announcementsChannel, artChannel,
    mapsChannel, moderationChannel, schematicsChannel, baseSchematicsChannel,
    logChannel, joinChannel, videosChannel, streamsChannel, testingChannel,
    alertsChannel;

    public Messages(){
        String token = System.getenv("CORE_BOT_TOKEN");

        register();

        try{
            jda = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .setMemberCachePolicy(MemberCachePolicy.ALL).disableCache(CacheFlag.VOICE_STATE).build();
            jda.awaitReady();
            jda.addEventListener(this);
            
            loadChannels();

            Log.info("Discord bot up.");
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
    
    TextChannel channel(long id){
        return guild.getTextChannelById(id);
    }
    
    void loadChannels(){

        //all guilds and channels are loaded here for faster lookup
        guild = jda.getGuildById(391020510269669376L);
        pluginChannel = channel(617833229973717032L);
        crashReportChannel = channel(467033526018113546L);
        announcementsChannel = channel(391020997098340352L);
        artChannel = channel(754011833928515664L);
        mapsChannel = channel(416719902641225732L);
        moderationChannel = channel(488049830275579906L);
        schematicsChannel = channel(640604827344306207L);
        baseSchematicsChannel = channel(718536034127839252L);
        logChannel = channel(568416809964011531L);
        joinChannel = channel(832688792338038844L);
        streamsChannel = channel(833420066238103604L);
        videosChannel = channel(833826797048692747L);
        testingChannel = channel(432984286099144706L);
        alertsChannel = channel(864139464401223730L);
    }

    void register(){
        handler.<Message>register("help", "Displays all bot commands.", (args, msg) -> {
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

            info(msg.getChannel(), "Commands", builder.toString());
        });

        handler.<Message>register("ping", "<ip>", "Pings a server.", (args, msg) -> {
            if(!msg.getChannel().getName().equalsIgnoreCase("bots")){
                errDelete(msg, "Use this command in #bots.");
                return;
            }

            net.pingServer(args[0], result -> {
                if(result.name != null){
                    info(msg.getChannel(), "Server Online", "Host: @\nPlayers: @\nMap: @\nWave: @\nVersion: @\nPing: @ms",
                    Strings.stripColors(result.name), result.players, Strings.stripColors(result.mapname), result.wave, result.version, result.ping);
                }else{
                    errDelete(msg, "Server Offline", "Timed out.");
                }
            });
        });

        handler.<Message>register("info", "<topic>", "Displays information about a topic.", (args, msg) -> {
            try{
                Info info = Info.valueOf(args[0]);
                info(msg.getChannel(), info.title, info.text);
            }catch(IllegalArgumentException e){
                errDelete(msg, "Error", "Invalid topic '@'.\nValid topics: *@*", args[0], Arrays.toString(Info.values()));
            }
        });


        handler.<Message>register("postplugin", "<github-url>", "Post a plugin via Github repository URL.", (args, msg) -> {
            if(!args[0].startsWith("https") || !args[0].contains("github")){
                errDelete(msg, "That's not a valid Github URL.");
            }else{
                try{
                    Document doc = Jsoup.connect(args[0]).get();

                    EmbedBuilder builder = new EmbedBuilder().setColor(normalColor).
                    setColor(normalColor)
                    .setAuthor(msg.getAuthor().getName(), msg.getAuthor().getEffectiveAvatarUrl(), msg.getAuthor().getEffectiveAvatarUrl())
                    .setTitle(doc.select("strong[itemprop=name]").text());

                    Elements elem = doc.select("span[itemprop=about]");
                    if(!elem.isEmpty()){
                        builder.addField("About", elem.text(), false);
                    }

                    builder
                    .addField("Link", args[0], false)
                    .addField("Downloads", args[0] + (args[0].endsWith("/") ? "" : "/") + "releases", false);

                    pluginChannel.sendMessage(builder.build()).queue();

                    text(msg, "*Plugin posted.*");
                }catch(IOException e){
                    errDelete(msg, "Failed to fetch plugin info from URL.");
                }
            }
        });

        handler.<Message>register("postmap", "Post a .msav file to the #maps channel.", (args, msg) -> {

            if(msg.getAttachments().size() != 1 || !msg.getAttachments().get(0).getFileName().endsWith(".msav")){
                errDelete(msg, "You must have one .msav file in the same message as the command!");
                return;
            }

            Attachment a = msg.getAttachments().get(0);

            try{
                ContentHandler.Map map = contentHandler.readMap(net.download(a.getUrl()));
                new File("cache/").mkdir();
                File mapFile = new File("cache/" + a.getFileName());
                Fi imageFile = Fi.get("cache/image_" + a.getFileName().replace(".msav", ".png"));
                Streams.copy(net.download(a.getUrl()), new FileOutputStream(mapFile));
                ImageIO.write(map.image, "png", imageFile.file());

                EmbedBuilder builder = new EmbedBuilder().setColor(normalColor).setColor(normalColor)
                .setImage("attachment://" + imageFile.name())
                .setAuthor(msg.getAuthor().getName(), msg.getAuthor().getEffectiveAvatarUrl(), msg.getAuthor().getEffectiveAvatarUrl())
                .setTitle(map.name == null ? a.getFileName().replace(".msav", "") : map.name);

                if(map.description != null) builder.setFooter(map.description);

                mapsChannel.sendFile(mapFile).addFile(imageFile.file()).embed(builder.build()).queue();

                text(msg, "*Map posted successfully.*");
            }catch(Exception e){
                String err = Strings.neatError(e, true);
                int max = 900;
                errDelete(msg, "Error parsing map.", err.length() < max ? err : err.substring(0, max));
            }
        });

        handler.<Message>register("google", "<phrase...>", "Let me google that for you.", (args, msg) -> {
            text(msg, "http://lmgtfy.com/?q=@", Strings.encode(args[0]));
        });

        handler.<Message>register("cleanmod", "Clean up a modded zip archive. Changes json into hjson and formats code.", (args, msg) -> {

            if(msg.getAttachments().size() != 1 || !msg.getAttachments().get(0).getFileName().endsWith(".zip")){
                errDelete(msg, "You must have one .zip file in the same message as the command!");
                return;
            }

            Attachment a = msg.getAttachments().get(0);

            if(a.getSize() > 1024 * 1024 * 6){
                errDelete(msg, "Zip files may be no more than 6 MB.");
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

                msg.getChannel().sendFile(destFile.file()).queue();

                text(msg, "*Mod converted successfully.*");
            }catch(Throwable e){
                errDelete(msg, "Error parsing mod.", Strings.neatError(e, false));
            }
        });

        handler.<Message>register("file", "<filename...>", "Find a Mindustry source file by name", (args, msg) -> {
            //epic asynchronous code, I know
            Http.get("https://api.github.com/search/code?q=" +
            "filename:" + Strings.encode(args[0]) + "%20" +
            "repo:Anuken/Mindustry")
            .header("Accept", "application/vnd.github.v3+json")
            .error(err -> errDelete(msg, "Error querying Github", Strings.getSimpleMessage(err)))
            .block(result -> {
                msg.delete().queue();
                Jval val = Jval.read(result.getResultAsString());

                int count = val.getInt("total_count", 0);

                if(count > 0){
                    val.get("items").asArray().removeAll(j -> !j.getString("name").contains(args[0]));
                    count = val.get("items").asArray().size;
                }

                if(count == 0){
                    errDelete(msg, "No results found.");
                    return;
                }

                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(normalColor);
                embed.setAuthor(msg.getAuthor().getName() + ": Github Search Results", val.get("items").asArray().first().getString("html_url"), "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png");
                embed.setTitle("Github Search Results");

                if(count == 1){
                    Jval item = val.get("items").asArray().first();
                    embed.setTitle(item.getString("name"));
                    embed.setDescription("[View on Github](" + item.getString("html_url") + ")");
                }else{
                    int maxResult = 5, i = 0;
                    StringBuilder results = new StringBuilder();
                    for(Jval item : val.get("items").asArray()){
                        if(i++ > maxResult){
                            break;
                        }
                        results.append("[").append(item.getString("name")).append("]").append("(").append(item.getString("html_url")).append(")\n");
                    }

                    embed.setTitle((count > maxResult ? maxResult + "+" : count) + " Source Results");
                    embed.setDescription(results.toString());
                }

                msg.getChannel().sendMessage(embed.build()).queue();
            });
        });

        adminHandler.<Message>register("userinfo", "<@user>", "Get user info.", (args, msg) -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();

                if(user == null){
                    errDelete(msg, "That user (ID @) is not in the cache. How did this happen?", l);
                }else{
                    Member member = guild.retrieveMember(user).complete();

                    info(msg.getChannel(), "Info for " + member.getEffectiveName(),
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
                errDelete(msg, "Incorrect name format or missing user.");
            }
        });

        adminHandler.<Message>register("warnings", "<@user>", "Get number of warnings a user has.", (args, msg) -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();
                var list = getWarnings(user);
                text(msg, "User '@' has **@** @.\n@", user.getName(), list.size, list.size == 1 ? "warning" : "warnings",
                list.map(s -> {
                    String[] split = s.split(":::");
                    long time = Long.parseLong(split[0]);
                    String warner = split.length > 1 ? split[1] : null, reason = split.length > 2 ? split[2] : null;
                    return "- `" + fmt.format(new Date(time)) + "`: Expires in " + (warnExpireDays - Duration.ofMillis((System.currentTimeMillis() - time)).toDays()) + " days" +
                    (warner == null ? "" : "\n  ↳ *From:* " + warner) +
                    (reason == null ? "" : "\n  ↳ *Reason:* " + reason);
                }).toString("\n"));
            }catch(Exception e){
                errDelete(msg, "Incorrect name format.");
            }
        });

        adminHandler.<Message>register("delete", "<amount>", "Delete some ", (args, msg) -> {
            try{
                int number = Integer.parseInt(args[0]);
                MessageHistory hist = msg.getChannel().getHistoryBefore(msg, number).complete();
                msg.delete().queue();
                msg.getTextChannel().deleteMessages(hist.getRetrievedHistory()).queue();
            }catch(NumberFormatException e){
                errDelete(msg, "Invalid number.");
            }
        });

        adminHandler.<Message>register("warn", "<@user> [reason...]", "Warn a user.", (args, msg) -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();
                var list = getWarnings(user);
                list.add(System.currentTimeMillis() + ":::" + msg.getAuthor().getName() + (args.length > 1 ? ":::" + args[1] : ""));
                text(msg, "**@**, you've been warned *@*.", user.getAsMention(), warningStrings[Mathf.clamp(list.size - 1, 0, warningStrings.length - 1)]);
                prefs.putArray("warning-list-" + user.getIdLong(), list);
                if(list.size >= 3){
                    moderationChannel.sendMessage("User " + user.getAsMention() + " has been warned 3 or more times!").queue();
                }
            }catch(Exception e){
                errDelete(msg, "Incorrect name format.");
            }
        });

        adminHandler.<Message>register("clearwarnings", "<@user>", "Clear number of warnings for a person.", (args, msg) -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();
                prefs.putArray("warning-list-" + user.getIdLong(), new Seq<>());
                text(msg, "Cleared warnings for user '@'.", user.getName());
            }catch(Exception e){
                errDelete(msg, "Incorrect name format.");
            }
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        var msg = event.getMessage();

        if(msg.getAuthor().isBot() || msg.getChannel().getType() != ChannelType.TEXT) return;

        EmbedBuilder log = new EmbedBuilder()
        .setAuthor(msg.getAuthor().getName(), msg.getAuthor().getEffectiveAvatarUrl(), msg.getAuthor().getEffectiveAvatarUrl())
        .setDescription(msg.getContentRaw().length() >= 2040 ? msg.getContentRaw().substring(0, 2040) + "..." : msg.getContentRaw())
        .addField("Author", msg.getAuthor().getAsMention(), false)
        .addField("Channel", msg.getTextChannel().getAsMention(), false)
        .setColor(normalColor);

        if(msg.getReferencedMessage() != null){
            log.addField("Replying to", msg.getReferencedMessage().getAuthor().getAsMention() + " [Jump](" + msg.getReferencedMessage().getJumpUrl() + ")", false);
        }

        if(msg.getMentionedUsers().stream().anyMatch(u -> u.getIdLong() == 123539225919488000L)){
            log.addField("Note", "thisisamention", false);
        }

        if(msg.getChannel().getIdLong() != testingChannel.getIdLong()){
            logChannel.sendMessage(log.build()).queue();
        }

        Log.info(msg.getContentRaw().toLowerCase(Locale.ROOT));
        //delete stray invites
        if(!isAdmin(msg.getAuthor()) && checkInvite(msg)){
            return;
        }

        //delete non-art
        if(!isAdmin(msg.getAuthor()) && msg.getChannel().getIdLong() == artChannel.getIdLong() && msg.getAttachments().isEmpty()){
            msg.delete().queue();

            if(msg.getType() != MessageType.CHANNEL_PINNED_ADD){
                try{
                    msg.getAuthor().openPrivateChannel().complete().sendMessage("Don't send messages without images in that channel.").queue();
                }catch(Exception e1){
                    e1.printStackTrace();
                }
            }
        }

        String text = msg.getContentRaw();

        //schematic preview
        if((msg.getContentRaw().startsWith(ContentHandler.schemHeader) && msg.getAttachments().isEmpty()) ||
        (msg.getAttachments().size() == 1 && msg.getAttachments().get(0).getFileExtension() != null && msg.getAttachments().get(0).getFileExtension().equals(Vars.schematicExtension))){
            try{
                Schematic schem = msg.getAttachments().size() == 1 ? contentHandler.parseSchematicURL(msg.getAttachments().get(0).getUrl()) : contentHandler.parseSchematic(msg.getContentRaw());
                BufferedImage preview = contentHandler.previewSchematic(schem);
                String sname = schem.name().replace("/", "_").replace(" ", "_");
                if(sname.isEmpty()) sname = "empty";

                new File("cache").mkdir();
                File previewFile = new File("cache/img_" + UUID.randomUUID() + ".png");
                File schemFile = new File("cache/" + sname + "." + Vars.schematicExtension);
                Schematics.write(schem, new Fi(schemFile));
                ImageIO.write(preview, "png", previewFile);

                EmbedBuilder builder = new EmbedBuilder().setColor(normalColor).setColor(normalColor)
                .setImage("attachment://" + previewFile.getName())
                .setAuthor(msg.getAuthor().getName(), msg.getAuthor().getEffectiveAvatarUrl(), msg.getAuthor().getEffectiveAvatarUrl()).setTitle(schem.name());

                if(!schem.description().isEmpty()) builder.setFooter(schem.description());

                StringBuilder field = new StringBuilder();

                for(ItemStack stack : schem.requirements()){
                    List<Emote> emotes = guild.getEmotesByName(stack.item.name.replace("-", ""), true);
                    Emote result = emotes.isEmpty() ? guild.getEmotesByName("ohno", true).get(0) : emotes.get(0);

                    field.append(result.getAsMention()).append(stack.amount).append("  ");
                }
                builder.addField("Requirements", field.toString(), false);

                msg.getChannel().sendFile(schemFile).addFile(previewFile).embed(builder.build()).queue();
                msg.delete().queue();
            }catch(Throwable e){
                if(msg.getChannel().getIdLong() == schematicsChannel.getIdLong() || msg.getChannel().getIdLong() == baseSchematicsChannel.getIdLong()){
                    msg.delete().queue();
                    try{
                        msg.getAuthor().openPrivateChannel().complete().sendMessage("Invalid schematic: " + e.getClass().getSimpleName() + (e.getMessage() == null ? "" : " (" + e.getMessage() + ")")).queue();
                    }catch(Exception e2){
                        e2.printStackTrace();
                    }
                }
                //ignore errors
            }
        }else if((msg.getChannel().getIdLong() == schematicsChannel.getIdLong() || msg.getChannel().getIdLong() == baseSchematicsChannel.getIdLong()) && !isAdmin(msg.getAuthor())){
            //delete non-schematics
            msg.delete().queue();
            try{
                msg.getAuthor().openPrivateChannel().complete().sendMessage("Only send valid schematics in the #schematics channel. You may send them either as clipboard text or as a schematic file.").queue();
            }catch(Exception e){
                e.printStackTrace();
            }
            return;
        }

        if(!text.trim().equals("!")){
            if(isAdmin(msg.getAuthor())){
                boolean unknown = handleResponse(msg, adminHandler.handleMessage(text, msg), false);
                handleResponse(msg, handler.handleMessage(text, msg), !unknown);
            }else{
                handleResponse(msg, handler.handleMessage(text, msg), true);
            }
        }
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event){
        var msg = event.getMessage();

        if(isAdmin(msg.getAuthor()) || checkInvite(msg)){
            return;
        }

        if((msg.getChannel().getIdLong() == artChannel.getIdLong()) && msg.getAttachments().isEmpty()){
            msg.delete().queue();
            try{
                msg.getAuthor().openPrivateChannel().complete().sendMessage("Don't send messages without images in that channel.").queue();
            }catch(Exception e){
                e.printStackTrace();
            }
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

        joinChannel
        .sendMessage(new EmbedBuilder()
            .setAuthor(event.getUser().getName(), event.getUser().getAvatarUrl(), event.getUser().getAvatarUrl())
            .addField("User", event.getUser().getAsMention(), false)
            .addField("ID", "`" + event.getUser().getId() + "`", false)
            .setColor(normalColor).build())
        .queue();
    }

    public void text(MessageChannel channel, String text, Object... args){
        channel.sendMessage(Strings.format(text, args)).queue();
    }

    public void text(Message message, String text, Object... args){
        text(message.getChannel(), text, args);
    }

    public void info(MessageChannel channel, String title, String text, Object... args){
        channel.sendMessage(new EmbedBuilder().addField(title, Strings.format(text, args), true).setColor(normalColor).build()).queue();
    }

    /** Sends an error, deleting the base message and the error message after a delay. */
    public void errDelete(Message message, String text, Object... args){
        errDelete(message, "Error", text, args);
    }

    /** Sends an error, deleting the base message and the error message after a delay. */
    public void errDelete(Message message, String title, String text, Object... args){
        message.getChannel().sendMessage(new EmbedBuilder()
        .addField(title, Strings.format(text, args), true).setColor(errorColor).build())
        .queue(result -> result.delete().queueAfter(messageDeleteTime, TimeUnit.MILLISECONDS));

        //delete base message too
        message.delete().queueAfter(messageDeleteTime, TimeUnit.MILLISECONDS);
    }

    private Seq<String> getWarnings(User user){
        var list = prefs.getArray("warning-list-" + user.getIdLong());
        //remove invalid warnings
        list.removeAll(s -> {
            String[] split = s.split(":::");
            return Duration.ofMillis((System.currentTimeMillis() - Long.parseLong(split[0]))).toDays() >= warnExpireDays;
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
        var member = guild.retrieveMember(user).complete();
        return member != null && member.getRoles().stream().anyMatch(role -> role.getName().equals("Developer") || role.getName().equals("Moderator") || role.getName().equals("\uD83D\uDD28 \uD83D\uDD75️\u200D♂️"));
    }

    boolean checkInvite(Message message){

        if(message.getChannel().getType() != ChannelType.PRIVATE){
            if(invitePattern.matcher(message.getContentRaw()).find()){
                Log.warn("User @ just sent a discord invite in @.", message.getAuthor().getName(), message.getChannel().getName());
                message.delete().queue();
                message.getAuthor().openPrivateChannel().complete().sendMessage("Do not send invite links in the Mindustry Discord server! Read the rules.").queue();
                return true;
            }else if(scamPattern.matcher(message.getContentRaw().toLowerCase(Locale.ROOT)).find()){
                Log.warn("User @ just sent a potential scam message in @.", message.getAuthor().getName(), message.getChannel().getName());

                int count = scamMessagesSent.increment(message.getAuthor().getId());

                alertsChannel.sendMessage(
                    message.getAuthor().getAsMention() +
                    " **has sent a potential scam message** in " + message.getTextChannel().getAsMention() +
                    ":\n\n" + message.getContentRaw()
                ).queue();

                message.delete().queue();
                message.getAuthor().openPrivateChannel().complete().sendMessage("Your message has been flagged as a potential scam. Do not send any similar messages, or **you will be auto-banned.**").queue();

                if(count >= scamAutobanLimit - 1){
                    Log.warn("User @ (@) has been auto-banned after @ scam messages.", message.getAuthor().getName(), message.getAuthor().getAsMention(), count + 1);

                    alertsChannel.sendMessage(message.getAuthor().getAsMention() + " **has been auto-banned for posting " + scamAutobanLimit + " scam messages in a row!**").queue();

                    message.getGuild().ban(message.getAuthor(), 0, "Posting several potential scam messages in a row.").queue();
                }

                return true;
            }else if(scamMessagesSent.containsKey(message.getAuthor().getId())){
                //non-consecutive scam messages don't count
                scamMessagesSent.remove(message.getAuthor().getId(), 0);
            }

        }
        return false;
    }

    boolean handleResponse(Message msg, CommandResponse response, boolean logUnknown){
        if(response.type == ResponseType.unknownCommand){
            if(logUnknown){
                errDelete(msg, "Error", "Unknown command. Type !help for a list of commands.");
            }
            return false;
        }else if(response.type == ResponseType.manyArguments || response.type == ResponseType.fewArguments){
            if(response.command.params.length == 0){
                errDelete(msg, "Invalid arguments.", "Usage: @@", prefix, response.command.text);
            }else{
                errDelete(msg, "Invalid arguments.", "Usage: @@ *@*", prefix, response.command.text, response.command.paramText);
            }
            return false;
        }
        return true;
    }
}
