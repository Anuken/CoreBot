package corebot;

import arc.*;
import arc.Net.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import net.dv8tion.jda.api.*;

import java.time.*;
import java.time.format.*;
import java.util.Timer;
import java.util.*;

public class StreamScanner{
    private static final long updatePeriod = 1000 * 60 * 3, seenCleanPeriod = 1000 * 60 * 60 * 24 * 2, startDelayMins = 15;
    private static final String minId = "502103", testId = "31376", clientId = "worwycsp7vvr6049q2f88l1cj1jj1i", clientSecret = OS.env("TWITCH_SECRET");

    private ObjectSet<String> seenIds;
    private Net net = new Net();
    private String token;

    public StreamScanner(){
        //clean up old file
        Fi.get("seen_" + (Time.millis() / seenCleanPeriod - 1) + ".txt").delete();

        seenIds = Seq.with(seen().exists() ? seen().readString().split("\n") : new String[0]).asSet();

        //periodically re-authorize
        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                net.http(new HttpRequest().method(HttpMethod.POST).url("https://id.twitch.tv/oauth2/token?client_id=" + clientId + "&client_secret=" + clientSecret + "&grant_type=client_credentials"), result -> {
                    try{
                        if(result.getStatus() == HttpStatus.OK){
                            Log.info("Authenticated with Twitch.");
                            token = Jval.read(result.getResultAsString()).getString("access_token");
                        }else{
                            Log.err("Failed to authorize: @", result.getStatus().toString());
                        }
                    }catch(Exception e){
                        Log.err(e);
                    }
                }, Log::err);
            }
        }, 0, 1000 * 60 * 60); //once an hour

        //periodically refresh (with delay)
        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                if(token == null) return;

                try{
                    var list = request("https://api.twitch.tv/helix/streams?game_id=" + testId); //TODO use min id

                    for(var stream : list.get("data").asArray()){
                        var instant = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(stream.getString("started_at")));
                        //only display streams that started a few minutes ago, so the thumbnail is correct
                        //if(!Duration.between(instant, Instant.now()).minus(Duration.ofMinutes(startDelayMins)).isNegative() &&
                        //seenIds.add(stream.getString("id"))){
                        newStream(stream);
                        //}
                    }

                    seen().writeString(seenIds.asArray().toString("\n"));
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }, 5000, updatePeriod);
    }

    void newStream(Jval stream){
        Jval users = request("https://api.twitch.tv/helix/users?id=" + stream.getString("user_id")).get("data");

        if(users.asArray().size > 0){
            var avatar = users.asArray().first().getString("profile_image_url");

            CoreBot.messages.guild.getTextChannelById(CoreBot.testingChannelID) //TODO switch to stream channel ID
            .sendMessage(
            new EmbedBuilder()
            .setTitle(stream.getString("title"), "https://twitch.tv/" + stream.getString("user_login"))
            .setColor(CoreBot.normalColor)
            .setAuthor(stream.getString("user_name"), "https://twitch.tv/" + stream.getString("user_login"), avatar)
            .setImage(stream.getString("thumbnail_url").replace("{width}", "390").replace("{height}", "220"))
            .setTimestamp(DateTimeFormatter.ISO_INSTANT.parse(stream.getString("started_at")))
            .build()).queue();
        }
    }

    Jval request(String url){
        Jval[] val = {null};

        net.http(
            new HttpRequest().block(true)
            .method(HttpMethod.GET)
            .header("Client-Id", clientId)
            .header("Authorization", "Bearer " + token)
            .url(url),
            res -> val[0] = Jval.read(res.getResultAsString()),
            e -> { throw new RuntimeException(e); });

        return val[0];
    }

    Fi seen(){
        return Fi.get("seen_" + (Time.millis() / seenCleanPeriod) + ".txt");
    }
}
