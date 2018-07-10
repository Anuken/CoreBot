package io.anuke.corebot;

import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Log;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

public class CrashReport {
    private static final String header = "--CRASH REPORT--";
    private static final String infoHeader = "--GAME INFO--";
    private static final String errorHeader = "--error getting additional info--";
    private static final String traceHeader = "----";

    public boolean valid = true;
    public boolean hasInfoError;

    public String os;
    public int version;
    public boolean netServer, netActive, multithreading;
    public String trace;
    public ObjectMap<String, String> values = new ObjectMap<>();

    public CrashReport(String text){
        try {
            Scanner scan = new Scanner(new ByteArrayInputStream(text.getBytes()));

            String line = scan.nextLine().trim();
            Log.info("Read line: {0}", line);
            if(line.equals(header)){
                scan.nextLine();
            }else if(!line.equals(infoHeader)){
                stop("Invalid header.");
            }

            while(true){
                String next = scan.nextLine();
                Log.info("Process " + next);
                if(next.equals(traceHeader) || next.equals(errorHeader)){
                    break;
                }
                values.put(next.substring(0, next.indexOf(':')), next.substring(next.indexOf(':') + 1));
            }

            StringBuilder builder = new StringBuilder();
            while(scan.hasNextLine()){
                builder.append(scan.nextLine());
            }

            trace = builder.toString();
        }catch (Exception e){
            e.printStackTrace();
            valid = false;
        }
    }

    private void stop(String text){
        throw new RuntimeException(text);
    }
}
