package io.anuke.corebot;

import com.jcraft.jorbis.Block;
import io.anuke.arc.collection.IntIntMap;
import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.util.Log;
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
    boolean[] hasEntities;
    ObjectIntMap<String> blockNames = new ObjectIntMap<>();
    String[] nameMap;

    public Maps(){
        try(DataInputStream stream = new DataInputStream(new InflaterInputStream(new FileInputStream("mapping.dat")))){
            teamColors = new int[stream.readByte()];
            for(int i = 0; i < teamColors.length; i++){
                teamColors[i] = stream.readInt();
            }

            blockColors = new int[stream.readUnsignedByte()];
            hasEntities = new boolean[blockColors.length];
            nameMap = new String[blockColors.length];
            for(int i = 0; i < blockColors.length; i++){
                int color = stream.readInt();
                hasEntities[i] = stream.readBoolean();
                String name = stream.readUTF();
                blockColors[i] = color;
                nameMap[i] = name;
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
            readMap(stream, map);
            return map;
        }
    }

    void readMap(DataInputStream input, Map map) throws IOException{
        int build = input.readInt();
        short width = input.readShort(), height = input.readShort();
        byte tagAmount = input.readByte();

        for(int i = 0; i < tagAmount; i++){
            String name = input.readUTF();
            String value = input.readUTF();
            map.tags.put(name, value);
        }

        int partID = blockNames.get("part", 0);

        if(width > 1024 || height > 1024) throw new IllegalArgumentException("Map size too large: " + width + " " + height);

        try(DataInputStream stream = new DataInputStream(new InflaterInputStream(input))){
            byte mapped = stream.readByte();
            IntIntMap mapping = new IntIntMap();

            for(int i = 0; i < mapped; i++){
                byte type = stream.readByte();
                short total = stream.readShort();

                for(int id = 0; id < total; id++){
                    String name = stream.readUTF();

                    //is block content type
                    if(type == 1){
                        mapping.put(id, blockNames.get(name, 0));
                    }
                }
            }

            map.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            Color tmp = new Color();

            //read floor and create tiles first
            for(int i = 0; i < width * height; i++){
                int x = i % width, y = i / width;
                int floorid = mapping.get(stream.readByte(), 0);
                int oreid = mapping.get(stream.readByte(), 0);
                int consecutives = stream.readUnsignedByte();
                int color = blockColors[oreid == 0 ? floorid : oreid];

                tmp.set(color);
                map.image.setRGB(x, height - 1 - y, Color.argb8888(tmp));

                for(int j = i + 1; j < i + 1 + consecutives; j++){
                    int newx = j % width, newy = j / width;
                    map.image.setRGB(newx, height - 1 - newy, Color.argb8888(tmp));
                }

                i += consecutives;
            }

            //read blocks
            for(int i = 0; i < width * height; i++){
                int x = i % width, y = i / width;
                int id = stream.readByte();
                if(id < 0) id += 256;
                int blockid = mapping.get(id, 0);

                if(blockid == partID){
                    Log.info("read part");
                    stream.readByte();
                }else if(hasEntities[blockid]){
                    byte tr = stream.readByte();
                    short health = stream.readShort();

                    byte team = Pack.leftByte(tr);
                    byte rotation = Pack.rightByte(tr);

                    tmp.set(teamColors[team]);
                    map.image.setRGB(x, height - 1 - y, Color.argb8888(tmp));
                }else{ //no entity/part, read consecutives
                    int consecutives = stream.readUnsignedByte();

                    for(int j = i + 1; j < i + 1 + consecutives; j++){
                        int newx = j % width, newy = j / width;

                        tmp.set(blockColors[blockid]);
                        if(blockid != 0) map.image.setRGB(newx, height - 1 - newy, Color.argb8888(tmp));
                    }

                    i += consecutives;
                }
            }
        }
    }

    public static class Map{
        public String name, author, description;
        public ObjectMap<String, String> tags = new ObjectMap<>();
        public BufferedImage image;
        public int version;
    }
}
