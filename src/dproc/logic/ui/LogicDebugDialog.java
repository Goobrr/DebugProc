package dproc.logic.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.struct.*;
import arc.util.*;
import dproc.logic.DebugLogicBlock.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.logic.LExecutor.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.logic.LogicBlock.*;

public class LogicDebugDialog extends BaseDialog{
    public String[] code;
    public LExecutor executor;
    public DebugLogicBuild build;

    private Table codeTable;
    private Table varsTable;
    private Element hovered;

    private final IntMap<Integer> bpIndexes = new IntMap<>();

    public LogicDebugDialog(){
        super(Core.bundle.get("ui.debug.title"));

        cont.fill(t -> {
            t.top().left();
            t.table(t1 -> {
                t1.top().left();

                t1.label(() -> Core.bundle.get("ui.debug.code"));
                t1.row();
                t1.pane(Styles.defaultPane, p -> {
                    p.setBackground(Styles.black5);
                    p.top().left();
                    codeTable = p.table().top().left().grow().get();
                }).pad(5).top().left().grow().get().setScrollingDisabledX(false);

                t1.row();
                t1.table(b -> {
                    b.table(Styles.black5, bt -> {
                        bt.top().left();
                        Label ln = bt.labelWrap(() -> currentInstruction() + "")
                            .top().left().growY().width(100)
                            .get();
                        ln.setAlignment(Align.left);
                        ln.setStyle(DebugStyles.code);

                        Label inst = bt.labelWrap(() -> code[currentInstruction()])
                            .top().left().grow()
                            .get();
                        inst.setAlignment(Align.left);
                        inst.setStyle(DebugStyles.code);
                    }).top().left().grow().pad(5);

                    b.button(Icon.down, () -> {
                        executor.runOnce();
                        build.auto = false;
                    }).top().right().pad(5).size(40).tooltip(Core.bundle.get("ui.debug.next"));

                    b.button(Icon.up, () -> {
                        executor.var(0).numval -= 1;
                        build.auto = false;
                    }).top().right().pad(5).size(40).tooltip(Core.bundle.get("ui.debug.prev"));

                    b.button(Icon.rotate, this::resetExecutor)
                    .top().right().pad(5)
                    .size(40).tooltip(Core.bundle.get("ui.debug.reset"));

                    b.button(Icon.logic, () -> build.auto = !build.auto)
                        .top().right().pad(5)
                        .size(40).tooltip(Core.bundle.get("ui.debug.auto"));
                }).pad(5).top().growX();
            }).pad(5).top().left().grow();

            t.table(t2 -> {
                t2.top().left();
                t2.table(vt -> {
                    vt.label(() -> Core.bundle.get("ui.debug.vars"));
                    vt.row();
                    vt.pane(Styles.defaultPane, p -> {
                        p.setBackground(Styles.black5);
                        varsTable = p.table().top().left().grow().get();
                    }).pad(5).top().left().grow();
                }).pad(5).top().left().grow();
            }).pad(5).top().left().grow();
        });

        addCloseButton();
    }

    @Override
    public void act(float delta){
        updateVars();

        if(build.auto && build.breakpoints.contains(currentInstruction())){
            build.auto = false;
        };

        super.act(delta);
    }

    // TODO optimize
    public void updateVars(){
        varsTable.clearChildren();
        varsTable.top().left();
        for(Var var : executor.vars){
            if(var.constant) continue;
            varsTable.table(t -> {
                t.labelWrap(() -> !var.isobj ? "number" :
                var.objval == null ? "[scarlet]null" :
                var.objval instanceof String ? "string" :
                var.objval instanceof Content ? "content" :
                var.objval instanceof Building ? "building" :
                var.objval instanceof Unit ? "unit" :
                var.objval instanceof Enum<?> ? "enum" :
                "[gray]unknown"
                ).top().left().grow();

                t.labelWrap(() -> var.name).top().left().grow().get();
                t.labelWrap(() -> var.isobj ? var.objval == null ? "null" : var.objval.toString() : var.numval + "").top().left().grow();
            }).growX();
            varsTable.row();
        }
    }

    public void resetExecutor(){
        build.auto = false;
        LAssembler assembler = LAssembler.assemble(build.code);

        for(LogicLink link : build.links){
            assembler.putConst(link.name, Vars.world.build(link.x, link.y));
        }

        executor.load(assembler);
        updateVars();
    }

    public void updateCode(){
        codeTable.clearChildren();
        codeTable.top().left();
        int nl = String.valueOf(code.length).length();
        for(int i = 0; i < code.length; i++){
            int fi = i;

            // honestly this would've been nicer if String#repeat existed earlier
            StringBuilder lineNumber = new StringBuilder(i + " ");
            for(int j = 0; j < nl - String.valueOf(i).length(); j++){
                lineNumber.append(" ");
            }

            Table c = codeTable.add(new Table(t -> {
                t.top().left();
                ImageButton b = t.button(Icon.rightOpenSmall, Styles.emptyi, () -> {
                    if(bpIndexes.containsKey(fi) && bpIndexes.get(fi) < build.breakpoints.size){
                        build.breakpoints.removeIndex(bpIndexes.get(fi));
                    }else{
                        build.breakpoints.add(fi);
                        bpIndexes.put(fi, build.breakpoints.size - 1);
                    }
                }).padLeft(5).left().size(20).tooltip(Core.bundle.get("ui.debug.breakpoint")).get();
                b.visible(() -> hovered == t);

                Label l = new Label(lineNumber + code[fi], DebugStyles.code);
                l.setWrap(true);
                l.setAlignment(Align.left);

                t.add(l).top().left().padLeft(5);
            }){
                @Override
                protected void drawBackground(float x, float y){
                    Draw.color(
                    build.breakpoints.contains(fi) ?
                    currentInstruction() == fi ? Pal.heal : Color.sky :
                    currentInstruction() == fi ? Pal.accent :
                    hovered == this ? Pal.gray : Color.clear
                    );
                    Draw.alpha(0.7f * this.parentAlpha);
                    Fill.crect(x, y, this.getWidth(), this.getHeight());
                }
            }).top().left().growX().get();
            c.touchable(() -> Touchable.enabled);
            c.addListener(new InputListener(){
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                    hovered = c;
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                    hovered = null;
                }
            });
            codeTable.row();
        }
    }

    public int currentInstruction(){
        return (int)Math.max(executor.var(0).numval, 0) % code.length;
    }

    public void show(String code, LExecutor executor, DebugLogicBuild build){
        this.executor = executor;
        this.code = code.split("\\n");
        this.build = build;

        bpIndexes.clear();
        for(int i = 0; i < build.breakpoints.size; i++){
            bpIndexes.put(build.breakpoints.get(i), i);
        }

        updateCode();
        show();
    }
}
