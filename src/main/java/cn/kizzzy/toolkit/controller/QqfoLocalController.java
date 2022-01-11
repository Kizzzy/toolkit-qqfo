package cn.kizzzy.toolkit.controller;

import cn.kizzzy.event.EventArgs;
import cn.kizzzy.helper.FileHelper;
import cn.kizzzy.helper.LogHelper;
import cn.kizzzy.helper.StringHelper;
import cn.kizzzy.javafx.TreeItemCell;
import cn.kizzzy.javafx.TreeItemComparator;
import cn.kizzzy.javafx.common.JavafxHelper;
import cn.kizzzy.javafx.common.MenuItemArg;
import cn.kizzzy.javafx.display.DisplayTabView;
import cn.kizzzy.javafx.display.DisplayType;
import cn.kizzzy.javafx.setting.ISettingDialogFactory;
import cn.kizzzy.javafx.setting.SettingDialogFactory;
import cn.kizzzy.qqfo.GsoFile;
import cn.kizzzy.qqfo.GsoFileItem;
import cn.kizzzy.qqfo.GsoFileItems;
import cn.kizzzy.qqfo.PkgFile;
import cn.kizzzy.qqfo.PkgFileItem;
import cn.kizzzy.qqfo.QqfoConfig;
import cn.kizzzy.qqfo.display.Display;
import cn.kizzzy.qqfo.display.DisplayContext;
import cn.kizzzy.qqfo.display.DisplayHelper;
import cn.kizzzy.qqfo.helper.QqfoImgHelper;
import cn.kizzzy.toolkit.extrator.PlayThisTask;
import cn.kizzzy.toolkit.view.AbstractView;
import cn.kizzzy.vfs.IPackage;
import cn.kizzzy.vfs.ITree;
import cn.kizzzy.vfs.handler.BufferedImageHandler;
import cn.kizzzy.vfs.handler.JsonFileHandler;
import cn.kizzzy.vfs.handler.PkgFileHandler;
import cn.kizzzy.vfs.pack.FilePackage;
import cn.kizzzy.vfs.pack.QqfoPackage;
import cn.kizzzy.vfs.tree.IdGenerator;
import cn.kizzzy.vfs.tree.Leaf;
import cn.kizzzy.vfs.tree.Node;
import cn.kizzzy.vfs.tree.QqfoTreeBuilder;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

abstract class QqfoViewBase extends AbstractView {
    
    @FXML
    protected ChoiceBox<String> show_choice;
    
    @FXML
    protected TextField filterValue;
    
    @FXML
    protected CheckBox include_leaf;
    
    @FXML
    protected CheckBox lock_tab;
    
    @FXML
    protected TreeView<Node<PkgFileItem>> tree_view;
    
    @FXML
    protected DisplayTabView display_tab;
    
    @FXML
    protected ProgressBar progress_bar;
    
    @FXML
    protected Label tips;
    
    @Override
    public String getName() {
        return "QqfoDisplayer";
    }
}

@MenuParameter(path = "辅助/QQ幻想/解包器(本地)")
@PluginParameter(url = "/fxml/toolkit/qqfo_local_view.fxml", title = "QQ幻想(解包)")
public class QqfoLocalController extends QqfoViewBase implements DisplayContext, Initializable {
    
    protected static final String CONFIG_PATH = "qqfo/local.config";
    
    protected static final TreeItemComparator comparator
        = new TreeItemComparator();
    
    protected IPackage userVfs;
    protected QqfoConfig config;
    protected ISettingDialogFactory dialogFactory;
    
    protected IPackage vfs;
    protected ITree<PkgFileItem> tree;
    
    protected Display display = new Display();
    protected TreeItem<Node<PkgFileItem>> dummyTreeItem;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userVfs = new FilePackage(System.getProperty("user.home") + "/.user");
        userVfs.getHandlerKvs().put(QqfoConfig.class, new JsonFileHandler<>(QqfoConfig.class));
        
        config = userVfs.load(CONFIG_PATH, QqfoConfig.class);
        config = config != null ? config : new QqfoConfig();
        
        JavafxHelper.initContextMenu(tree_view, () -> stage.getScene().getWindow(), new MenuItemArg[]{
            new MenuItemArg(0, "设置", this::openSetting),
            new MenuItemArg(1, "加载PKG", this::loadPackage),
            new MenuItemArg(2, "导出/文件", this::exportFile),
            new MenuItemArg(2, "导出/图片", this::exportImage),
            new MenuItemArg(3, "复制路径", this::copyPath),
        });
        
        addListener(DisplayType.TOAST_TIPS, this::toastTips);
        addListener(DisplayType.SHOW_TEXT, this::onDisplayEvent);
        addListener(DisplayType.SHOW_IMAGE, this::onDisplayEvent);
        addListener(DisplayType.SHOW_TABLE, this::onDisplayEvent);
        
        dummyTreeItem = new TreeItem<>();
        tree_view.setRoot(dummyTreeItem);
        tree_view.setShowRoot(false);
        tree_view.getSelectionModel().selectedItemProperty().addListener(this::onSelectItem);
        tree_view.setCellFactory(callback -> new TreeItemCell());
        
        lock_tab.selectedProperty().addListener((observable, oldValue, newValue) -> {
            display_tab.setPin(newValue);
        });
        
        DisplayHelper.load();
    }
    
    @Override
    public void stop() {
        play = false;
        if (playThisTask != null) {
            playThisTask.stop();
        }
        
        if (tree != null) {
            tree.stop();
        }
        
        userVfs.save(CONFIG_PATH, config);
        
        super.stop();
    }
    
    @Override
    public int provideIndex() {
        return show_choice.getSelectionModel().getSelectedIndex();
    }
    
    @Override
    public boolean isFilterColor() {
        return false;//image_filter.isSelected();
    }
    
    protected void toastTips(EventArgs args) {
        Platform.runLater(() -> tips.setText((String) args.getParams()));
    }
    
    protected void onDisplayEvent(final EventArgs args) {
        Platform.runLater(() -> {
            display_tab.show(args.getType(), args.getParams());
        });
    }
    
    protected void loadPackage(ActionEvent actionEvent) {
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
        IPackage iPackage = new FilePackage(file.getParent());
        iPackage.getHandlerKvs().put(PkgFile.class, new PkgFileHandler());
        
        PkgFile pkgFile = iPackage.load(FileHelper.getName(file.getAbsolutePath()), PkgFile.class);
        
        tree = new QqfoTreeBuilder(pkgFile, new IdGenerator()).build();
        
        vfs = new QqfoPackage(file.getParent(), tree);
        
        Platform.runLater(() -> {
            dummyTreeItem.getChildren().clear();
            
            final List<Node<PkgFileItem>> nodes = tree.listNode(0);
            for (Node<PkgFileItem> node : nodes) {
                dummyTreeItem.getChildren().add(new TreeItem<>(node));
            }
        });
    }
    
    @Override
    public <T> T load(String path, Class<T> clazz) {
        if (vfs != null) {
            return vfs.load(path, clazz);
        }
        return null;
    }
    
    protected void onSelectItem(Observable observable, TreeItem<Node<PkgFileItem>> oldValue, TreeItem<Node<PkgFileItem>> newValue) {
        if (newValue != null) {
            Node<PkgFileItem> folder = newValue.getValue();
            Leaf<PkgFileItem> thumbs = null;
            
            if (folder.leaf) {
                thumbs = (Leaf<PkgFileItem>) folder;
            } else {
                newValue.getChildren().clear();
                
                Iterable<Node<PkgFileItem>> list = folder.children.values();
                for (Node<PkgFileItem> temp : list) {
                    TreeItem<Node<PkgFileItem>> child = new TreeItem<>(temp);
                    newValue.getChildren().add(child);
                }
                newValue.getChildren().sort(comparator);
            }
            
            if (thumbs != null) {
                if (display != null) {
                    display.stop();
                }
                display = DisplayHelper.newDisplay(this, thumbs.path);
                display.init();
            }
        }
    }
    
    protected void onChangeLayer(Observable observable, Number oldValue, Number newValue) {
        display.select(newValue.intValue());
    }
    
    @FXML
    protected void openSetting(ActionEvent actionEvent) {
        if (dialogFactory == null) {
            dialogFactory = new SettingDialogFactory(stage);
        }
        dialogFactory.show(config);
    }
    
    @FXML
    protected void showPrev(ActionEvent actionEvent) {
        display.prev();
    }
    
    @FXML
    protected void showNext(ActionEvent actionEvent) {
        display.next();
    }
    
    private boolean play;
    
    @FXML
    protected void play(ActionEvent actionEvent) {
        if (display != null) {
            play = !play;
            ((Button) actionEvent.getSource()).setText(play ? "暂停" : "播放");
            if (play) {
                new Thread(() -> {
                    while (play) {
                        try {
                            Platform.runLater(() -> display.play());
                            Thread.sleep(125);
                        } catch (InterruptedException e) {
                            LogHelper.error(null, e);
                        }
                    }
                }).start();
            }
        }
    }
    
    private boolean playThis;
    private PlayThisTask playThisTask;
    
    @FXML
    private void playThis(ActionEvent event) {
        playThis = !playThis;
        ((Button) event.getSource()).setText(playThis ? "暂停播放" : "连续播放");
        if (playThis) {
            TreeItem<Node<PkgFileItem>> selected = tree_view.getSelectionModel().getSelectedItem();
            
            List<Display> displays = new ArrayList<>();
            
            List<Leaf<PkgFileItem>> fileList = tree.listLeaf(selected.getValue());
            for (Leaf<PkgFileItem> file : fileList) {
                displays.add(DisplayHelper.newDisplay(this, file.path));
            }
            
            playThisTask = new PlayThisTask(displays);
            
            new Thread(playThisTask).start();
        } else {
            if (playThisTask != null) {
                playThisTask.stop();
            }
        }
    }
    
    @FXML
    protected void exportFile(ActionEvent event) {
        TreeItem<Node<PkgFileItem>> selected = tree_view.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        
        if (StringHelper.isNullOrEmpty(config.export_file_path) || !new File(config.export_file_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存文件的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_file_path = file.getAbsolutePath();
        }
        
        IPackage target = null;
        Node<PkgFileItem> node = selected.getValue();
        
        List<Leaf<PkgFileItem>> list = tree.listLeaf(node, true);
        for (Leaf<PkgFileItem> leaf : list) {
            try {
                if (target == null) {
                    String pkgName = leaf.pack.replace(".pkg", "");
                    target = new FilePackage(config.export_file_path + "/" + pkgName);
                }
                
                byte[] data = vfs.load(leaf.path, byte[].class);
                target.save(leaf.path, data);
            } catch (Exception e) {
                LogHelper.info(String.format("export file failed: %s", leaf.path), e);
            }
        }
    }
    
    @FXML
    protected void exportImage(ActionEvent event) {
        final TreeItem<Node<PkgFileItem>> selected = tree_view.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        
        if (StringHelper.isNullOrEmpty(config.export_image_path) || !new File(config.export_image_path).exists()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择保存图片的文件夹");
            File file = chooser.showDialog(stage);
            if (file == null) {
                return;
            }
            config.export_image_path = file.getAbsolutePath();
        }
        
        IPackage target = null;
        Node<PkgFileItem> node = selected.getValue();
        
        List<Leaf<PkgFileItem>> list = tree.listLeaf(selected.getValue(), true);
        for (Leaf<PkgFileItem> leaf : list) {
            try {
                if (target == null) {
                    String pkgName = leaf.pack.replace(".pkg", "");
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
                                    BufferedImage image = QqfoImgHelper.toImage(item);
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
    
    protected void copyPath(ActionEvent actionEvent) {
        TreeItem<Node<PkgFileItem>> selected = tree_view.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Node<PkgFileItem> node = selected.getValue();
            if (node.leaf) {
                Leaf<PkgFileItem> leaf = (Leaf<PkgFileItem>) node;
                
                String path = leaf.path.replace("\\", "\\\\");
                StringSelection selection = new StringSelection(path);
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(selection, selection);
            }
        }
    }
    
    private TreeItem<Node<PkgFileItem>> filterRoot;
    
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
            filterRoot = new TreeItem<>(new Node<>(0, "[Filter]"));
            dummyTreeItem.getChildren().add(filterRoot);
        }
        
        filterRoot.getChildren().clear();
        
        List<Node<PkgFileItem>> list = tree.listNodeByRegex(regex);
        for (Node<PkgFileItem> folder : list) {
            filterRoot.getChildren().add(new TreeItem<>(folder));
        }
        
        filterRoot.getChildren().sort(comparator);
    }
}