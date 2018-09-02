package io.anuke.corebot;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Strings;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Net {
    public static final int timeout = 2000;

    public Net(){
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            getChangelog(list -> {
                try {
                    VersionInfo latest = list.first();

                    int lastVersion = getLastBuild();

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

    public int getLastBuild(){
        return Integer.parseInt(CoreBot.prefs.get("lastBuild", "33"));
    }

    public String getText(String url){
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
            return IOUtils.toString(connection.getInputStream(), Charset.defaultCharset());
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void pingServer(String ip, Consumer<PingResult> listener){
        run(0, () -> {
            try{
                String resultIP = ip;
                int port = 6567;
                if(ip.contains(":") && Strings.canParsePostiveInt(ip.split(":")[1])){
                    resultIP = ip.split(":")[0];
                    port = Strings.parseInt(ip.split(":")[1]);
                }

                DatagramSocket socket = new DatagramSocket();
                socket.send(new DatagramPacket(new byte[]{-2, 1}, 2, InetAddress.getByName(resultIP), port));

                socket.setSoTimeout(2000);

                DatagramPacket packet = new DatagramPacket(new byte[128], 128);

                long start = System.currentTimeMillis();
                socket.receive(packet);

                ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
                listener.accept(readServerData(buffer, ip, System.currentTimeMillis() - start));
                socket.disconnect();
            }catch (Exception e){
                Log.info("Got send error:");
                listener.accept(new PingResult("Failed to connect."));
                e.printStackTrace();
            }
        });
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

        return new PingResult(ip, ping, players + "", host, map, wave + "", version == -1 ? "Custom Build" : ("v" + version));
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
