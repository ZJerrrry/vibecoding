package org.example;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个完整的JavaFX应用程序，用于Vibecoding，集成了摄像头和交互功能。
 */
public class Main extends Application {

    // =================================================================================
    // 核心数据模型 (与之前类似，但现在将与UI交互)
    // =================================================================================

    enum UserPresence { FOCUS, COLLABORATE, AWAY }
    enum VideoMode { AVATAR, REAL_VIDEO }
    enum BackgroundMode { NONE, BLUR, VIRTUAL_IMAGE }

    static class UserSessionState {
        public UserPresence presence = UserPresence.COLLABORATE;
        // ... 其他状态属性
    }

    static class PrivacySettings {
        public BackgroundMode backgroundMode = BackgroundMode.NONE;
        public boolean useAvatar = false;
        // ... 其他隐私设置
    }

    static class ClientAudioMixer {
        private final Map<String, Float> channels = new ConcurrentHashMap<>();
        public void setVolume(String channelId, float volume) {
            channels.put(channelId, volume);
            System.out.printf("音频混合器: 设置通道 '%s' 的音量为 %.2f%n", channelId, volume);
        }
        public void addChannel(String id, float initialVolume) { channels.put(id, initialVolume); }
    }

    // =================================================================================
    // JavaFX UI组件 和 摄像头相关
    // =================================================================================

    private OpenCVFrameGrabber grabber;
    private volatile boolean stopCamera = false;
    private final ObjectProperty<Image> fxImageProperty = new SimpleObjectProperty<>();
    private Label statusLabel;
    private Label privacyLabel;
    private Label videoStatusLabel; // 用于在视频区域显示状态

    // 实例化核心逻辑类
    private final UserSessionState sessionState = new UserSessionState();
    private final PrivacySettings privacySettings = new PrivacySettings();
    private final ClientAudioMixer audioMixer = new ClientAudioMixer();

    @Override
    public void start(Stage primaryStage) {
        // --- 构建UI ---
        primaryStage.setTitle("Vibecoding Helper");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // 1. 视频显示区域 (左侧)
        ImageView videoView = new ImageView();
        videoView.imageProperty().bind(fxImageProperty);
        videoView.setFitWidth(640);
        videoView.setFitHeight(480);
        videoView.setPreserveRatio(true);

        videoStatusLabel = new Label("正在连接摄像头...");
        videoStatusLabel.setTextFill(Color.WHITE);
        videoStatusLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        StackPane videoPane = new StackPane(videoView, videoStatusLabel);
        videoPane.setStyle("-fx-background-color: black;");
        root.setCenter(videoPane);

        // 2. 控制面板 (右侧)
        VBox controlPanel = createControlPanel();
        root.setRight(controlPanel);

        // 启动后台任务来初始化并从摄像头捕获图像
        startCameraTask();

        Scene scene = new Scene(root, 1000, 520);
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            // 设置标志以停止后台线程
            stopCamera = true;
        });
    }

    private void startCameraTask() {
        Task<Void> cameraTask = new Task<>() {
            @Override
            protected Void call() {
                grabber = new OpenCVFrameGrabber(0); // 0 for default camera
                try {
                    // 在后台线程启动摄像头
                    grabber.start();
                    Platform.runLater(() -> videoStatusLabel.setVisible(false)); // 成功则隐藏状态标签

                    Java2DFrameConverter converter = new Java2DFrameConverter();
                    while (!stopCamera) {
                        Frame frame = grabber.grab();
                        if (frame != null) {
                            BufferedImage bImage = converter.convert(frame);
                            if (bImage != null) {
                                Platform.runLater(() -> fxImageProperty.set(SwingFXUtils.toFXImage(bImage, null)));
                            }
                        }
                    }
                } catch (Exception e) {
                    // 如果启动失败，更新UI提示用户
                    Platform.runLater(() -> {
                        videoStatusLabel.setText("摄像头启动失败！\n请检查系统权限或连接。");
                        videoStatusLabel.setTextFill(Color.RED);
                    });
                    e.printStackTrace();
                } finally {
                    // 确保在任务结束时释放资源
                    if (grabber != null) {
                        try {
                            grabber.stop();
                            grabber.release();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }
        };
        Thread cameraThread = new Thread(cameraTask);
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    @Override
    public void stop() throws Exception {
        // 在应用关闭时，确保摄像头资源被释放
        stopCamera = true;
        // 资源释放已移至后台任务的finally块中，这里可以简化
        super.stop();
    }

    /**
     * 创建右侧的控制面板
     */
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

    // --- UI构建辅助方法 ---

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
            } else if (focusBtn.isSelected()) {
                sessionState.presence = UserPresence.FOCUS;
                statusLabel.setText("当前状态: 专注");
            } else if (awayBtn.isSelected()) {
                sessionState.presence = UserPresence.AWAY;
                statusLabel.setText("当前状态: 离开");
            }
        });

        return new HBox(10, collaborateBtn, focusBtn, awayBtn);
    }

    private Node createPrivacyControls() {
        CheckBox avatarCb = new CheckBox("使用虚拟形象");
        CheckBox blurBgCb = new CheckBox("背景模糊");

        avatarCb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            privacySettings.useAvatar = newVal;
            blurBgCb.setDisable(newVal); // 使用虚拟形象时，禁用背景模糊选项
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
        aliceSlider.valueProperty().addListener((obs, oldVal, newVal) -> audioMixer.setVolume("user_Alice", newVal.floatValue()));
        mixer.getChildren().add(aliceSlider);

        mixer.getChildren().add(new Label("背景音乐:"));
        Slider musicSlider = new Slider(0, 1, 0.2);
        musicSlider.valueProperty().addListener((obs, oldVal, newVal) -> audioMixer.setVolume("background_music", newVal.floatValue()));
        mixer.getChildren().add(musicSlider);

        return mixer;
    }

    private Node createInteractionControls() {
        Button thumbsUpBtn = new Button("👍");
        Button thinkingBtn = new Button("🤔");
        Button celebrateBtn = new Button("🎉");

        // 在实际应用中，这些按钮会通过网络发送事件
        thumbsUpBtn.setOnAction(e -> System.out.println("交互: 发送 👍"));
        thinkingBtn.setOnAction(e -> System.out.println("交互: 发送 🤔"));
        celebrateBtn.setOnAction(e -> System.out.println("交互: 发送 🎉"));

        return new HBox(10, thumbsUpBtn, thinkingBtn, celebrateBtn);
    }


    public static void main(String[] args) {
        // 启动JavaFX应用
        launch(args);
    }
}
