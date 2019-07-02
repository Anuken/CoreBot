package io.anuke.corebot;

import io.anuke.arc.function.Consumer;
import io.anuke.arc.util.Log;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

public class ServerBridge{
    private static final int port = 6859;
    private static final int waitPeriod = 5000;

    private boolean connected;
    private ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(32);

    public void connect(Consumer<String> inputHandler){
        Thread thread = new Thread(() -> {
            while(true){
                try(Socket sock = new Socket()){
                    sock.connect(new InetSocketAddress("localhost", port));
                    Log.info("Connected to server.");
                    connected = true;
                    PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                    Thread readerThread = new Thread(() -> {
                        try{
                            String line;
                            while((line = in.readLine()) != null){
                                inputHandler.accept(line);
                            }
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    });
                    readerThread.setDaemon(true);
                    readerThread.start();

                    String send;
                    while(true){
                        send = queue.take();
                        Log.info("Sending command: {0}", send);
                        out.println(send);
                    }
                }catch(Exception ignored){}
                connected = false;

                try{
                    Thread.sleep(waitPeriod);
                }catch(InterruptedException ignored){}
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void send(String s){
        if(!connected){
            return;
        }

        try{
            queue.put(s);
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }
    }
}
