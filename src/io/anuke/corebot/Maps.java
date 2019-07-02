package io.anuke.corebot;

import io.anuke.arc.collection.ObjectMap;

import java.awt.image.BufferedImage;
import java.io.*;

public class Maps{

    public Maps(){

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
        //TODO
    }

    public static class Map{
        public String name, author, description;
        public ObjectMap<String, String> tags = new ObjectMap<>();
        public BufferedImage image;
        public int version;
    }
}
