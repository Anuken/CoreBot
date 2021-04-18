package corebot;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import com.github.twitch4j.helix.*;
import com.github.twitch4j.helix.domain.*;
import net.dv8tion.jda.api.*;

import java.util.*;
import java.util.Timer;
import java.util.concurrent.*;

public class StreamScanner{
    private static final long updatePeriod = 1000 * 5, seenCleanPeriod = 1000 * 60 * 60 * 24 * 2;
    private static final String minId = "502103", testId = "31376";

    private ObjectSet<String> seenIds;

    public StreamScanner(){
        //clean up old file
        Fi.get("seen_" + (Time.millis() / seenCleanPeriod - 1) + ".txt").delete();

        TwitchHelix client = TwitchHelixBuilder.builder()
        .withClientId("worwycsp7vvr6049q2f88l1cj1jj1i")
        .withClientSecret(OS.env("TWITCH_SECRET"))
        .build();

        seenIds = Seq.with(seen().exists() ? seen().readString().split("\n") : new String[0]).asSet();

        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                try{
                    var list = client.getStreams(null, null, null, null, List.of(testId), null, null, null).execute();

                    for(var stream : list.getStreams()){
                        if(seenIds.add(stream.getId())){
                            newStream(stream);
                        }
                    }

                    seen().writeString(seenIds.asArray().toString("\n"));
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }, 5000, updatePeriod);
    }

    void newStream(Stream stream){

        CoreBot.messages.guild.getTextChannelById(CoreBot.testingChannelID)
            .sendMessage(
            new EmbedBuilder()
            .setTitle(stream.getTitle(), "https://twitch.tv/" + stream.getUserLogin())
            .setColor(CoreBot.normalColor)
            .setAuthor(stream.getUserName(), "https://twitch.tv/" + stream.getUserLogin())
            .setImage(stream.getThumbnailUrl(390, 200))
            .build()).queue();
    }

    Fi seen(){
        return Fi.get("seen_" + (Time.millis() / seenCleanPeriod) + ".txt");
    }

    //public static void main(String[] args){
        //new StreamScanner();
    //}
}
