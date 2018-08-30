package io.anuke.corebot;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue.PrettyPrintSettings;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.sun.net.httpserver.HttpServer;
import io.anuke.ucore.util.Log;

import java.io.DataInputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;

import static java.lang.System.currentTimeMillis;

public class Reports{
    private static final long REQUEST_TIME = 1000 * 10;

    public Reports(){
        try{

            HashMap<String, Long> rateLimit = new HashMap<>();

            HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
            server.createContext("/report", t -> {
                String key = t.getRemoteAddress().getAddress().getHostName();
                if(rateLimit.get(key) != null && (currentTimeMillis() - rateLimit.get(key)) < REQUEST_TIME){
                    rateLimit.put(key, currentTimeMillis());
                    Log.err("Connection " + key + " is being rate limited!");
                    return;
                }

                rateLimit.put(key, currentTimeMillis());
                byte[] bytes = new byte[t.getRequestBody().available()];
                new DataInputStream(t.getRequestBody()).readFully(bytes);

                String message = new String(bytes);
                Json json = new Json();
                String result = json.prettyPrint(message, new PrettyPrintSettings(){{
                    outputType = OutputType.json;
                }});

                CoreBot.messages.getGuild().getChannelsByName(CoreBot.crashReportChannelName).get(0).sendMessage(result);

                t.sendResponseHeaders(200, 0);
            });
            server.setExecutor(null);
            server.start();
            Log.info("Crash reporting server up.");

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
