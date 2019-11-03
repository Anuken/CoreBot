package io.anuke.corebot;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.func.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Pixmap.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.g2d.TextureAtlas.*;
import io.anuke.arc.graphics.g2d.TextureAtlas.TextureAtlasData.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.ctype.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

import javax.imageio.*;
import java.awt.image.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.geom.*;

import static io.anuke.mindustry.Vars.*;

public class ContentHandler{
    public static final byte[] mapHeader = {77, 83, 65, 86};
    public static final String schemHeader = "bXNjaAB";

    Graphics2D currentGraphics;
    BufferedImage currentImage;

    public ContentHandler(){
        Version.enabled = false;
        Vars.content = new ContentLoader();
        Vars.content.createContent();
        for(ContentType type : ContentType.values()){
            for(Content content : Vars.content.getBy(type)){
                try{
                    content.init();
                }catch(Throwable ignored){
                }
            }
        }

        String assets = "../Mindustry/core/assets/";

        TextureAtlasData data = new TextureAtlasData(new FileHandle(assets + "sprites/sprites.atlas"), new FileHandle(assets + "sprites"), false);
        Core.atlas = new TextureAtlas();

        ObjectMap<Page, BufferedImage> images = new ObjectMap<>();
        ObjectMap<String, BufferedImage> regions = new ObjectMap<>();

        data.getPages().each(page -> {
            try{
                BufferedImage image = ImageIO.read(page.textureFile.file());
                images.put(page, image);
                page.texture = Texture.createEmpty(new ImageData(image));
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        });

        data.getRegions().each(reg -> {
            try{
                BufferedImage image = new BufferedImage(reg.width, reg.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();

                graphics.drawImage(images.get(reg.page), 0, 0, reg.width, reg.height, reg.left, reg.top, reg.left + reg.width, reg.top + reg.height, null);

                ImageRegion region = new ImageRegion(reg.name, reg.page.texture, reg.left, reg.top, image);
                Core.atlas.addRegion(region.name, region);
                regions.put(region.name, image);

               // ImageIO.write(image, "png", new File("out/" + reg.name + ".png"));
            }catch(Exception e){
                e.printStackTrace();
            }
        });

        Core.atlas.setErrorRegion("error");
        Draw.scl = 1f / 4f;
        Core.batch = new SpriteBatch(null){
            @Override
            protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
                x += 4;
                y += 4;

                x *= 4;
                y *= 4;
                width *= 4;
                height *= 4;

                y = currentImage.getHeight() - (y + height/2f) - height/2f;

                AffineTransform at = new AffineTransform();
                at.translate(x, y);
                at.rotate(-rotation * Mathf.degRad, originX * 4, originY * 4);

                currentGraphics.setTransform(at);
                BufferedImage image = regions.get(((AtlasRegion)region).name);
                if(!color.equals(Color.white)){
                    image = tint(image, color);
                }

                currentGraphics.drawImage(image, 0, 0, (int)width, (int)height, null);
            }
        };

        for(ContentType type : ContentType.values()){
            for(Content content : Vars.content.getBy(type)){
                try{
                    content.load();
                }catch(Throwable e){
                    e.printStackTrace();
                }
            }
        }

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

        world = new World(){

            public Tile tile(int x, int y){
                return new Tile(x, y);
            }
        };

        /*
        try{
            Schematic schem = Schematics.read(new FileHandle(new File("test.msch")));
            ImageIO.write(previewSchematic(schem), "png", new File("out.png"));
        }catch(Exception e){
            e.printStackTrace();
        }
        */

    }

    private BufferedImage tint(BufferedImage image, Color color){
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Color tmp = new Color();
        for(int x = 0; x < copy.getWidth(); x++){
            for(int y = 0; y < copy.getHeight(); y++){
                int argb = image.getRGB(x, y);
                Color.argb8888ToColor(tmp, argb);
                tmp.mul(color);
                copy.setRGB(x, y, Color.argb8888(tmp));
            }
        }
        return copy;
    }

    public Schematic parseSchematic(String text) throws Exception{
        return Schematics.read(new ByteArrayInputStream(Base64Coder.decode(text)));
    }

    public Schematic parseSchematicURL(String text) throws Exception{
        return Schematics.read(CoreBot.net.download(text));
    }

    public BufferedImage previewSchematic(Schematic schem) throws Exception{
        BufferedImage image = new BufferedImage(schem.width * 32, schem.height * 32, BufferedImage.TYPE_INT_ARGB);

        Array<BuildRequest> requests = schem.tiles.map(t -> new BuildRequest(t.x, t.y, t.rotation, t.block).configure(t.config));
        currentGraphics = image.createGraphics();
        currentImage = image;
        requests.each(req -> {
            req.animScale = 1f;
            req.worldContext = false;
            req.block.drawRequestRegion(req, requests::each);
        });

        requests.each(req -> req.block.drawRequestConfigTop(req, requests::each));
        ImageIO.write(image, "png", new File("out.png"));

        return image;
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

        Func<Short, Block> mapper = i -> (Block)cmap[ContentType.block.ordinal()][i];

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
        return isByte ? input.readUnsignedShort() : input.readInt();
    }

    public static void readHeader(DataInput input) throws IOException{
        byte[] bytes = new byte[mapHeader.length];
        input.readFully(bytes);
        if(!Arrays.equals(bytes, mapHeader)){
            throw new IOException("Incorrect header! Expecting: " + Arrays.toString(mapHeader) + "; Actual: " + Arrays.toString(bytes));
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

    static class ImageData implements TextureData{
        final BufferedImage image;

        public ImageData(BufferedImage image){
            this.image = image;
        }

        @Override
        public TextureDataType getType(){
            return TextureDataType.Custom;
        }

        @Override
        public boolean isPrepared(){
            return false;
        }

        @Override
        public void prepare(){

        }

        @Override
        public Pixmap consumePixmap(){
            return null;
        }

        @Override
        public boolean disposePixmap(){
            return false;
        }

        @Override
        public void consumeCustomData(int target){

        }

        @Override
        public int getWidth(){
            return image.getWidth();
        }

        @Override
        public int getHeight(){
            return image.getHeight();
        }

        @Override
        public Format getFormat(){
            return Format.RGBA8888;
        }

        @Override
        public boolean useMipMaps(){
            return false;
        }

        @Override
        public boolean isManaged(){
            return false;
        }
    }

    static class ImageRegion extends AtlasRegion{
        final BufferedImage image;
        final int x, y;

        public ImageRegion(String name, Texture texture, int x, int y, BufferedImage image){
            super(texture, x, y, image.getWidth(), image.getHeight());
            this.name = name;
            this.image = image;
            this.x = x;
            this.y = y;
        }
    }
}
