package io.anuke.corebot;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

import static io.anuke.mindustry.Vars.content;

public class Maps{
    public static final byte[] header = {77, 83, 65, 86};

    public Maps(){
        Version.enabled = false;
        Vars.content = new ContentLoader();
        Vars.content.load();
        Vars.content.initialize(c -> { try{ c.init(); }catch(Throwable ignored){} });
        Vars.content.initialize(c -> { try{ c.load(); }catch(Throwable ignored){} });

        try{
            BufferedImage image = ImageIO.read(new File("../Mindustry/core/assets/sprites/block_colors.png"));

            for(Block block : Vars.content.blocks()){
                Color.argb8888ToColor(block.color, image.getRGB(block.id, 0));
                if(block instanceof OreBlock){
                    block.color.set(((OreBlock)block).itemDrop.color);
                }
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public Map parseMap(InputStream in) throws IOException{
        Map out = new Map();
        try(InflaterInputStream inf = new InflaterInputStream(in); DataInputStream stream = new DataInputStream(inf)){
            readHeader(stream);
            out.version = stream.readInt();
            readMap(stream, out);
        }
        return out;
    }

    void readMap(DataInputStream stream, Map out) throws IOException{
        //meta
        region(stream);
        StringMap map = new StringMap();
        short size = stream.readShort();
        for(int i = 0; i < size; i++){
            map.put(stream.readUTF(), stream.readUTF());
        }

        out.name = map.get("name", "Unknown");
        out.author = map.get("author");
        out.description = map.get("description");
        out.tags = map;

        //content
        region(stream);
        byte mapped = stream.readByte();

        MappableContent[][] cmap = new MappableContent[ContentType.values().length][0];

        for(int i = 0; i < mapped; i++){
            ContentType type = ContentType.values()[stream.readByte()];
            short total = stream.readShort();
            cmap[type.ordinal()] = new MappableContent[total];

            for(int j = 0; j < total; j++){
                String name = stream.readUTF();
                cmap[type.ordinal()][j] = content.getByName(type, name);
            }
        }

        //map
        region(stream);
        Mtile[][] tiles = readMapData(stream, cmap);

        BufferedImage img = new BufferedImage(tiles.length, tiles[0].length, BufferedImage.TYPE_INT_ARGB);
        for(int x = 0; x < img.getWidth(); x++){
            for(int y = 0; y < img.getHeight(); y++){
                Mtile tile = tiles[x][y];
                img.setRGB(x, img.getHeight() - 1 - y, Color.argb8888(Tmp.c1.set(MapIO.colorFor(tile.floor, tile.wall, tile.ore, Team.derelict))));
            }
        }

        out.image = img;
    }

    Mtile[][] readMapData(DataInputStream stream, MappableContent[][] cmap) throws IOException{
        int width = stream.readUnsignedShort();
        int height = stream.readUnsignedShort();

        Function<Short, Block> mapper = i -> (Block)cmap[ContentType.block.ordinal()][i];

        if(width * height > 800 * 800) throw new IllegalArgumentException("Map too large (800 * 800 tiles).");

        Mtile[][] tiles = new Mtile[width][height];

        //read floor and create tiles first
        for(int i = 0; i < width * height; i++){
            int x = i % width, y = i / width;
            short floorid = stream.readShort();
            short oreid = stream.readShort();
            int consecutives = stream.readUnsignedByte();

            tiles[x][y] = new Mtile(mapper.get(floorid), mapper.get(oreid), Blocks.air);

            for(int j = i + 1; j < i + 1 + consecutives; j++){
                int newx = j % width, newy = j / width;
                tiles[newx][newy] = new Mtile(mapper.get(floorid), mapper.get(oreid), Blocks.air);
            }

            i += consecutives;
        }

        //read blocks
        for(int i = 0; i < width * height; i++){
            int x = i % width, y = i / width;
            Block block = mapper.get(stream.readShort());
            Mtile tile = tiles[x][y];
            tile.wall = block;

            if(block.hasEntity()){
                int length = stream.readUnsignedShort();
                stream.skipBytes(length);
            }else{
                int consecutives = stream.readUnsignedByte();

                for(int j = i + 1; j < i + 1 + consecutives; j++){
                    int newx = j % width, newy = j / width;
                    tiles[newx][newy].wall = block;
                }

                i += consecutives;
            }
        }

        return tiles;
    }

    protected void region(DataInput stream) throws IOException{
        readChunk(stream, false);
    }

    protected int readChunk(DataInput input, boolean isByte) throws IOException{
        int length = isByte ? input.readUnsignedShort() : input.readInt();
        return length;
    }

    public static void readHeader(DataInput input) throws IOException{
        byte[] bytes = new byte[header.length];
        input.readFully(bytes);
        if(!Arrays.equals(bytes, header)){
            throw new IOException("Incorrect header! Expecting: " + Arrays.toString(header) + "; Actual: " + Arrays.toString(bytes));
        }
    }

    public static class Map{
        public String name, author, description;
        public ObjectMap<String, String> tags = new ObjectMap<>();
        public BufferedImage image;
        public int version;
    }

    protected static class Mtile{
        Block floor, ore, wall;

        public Mtile(Block floor, Block ore, Block wall){
            this.floor = floor;
            this.ore = ore;
            this.wall = wall;
        }
    }
}
