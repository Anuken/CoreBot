package io.anuke.corebot;

import com.badlogic.gdx.utils.Base64Coder;
import io.anuke.ucore.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Net {
    public static final int timeout = 2000;

    public void pingServer(String ip, Consumer<PingResult> listener){
        AtomicBoolean sent = new AtomicBoolean();

        long[] start = {0};

        try{
            WebSocketClient[] clients = new WebSocketClient[1];
            clients[0] = new WebSocketClient(new URI("ws://" + ip + ":" + 6568)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    start[0] = System.currentTimeMillis();
                    clients[0].send("ping");
                    Log.info("Pinging!");
                }

                @Override
                public void onMessage(String message) {
                    synchronized (sent) {
                        byte[] bytes = Base64Coder.decode(message);


                        Log.info("Got ping packet");
                        if (sent.get()) return;
                        sent.set(true);
                        if(bytes.length != 128)
                            listener.accept(new PingResult(ip, System.currentTimeMillis() - start[0], "Unknown", "Unknown", "Unknown", "Unknown", "Outdated"));
                        else
                            listener.accept(readServerData(ByteBuffer.wrap(bytes), ip, System.currentTimeMillis() - start[0]));
                        clients[0].close();
                        Log.info("Finish get ping packet");
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Net.this.run(100, () -> {
                        synchronized (sent) {
                            if (sent.get()) return;
                            sent.set(true);
                            Log.info("Got close.");
                            listener.accept(new PingResult("Closed: " + reason));
                        }
                    });
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

            clients[0].setConnectionLostTimeout(timeout);
            clients[0].connect();

            run(timeout, () -> {
                synchronized (sent) {
                    Log.info("Got timeout.");

                    if (sent.get()) return;
                    sent.set(true);
                    listener.accept(new PingResult("Timed out."));
                    Log.info("Finish get timeout.");
                }
            });

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

    public void run(long delay, Runnable r){
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        r.run();
                    }
                },
                delay
        );
    }

    public PingResult readServerData(ByteBuffer buffer, String ip, long ping){
        byte hlength = buffer.get();
        byte[] hb = new byte[hlength];
        buffer.get(hb);

        byte mlength = buffer.get();
        byte[] mb = new byte[mlength];
        buffer.get(mb);

        String host = new String(hb);
        String map = new String(mb);

        int players = buffer.getInt();
        int wave = buffer.getInt();
        int version = buffer.getInt();

        return new PingResult(ip, ping, players + "", host, map, wave + "", version + "");
    }

    class PingResult{
        boolean valid;
        String players;
        String host;
        String error;
        String wave;
        String map;
        String ip;
        String version;
        long ping;

        public PingResult(String error) {
            this.valid = false;
            this.error = error;
        }

        public PingResult(String ip, long ping, String players, String host, String map, String wave, String version) {
            this.ping = ping;
            this.ip = ip;
            this.valid = true;
            this.players = players;
            this.host = host;
            this.map = map;
            this.wave = wave;
            this.version = version;
        }
    }
}
