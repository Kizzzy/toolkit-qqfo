package cn.kizzzy.qqfo.display;

import cn.kizzzy.javafx.display.Display;
import cn.kizzzy.javafx.display.DisplayAAA;
import cn.kizzzy.javafx.display.DisplayAttribute;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.javafx.display.image.DisplayFrame;
import cn.kizzzy.javafx.display.image.DisplayTrack;
import cn.kizzzy.javafx.display.image.DisplayTracks;
import cn.kizzzy.qqfo.GsoFile;
import cn.kizzzy.qqfo.GsoFileItem;
import cn.kizzzy.qqfo.GsoFileItems;
import cn.kizzzy.qqfo.helper.QqfoImgHelper;
import cn.kizzzy.vfs.IPackage;

import java.awt.image.BufferedImage;

@DisplayAttribute(suffix = {
    "gso",
})
public class GsoDisplay extends Display<IPackage> {
    
    public GsoDisplay(IPackage context, String path) {
        super(context, path);
    }
    
    @Override
    public DisplayAAA load() {
        GsoFile file = context.load(path, GsoFile.class);
        if (file == null) {
            return null;
        }
        
        DisplayTrack track = new DisplayTrack();
        int i = 0;
        for (GsoFileItems items : file.items) {
            for (GsoFileItem item : items.items) {
                if (item != null) {
                    BufferedImage image = QqfoImgHelper.toImage(item);
                    if (image != null) {
                        DisplayFrame frame = new DisplayFrame();
                        frame.x = 200;
                        frame.y = 200;
                        frame.width = item.width;
                        frame.height = item.height;
                        frame.image = image;
                        frame.time = 167 * (i++);
                        frame.extra = String.format("%02d/%02d", i, file.count);
                        
                        track.frames.add(frame);
                    }
                }
            }
        }
        
        DisplayTracks tracks = new DisplayTracks();
        tracks.tracks.add(track);
        return new DisplayAAA(DisplayType.SHOW_IMAGE, tracks);
    }
}
