package dproc.logic.ui;

import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.freetype.*;
import arc.freetype.FreeTypeFontGenerator.*;
import arc.freetype.FreetypeFontLoader.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.TextField.*;
import arc.struct.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;

public class DebugStyles{
    public static LabelStyle code;
    public static Font codeFont;
    public static TextFieldStyle codeField;

    public static void init(){
        Core.assets.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(Vars.tree){
            @Override
            public FreeTypeFontGenerator load(AssetManager assetManager, String fileName, Fi file, FreeTypeFontGeneratorParameters parameter){
                return new FreeTypeFontGenerator(Vars.tree.get(file.pathWithoutExtension()));
            }
        });

        Core.assets.setLoader(Font.class, "-pu", new FreetypeFontLoader(Vars.tree){
            @Override
            public Font loadSync(AssetManager manager, String fileName, Fi file, FreeTypeFontLoaderParameter parameter){
                if(parameter == null) throw new IllegalArgumentException("parameter is null");

                FreeTypeFontGenerator generator = manager.get(parameter.fontFileName, FreeTypeFontGenerator.class);
                return generator.generateFont(parameter.fontParameters);
            }

            @Override
            @SuppressWarnings("rawtypes")
            public Seq<AssetDescriptor> getDependencies(String fileName, Fi file, FreeTypeFontLoaderParameter parameter){
                return Seq.with(new AssetDescriptor<>(parameter.fontFileName, FreeTypeFontGenerator.class));
            }
        });



        // Jetbrains Mono
        Core.assets.load("code", Font.class, new FreeTypeFontLoaderParameter("fonts/code.ttf", new FreeTypeFontParameter(){{
            size = 18;
        }})).loaded = f -> {
            codeFont = f;
            code = new LabelStyle(codeFont, Color.white);
            codeField = new TextFieldStyle(){{
                font = codeFont;
                fontColor = Color.white;
                disabledFontColor = Color.gray;
                selection = Tex.selection;
                invalidBackground = Tex.underlineRed;
                cursor = Tex.cursor;
                messageFont = codeFont;
                messageFontColor = Color.gray;
            }};
        };

    }
}
