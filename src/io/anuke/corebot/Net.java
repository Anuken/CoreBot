package io.anuke.corebot;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import io.anuke.ucore.util.Log;
import org.apache.commons.io.IOUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Net {
    public static final int timeout = 2000;

    public Net(){
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            getChangelog(list -> {
                try {
                    VersionInfo latest = list.first();

                    int lastVersion = Integer.parseInt(CoreBot.prefs.get("lastBuild", "33"));

                    if(latest.build > lastVersion){
                        Log.info("Posting update!");

                        CoreBot.messages.sendUpdate(latest);

                        CoreBot.prefs.put("lastBuild", latest.build + "");
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }, Log::err);
        }, 60, 240, TimeUnit.SECONDS);
    }

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

    public void getChangelog(Consumer<Array<VersionInfo>> success, Consumer<Throwable> fail){
        try {
            URL url = new URL(CoreBot.releasesURL);
            URLConnection con = url.openConnection();
            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            String body = IOUtils.toString(in, encoding);

            Json j = new Json();
            Array<JsonValue> list = j.fromJson(null, body);
            Array<VersionInfo> out = new Array<>();
            for(JsonValue value : list){
                String name = value.getString("name");
                String description = value.getString("body").replace("\r", "");
                int id = value.getInt("id");
                int build = Integer.parseInt(value.getString("tag_name").substring(1));
                out.add(new VersionInfo(name, description, id, build));
            }
            success.accept(out);
        }catch (Exception e){
            fail.accept(e);
        }
    }

    public static class VersionInfo{
        public final String name, description;
        public final int id, build;

        public VersionInfo(String name, String description, int id, int build) {
            this.name = name;
            this.description = description;
            this.id = id;
            this.build = build;
        }

        @Override
        public String toString() {
            return "VersionInfo{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", id=" + id +
                    ", build=" + build +
                    '}';
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
