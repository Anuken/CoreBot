package io.anuke.corebot;

import io.anuke.ucore.function.Consumer;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

public class CrashReport {
    private static final String header = "--CRASH REPORT--";
    private static final String infoHeader = "--GAME INFO--";
    private static final String errorHeader = "--error getting additional info--";
    private static final String traceHeader = "----";

    public boolean valid;
    public boolean hasInfoError;

    public String os;
    public int version;
    public boolean netServer, netActive;
    public String trace;

    public CrashReport(String text){
        try {
            Scanner scan = new Scanner(new ByteArrayInputStream(text.getBytes()));

            String line = scan.nextLine();
            if(line.equals(header)){
                scan.nextLine();
            }else if(!line.equals(infoHeader)){
                stop("Invalid header.");
            }

            scanOr(scan, str -> version = Integer.parseInt(str.substring("Build: ".length())));
            scanOr(scan, str -> netActive = Boolean.parseBoolean(str.substring("Net Active: ".length())));
            scanOr(scan, str -> netServer = Boolean.parseBoolean(str.substring("Net Server: ".length())));
            scanOr(scan, str -> os = str.substring("OS: ".length()));
            scanOr(scan, str -> {
                if(!str.equals(traceHeader)) stop("Invalid stack trace header");
            });

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

    private void scanOr(Scanner scan, Consumer<String> cons){
        if(hasInfoError) return;
        String line = scan.nextLine();
        if(line.equals(errorHeader)){
            hasInfoError = true;
        }else{
            cons.accept(line);
        }
    }

    private void stop(String text){
        throw new RuntimeException(text);
    }
}
