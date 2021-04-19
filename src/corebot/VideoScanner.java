package corebot;

import arc.*;
import arc.Net;
import arc.Net.*;
import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import net.dv8tion.jda.api.*;

import java.time.format.*;
import java.util.*;
import java.util.Timer;


public class VideoScanner{
    private static final String api = "https://youtube.googleapis.com/youtube/v3/";
    private static final long updatePeriod = 1000 * 60 * 60;
    private static final String minChannelId = "UCR-XYZA9YQVhCIdkssqVNuQ";
    private static final String key = OS.env("GOOGLE_API_KEY");

    private final Fi seenfi = new Fi("videos.txt");
    private final Net net = new Net();
    private final ObjectSet<String> seen;

    public VideoScanner(){
        net.setBlock(true);

        seen = Seq.with(seenfi.exists() ? seenfi.readString().split("\n") : new String[0]).asSet();

        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                try{
                    query("playlistItems",
                    StringMap.of(
                    "part", "snippet",
                    "playlistId", "PLO5a8SnRwlbQ8zKnz_upUGlxQv9qF2Mxo",
                    "maxResults", "25"
                    ), result -> {
                        var items = result.get("items");
                        for(var video : items.asArray()){
                            if(seen.add(video.getString("id"))){
                                newVideo(video.get("snippet"));
                            }
                        }
                    });

                    seenfi.writeString(seen.asArray().toString("\n"));
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }, 1000, updatePeriod);
    }

    void query(String url,StringMap params, Cons<Jval> cons){
        params.put("key", key);
        net.http(new HttpRequest()
        .timeout(10000)
        .header("Accept", "application/json")
        .method(HttpMethod.GET)
        .url(api + url + "?" + params.keys().toSeq().map(entry -> Strings.encode(entry) + "=" + Strings.encode(params.get(entry))).toString("&")), response -> {
            try{
                cons.get(Jval.read(response.getResultAsString()));
            }catch(Throwable error){
                Log.err(error);
            }
        }, Log::err);
    }

    void newVideo(Jval video){

        var id = video.getString("videoOwnerChannelId");
        Jval[] user = {null};

        query("channels", StringMap.of("part", "snippet", "id", id), result -> {
            var items = result.get("items").asArray();
            if(items.any()){
                user[0] = items.get(0);
            }
        });

        var videoUrl = "https://youtube.com/watch/?v=" + video.get("resourceId").get("videoId");

        if(user[0] != null){
            var avatar = user[0].get("snippet").get("thumbnails").get("default").getString("url");

            CoreBot.messages.guild.getTextChannelById(CoreBot.streamsChannelID)
            .sendMessage(
            new EmbedBuilder()
            .setTitle(video.getString("title"), "https://youtube.com/watch/?v=" + videoUrl)
            .setColor(CoreBot.normalColor)
            .setAuthor(video.getString("videoOwnerChannelTitle"), videoUrl, avatar)
            .setImage(video.get("thumbnails").getString("high"))
            .setTimestamp(DateTimeFormatter.ISO_INSTANT.parse(video.getString("publishedAt")))
            .build());
        }else{
            Log.warn("unable to get user with ID @", id);
        }
    }

}
