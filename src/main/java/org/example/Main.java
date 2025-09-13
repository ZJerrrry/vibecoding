package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个完整的JavaFX应用程序，用于Vibecoding，集成了交互功能。
 * 暂时移除摄像头功能，专注于UI界面的实现。
 */
public class Main extends Application {

    // =================================================================================
    // 核心数据模型
    // =================================================================================

    enum UserPresence { FOCUS, COLLABORATE, AWAY }
    enum VideoMode { AVATAR, REAL_VIDEO }
    enum BackgroundMode { NONE, BLUR, VIRTUAL_IMAGE }

    static class UserSessionState {
        public UserPresence presence = UserPresence.COLLABORATE;
    }

    static class PrivacySettings {
        public BackgroundMode backgroundMode = BackgroundMode.NONE;
        public boolean useAvatar = false;
    }

    static class ClientAudioMixer {
        private final Map<String, Float> channels = new ConcurrentHashMap<>();
        public void setVolume(String channelId, float volume) {
            channels.put(channelId, volume);
            System.out.printf("音频混合器: 设置通道 '%s' 的音量为 %.2f%n", channelId, volume);
        }
        public void addChannel(String id, float initialVolume) {
            channels.put(id, initialVolume);
        }
    }

    // =================================================================================
    // JavaFX UI组件
    // =================================================================================

    private Label statusLabel;
    private Label privacyLabel;
    private Label videoStatusLabel;

    // 实例化核心逻辑类
    private final UserSessionState sessionState = new UserSessionState();
    private final PrivacySettings privacySettings = new PrivacySettings();
    private final ClientAudioMixer audioMixer = new ClientAudioMixer();

    @Override
    public void start(Stage primaryStage) {
        // 为macOS特别设置，避免常见崩溃问题
        System.setProperty("glass.gtk.uiScale", "1.0");
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("javafx.animation.fullspeed", "true");

        primaryStage.setTitle("Vibecoding Helper");

        // 设置窗口属性以确保在macOS上正确显示
        primaryStage.setAlwaysOnTop(false); // 改为false避免macOS权限问题
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // 1. 视频显示区域 (左侧) - 暂时显示占位符
        createVideoPlaceholder(root);

        // 2. 控制面板 (右侧)
        VBox controlPanel = createControlPanel();
        root.setRight(controlPanel);

        Scene scene = new Scene(root, 1000, 520);
        primaryStage.setScene(scene);

        // 确保安全显示
        Platform.runLater(() -> {
            try {
                primaryStage.show();
                System.out.println("窗口已成功显示");
            } catch (Exception e) {
                System.err.println("显示窗口时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });

        System.out.println("Vibecoding Helper 已启动！");
    }

    private void createVideoPlaceholder(BorderPane root) {
        // 创建一个占位符区域，模拟视频显示
        StackPane videoPane = new StackPane();
        videoPane.setPrefSize(640, 480);
        videoPane.setStyle("-fx-background-color: #2C2C2C; -fx-border-color: #666666; -fx-border-width: 2;");

        VBox placeholder = new VBox(10);
        placeholder.setAlignment(javafx.geometry.Pos.CENTER);

        Label titleLabel = new Label("🎥 摄像头区域");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));

        videoStatusLabel = new Label("摄像头功能将在后续版本中实现");
        videoStatusLabel.setTextFill(Color.LIGHTGRAY);
        videoStatusLabel.setFont(Font.font("System", 14));

        Label infoLabel = new Label("当前显示：UI界面演示版本");
        infoLabel.setTextFill(Color.LIGHTBLUE);
        infoLabel.setFont(Font.font("System", 12));

        placeholder.getChildren().addAll(titleLabel, videoStatusLabel, infoLabel);
        videoPane.getChildren().add(placeholder);

        root.setCenter(videoPane);
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(10));
        panel.setMinWidth(300);

        // 添加各个功能的UI模块
        panel.getChildren().add(createSectionLabel("状态管理"));
        panel.getChildren().add(createStatusControls());

        panel.getChildren().add(createSectionLabel("隐私与边界"));
        panel.getChildren().add(createPrivacyControls());

        panel.getChildren().add(createSectionLabel("个性化氛围"));
        panel.getChildren().add(createAudioMixerControls());

        panel.getChildren().add(createSectionLabel("非语言交流"));
        panel.getChildren().add(createInteractionControls());

        // 用于显示当前状态的标签
        statusLabel = new Label("当前状态: 协作");
        privacyLabel = new Label("隐私设置: 无");
        VBox infoBox = new VBox(5, statusLabel, privacyLabel);
        panel.getChildren().add(infoBox);

        return panel;
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 16));
        label.setPadding(new Insets(10, 0, 0, 0));
        return label;
    }

    private Node createStatusControls() {
        ToggleGroup group = new ToggleGroup();
        RadioButton collaborateBtn = new RadioButton("协作模式");
        collaborateBtn.setToggleGroup(group);
        collaborateBtn.setSelected(true);
        RadioButton focusBtn = new RadioButton("专注模式");
        focusBtn.setToggleGroup(group);
        RadioButton awayBtn = new RadioButton("暂时离开");
        awayBtn.setToggleGroup(group);

        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (collaborateBtn.isSelected()) {
                sessionState.presence = UserPresence.COLLABORATE;
                statusLabel.setText("当前状态: 协作");
                videoStatusLabel.setText("协作模式：准备与团队成员进行编程协作");
            } else if (focusBtn.isSelected()) {
                sessionState.presence = UserPresence.FOCUS;
                statusLabel.setText("当前状态: 专注");
                videoStatusLabel.setText("专注模式：减少干扰，集中精力编程");
            } else if (awayBtn.isSelected()) {
                sessionState.presence = UserPresence.AWAY;
                statusLabel.setText("当前状态: 离开");
                videoStatusLabel.setText("离开模式：暂时不参与协作");
            }
        });

        return new HBox(10, collaborateBtn, focusBtn, awayBtn);
    }

    private Node createPrivacyControls() {
        CheckBox avatarCb = new CheckBox("使用虚拟形象");
        CheckBox blurBgCb = new CheckBox("背景模糊");

        avatarCb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            privacySettings.useAvatar = newVal;
            blurBgCb.setDisable(newVal);
            updatePrivacyLabel();
        });
        blurBgCb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            privacySettings.backgroundMode = newVal ? BackgroundMode.BLUR : BackgroundMode.NONE;
            updatePrivacyLabel();
        });

        return new VBox(5, avatarCb, blurBgCb);
    }

    private void updatePrivacyLabel() {
        String text = "隐私设置: ";
        if (privacySettings.useAvatar) {
            text += "虚拟形象";
        } else {
            text += "真实视频" + (privacySettings.backgroundMode == BackgroundMode.BLUR ? " + 背景模糊" : "");
        }
        privacyLabel.setText(text);
    }

    private Node createAudioMixerControls() {
        audioMixer.addChannel("user_Alice", 1.0f);
        audioMixer.addChannel("background_music", 0.2f);

        VBox mixer = new VBox(5);
        mixer.getChildren().add(new Label("同伴音量 (Alice):"));
        Slider aliceSlider = new Slider(0, 1, 1);
        aliceSlider.valueProperty().addListener((obs, oldVal, newVal) ->
            audioMixer.setVolume("user_Alice", newVal.floatValue()));
        mixer.getChildren().add(aliceSlider);

        mixer.getChildren().add(new Label("背景音乐:"));
        Slider musicSlider = new Slider(0, 1, 0.2);
        musicSlider.valueProperty().addListener((obs, oldVal, newVal) ->
            audioMixer.setVolume("background_music", newVal.floatValue()));
        mixer.getChildren().add(musicSlider);

        return mixer;
    }

    private Node createInteractionControls() {
        Button thumbsUpBtn = new Button("👍");
        Button thinkingBtn = new Button("🤔");
        Button celebrateBtn = new Button("🎉");

        thumbsUpBtn.setOnAction(e -> {
            System.out.println("交互: 发送 👍");
            showNotification("发送了赞同表情");
        });
        thinkingBtn.setOnAction(e -> {
            System.out.println("交互: 发送 🤔");
            showNotification("发送了思考表情");
        });
        celebrateBtn.setOnAction(e -> {
            System.out.println("交互: 发送 🎉");
            showNotification("发送了庆祝表情");
        });

        return new HBox(10, thumbsUpBtn, thinkingBtn, celebrateBtn);
    }

    private void showNotification(String message) {
        // 在视频区域短暂显示通知
        Platform.runLater(() -> {
            String originalText = videoStatusLabel.getText();
            videoStatusLabel.setText(message);
            videoStatusLabel.setTextFill(Color.YELLOW);

            // 2秒后恢复原文本
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Thread.sleep(2000);
                    return null;
                }

                @Override
                protected void succeeded() {
                    videoStatusLabel.setText(originalText);
                    videoStatusLabel.setTextFill(Color.LIGHTGRAY);
                }
            };
            new Thread(task).start();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
