package cn.kizzzy.qqfo.display;

import cn.kizzzy.javafx.display.Display;
import cn.kizzzy.javafx.display.DisplayAAA;
import cn.kizzzy.javafx.display.DisplayAttribute;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.vfs.IPackage;

@DisplayAttribute(suffix = {
    "txt",
    "ini",
    "xml",
    "lua",
    "eff",
})
public class TxtDisplay extends Display<IPackage> {
    
    public TxtDisplay(IPackage context, String path) {
        super(context, path);
    }
    
    @Override
    public DisplayAAA load() {
        return new DisplayAAA(DisplayType.SHOW_TEXT, context.load(path, String.class));
    }
}
