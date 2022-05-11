package corebot;

import arc.*;
import arc.files.*;
import arc.graphics.Color;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.io.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.legacy.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.zip.*;

import static mindustry.Vars.*;

public class ContentHandler{
    public static final String schemHeader = schematicBaseStart;

    Color co = new Color();
    Graphics2D currentGraphics;
    BufferedImage currentImage;
    ObjectMap<String, Fi> imageFiles = new ObjectMap<>();
    ObjectMap<String, BufferedImage> regions = new ObjectMap<>();

    //for testing only
    //public static void main(String[] args) throws Exception{
    //    new ContentHandler().previewSchematic(Schematics.readBase64("bXNjaAF4nDWQXW6DQAyEB3b5MX/JW0/BQ6repuoDJa6EBEsFJFJu01v0WL1C7XWLhD6NGc8sizPOKXwYFsbTyzIF7i/P+zgcB2/9lT84jIx8Ht553pG9/nx9v3kUfwaU4xru/Fg31NPBS7+vt038p8/At2U4prG/btM8A7jIiwzxISBBihypghTOlFMlx4EXayIDr3MICkRFqmJMIog72f+w06HancIZvCGD04ocsak0Z4VEURsaQyufpM1rZiGW1Ik97pW6F0+v62RFZEVkRaRFihhNFk0WTRZNds5KMyGIP1bZndQ6VETVmGpMtaZa6+/sEjpVv/XMJCs="));
    //}

    public ContentHandler(){
        //clear cache
        new Fi("cache").deleteDirectory();

        Version.enabled = false;
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();
        for(ContentType type : ContentType.all){
            for(Content content : Vars.content.getBy(type)){
                try{
                    content.init();
                }catch(Throwable ignored){
                }
            }
        }

        String assets = "../Mindustry/core/assets/";
        Vars.state = new GameState();

        TextureAtlasData data = new TextureAtlasData(new Fi(assets + "sprites/sprites.aatls"), new Fi(assets + "sprites"), false);
        Core.atlas = new TextureAtlas();

        new Fi("../Mindustry/core/assets-raw/sprites_out").walk(f -> {
            if(f.extEquals("png")){
                imageFiles.put(f.nameWithoutExtension(), f);
            }
        });

        data.getPages().each(page -> {
            page.texture = Texture.createEmpty(null);
            page.texture.width = page.width;
            page.texture.height = page.height;
        });

        data.getRegions().each(reg -> Core.atlas.addRegion(reg.name, new AtlasRegion(reg.page.texture, reg.left, reg.top, reg.width, reg.height){{
            name = reg.name;
            texture = reg.page.texture;
        }}));

        Lines.useLegacyLine = true;
        Core.atlas.setErrorRegion("error");
        Draw.scl = 1f / 4f;
        Core.batch = new SpriteBatch(0){
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
                BufferedImage image = getImage(((AtlasRegion)region).name);
                if(!color.equals(Color.white)){
                    image = tint(image, color);
                }

                currentGraphics.drawImage(image, 0, 0, (int)width, (int)height, null);
            }

            @Override
            protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
                //do nothing
            }
        };

        for(ContentType type : ContentType.values()){
            for(Content content : Vars.content.getBy(type)){
                try{
                    content.load();
                    content.loadIcon();
                }catch(Throwable ignored){
                }
            }
        }

        try{
            BufferedImage image = ImageIO.read(new File("../Mindustry/core/assets/sprites/block_colors.png"));

            for(Block block : Vars.content.blocks()){
                block.mapColor.argb8888(image.getRGB(block.id, 0));
                if(block instanceof OreBlock){
                    block.mapColor.set(block.itemDrop.color);
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
    }

    private BufferedImage getImage(String name){
        return regions.get(name, () -> {
            try{
                return ImageIO.read(imageFiles.get(name, imageFiles.get("error")).file());
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        });
    }

    private BufferedImage tint(BufferedImage image, Color color){
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Color tmp = new Color();
        for(int x = 0; x < copy.getWidth(); x++){
            for(int y = 0; y < copy.getHeight(); y++){
                int argb = image.getRGB(x, y);
                tmp.argb8888(argb);
                tmp.mul(color);
                copy.setRGB(x, y, tmp.argb8888());
            }
        }
        return copy;
    }

    public Schematic parseSchematic(String text) throws Exception{
        return read(new ByteArrayInputStream(Base64Coder.decode(text)));
    }

    public Schematic parseSchematicURL(String text) throws Exception{
        return read(CoreBot.net.download(text));
    }

    static Schematic read(InputStream input) throws IOException{
        byte[] header = {'m', 's', 'c', 'h'};
        for(byte b : header){
            if(input.read() != b){
                throw new IOException("Not a schematic file (missing header).");
            }
        }

        //discard version
        input.read();

        try(DataInputStream stream = new DataInputStream(new InflaterInputStream(input))){
            short width = stream.readShort(), height = stream.readShort();

            StringMap map = new StringMap();
            byte tags = stream.readByte();
            for(int i = 0; i < tags; i++){
                map.put(stream.readUTF(), stream.readUTF());
            }

            IntMap<Block> blocks = new IntMap<>();
            byte length = stream.readByte();
            for(int i = 0; i < length; i++){
                String name = stream.readUTF();
                Block block = Vars.content.getByName(ContentType.block, SaveFileReader.fallback.get(name, name));
                blocks.put(i, block == null || block instanceof LegacyBlock ? Blocks.air : block);
            }

            int total = stream.readInt();
            if(total > 64 * 64) throw new IOException("Schematic has too many blocks.");
            Seq<Stile> tiles = new Seq<>(total);
            for(int i = 0; i < total; i++){
                Block block = blocks.get(stream.readByte());
                int position = stream.readInt();
                Object config = TypeIO.readObject(Reads.get(stream));
                byte rotation = stream.readByte();
                if(block != Blocks.air){
                    tiles.add(new Stile(block, Point2.x(position), Point2.y(position), config, rotation));
                }
            }

            return new Schematic(tiles, map, width, height);
        }
    }

    public BufferedImage previewSchematic(Schematic schem) throws Exception{
        if(schem.width > 64 || schem.height > 64) throw new IOException("Schematic cannot be larger than 64x64.");
        BufferedImage image = new BufferedImage(schem.width * 32, schem.height * 32, BufferedImage.TYPE_INT_ARGB);

        Draw.reset();
        Seq<BuildPlan> requests = schem.tiles.map(t -> new BuildPlan(t.x, t.y, t.rotation, t.block, t.config));
        currentGraphics = image.createGraphics();
        currentImage = image;
        requests.each(req -> {
            req.animScale = 1f;
            req.worldContext = false;
            req.block.drawPlanRegion(req, requests);
            Draw.reset();
        });

        requests.each(req -> req.block.drawPlanConfigTop(req, requests));

        return image;
    }

    public Map readMap(InputStream is) throws IOException{
        try(InputStream ifs = new InflaterInputStream(is); CounterInputStream counter = new CounterInputStream(ifs); DataInputStream stream = new DataInputStream(counter)){
            Map out = new Map();

            SaveIO.readHeader(stream);
            int version = stream.readInt();
            SaveVersion ver = SaveIO.getSaveWriter(version);
            StringMap[] metaOut = {null};
            ver.region("meta", stream, counter, in -> metaOut[0] = ver.readStringMap(in));

            StringMap meta = metaOut[0];

            out.name = meta.get("name", "Unknown");
            out.author = meta.get("author");
            out.description = meta.get("description");
            out.tags = meta;

            int width = meta.getInt("width"), height = meta.getInt("height");

            var floors = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            var walls = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            var fgraphics = floors.createGraphics();
            var jcolor = new java.awt.Color(0, 0, 0, 64);
            int black = 255;
            CachedTile tile = new CachedTile(){
                @Override
                public void setBlock(Block type){
                    super.setBlock(type);

                    int c = MapIO.colorFor(block(), Blocks.air, Blocks.air, team());
                    if(c != black && c != 0){
                        walls.setRGB(x, floors.getHeight() - 1 - y, conv(c));
                        fgraphics.setColor(jcolor);
                        fgraphics.drawRect(x, floors.getHeight() - 1 - y + 1, 1, 1);
                    }
                }
            };

            ver.region("content", stream, counter, ver::readContentHeader);
            ver.region("preview_map", stream, counter, in -> ver.readMap(in, new WorldContext(){
                @Override public void resize(int width, int height){}
                @Override public boolean isGenerating(){return false;}
                @Override public void begin(){
                    world.setGenerating(true);
                }
                @Override public void end(){
                    world.setGenerating(false);
                }

                @Override
                public void onReadBuilding(){
                    //read team colors
                    if(tile.build != null){
                        int c = tile.build.team.color.argb8888();
                        int size = tile.block().size;
                        int offsetx = -(size - 1) / 2;
                        int offsety = -(size - 1) / 2;
                        for(int dx = 0; dx < size; dx++){
                            for(int dy = 0; dy < size; dy++){
                                int drawx = tile.x + dx + offsetx, drawy = tile.y + dy + offsety;
                                walls.setRGB(drawx, floors.getHeight() - 1 - drawy, c);
                            }
                        }
                    }
                }

                @Override
                public Tile tile(int index){
                    tile.x = (short)(index % width);
                    tile.y = (short)(index / width);
                    return tile;
                }

                @Override
                public Tile create(int x, int y, int floorID, int overlayID, int wallID){
                    if(overlayID != 0){
                        floors.setRGB(x, floors.getHeight() - 1 - y, conv(MapIO.colorFor(Blocks.air, Blocks.air, content.block(overlayID), Team.derelict)));
                    }else{
                        floors.setRGB(x, floors.getHeight() - 1 - y, conv(MapIO.colorFor(Blocks.air, content.block(floorID), Blocks.air, Team.derelict)));
                    }
                    return tile;
                }
            }));

            fgraphics.drawImage(walls, 0, 0, null);
            fgraphics.dispose();

            out.image = floors;

            return out;

        }finally{
            content.setTemporaryMapper(null);
        }
    }

    int conv(int rgba){
        return co.set(rgba).argb8888();
    }

    public static class Map{
        public String name, author, description;
        public ObjectMap<String, String> tags = new ObjectMap<>();
        public BufferedImage image;
    }
}
