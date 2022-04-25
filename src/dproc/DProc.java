package dproc;

import arc.*;
import dproc.logic.*;
import dproc.logic.ui.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.meta.*;

public class DProc extends Mod{
    public static LogicDebugDialog debug;

    public DProc(){
        Events.on(FileTreeInitEvent.class, e -> {
           DebugStyles.init();
        });

        Events.on(ClientLoadEvent.class, e -> {
            debug = new LogicDebugDialog();
        });
    }

    @Override
    public void loadContent(){
        new DebugLogicBlock("debug-processor"){{
            requirements(Category.logic, BuildVisibility.sandboxOnly, ItemStack.with(Items.copper, 1));

            size = 1;
            health = 200;
            instructionsPerTick = 1;
        }};
    }

}
