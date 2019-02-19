package io.anuke.corebot;

import io.anuke.arc.collection.IntIntMap;
import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.util.Pack;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

public class Maps{
    int[] teamColors;
    int[] blockColors;
    ObjectIntMap<String> blockNames = new ObjectIntMap<>();

    public Maps(){
        try(DataInputStream stream = new DataInputStream(new InflaterInputStream(new FileInputStream("mapping.dat")))){
            teamColors = new int[stream.readByte()];
            for(int i = 0; i < teamColors.length; i++){
                teamColors[i] = stream.readInt();
            }

            blockColors = new int[stream.readUnsignedByte()];
            for(int i = 0; i < blockColors.length; i++){
                int color = stream.readInt();
                String name = stream.readUTF();
                blockColors[i] = color;
                blockNames.put(name, i);
            }
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public Map parseMap(InputStream in) throws IOException{
        try(DataInputStream stream = new DataInputStream(in)){
            Map map = new Map();

            map.version = stream.readInt();
            byte tags = stream.readByte();
            for(int i = 0; i < tags; i++){
                map.tags.put(stream.readUTF(), stream.readUTF());
            }

            map.name = map.tags.get("name");
            map.author = map.tags.get("author");
            map.description = map.tags.get("description");

            IntIntMap mapping = new IntIntMap();

            short blocks = stream.readShort();
            for(int i = 0; i < blocks; i++){
                short id = stream.readShort();
                String name = stream.readUTF();
                mapping.put(id, blockNames.get(name, 0));
            }

            int width = stream.readInt();
            int height = stream.readInt();

            map.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            Color tmp = new Color();

            for(int x = 0; x < width; x ++){
                for(int y = 0; y < height; y ++){
                    int ground = mapping.get(stream.readByte(), 0), wall = mapping.get(stream.readByte(), 0), team = Pack.rightByte(stream.readByte()), unused1 = stream.readByte(), unused2 = stream.readByte();

                    int id = wall <= 1 ? ground : wall;
                    int color = blockColors[id];
                    tmp.set(color);
                    map.image.setRGB(x, y, Color.argb8888(tmp));
                }
            }

            return map;
        }
    }

    public static class Map{
        public String name, author, description;
        public ObjectMap<String, String> tags = new ObjectMap<>();
        public BufferedImage image;
        public int version;
    }
}
