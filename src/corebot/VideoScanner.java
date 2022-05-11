package corebot;

import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import net.dv8tion.jda.api.*;

import java.time.format.*;
import java.util.Timer;
import java.util.*;

public class VideoScanner{
    private static final String api = "https://youtube.googleapis.com/youtube/v3/";
    private static final long updatePeriod = 1000 * 60 * 30;
    private static final String minChannelId = "UCR-XYZA9YQVhCIdkssqVNuQ", popularVideosPlaylist = "PLO5a8SnRwlbQ8zKnz_upUGlxQv9qF2Mxo";
    private static final String key = OS.env("GOOGLE_API_KEY");

    private final Fi seenfi = new Fi("videos.txt");
    private final ObjectSet<String> seen;

    public VideoScanner(){
        seen = Seq.with(seenfi.exists() ? seenfi.readString().split("\n") : new String[0]).asSet();

        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                try{
                    query("playlistItems",
                    StringMap.of(
                    "part", "snippet",
                    "playlistId", popularVideosPlaylist,
                    "maxResults", "25"
                    ), result -> {
                        var items = result.get("items");
                        for(var video : items.asArray()){
                            String id = video.get("snippet").get("resourceId").getString("videoId");
                            if(seen.add(id)){
                                newVideo(video.get("snippet"));
                            }
                        }
                    });

                    seenfi.writeString(seen.toSeq().toString("\n"));
                }catch(Exception e){
                    Log.err(e);
                }
            }
        }, 1000, updatePeriod);
    }

    void query(String url,StringMap params, Cons<Jval> cons){
        params.put("key", key);

        Http.get(api + url + "?" + params.keys().toSeq().map(entry -> Strings.encode(entry) + "=" + Strings.encode(params.get(entry))).toString("&"))
        .timeout(10000)
        .header("Accept", "application/json")
        .block(response -> cons.get(Jval.read(response.getResultAsString())));
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
            var desc = video.getString("description").replace("\\n", "\n");

            if(desc.length() > 200) desc = desc.substring(0, 200) + "...";

            CoreBot.messages.videosChannel
            .sendMessageEmbeds(
            new EmbedBuilder()
            .setTitle(video.getString("title"), videoUrl)
            .setColor(CoreBot.normalColor)
            .setAuthor(video.getString("videoOwnerChannelTitle"), videoUrl, avatar)
            .setImage(video.get("thumbnails").get("high").getString("url"))
            .setTimestamp(DateTimeFormatter.ISO_INSTANT.parse(video.getString("publishedAt")))
            .setFooter(desc)
            .build()).queue();
        }else{
            Log.warn("unable to get user with ID @", id);
        }
    }

}
