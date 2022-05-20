package cn.kizzzy.toolkit.controller;

import cn.kizzzy.javafx.viewer.ViewerExecutor;
import cn.kizzzy.javafx.viewer.executor.QqfoViewerExecutor;

@MenuParameter(path = "文件浏览/QQ幻想/解包器(本地)")
@PluginParameter(url = "/fxml/explorer_view.fxml", title = "文件浏览(QFO)")
public class QqfoLocalController extends ExplorerView {
    
    @Override
    public String getName() {
        return "Qqfo-Display";
    }
    
    @Override
    protected ViewerExecutor initialViewExecutor() {
        return new QqfoViewerExecutor();
    }
}