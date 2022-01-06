package cn.kizzzy.toolkit;

import cn.kizzzy.toolkit.controller.Controllers;
import cn.kizzzy.toolkit.controller.QqfoLocalController;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainOfQqfo extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        Controllers.start(null, primaryStage, QqfoLocalController.class);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
