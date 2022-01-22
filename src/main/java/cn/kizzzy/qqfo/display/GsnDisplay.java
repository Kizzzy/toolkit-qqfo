package cn.kizzzy.qqfo.display;

import cn.kizzzy.helper.LogHelper;
import cn.kizzzy.javafx.display.DisplayParam;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.qqfo.GsnFile;
import cn.kizzzy.qqfo.GsnFrame;
import cn.kizzzy.qqfo.helper.QqfoImgHelper;

import java.awt.image.BufferedImage;
import java.util.Collections;

@DisplayFlag(suffix = {
    "gsn",
})
public class GsnDisplay extends Display {
    
    private int index;
    private int total;
    
    private String[] infos;
    private DisplayParam[] params;
    
    public GsnDisplay(DisplayContext context, String path) {
        super(context, path);
    }
    
    @Override
    public void init() {
        GsnFile file = context.load(path, GsnFile.class);
        
        index = 0;
        total = file.count;
        
        infos = new String[file.count];
        params = new DisplayParam[file.count];
        
        int i = 0;
        for (GsnFrame frame : file.frames) {
            if (frame != null) {
                
                infos[i] = String.format(
                    "Show Image(%d/%d) [%d * %d * %s]",
                    i + 1,
                    file.count,
                    frame.getWidth(),
                    frame.getHeight(),
                    retrieveImageType(frame.getType())
                );
                
                BufferedImage image = QqfoImgHelper.toImage(frame);
                wrapperImage(image);
                
                params[i] = new DisplayParam.Builder()
                    .setX(200)
                    .setY(200)
                    .setWidth(frame.getWidth())
                    .setHeight(frame.getHeight())
                    .setImage(image)
                    .build();
                
                i++;
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
            if (params[index] != null) {
                context.notifyListener(DisplayType.SHOW_IMAGE, Collections.singletonList(params[index]));
            }
        } catch (Exception e) {
            LogHelper.error(null, e);
        }
    }
}
