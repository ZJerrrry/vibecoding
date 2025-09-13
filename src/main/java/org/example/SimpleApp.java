package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Vibecoding简化版 - 专为解决macOS兼容性问题
 */
public class SimpleApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 最小化的设置，减少macOS兼容性问题的可能性
        primaryStage.setTitle("Vibecoding Helper - 简化版");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // 创建中央显示区域
        VBox centerArea = new VBox(20);
        centerArea.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Vibecoding Helper");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label statusLabel = new Label("应用程序成功启动！");
        statusLabel.setFont(Font.font("System", 16));
        statusLabel.setTextFill(Color.GREEN);

        Button testButton = new Button("测试交互");
        testButton.setOnAction(e -> {
            statusLabel.setText("按钮点击成功！UI正常工作");
            statusLabel.setTextFill(Color.BLUE);
        });

        centerArea.getChildren().addAll(titleLabel, statusLabel, testButton);
        root.setCenter(centerArea);

        // 底部区域 - 说明文字
        HBox bottomArea = new HBox();
        bottomArea.setAlignment(Pos.CENTER);
        Label infoLabel = new Label("这是一个简化版的Vibecoding Helper，用于测试基本功能");
        bottomArea.getChildren().add(infoLabel);
        root.setBottom(bottomArea);

        // 使用较小的窗口尺寸，减少在macOS上出现的渲染问题
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("简化版应用程序已启动 - 窗口应该可见");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
