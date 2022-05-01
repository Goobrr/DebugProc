package dproc.logic;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import dproc.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.logic.*;

import java.nio.*;
import java.util.*;

public class DebugLogicBlock extends LogicBlock{
    public TextureRegion shineRegion;

    public DebugLogicBlock(String name){
        super(name);

        // TODO remove unnecessary wrapper?
        config(byte[].class, (DebugLogicBuild l, byte[] b) -> {
            if(b[0] > 0){
                ByteBufferInput buffer = new ByteBufferInput(ByteBuffer.wrap(b));

                buffer.buffer.position(1);
                l.auto = buffer.readBoolean();
                int len = buffer.readInt();

                byte[] data = new byte[len];
                for(int i = 0; i < len; i++){
                    data[i] = buffer.readByte();
                }
                l.readCompressed(data, true);

                while(buffer.buffer.hasRemaining()){
                    l.breakpoints.add(buffer.readInt());
                }
            }else{
                l.readCompressed(Arrays.copyOfRange(b, 1, b.length), true);
            }
        });
    }

    @Override
    public void load(){
        super.load();

        shineRegion = Core.atlas.find(name + "-shine");
    }

    public class DebugLogicBuild extends LogicBuild {
        public boolean auto = false;
        public IntSet breakpoints = new IntSet();

        // TODO compress?
        @Override
        public byte[] config(){
            return config(code, true);
        }

        public byte[] config(String code, boolean b){
            if(b){
                byte[] pd = compress(code, relativeConnections());

                ByteBufferOutput buffer = new ByteBufferOutput(ByteBuffer.wrap(new byte[6 + pd.length + (breakpoints.size * 4)]));

                buffer.writeBoolean(true); // long form config flag
                buffer.writeBoolean(auto);
                buffer.writeInt(pd.length);
                buffer.write(pd);
                breakpoints.each(buffer::writeInt);

                return buffer.buffer.array();
            }else{
                ByteSeq data = ByteSeq.with((byte)0); // short form config flag
                data.addAll(compress(code, relativeConnections()));

                return data.toArray();
            }
        }

        @Override
        public void draw(){
            super.draw();

            if(auto){
                Draw.color(Pal.logicBlocks, (Mathf.sin((Time.time / 50) - x - y) + 1f) / 4f);

                Draw.blend(Blending.additive);
                Draw.rect(shineRegion, x, y);
                Draw.blend();
            }
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Icon.pencil, Styles.clearTransi, () -> {
                Vars.ui.logic.show(code, executor, code -> configure(config(code, false)));
            }).size(40);

            table.button(Icon.settings, Styles.clearTransi, () -> {
                DProc.debug.show(code, executor, this);
            }).size(40).disabled(b -> !executor.initialized());
        }

        @Override
        public void updateTile(){
            //load up code from read()
            if(loadBlock != null){
                loadBlock.run();
                loadBlock = null;
            }

            executor.team = team;

            if(!checkedDuplicates){
                checkedDuplicates = true;
                var removal = new IntSet();
                var removeLinks = new Seq<LogicLink>();
                for(var link : links){
                    var build = Vars.world.build(link.x, link.y);
                    if(build != null){
                        if(!removal.add(build.id)){
                            removeLinks.add(link);
                        }
                    }
                }
                links.removeAll(removeLinks);
            }

            //check for previously invalid links to add after configuration
            boolean changed = false, updates = true;

            while(updates){
                updates = false;

                for(int i = 0; i < links.size; i++){
                    LogicLink l = links.get(i);

                    if(!l.active) continue;

                    var cur = Vars.world.build(l.x, l.y);

                    boolean valid = validLink(cur);
                    if(l.lastBuild == null) l.lastBuild = cur;
                    if(valid != l.valid || l.lastBuild != cur){
                        l.lastBuild = cur;
                        changed = true;
                        l.valid = valid;
                        if(valid){

                            //this prevents conflicts
                            l.name = "";
                            //finds a new matching name after toggling
                            l.name = findLinkName(cur.block);

                            //remove redundant links
                            links.removeAll(o -> Vars.world.build(o.x, o.y) == cur && o != l);

                            //break to prevent concurrent modification
                            updates = true;
                            break;
                        }
                    }
                }
            }

            if(changed){
                updateCode(code, true, null);
            }

            if(enabled && auto){
                accumulator += edelta() * instructionsPerTick * (consValid() ? 1 : 0);

                if(accumulator > maxInstructionScale * instructionsPerTick) accumulator = maxInstructionScale * instructionsPerTick;

                for(int i = 0; i < (int)accumulator; i++){
                    if(executor.initialized()){
                        executor.runOnce();
                    }
                    accumulator --;
                }
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision >= 1){
                auto = read.bool();

                int len = read.i();
                for(int i = 0; i < len; i++){
                    breakpoints.add(read.i());
                }
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.bool(auto);

            write.i(breakpoints.size);
            breakpoints.each(write::i);
        }

        @Override
        public byte version(){
            return 1;
        }
    }
}
