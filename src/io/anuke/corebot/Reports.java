package io.anuke.corebot;

import com.sun.net.httpserver.HttpServer;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.serialization.Json;
import io.anuke.arc.util.serialization.JsonValue;

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
                    Log.err("Connection " + key + " is being rate limited!");
                    return;
                }

                rateLimit.put(key, currentTimeMillis());
                byte[] bytes = new byte[t.getRequestBody().available()];
                new DataInputStream(t.getRequestBody()).readFully(bytes);

                String message = new String(bytes);
                Json json = new Json();
                JsonValue value = json.fromJson(null, message);
                int build = value.getInt("build");

                //only the latest build is processed, everything else is skipped
                if(build == CoreBot.net.getLastBuild()){
                    CoreBot.messages.sendCrash(value);
                }else{
                    Log.info("Rejecting report with invalid build: " + build);
                }

                Log.info("Recieved crash report.");

                t.sendResponseHeaders(200, 0);
            });
            server.setExecutor(null);
            server.start();
            Log.info("Crash reporting server up.");

        }catch(Exception e){
            Log.info("Error parsing report: ");
            e.printStackTrace();
        }
    }
}
