package cn.kizzzy.toolkit.controller;

import cn.kizzzy.helper.FileHelper;
import cn.kizzzy.helper.LogHelper;
import cn.kizzzy.helper.StringHelper;
import cn.kizzzy.javafx.StageHelper;
import cn.kizzzy.javafx.common.JavafxHelper;
import cn.kizzzy.javafx.common.MenuItemArg;
import cn.kizzzy.javafx.display.DisplayOperator;
import cn.kizzzy.javafx.display.DisplayTabView;
import cn.kizzzy.javafx.setting.SettingDialog;
import cn.kizzzy.qqfo.GsoFile;
import cn.kizzzy.qqfo.GsoFileItem;
import cn.kizzzy.qqfo.GsoFileItems;
import cn.kizzzy.qqfo.QqfoConfig;
import cn.kizzzy.qqfo.helper.QqfoImgHelper;
import cn.kizzzy.tencent.IdxFile;
import cn.kizzzy.toolkit.view.AbstractView;
import cn.kizzzy.vfs.IPackage;
import cn.kizzzy.vfs.ITree;
import cn.kizzzy.vfs.handler.BufferedImageHandler;
import cn.kizzzy.vfs.handler.IdxFileHandler;
import cn.kizzzy.vfs.handler.JsonFileHandler;
import cn.kizzzy.vfs.handler.StringFileHandler;
import cn.kizzzy.vfs.pack.CombinePackage;
import cn.kizzzy.vfs.pack.FilePackage;
import cn.kizzzy.vfs.pack.QqfoPackage;
import cn.kizzzy.vfs.tree.IdGenerator;
import cn.kizzzy.vfs.tree.IdxTreeBuilder;
import cn.kizzzy.vfs.tree.Leaf;
import cn.kizzzy.vfs.tree.Node;
import cn.kizzzy.vfs.tree.NodeComparator;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

abstract class QqfoViewBase extends AbstractView {
    
    @FXML
    protected TextField filterValue;
    
    @FXML
    protected TreeView<Node> tree_view;
    
    @FXML
    protected DisplayTabView display_tab;
    
    @FXML
    protected Label tips;
    
    @Override
    public String getName() {
        return "Qqfo-Display";
    }
}

@MenuParameter(path = "辅助/QQ幻想/解包器(本地)")
@PluginParameter(url = "/fxml/toolkit/qqfo_local_view.fxml", title = "QQ幻想(解包)")
public class QqfoLocalController extends QqfoViewBase implements Initializable {
    
    private static final String CONFIG_PATH = "qqfo/local.config";
    
    private static final Comparator<TreeItem<Node>> comparator
        = Comparator.comparing(TreeItem<Node>::getValue, new NodeComparator());
    
    private final StageHelper stageHelper
        = new StageHelper();
    
    private IPackage userVfs;
    private QqfoConfig config;
    
    private CombinePackage vfs;
    private IdGenerator idGenerator;
    
    private DisplayOperator<IPackage> displayer;
    
    private TreeItem<Node> dummyRoot;
    private TreeItem<Node> filterRoot;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userVfs = new FilePackage(System.getProperty("user.home") + "/.user");
        userVfs.getHandlerKvs().put(QqfoConfig.class, new JsonFileHandler<>(QqfoConfig.class));
        
        config = userVfs.load(CONFIG_PATH, QqfoConfig.class);
        config = config != null ? config : new QqfoConfig();
        
        stageHelper.addFactory(SettingDialog::new, SettingDialog.class);
        
        JavafxHelper.initContextMenu(tree_view, () -> stage.getScene().getWindow(), new MenuItemArg[]{
            new MenuItemArg(0, "设置", this::openSetting),
            new MenuItemArg(1, "加载PKG", this::loadPkg),
            new MenuItemArg(2, "打开/根目录", this::openFolderQqfoData),
            new MenuItemArg(2, "打开/文件目录", this::openFolderExportFile),
            new MenuItemArg(2, "打开/图片目录", this::openFolderExportImage),
            new MenuItemArg(3, "导出/文件", event -> exportFile(false)),
            new MenuItemArg(3, "导出/文件(递归)", event -> exportFile(true)),
            new MenuItemArg(3, "导出/图片", event -> exportImage(false, false)),
            new MenuItemArg(3, "导出/图片(递归)", event -> exportImage(true, false)),
            new MenuItemArg(3, "导出/图片(等尺寸)", event -> exportImage(false, true)),
            new MenuItemArg(3, "导出/图片(等尺寸)(递归)", event -> exportImage(true, true)),
            new MenuItemArg(4, "复制路径", this::copyPath),
        });
        
        dummyRoot = new TreeItem<>();
        tree_view.setRoot(dummyRoot);
        tree_view.setShowRoot(false);
        tree_view.getSelectionModel().selectedItemProperty().addListener(this::onSelectItem);
        
        vfs = new CombinePackage();
        idGenerator = new IdGenerator();
        
        displayer = new DisplayOperator<>("cn.kizzzy.qqfo.display", display_tab, IPackage.class);
        displayer.load();
        displayer.setContext(vfs);
    }
    
    @Override
    public void stop() {
        if (vfs != null) {
            vfs.stop();
        }
        
        userVfs.save(CONFIG_PATH, config);
        
        super.stop();
    }
    
    @FXML
    protected void onFilter(ActionEvent event) {
        final String regex = filterValue.getText();
        if (StringHelper.isNullOrEmpty(regex)) {
            return;
        }
        
        try {
            Pattern.compile(regex);
        } catch (Exception e) {
            return;
        }
        
        if (filterRoot == null) {
            filterRoot = new TreeItem<>(new Node(-1, "[Filter]"));
            dummyRoot.getChildren().add(filterRoot);
        }
        
        filterRoot.getChildren().clear();
        
        List<Node> list = vfs.listNodeByRegex(regex);
        for (Node folder : list) {
            filterRoot.getChildren().add(new TreeItem<>(folder));
        }
        
        filterRoot.getChildren().sort(comparator);
    }
    
    private void onSelectItem(Observable observable, TreeItem<Node> oldValue, TreeItem<Node> newValue) {
        Node node = newValue == null ? null : newValue.getValue();
        if (node != null) {
            if (node.leaf) {
                Leaf leaf = (Leaf) node;
                
                displayer.display(leaf.path);
            } else {
                newValue.getChildren().clear();
                
                Iterable<Node> list = vfs.listNode(node.id, false);
                for (Node temp : list) {
                    TreeItem<Node> child = new TreeItem<>(temp);
                    newValue.getChildren().add(child);
                }
                newValue.getChildren().sort(comparator);
            }
        }
    }
    
    private void openSetting(ActionEvent actionEvent) {
        SettingDialog.Args args = new SettingDialog.Args();
        args.target = config;
        
        stageHelper.show(stage, args, SettingDialog.class);
    }
    
    private void loadPkg(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择pkg文件");
        if (StringHelper.isNotNullAndEmpty(config.data_path)) {
            File lastFolder = new File(config.data_path);
            if (lastFolder.exists()) {
                chooser.setInitialDirectory(lastFolder);
            }
        }
        
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PKG", "*.pkg")
        );
        
        File file = chooser.showOpenDialog(stage);
        if (file != null && file.getAbsolutePath().endsWith(".pkg")) {
            config.data_path = file.getParent();
            
            new Thread(() -> {
                try {
                    loadPkgImpl(file);
                } catch (Exception e) {
                    LogHelper.error("load pkg error", e);
                }
            }).start();
        }
    }
    
    private void loadPkgImpl(File file) {
        IPackage dataVfs = new FilePackage(file.getParent());
        dataVfs.getHandlerKvs().put(IdxFile.class, new IdxFileHandler());
        
        IdxFile idxFile = dataVfs.load(FileHelper.getName(file.getAbsolutePath()), IdxFile.class);
        if (idxFile == null) {
            return;
        }
        
        ITree tree = new IdxTreeBuilder(idxFile, idGenerator).build();
        IPackage vfs = new QqfoPackage(file.getParent(), tree);
        vfs.getHandlerKvs().put(String.class, new StringFileHandler(Charset.forName("GB2312")));
        
        doAfterLoadVfs(vfs);
    }
    
    private void doAfterLoadVfs(IPackage _vfs) {
        vfs.addPackage(_vfs);
        
        Platform.runLater(() -> {
            dummyRoot.getChildren().clear();
            
            final List<Node> nodes = vfs.listNode(0, false);
            for (Node node : nodes) {
                TreeItem<Node> child = new TreeItem<>(node);
                dummyRoot.getChildren().add(child);
            }
            
            if (filterRoot != null) {
                dummyRoot.getChildren().add(filterRoot);
            }
        });
    }
    
    private void openFolderQqfoData(ActionEvent actionEvent) {
        openFolderImpl(config.data_path);
    }
    
    private void openFolderExportFile(ActionEvent actionEvent) {
        openFolderImpl(config.export_file_path);
    }
    
    private void openFolderExportImage(ActionEvent actionEvent) {
        openFolderImpl(config.export_image_path);
    }
    
    private void openFolderImpl(String path) {
        if (StringHelper.isNotNullAndEmpty(path)) {
            new Thread(() -> {
                try {
                    Desktop.getDesktop().open(new File(path));
                } catch (Exception e) {
                    LogHelper.error(String.format("open folder error, %s", path), e);
                }
            }).start();
        }
    }
    
    private void exportFile(boolean recursively) {
        if (StringHelper.isNullOrEmpty(config.export_file_path) || !new File(config.export_file_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存文件的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_file_path = file.getAbsolutePath();
        }
        
        TreeItem<Node> selected = tree_view.getSelectionModel().getSelectedItem();
        Node node = selected == null ? null : selected.getValue();
        if (node == null) {
            return;
        }
        
        IPackage target = null;
        
        List<Leaf> list = vfs.listLeaf(node, recursively);
        for (Leaf leaf : list) {
            try {
                if (target == null) {
                    String pkgName = leaf.pack.replace(".pkg", "");
                    target = new FilePackage(config.export_file_path + "/" + pkgName);
                }
                
                byte[] data = vfs.load(leaf.path, byte[].class);
                if (data != null) {
                    target.save(leaf.path, data);
                }
            } catch (Exception e) {
                LogHelper.info(String.format("export file failed: %s", leaf.path), e);
            }
        }
    }
    
    private void exportImage(boolean recursively, boolean fixed) {
        if (StringHelper.isNullOrEmpty(config.export_image_path) || !new File(config.export_image_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存图片的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_image_path = file.getAbsolutePath();
        }
        
        final TreeItem<Node> selected = tree_view.getSelectionModel().getSelectedItem();
        Node node = selected == null ? null : selected.getValue();
        if (node == null) {
            return;
        }
        
        IPackage target = null;
        
        List<Leaf> list = vfs.listLeaf(node, recursively);
        for (Leaf leaf : list) {
            try {
                if (target == null) {
                    String pkgName = leaf.pack.replace(".pkg", fixed ? "-fixed" : "");
                    target = new FilePackage(config.export_image_path + "/" + pkgName);
                    target.getHandlerKvs().put(BufferedImage.class, new BufferedImageHandler());
                }
                
                if (leaf.path.contains(".gso")) {
                    System.out.println("export: " + leaf.path);
                    
                    GsoFile file = vfs.load(leaf.path, GsoFile.class);
                    if (file != null) {
                        for (GsoFileItems items : file.items) {
                            for (GsoFileItem item : items.items) {
                                if (item != null) {
                                    BufferedImage image = fixed ? QqfoImgHelper.toImageFix(item) : QqfoImgHelper.toImage(item);
                                    if (image != null) {
                                        String fullPath = leaf.path.replace(".gso", String.format("-%02d-%02d.png", item.i, item.j));
                                        target.save(fullPath, image);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LogHelper.info(String.format("export image failed: %s", leaf.name), e);
            }
        }
    }
    
    private void copyPath(ActionEvent actionEvent) {
        TreeItem<Node> selected = tree_view.getSelectionModel().getSelectedItem();
        Node node = selected == null ? null : selected.getValue();
        if (node != null && node.leaf) {
            Leaf leaf = (Leaf) node;
            
            String path = leaf.path.replace("\\", "\\\\");
            StringSelection selection = new StringSelection(path);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        }
    }
}