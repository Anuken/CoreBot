package io.anuke.corebot;

import io.anuke.ucore.util.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.WebSocketListener;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class Net {
    private WebSocketClient socket;
    private int timeout = 1100;

    public void pingServer(String ip, Consumer<PingResult> listener){
        boolean[] sent = {false};

        try{
            socket = new WebSocketClient(new URI("ws://" + ip + ":" + 6568)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    socket.send("_ping_");
                    Log.info("Pinging!");
                }

                @Override
                public void onMessage(String message) {
                    if(sent[0]) return;
                    if(message.startsWith("---")){
                        String[] split = message.substring(3).split("\\|");
                        listener.accept(new PingResult(split[0], split[1]));
                        sent[0] = true;
                        socket.close();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if(sent[0]) return;
                    Log.info("{0} {1} {2}", code, reason, remote);
                    listener.accept(new PingResult("Closed: " + reason));
                    sent[0] = true;
                }

                @Override
                public void onError(Exception ex) {
                    if(sent[0]) return;
                    if(ex instanceof IllegalArgumentException || ex instanceof UnknownHostException){
                        listener.accept(new PingResult("Invalid IP."));
                    }else if (ex instanceof ConnectException){
                        listener.accept(new PingResult("Connection refused."));
                    }else{
                        listener.accept(new PingResult(ex.getMessage()));
                    }

                    sent[0] = true;
                    ex.printStackTrace();
                }
            };

            socket.connect();

            new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if(!sent[0]){
                            listener.accept(new PingResult("Timed out."));
                            sent[0] = true;
                        }
                    }
                },
                timeout
            );

        }catch (Exception e){
            if(sent[0]) return;
            listener.accept(new PingResult("Timed out."));
            sent[0] = true;
            e.printStackTrace();
        }
    }

    class PingResult{
        boolean valid;
        String players;
        String host;
        String error;

        public PingResult(String error) {
            this.valid = false;
            this.error = error;
        }

        public PingResult(String players, String host) {
            this.valid = true;
            this.players = players;
            this.host = host;
        }
    }
}
