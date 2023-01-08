package corebot;

import arc.files.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Nullable;
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
import net.dv8tion.jda.api.events.message.react.*;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.requests.*;
import net.dv8tion.jda.api.utils.*;
import net.dv8tion.jda.api.utils.cache.*;
import org.jetbrains.annotations.*;

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
    private static final int scamAutobanLimit = 3, pingSpamLimit = 20, minModStars = 10, naughtyTimeoutMins = 20;
    private static final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
    private static final String[] warningStrings = {"once", "twice", "thrice", "too many times"};

    private static final String
    cyrillicFrom = "абсдефгнигклмпоркгзтюушхуз",
    cyrillicTo =   "abcdefghijklmnopqrstuvwxyz";

    // https://stackoverflow.com/a/48769624
    private static final Pattern urlPattern = Pattern.compile("(?:(?:https?):\\/\\/)?[\\w/\\-?=%.]+\\.[\\w/\\-&?=%.]+");
    private static final Set<String> trustedDomains = Set.of(
        "discord.com",
        "discord.co",
        "discord.gg",
        "discord.media",
        "discord.gift",
        "discordapp.com",
        "discordapp.net",
        "discordstatus.com"
    );

    //yes it's base64 encoded, I don't want any of these words typed here
    private static final Pattern badWordPattern = Pattern.compile(new String(Base64Coder.decode("KD88IVthLXpBLVpdKSg/OmN1bXxzZW1lbnxjb2NrfHB1c3N5fGN1bnR8bmlnZy5yKSg/IVthLXpBLVpdKQ==")));
    private static final Pattern notBadWordPattern = Pattern.compile("");
    private static final Pattern invitePattern = Pattern.compile("(discord\\.gg/\\w|discordapp\\.com/invite/\\w|discord\\.com/invite/\\w)");
    private static final Pattern linkPattern = Pattern.compile("http(s?)://");
    private static final Pattern notScamPattern = Pattern.compile("discord\\.py|discord\\.js|nitrome\\.com");
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
        "@everyone.*http",
        "http.*@everyone",
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
        "discord.*nitro.*steam.*get",
        "gift.*nitro.*http",
        "http.*discord.*gift",
        "discord.*nitro.*http",
        "personalize.*your*profile.*http",
        "nitro.*steam.*http",
        "steam.*nitro.*http",
        "nitro.*http.*d",
        "http.*d.*gift",
        "gift.*http.*d.*s",
        "discord.*steam.*http.*d",
        "nitro.*steam.*http",
        "steam.*nitro.*http",
        "dliscord.com",
        "free.*nitro.*http",
        "discord.*nitro.*http",
        "@everyone.*http",
        "http.*@everyone",
        "@everyone.*nitro",
        "nitro.*@everyone",
        "discord.*gi.*nitro"
    ));

    private final ObjectMap<String, UserData> userData = new ObjectMap<>();
    private final CommandHandler handler = new CommandHandler(prefix);
    private final CommandHandler adminHandler = new CommandHandler(prefix);
    private final JDA jda;

    public Guild guild;
    public TextChannel
    pluginChannel, crashReportChannel, announcementsChannel, artChannel,
    mapsChannel, moderationChannel, schematicsChannel, baseSchematicsChannel,
    logChannel, joinChannel, videosChannel, streamsChannel, testingChannel,
    alertsChannel, curatedSchematicsChannel, botsChannel;
    public Emote aaaaa;

    public Role modderRole;

    LongSeq schematicChannels = new LongSeq();

    public Messages(){
        String token = System.getenv("CORE_BOT_TOKEN");

        register();

        try{
            jda = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_REACTIONS)
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

        modderRole = guild.getRoleById(965691639811149865L);

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
        curatedSchematicsChannel = channel(878022862915653723L);
        botsChannel = channel(414179246693679124L);
        aaaaa = guild.getEmotesByName("alphaaaaaaaa", true).get(0);

        schematicChannels.add(schematicsChannel.getIdLong(), baseSchematicsChannel.getIdLong(), curatedSchematicsChannel.getIdLong());
    }

    void printCommands(CommandHandler handler, StringBuilder builder){
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
    }

    void register(){
        handler.<Message>register("help", "Displays all bot commands.", (args, msg) -> {
            StringBuilder builder = new StringBuilder();
            printCommands(handler, builder);
            info(msg.getChannel(), "Commands", builder.toString());
        });

        handler.<Message>register("ping", "<ip>", "Pings a server.", (args, msg) -> {
            if(msg.getChannel().getIdLong() != botsChannel.getIdLong()){
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
                infoDesc(msg.getChannel(), info.title, info.text);
            }catch(IllegalArgumentException e){
                errDelete(msg, "Error", "Invalid topic '@'.\nValid topics: *@*", args[0], Arrays.toString(Info.values()));
            }
        });


        handler.<Message>register("postplugin", "<user> <repository>", "Post a plugin via Github repository URL.", (args, msg) -> {
            // https://docs.github.com/en/rest/repos/repos#get-a-repository
            Http.get("https://api.github.com/repos/" + args[0] + "/" + args[1])
            .header("Accept", "application/vnd.github+json")
            .error(err -> errDelete(msg, "Error querying Github", Strings.getSimpleMessage(err)))
            .block(result -> {
                try{
                    Jval repo = Jval.read(result.getResultAsString());
                    String repoUrl = repo.getString("html_url");
                    Jval author = repo.get("owner");

                    EmbedBuilder builder = new EmbedBuilder()
                    .setColor(normalColor)
                    .setTitle(repo.getString("name"), repoUrl);

                    if(!repo.getString("description").isBlank()){
                        builder.addField("About", repo.getString("description"), false);
                    }

                    builder.addField("Downloads", repoUrl + "/releases", false);

                    pluginChannel.sendMessageEmbeds(builder.build()).queue();
                    text(msg, "*Plugin posted.*");
                }catch(Exception e){
                    errDelete(msg, "Failed to fetch plugin info from URL.");
                }
            });
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

                mapsChannel.sendFile(mapFile).addFile(imageFile.file()).setEmbeds(builder.build()).queue();

                text(msg, "*Map posted successfully.*");
            }catch(Exception e){
                String err = Strings.neatError(e, true);
                int max = 900;
                errDelete(msg, "Error parsing map.", err.length() < max ? err : err.substring(0, max));
            }
        });

        handler.<Message>register("verifymodder", "[user/repo]", "Verify yourself as a modder by showing a mod repository that you own. Invoke with no arguments for additional info.", (args, msg) -> {
            if(msg.getChannel().getIdLong() != botsChannel.getIdLong()){
                errDelete(msg, "Use this command in #bots.");
                return;
            }

            if(msg.getMember() == null){
                errDelete(msg, "Absolutely no ghosts allowed.");
                return;
            }

            String rawSearchString = (msg.getAuthor().getName() + "#" + msg.getAuthor().getDiscriminator());

            if(args.length == 0){
                info(msg.getChannel(), "Modder Verification", """
                To obtain the Modder role, you must do the following:
                
                1. Own a Github repository with the `mindustry-mod` tag.
                2. Have at least @ stars on the repository.
                3. Temporarily add your Discord `USERNAME#DISCRIMINATOR` (`@`) to the repository description or your user bio, to verify ownership.
                4. Run this command with the repository URL or `Username/Repo` as an argument.
                """, minModStars, rawSearchString);
            }else{
                if(msg.getMember().getRoles().stream().anyMatch(r -> r.equals(modderRole))){
                    errDelete(msg, "You already have that role.");
                    return;
                }

                String repo = args[0];
                int offset = "https://github.com/".length();
                if(repo.startsWith("https://") && repo.length() > offset + 1){
                    repo = repo.substring(offset);
                }

                Http.get("https://api.github.com/repos/" + repo)
                .header("Accept", "application/vnd.github.v3+json")
                .error(err -> errDelete(msg, "Error fetching repository (Did you type the name correctly?)", Strings.getSimpleMessage(err)))
                .block(res -> {
                    Jval val = Jval.read(res.getResultAsString());
                    String searchString = rawSearchString.toLowerCase(Locale.ROOT);

                    boolean contains = val.getString("description").toLowerCase(Locale.ROOT).contains(searchString);
                    boolean[] actualContains = {contains};

                    //check bio if not found
                    if(!contains){
                        Http.get(val.get("owner").getString("url"))
                        .error(Log::err) //why would this ever happen
                        .block(user -> {
                            Jval userVal = Jval.read(user.getResultAsString());
                            if(userVal.getString("bio", "").toLowerCase(Locale.ROOT).contains(searchString)){
                                actualContains[0] = true;
                            }
                        });
                    }

                    if(!val.get("topics").asArray().contains(j -> j.asString().contains("mindustry-mod"))){
                        errDelete(msg, "Unable to find `mindustry-mod` in the list of repository topics.\nAdd it in the topics section *(this can be edited next to the 'About' section)*.");
                        return;
                    }

                    if(!actualContains[0]){
                        errDelete(msg, "Unable to find your Discord username + discriminator in the repo description or owner bio.\n\nMake sure `" + rawSearchString + "` is written in one of these locations.");
                        return;
                    }

                    if(val.getInt("stargazers_count", 0) < minModStars){
                        errDelete(msg, "You need at least " + minModStars + " stars on your repository to get the Modder role.");
                        return;
                    }

                    guild.addRoleToMember(msg.getMember(), modderRole).queue();

                    info(msg.getChannel(), "Success!", "You have now obtained the Modder role.");
                });
            }
        });

        handler.<Message>register("google", "<phrase...>", "Let me google that for you.", (args, msg) -> {
            text(msg, "https://lmgt.org/?q=@", Strings.encode(args[0]));
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

                //merge with arc results
                Http.get("https://api.github.com/search/code?q=" +
                "filename:" + Strings.encode(args[0]) + "%20" +
                "repo:Anuken/Arc")
                .header("Accept", "application/vnd.github.v3+json")
                .block(arcResult -> {
                    Jval arcVal = Jval.read(arcResult.getResultAsString());

                    val.get("items").asArray().addAll(arcVal.get("items").asArray());
                    val.put("total_count", val.getInt("total_count", 0) + arcVal.getInt("total_count", 0));
                });

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

                msg.getChannel().sendMessageEmbeds(embed.build()).queue();
            });
        });


        handler.<Message>register("mywarnings", "Get information about your own warnings. Only usable in #bots.", (args, msg) -> {
            if(msg.getChannel().getIdLong() != botsChannel.getIdLong()){
                errDelete(msg, "Use this command in #bots.");
                return;
            }

            sendWarnings(msg, msg.getAuthor());
        });

        handler.<Message>register("avatar", "[@user]", "Get a user's full avatar.", (args, msg) -> {
            if(msg.getChannel().getIdLong() != botsChannel.getIdLong() && !isAdmin(msg.getAuthor())){
                errDelete(msg, "Use this command in #bots.");
                return;
            }

            try{
                User user;
                if(args.length > 0){
                    long id;
                    try{
                        id = Long.parseLong(args[0]);
                    }catch(NumberFormatException e){
                        String author = args[0].substring(2, args[0].length() - 1);
                        if(author.startsWith("!")) author = author.substring(1);
                        id = Long.parseLong(author);
                    }

                    user = jda.retrieveUserById(id).complete();
                }else{
                    user = msg.getAuthor();
                }

                //if(user.getIdLong() == 737869099811733527L){
                //    text(msg, "no");
                //}else
                if(user.getIdLong() == jda.getSelfUser().getIdLong() && Mathf.chance(0.5)){
                    msg.getChannel().sendMessage(aaaaa.getAsMention()).queue();
                }else{
                    String link = user.getEffectiveAvatarUrl() + "?size=1024";

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(normalColor);
                    embed.setTitle("Avatar: " + user.getName() + "#" + user.getDiscriminator());
                    embed.setImage(link);
                    embed.setDescription("[Link](" + link + ")");
                    embed.setFooter("Requested by " + msg.getAuthor().getName() + "#" + msg.getAuthor().getDiscriminator());
                    msg.getChannel().sendMessageEmbeds(embed.build()).queue();
                }

            }catch(Exception e){
                errDelete(msg, "Incorrect name format or ID.");
            }
        });

        adminHandler.<Message>register("adminhelp", "Displays all bot commands.", (args, msg) -> {
            StringBuilder builder = new StringBuilder();
            printCommands(adminHandler, builder);
            info(msg.getChannel(), "Admin Commands", builder.toString());
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
                sendWarnings(msg, user);
            }catch(Exception e){
                errDelete(msg, "Incorrect name format.");
            }
        });

        adminHandler.<Message>register("testemoji", "<ID>", "Send an emoji by ID.", (args, msg) -> {
            Emote emoji = null;

            try{
                emoji = guild.getEmoteById(args[0]);
            }catch(Exception ignored){
            }

            if(emoji == null){
                var emotes = guild.getEmotesByName(args[0], true);
                if(emotes.size() > 0){
                    emoji = emotes.get(0);
                }
            }

            if(emoji == null){
                errDelete(msg, "Emoji not found.");
            }else{
                msg.delete().queue();
                text(msg.getChannel(), emoji.getAsMention());
            }
        });

        adminHandler.<Message>register("delete", "<amount>", "Delete some messages.", (args, msg) -> {
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

        adminHandler.<Message>register("unwarn", "<@user> <index>", "Remove a warning.", (args, msg) -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                int index = Math.max(Integer.parseInt(args[1]), 1);
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();
                var list = getWarnings(user);
                if(list.size > index - 1){
                    list.remove(index - 1);
                    prefs.putArray("warning-list-" + user.getIdLong(), list);
                    text(msg, "Removed warning for user.");
                }else{
                    errDelete(msg, "Invalid index. @ > @", index, list.size);
                }
            }catch(Exception e){
                errDelete(msg, "Incorrect name/index format.");
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

        adminHandler.<Message>register("schemdesigner", "<add/remove> <@user>", "Make a user a verified schematic designer.", (args, msg) -> {
            String author = args[1].substring(2, args[1].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{

                var l = UserSnowflake.fromId(author);
                User user = jda.retrieveUserById(author).complete();
                boolean add = args[0].equals("add");
                if(add){
                    guild.addRoleToMember(l, guild.getRoleById(877171645427621889L)).queue();
                }else{
                    guild.removeRoleFromMember(l, guild.getRoleById(877171645427621889L)).queue();
                }

                text(msg, "**@** is @ a verified schematic designer.", user.getName(), add ? "now" : "no longer");
            }catch(Exception e){
                errDelete(msg, "Incorrect name format.");
            }
        });

        adminHandler.<Message>register("banid", "<id> [reason...]", "Ban a user by a raw numeric ID.", (args, msg) -> {
            try{
                long l = Long.parseLong(args[0]);
                User user = jda.retrieveUserById(l).complete();

                guild.ban(user, 0, args.length > 1 ? msg.getAuthor().getName() + " used banid: " + args[1] : msg.getAuthor().getName() + ": <no ban reason specified in command>").queue();
                text(msg, "Banned user: **@**", l);
            }catch(Exception e){
                errDelete(msg, "Incorrect name format, or user not found.");
            }
        });
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event){
        try{
            if(event.getUser() != null && event.getChannel().equals(mapsChannel) && event.getReactionEmote().isEmoji() && event.getReactionEmote().getEmoji().equals("❌")){
                event.getChannel().retrieveMessageById(event.getMessageIdLong()).queue(m -> {
                    try{
                        String baseUrl = event.retrieveUser().complete().getEffectiveAvatarUrl();

                        for(var embed : m.getEmbeds()){
                            if(embed.getAuthor() != null && embed.getAuthor().getIconUrl() != null && embed.getAuthor().getIconUrl().equals(baseUrl)){
                                m.delete().queue();
                                return;
                            }
                        }
                    }catch(Exception e){
                        Log.err(e);
                    }
                });
            }
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        try{

            var msg = event.getMessage();

            if(msg.getAuthor().isBot() || msg.getChannel().getType() != ChannelType.TEXT) return;

            if(msg.getMentionedUsers().contains(jda.getSelfUser())){
                msg.addReaction(aaaaa).queue();
            }

            EmbedBuilder log = new EmbedBuilder()
            .setAuthor(msg.getAuthor().getName(), msg.getAuthor().getEffectiveAvatarUrl(), msg.getAuthor().getEffectiveAvatarUrl())
            .setDescription(msg.getContentRaw().length() >= 2040 ? msg.getContentRaw().substring(0, 2040) + "..." : msg.getContentRaw())
            .addField("Author", msg.getAuthor().getAsMention(), false)
            .addField("Channel", msg.getTextChannel().getAsMention(), false)
            .setColor(normalColor);

            for(var attach : msg.getAttachments()){
                log.addField("File: " + attach.getFileName(), attach.getUrl(), false);
            }

            if(msg.getReferencedMessage() != null){
                log.addField("Replying to", msg.getReferencedMessage().getAuthor().getAsMention() + " [Jump](" + msg.getReferencedMessage().getJumpUrl() + ")", false);
            }

            if(msg.getMentionedUsers().stream().anyMatch(u -> u.getIdLong() == 123539225919488000L)){
                log.addField("Note", "thisisamention", false);
            }

            if(msg.getChannel().getIdLong() != testingChannel.getIdLong()){
                logChannel.sendMessageEmbeds(log.build()).queue();
            }

            //delete stray invites
            if(!isAdmin(msg.getAuthor()) && checkSpam(msg, false)){
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

                    msg.getChannel().sendFile(schemFile).addFile(previewFile).setEmbeds(builder.build()).queue();
                    msg.delete().queue();
                }catch(Throwable e){
                    if(schematicChannels.contains(msg.getChannel().getIdLong())){
                        msg.delete().queue();
                        try{
                            msg.getAuthor().openPrivateChannel().complete().sendMessage("Invalid schematic: " + e.getClass().getSimpleName() + (e.getMessage() == null ? "" : " (" + e.getMessage() + ")")).queue();
                        }catch(Exception e2){
                            e2.printStackTrace();
                        }
                    }
                    //ignore errors
                }
            }else if(schematicChannels.contains(msg.getChannel().getIdLong()) && !isAdmin(msg.getAuthor())){
                //delete non-schematics
                msg.delete().queue();
                try{
                    msg.getAuthor().openPrivateChannel().complete().sendMessage("Only send valid schematics in the #schematics channel. You may send them either as clipboard text or as a schematic file.").queue();
                }catch(Exception e){
                    e.printStackTrace();
                }
                return;
            }

            if(!text.replace(prefix, "").trim().isEmpty()){
                if(isAdmin(msg.getAuthor())){
                    boolean unknown = handleResponse(msg, adminHandler.handleMessage(text, msg), false);

                    handleResponse(msg, handler.handleMessage(text, msg), !unknown);
                }else{
                    handleResponse(msg, handler.handleMessage(text, msg), true);
                }
            }
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event){
        var msg = event.getMessage();

        if(isAdmin(msg.getAuthor()) || checkSpam(msg, true)){
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
        .sendMessageEmbeds(new EmbedBuilder()
            .setAuthor(event.getUser().getName(), event.getUser().getAvatarUrl(), event.getUser().getAvatarUrl())
            .addField("User", event.getUser().getAsMention(), false)
            .addField("ID", "`" + event.getUser().getId() + "`", false)
            .setColor(normalColor).build())
        .queue();
    }

    void sendWarnings(Message msg, User user){
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
    }

    public void text(MessageChannel channel, String text, Object... args){
        channel.sendMessage(Strings.format(text, args)).queue();
    }

    public void text(Message message, String text, Object... args){
        text(message.getChannel(), text, args);
    }

    public void info(MessageChannel channel, String title, String text, Object... args){
        channel.sendMessageEmbeds(new EmbedBuilder().addField(title, Strings.format(text, args), true).setColor(normalColor).build()).queue();
    }

    public void infoDesc(MessageChannel channel, String title, String text, Object... args){
        channel.sendMessageEmbeds(new EmbedBuilder().setTitle(title).setDescription(Strings.format(text, args)).setColor(normalColor).build()).queue();
    }

    /** Sends an error, deleting the base message and the error message after a delay. */
    public void errDelete(Message message, String text, Object... args){
        errDelete(message, "Error", text, args);
    }

    /** Sends an error, deleting the base message and the error message after a delay. */
    public void errDelete(Message message, String title, String text, Object... args){
        message.getChannel().sendMessageEmbeds(new EmbedBuilder()
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

    String replaceCyrillic(String in){
        StringBuilder out = new StringBuilder(in.length());
        for(int i = 0; i < in.length(); i++){
            char c = in.charAt(i);
            int index = cyrillicFrom.indexOf(c);
            if(index == -1){
                out.append(c);
            }else{
                out.append(cyrillicTo.charAt(index));
            }
        }
        return out.toString();
    }

    boolean checkSpam(Message message, boolean edit){

        if(message.getChannel().getType() != ChannelType.PRIVATE){
            Seq<String> mentioned =
                //ignore reply messages, bots don't use those
                message.getReferencedMessage() != null ? new Seq<>() :
                //get all mentioned members and roles in one list
                Seq.with(message.getMentionedMembers()).map(IMentionable::getAsMention).add(Seq.with(message.getMentionedRoles()).map(IMentionable::getAsMention));

            var data = data(message.getAuthor());
            String content = message.getContentStripped().toLowerCase(Locale.ROOT);

            //go through every ping individually
            for(var ping : mentioned){
                if(data.idsPinged.add(ping) && data.idsPinged.size >= pingSpamLimit){
                    String banMessage = "Banned for spamming member pings in a row. If you believe this was in error, file an issue on the CoreBot Github (https://github.com/Anuken/CoreBot/issues) or contact a moderator.";
                    Log.info("Autobanning @ for spamming @ pings in a row.", message.getAuthor().getName() + "#" + message.getAuthor().getId(), data.idsPinged.size);
                    alertsChannel.sendMessage(message.getAuthor().getAsMention() + " **has been auto-banned for pinging " + pingSpamLimit + " unique members in a row!**").queue();

                    Runnable banMember = () -> message.getGuild().ban(message.getAuthor(), 1, banMessage).queue();

                    try{
                        message.getAuthor().openPrivateChannel().complete().sendMessage(banMessage).queue(done -> banMember.run(), failed -> banMember.run());
                    }catch(Exception e){
                        //can fail to open PM channel sometimes.
                        banMember.run();
                    }
                }
            }

            if(mentioned.isEmpty()){
                data.idsPinged.clear();
            }

            //check for consecutive links
            if(!edit && linkPattern.matcher(content).find()){

                if(content.equals(data.lastLinkMessage) && !message.getChannel().getId().equals(data.lastLinkChannelId)){
                    Log.warn("User @ just spammed a link in @ (message: @): '@'", message.getAuthor().getName(), message.getChannel().getName(), message.getId(), content);

                    //only start deleting after 2 posts
                    if(data.linkCrossposts >= 1){
                        alertsChannel.sendMessage(
                            message.getAuthor().getAsMention() +
                            " **is spamming a link** in " + message.getTextChannel().getAsMention() +
                            ":\n\n" + message.getContentRaw()
                        ).queue();

                        message.delete().queue();
                        message.getAuthor().openPrivateChannel().complete().sendMessage("You have posted a link several times. Do not send any similar messages, or **you will be auto-banned.**").queue();
                    }

                    //4 posts = ban
                    if(data.linkCrossposts ++ >= 3){
                        Log.warn("User @ (@) has been auto-banned after spamming link messages.", message.getAuthor().getName(), message.getAuthor().getAsMention());

                        alertsChannel.sendMessage(message.getAuthor().getAsMention() + " **has been auto-banned for spam-posting links!**").queue();
                        message.getGuild().ban(message.getAuthor(), 1, "[Auto-Ban] Spam-posting links. If you are not a bot or spammer, please report this at https://github.com/Anuken/CoreBot/issues immediately!").queue();
                    }
                }

                data.lastLinkMessage = content;
                data.lastLinkChannelId = message.getChannel().getId();
            }else{
                data.linkCrossposts = 0;
                data.lastLinkMessage = null;
                data.lastLinkChannelId = null;
            }

            //zwj
            content = content.replaceAll("\u200B", "").replaceAll("\u200D", "");

            if(invitePattern.matcher(content).find()){
                Log.warn("User @ just sent a discord invite in @.", message.getAuthor().getName(), message.getChannel().getName());
                message.delete().queue();
                message.getAuthor().openPrivateChannel().complete().sendMessage("Do not send invite links in the Mindustry Discord server! Read the rules.").queue();
                return true;
            }else if((badWordPattern.matcher(content).find() || badWordPattern.matcher(replaceCyrillic(content)).find())){
                alertsChannel.sendMessage(
                    message.getAuthor().getAsMention() +
                    " **has sent a message with inaproppriate language** in " + message.getTextChannel().getAsMention() +
                    ":\n\n" + message.getContentRaw()
                ).queue();

                message.delete().queue();
                message.getAuthor().openPrivateChannel().complete().sendMessage("uou have been timed out for " + naughtyTimeoutMins +
                    " minutes for using an unacceptable word in `#" + message.getChannel().getName() + "`.\nYour message:\n\n" + message.getContentRaw()).queue();
                message.getMember().timeoutFor(Duration.ofMinutes(naughtyTimeoutMins)).queue();

                return true;
            }else if(containsScamLink(message)){
                Log.warn("User @ just sent a potential scam message in @: '@'", message.getAuthor().getName(), message.getChannel().getName(), message.getContentRaw());

                int count = data.scamMessages ++;

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
                    message.getGuild().ban(message.getAuthor(), 0, "[Auto-Ban] Posting several potential scam messages in a row. If you are not a bot or spammer, please report this at https://github.com/Anuken/CoreBot/issues immediately!").queue();
                }

                return true;
            }else{
                //non-consecutive scam messages don't count
                data.scamMessages = 0;
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
        }
        return true;
    }

    boolean containsScamLink(Message message){
        String content = message.getContentRaw().toLowerCase(Locale.ROOT);

        //some discord-related keywords are never scams (at least, not from bots)
        if(notScamPattern.matcher(content).find()){
            return false;
        }

        // Regular check
        if(scamPattern.matcher(content.replace("\n", " ")).find()){
            return true;
        }

        // Extracts the urls of the message
        List<String> urls = urlPattern.matcher(content).results().map(MatchResult::group).toList();

        for(String url : urls){
            // Gets the domain and splits its different parts
            String[] rawDomain = url
                    .replace("https://", "")
                    .replace("http://", "")
                    .split("/")[0]
                    .split("\\.");

            // Gets rid of the subdomains
            rawDomain = Arrays.copyOfRange(rawDomain, Math.max(rawDomain.length - 2, 0), rawDomain.length);

            // Re-assemble
            String domain = String.join(".", rawDomain);

            // Matches slightly altered links
            if(!trustedDomains.contains(domain) && trustedDomains.stream().anyMatch(genuine -> Strings.levenshtein(genuine, domain) <= 2)){
                return true;
            }
        }

        return false;
    }

    UserData data(User user){
        return userData.get(user.getId(), UserData::new);
    }

    static class UserData{
        /** consecutive scam messages sent */
        int scamMessages;
        /** last message that contained any link */
        @Nullable String lastLinkMessage;
        /** channel ID of last link posted */
        @Nullable String lastLinkChannelId;
        /** link cross-postings in a row */
        int linkCrossposts;
        /** all members pinged in consecutive messages */
        ObjectSet<String> idsPinged = new ObjectSet<>();
    }
}
