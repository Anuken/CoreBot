package io.anuke.corebot;

import io.anuke.ucore.util.CommandHandler;
import io.anuke.ucore.util.CommandHandler.Command;
import io.anuke.ucore.util.CommandHandler.Response;
import io.anuke.ucore.util.CommandHandler.ResponseType;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

import static io.anuke.corebot.CoreBot.*;

public class Commands {
    private final String prefix = "!";
    private CommandHandler handler = new CommandHandler(prefix);

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
                    messages.info("Server Online", "Host: {0}\nPlayers: {1}\nMap: {2}\nWave: {3}",
                            result.host, result.players, result.map, result.wave);
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

        handler.register("postmap", "<mapURL> <@author> <mapname> [description...]", "Post a map to the #maps channel. Mapper only command.", args -> {
            if(!messages.lastUser.hasRole(messages.channel.getGuild().getRolesByName("Mapper").get(0)) &&
                    !messages.lastUser.hasRole(messages.channel.getGuild().getRolesByName("Developer").get(0))){
                messages.err("You are unauthorized.");
            }

            String url = args[0];
            String author = args[1];
            String name = args[2];
            String desc = args.length < 4 ? "" : args[3];

            if(author.length() < 4){
                messages.err("Invalid author format. You must mention the author.");
                return;
            }

            String id = author.substring(2, author.length() - 1);
            try{
                long result = Long.parseLong(id);
                IUser user = messages.channel.getGuild().getUserByID(result);

                if(user == null){
                    messages.err("No such user.");
                    return;
                }

                EmbedBuilder builder = new EmbedBuilder().withColor(messages.normalColor).withColor(messages.normalColor)
                        .withImage(url).withAuthorName(user.getName()).withTitle(name)
                        .withAuthorIcon(user.getAvatarURL());

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
    }

    void handle(IMessage message){
        if(message.getContent() == null) return;

        String text = message.getContent();

        if(message.getContent() != null && message.getContent().startsWith(prefix)){
            messages.channel = message.getChannel();
            messages.lastUser = message.getAuthor();
        }

        Response response = handler.handleMessage(text);

        if(response.type == ResponseType.unknownCommand){
            messages.err("Unknown command. Type !help for a list of commands.");
        }else if(response.type == ResponseType.manyArguments || response.type == ResponseType.fewArguments){
            if(response.command.params.length == 0){
                messages.err("Invalid arguments.", "Usage: {0}{1}", prefix, response.command.text);
            }else {
                messages.err("Invalid arguments.", "Usage: {0}{1} *{2}*", prefix, response.command.text, response.command.paramText);
            }
        }
    }

}
