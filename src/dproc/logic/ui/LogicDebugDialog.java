package dproc.logic.ui;

import arc.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
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
    private Table bpTable;

    private int newBreakpoint = 0;

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

                t2.row();
                t2.table(bt -> {
                    bt.label(() -> Core.bundle.get("ui.debug.breakpoints"));
                    bt.row();
                    bt.table(btt -> {
                        btt.top().left();
                        btt.setBackground(Styles.black5);
                        btt.pane(p -> {
                            bpTable = p.table().top().left().grow().get();
                        }).top().left().grow();
                    }).pad(5).top().left().grow();

                    bt.row();
                    bt.table(bf -> {
                        bf.field("" + newBreakpoint, DebugStyles.codeField, s -> {
                            if(Strings.canParseInt(s)){
                                newBreakpoint = Math.max(Integer.parseInt(s.replaceAll("[^\\d]", "")), 0);
                            }
                        }).growX().top().left();

                        bf.button(Icon.add, () -> {
                            if(!build.breakpoints.contains(newBreakpoint)){
                                build.breakpoints.add(newBreakpoint);
                                updateBreakpoints();
                            }
                        }).top().right().size(40).pad(5);
                    }).pad(5).top().left().growX();

                }).pad(5).bottom().left().grow();
            }).pad(5).top().left().grow();
        });

        addCloseButton();
    }

    @Override
    public void draw(){
        updateVars();

        if(build.auto && build.breakpoints.contains(currentInstruction())){
            build.auto = false;
        };

        super.draw();
    }

    public void updateBreakpoints(){
        bpTable.clearChildren();
        bpTable.top().left();

        for(int i = 0; i < build.breakpoints.size; i++){
            int fi = i;
            int bp = build.breakpoints.get(fi);
            bpTable.table(t -> {
                t.button(Icon.cancel, () -> {
                    build.breakpoints.removeRange(fi, fi);
                    updateBreakpoints();
                }).tooltip("ui.debug.remove").top().left().size(40).pad(5);

                Label l;
                l = t.labelWrap(() -> bp + "").top().left().growY().width(100)
                .get();
                l.setAlignment(Align.left);
                l.setStyle(DebugStyles.code);

                l = t.labelWrap(() -> bp < code.length ? code[bp] : "-").top().left().grow()
                .get();
                l.setAlignment(Align.left);
                l.setStyle(DebugStyles.code);
            }).growX().top().left();
            bpTable.row();
        }
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

            Image im = new Image(Tex.whiteui);
            Label l = new Label(lineNumber + code[i], DebugStyles.code);

            l.setWrap(true);
            im.visible(() -> currentInstruction() == fi);
            im.setColor(Pal.accent);

            codeTable.stack(im, l).top().left().growX();
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

        updateBreakpoints();
        updateCode();
        show();
    }
}
