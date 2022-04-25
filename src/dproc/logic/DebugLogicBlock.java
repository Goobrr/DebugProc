package dproc.logic;

import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import dproc.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.blocks.logic.*;

public class DebugLogicBlock extends LogicBlock{
    public DebugLogicBlock(String name){
        super(name);
    }

    public class DebugLogicBuild extends LogicBuild {
        public boolean auto = false;

        @Override
        public void buildConfiguration(Table table){
            table.button(Icon.pencil, Styles.clearTransi, () -> {
                Vars.ui.logic.show(code, executor, code -> configure(compress(code, relativeConnections())));
            }).size(40);

            table.button(Icon.settings, Styles.clearTransi, () -> {
                DProc.debug.show(code, executor, this);
            }).size(40);
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
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.bool(auto);
        }

        @Override
        public byte version(){
            return 1;
        }
    }
}
