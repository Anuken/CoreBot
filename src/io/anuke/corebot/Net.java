package io.anuke.corebot;

import io.anuke.ucore.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Net {
    private static final int timeout = 1800;
    private WebSocketClient socket;

    public void pingServer(String ip, Consumer<PingResult> listener){
        AtomicBoolean sent = new AtomicBoolean();

        try{
            socket = new WebSocketClient(new URI("ws://" + ip + ":" + 6568)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    socket.send("_ping_");
                    Log.info("Pinging!");
                }

                @Override
                public void onMessage(String message) {
                    synchronized (sent) {
                        if (message.startsWith("---")) {
                            Log.info("Got ping packet");
                            if (sent.get()) return;
                            sent.set(true);
                            String[] split = message.substring(3).split("\\|");
                            listener.accept(split.length == 4 ?
                                    new PingResult(split[0], split[1], split[2], split[3]) :
                                    new PingResult(split[0], split[1], "Unknown", "Unknown"));
                            socket.close();
                            Log.info("Finish get ping packet");
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    new Timer().schedule(
                            new TimerTask() {
                                @Override
                                public void run() {
                                    synchronized (sent) {
                                        if (sent.get()) return;
                                        sent.set(true);
                                        Log.info("Got close.");
                                        listener.accept(new PingResult("Closed: " + reason));
                                    }
                                }
                            },
                            100
                    );
                }

                @Override
                public void onError(Exception ex) {
                    synchronized (sent) {
                        if (sent.get()) return;
                        sent.set(true);
                        Log.info("Got error:");
                        if (ex instanceof IllegalArgumentException || ex instanceof UnknownHostException) {
                            listener.accept(new PingResult("Invalid IP."));
                        } else if (ex instanceof ConnectException) {
                            listener.accept(new PingResult("Connection refused."));
                        } else {
                            listener.accept(new PingResult(ex.getMessage()));
                        }

                        ex.printStackTrace();
                    }
                }
            };
            socket.setConnectionLostTimeout(timeout);

            socket.connect();

            new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (sent) {
                            Log.info("Got timeout.");
                            if (sent.get()) {
                                sent.set(true);
                                listener.accept(new PingResult("Timed out."));
                                Log.info("Finish get timeout.");
                            }
                        }
                    }
                },
                timeout
            );

        }catch (Exception e){
            synchronized (sent) {
                if (sent.get()) return;
                sent.set(true);
                Log.info("Got send error:");
                listener.accept(new PingResult("Timed out."));
                e.printStackTrace();
            }
        }
    }

    class PingResult{
        boolean valid;
        String players;
        String host;
        String error;
        String wave;
        String map;

        public PingResult(String error) {
            this.valid = false;
            this.error = error;
        }

        public PingResult(String players, String host, String map, String wave) {
            this.valid = true;
            this.players = players;
            this.host = host;
            this.map = map;
            this.wave = wave;
        }
    }
}
