package io.anuke.corebot;

import com.sun.net.httpserver.HttpServer;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.serialization.Json;
import io.anuke.arc.util.serialization.JsonValue;

import java.io.DataInputStream;
import java.net.InetSocketAddress;

public class Reports{

    public Reports(){
        try{

            HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
            server.createContext("/report", t -> {
                byte[] bytes = new byte[t.getRequestBody().available()];
                new DataInputStream(t.getRequestBody()).readFully(bytes);

                String message = new String(bytes);
                Json json = new Json();
                JsonValue value = json.fromJson(null, message);
                int build = value.getInt("build");

                //only the latest build is processed, everything else is skipped
                if(build != -1){
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
