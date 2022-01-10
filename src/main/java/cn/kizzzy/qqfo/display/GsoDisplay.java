package cn.kizzzy.qqfo.display;

import cn.kizzzy.helper.LogHelper;
import cn.kizzzy.javafx.display.DisplayParam;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.qqfo.GsoFile;
import cn.kizzzy.qqfo.GsoFileItem;
import cn.kizzzy.qqfo.GsoFileItems;
import cn.kizzzy.qqfo.helper.QqfoImgHelper;

import java.awt.image.BufferedImage;
import java.util.Collections;

@DisplayFlag(suffix = {
    "gso",
})
public class GsoDisplay extends Display {
    
    private int index;
    private int total;
    
    private String[] infos;
    private DisplayParam[] params;
    
    public GsoDisplay(DisplayContext context, String path) {
        super(context, path);
    }
    
    @Override
    public void init() {
        GsoFile file = context.load(path, GsoFile.class);
        
        index = 0;
        total = file.items.length - 1;
        
        context.notifyListener(DisplayType.SHOW_TEXT, file.toString());
        
        infos = new String[file.count];
        params = new DisplayParam[file.count];
        
        int i = 0;
        for (GsoFileItems items : file.items) {
            for (GsoFileItem item : items.items) {
                if (item != null) {
                    
                    infos[i] = String.format(
                        "Show Image(%d/%d) [%d * %d * %s]",
                        i + 1,
                        item.file.count,
                        item.width,
                        item.height,
                        retrieveImageType(item.type)
                    );
                    
                    BufferedImage image = QqfoImgHelper.toImage(item);
                    wrapperImage(image);
                    
                    params[i] = new DisplayParam.Builder()
                        .setX(getLayoutX(item))
                        .setY(getLayoutY(item))
                        .setWidth(item.width)
                        .setHeight(item.height)
                        .setImage(image)
                        .build();
                    
                    i++;
                }
            }
        }
        
        displayImpl();
    }
    
    @Override
    public void prev() {
        index--;
        if (index < 0) {
            index = total - 1;
        }
        
        displayImpl();
    }
    
    @Override
    public void next() {
        index++;
        if (index >= total) {
            index = 0;
        }
        
        displayImpl();
    }
    
    @Override
    public void play() {
        next();
    }
    
    protected void displayImpl() {
        try {
            context.notifyListener(DisplayType.TOAST_TIPS, infos[index]);
            context.notifyListener(DisplayType.SHOW_IMAGE, Collections.singletonList(params[index]));
        } catch (Exception e) {
            LogHelper.error(null, e);
        }
    }
}
