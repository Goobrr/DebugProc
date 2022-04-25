package dproc.logic.ui;

import arc.*;
import arc.graphics.*;
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

public class LogicDebugDialog extends BaseDialog{
    public String[] code;
    public LExecutor executor;
    public DebugLogicBuild build;

    private Table codeTable;
    private Table varsTable;
    private int oldVarsLength;

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
                    b.button(Icon.down, () -> executor.runOnce())
                        .size(40).tooltip(Core.bundle.get("ui.debug.next"));
                    b.button(Icon.up, () -> executor.var(0).numval -= 1)
                        .size(40).tooltip(Core.bundle.get("ui.debug.prev"));
                    b.button(Icon.logic, () -> build.auto = !build.auto)
                        .size(40).tooltip(Core.bundle.get("ui.debug.auto"));
                }).pad(5).top() ;
            }).pad(5).top().left().grow();

            t.table(t2 -> {
                t2.top().left();
                t2.label(() -> Core.bundle.get("ui.debug.vars"));
                t2.row();
                t2.pane(Styles.defaultPane, p -> {
                    p.setBackground(Styles.black5);
                    p.top().left();
                    varsTable = p.table().top().left().grow().get();
                }).pad(5).top().left().grow();
            }).top().left().grow();
        });

        addCloseButton();
    }

    @Override
    public void draw(){
        updateVars();
        super.draw();
    }

    public void updateVars(){
        if(executor.vars.length != oldVarsLength){
            varsTable.clearChildren();
            varsTable.top().left();
            for(Var var : executor.vars){
                if(var.constant) continue;
                varsTable.table(t -> {
                    String typeName =
                    !var.isobj ? "number" :
                    var.objval == null ? "null" :
                    var.objval instanceof String ? "string" :
                    var.objval instanceof Content ? "content" :
                    var.objval instanceof Building ? "building" :
                    var.objval instanceof Unit ? "unit" :
                    var.objval instanceof Enum<?> ? "enum" :
                    "unknown";

                    t.labelWrap(() -> typeName == "null" ? "[scarlet]null[]" : typeName == "unknown" ? "[gray]unknown" : typeName)
                    .top().left().grow();

                    t.labelWrap(() -> var.name).top().left().grow().get();
                    t.labelWrap(() -> var.isobj ? var.objval == null ? "null" : var.objval.toString() : var.numval + "").top().left().grow();
                }).growX();
                varsTable.row();
            }
        }
        oldVarsLength = executor.vars.length;
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
            im.visible(() -> Mathf.clamp(executor.var(0).numval - 1, 0, Integer.MAX_VALUE) == fi);
            im.setColor(Pal.accent);

            codeTable.stack(im, l).top().left().growX();
            codeTable.row();
        }
    }

    public void show(String code, LExecutor executor, DebugLogicBuild build){
        this.executor = executor;
        this.code = code.split("\\n");
        this.build = build;

        updateCode();
        show();
    }
}
