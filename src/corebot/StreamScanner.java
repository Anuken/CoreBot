package corebot;

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
    private String token;

    public StreamScanner(){
        //clean up old file
        Fi.get("seen_" + (Time.millis() / seenCleanPeriod - 1) + ".txt").delete();

        seenIds = Seq.with(seen().exists() ? seen().readString().split("\n") : new String[0]).asSet();

        //periodically re-authorize
        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                Http.post("https://id.twitch.tv/oauth2/token?client_id=" + clientId + "&client_secret=" + clientSecret + "&grant_type=client_credentials").submit(result -> {
                    token = Jval.read(result.getResultAsString()).getString("access_token");
                });
            }
        }, 0, 1000 * 60 * 60); //once an hour

        //periodically refresh (with delay)
        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                if(token == null) return;

                try{
                    var list = request("https://api.twitch.tv/helix/streams?game_id=" + minId);

                    for(var stream : list.get("data").asArray()){
                        var instant = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(stream.getString("started_at")));
                        //only display streams that started a few minutes ago, so the thumbnail is correct
                        if(!Duration.between(instant, Instant.now()).minus(Duration.ofMinutes(startDelayMins)).isNegative() &&
                                seenIds.add(stream.getString("id"))){
                            newStream(stream);
                        }
                    }

                    seen().writeString(seenIds.toSeq().toString("\n"));
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

            //comedy
            if(stream.getString("title").contains("18")) return;

            CoreBot.messages.streamsChannel
            .sendMessageEmbeds(
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

        Http.get(url)
        .header("Client-Id", clientId)
        .header("Authorization", "Bearer " + token)
        .error(e -> { throw new RuntimeException(e); })
        .block(res -> val[0] = Jval.read(res.getResultAsString()));

        return val[0];
    }

    Fi seen(){
        return Fi.get("seen_" + (Time.millis() / seenCleanPeriod) + ".txt");
    }
}
