package corebot;

import arc.util.*;
import arc.util.serialization.*;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;

public class Reports{

    public Reports(){
        try{
            HttpServer server = HttpServer.create(new InetSocketAddress(6748), 0);
            server.createContext("/report", t -> {
                //who the HECK is this and why are they sending me the same crash report over and over
                if(t.getRemoteAddress().getAddress().getHostAddress().equals("221.229.196.229")) return;

                byte[] bytes = new byte[t.getRequestBody().available()];
                new DataInputStream(t.getRequestBody()).readFully(bytes);

                String message = new String(bytes);
                Json json = new Json();
                JsonValue value = json.fromJson(null, message);
                String build = value.getInt("build") + (value.getInt("revision") == 0 ? "" : "." + value.getInt("revision"));

                //custom builds and uninitialized builds (0) are skipped.
                if(build.equals(CoreBot.net.getLastBuild()) || String.valueOf(value.getInt("build")).equals(CoreBot.net.getLastBuild())){
                    JsonValue value1 = value;

                    StringBuilder builder = new StringBuilder();
                    value1 = value1.child;
                    while(value1 != null){
                        builder.append("**");
                        builder.append(value1.name);
                        builder.append("**");
                        builder.append(": ");
                        if(value1.name.equals("trace")){
                            builder.append("```xl\n"); //xl formatting looks nice
                            builder.append(value1.asString().replace("\\n", "\n").replace("\t", "  "));
                            builder.append("```");
                        }else{
                            builder.append(value1.asString());
                        }
                        builder.append("\n");
                        value1 = value1.next;
                    }
                    CoreBot.messages.crashReportChannel.sendMessage(builder.toString()).queue();
                }else{
                    Log.info("Rejecting report with invalid build: @. Current latest build is @.", build, CoreBot.net.getLastBuild());
                }

                Log.info("Recieved crash report.");

                t.sendResponseHeaders(200, 0);
            });
            server.setExecutor(null);
            server.start();
            Log.info("Crash reporting server up.");
        }catch(Exception e){
            Log.err("Error parsing report", e);
        }
    }
}
