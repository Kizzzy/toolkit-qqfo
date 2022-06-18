package cn.kizzzy.qqfo.display;

import cn.kizzzy.javafx.display.Display;
import cn.kizzzy.javafx.display.DisplayAAA;
import cn.kizzzy.javafx.display.DisplayAttribute;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.javafx.display.image.DisplayFrame;
import cn.kizzzy.javafx.display.image.DisplayTrack;
import cn.kizzzy.javafx.display.image.DisplayTracks;
import cn.kizzzy.qqfo.GsnFile;
import cn.kizzzy.qqfo.helper.QqfoImgHelper;
import cn.kizzzy.vfs.IPackage;

import java.awt.image.BufferedImage;

@DisplayAttribute(suffix = {
    "gsn",
})
public class GsnDisplay extends Display<IPackage> {
    
    public GsnDisplay(IPackage context, String path) {
        super(context, path);
    }
    
    @Override
    public DisplayAAA load() {
        GsnFile file = context.load(path, GsnFile.class);
        if (file == null) {
            return null;
        }
        
        DisplayTrack track = new DisplayTrack();
        int i = 0;
        for (GsnFile.GsnFrame gsnFrame : file.frames) {
            if (gsnFrame != null) {
                BufferedImage image = QqfoImgHelper.toImage(gsnFrame);
                if (image != null) {
                    DisplayFrame frame = new DisplayFrame();
                    frame.x = 200;
                    frame.y = 200;
                    frame.width = gsnFrame.width;
                    frame.height = gsnFrame.height;
                    frame.image = image;
                    frame.time = 167 * (i++);
                    frame.extra = String.format("%02d/%02d", i, file.count);
                    
                    track.frames.add(frame);
                }
            }
        }
        
        DisplayTracks tracks = new DisplayTracks();
        tracks.tracks.add(track);
        return new DisplayAAA(DisplayType.SHOW_IMAGE, tracks);
    }
}
