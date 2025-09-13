import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

// 添加WebcamCapture支持
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

/**
 * 使用Swing实现的Vibecoding Helper，包含用户状态分析、隐私设置和手势识别
 */
public class SwingOnlyApp {
    // 摄像头相关
    private static Webcam webcam;
    private static WebcamPanel webcamPanel;
    private static BufferedImage currentFrame;
    private static ScheduledExecutorService executor;

    // 状态跟踪
    private static enum UserState { COLLABORATE, FOCUS, AWAY }
    private static UserState currentState = UserState.COLLABORATE;
    private static boolean useVirtualAvatar = false;
    private static boolean useBlurBackground = false;

    // UI组件
    private static JLabel statusLabel;
    private static JLabel stateAnalysisLabel;
    private static JPanel videoPanel;
    private static JSlider musicVolumeSlider;
    private static JSlider userVolumeSlider;

    // 音频组件
    private static Clip backgroundMusic;
    private static float musicVolume = 0.5f;
    private static float userVolume = 1.0f;

    // 手势识别结果
    private static String lastDetectedGesture = "无";
    private static long lastGestureTime = 0;

    // 虚拟头像
    private static BufferedImage avatarImage;

    public static void main(String[] args) {
        // 确保在EDT线程中创建和修改Swing组件
        SwingUtilities.invokeLater(() -> {
            try {
                // 设置macOS上的应用程序外观
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Vibecoding Helper");
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                // 加载虚拟头像
                try {
                    // 使用默认头像图片
                    avatarImage = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = avatarImage.createGraphics();
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(0, 0, 640, 480);
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 20));
                    g.drawString("虚拟头像模式", 250, 240);
                    g.dispose();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 初始化音频
                try {
                    initAudio();
                } catch (Exception e) {
                    System.out.println("音频初始化失败: " + e.getMessage());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            createAndShowGUI();
        });
    }

    private static void initAudio() {
        try {
            // 模拟背景音乐
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                    SwingOnlyApp.class.getResource("/sounds/background.wav"));
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioInputStream);

            // 设置循环播放
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);

            // 初始化音量
            setMusicVolume(musicVolume);
        } catch (Exception e) {
            System.out.println("加载音频失败，使用静音模式: " + e.getMessage());
        }
    }

    private static void setMusicVolume(float volume) {
        try {
            if (backgroundMusic != null) {
                FloatControl gainControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
                float range = gainControl.getMaximum() - gainControl.getMinimum();
                float gain = (range * volume) + gainControl.getMinimum();
                gainControl.setValue(gain);
            }
        } catch (Exception e) {
            System.out.println("设置音量失败: " + e.getMessage());
        }
    }

    private static void createAndShowGUI() {
        // 创建主窗口
        JFrame frame = new JFrame("Vibecoding Helper");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);

        // 创建主面板，使用边界布局
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 创建标题标签
        JLabel titleLabel = new JLabel("Vibecoding Helper", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 创建中央区域 - 视频显示区域
        videoPanel = new JPanel();
        videoPanel.setBackground(Color.DARK_GRAY);
        videoPanel.setPreferredSize(new Dimension(640, 480));
        videoPanel.setLayout(new BorderLayout());

        // 状态标签
        statusLabel = new JLabel("初始化摄像头中...", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        videoPanel.add(statusLabel, BorderLayout.SOUTH);

        // 状态分析标签
        stateAnalysisLabel = new JLabel("当前状态: 协作模式", SwingConstants.CENTER);
        stateAnalysisLabel.setForeground(Color.YELLOW);
        stateAnalysisLabel.setFont(new Font("Arial", Font.BOLD, 14));
        JPanel topVideoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topVideoPanel.setOpaque(false);
        topVideoPanel.add(stateAnalysisLabel);
        videoPanel.add(topVideoPanel, BorderLayout.NORTH);

        // 初始化摄像头面板
        initializeWebcam();

        mainPanel.add(videoPanel, BorderLayout.CENTER);

        // 创建右侧控制面板
        JPanel controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(300, 500));
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("控制面板"));

        // 1. 状态控制
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setBorder(BorderFactory.createTitledBorder("状态管理 (自动/手动)"));

        ButtonGroup statusGroup = new ButtonGroup();
        JRadioButton collaborateBtn = new JRadioButton("协作模式", true);
        JRadioButton focusBtn = new JRadioButton("专注模式");
        JRadioButton awayBtn = new JRadioButton("暂时离开");

        statusGroup.add(collaborateBtn);
        statusGroup.add(focusBtn);
        statusGroup.add(awayBtn);

        // 添加事件监听器
        collaborateBtn.addActionListener(e -> {
            currentState = UserState.COLLABORATE;
            stateAnalysisLabel.setText("当前状态: 协作模式");
            statusLabel.setText("已切换到协作模式");
        });

        focusBtn.addActionListener(e -> {
            currentState = UserState.FOCUS;
            stateAnalysisLabel.setText("当前状态: 专注模式");
            statusLabel.setText("已切换到专注模式");
        });

        awayBtn.addActionListener(e -> {
            currentState = UserState.AWAY;
            stateAnalysisLabel.setText("当前状态: 暂时离开");
            statusLabel.setText("已切换到离开模式");
        });

        JLabel autoStateLabel = new JLabel("状态自动检测: 已启用");
        autoStateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox autoStateCheckbox = new JCheckBox("启用自动状态检测", true);
        autoStateCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoStateCheckbox.addActionListener(e -> {
            boolean isSelected = autoStateCheckbox.isSelected();
            autoStateLabel.setText("状态自动检测: " + (isSelected ? "已启用" : "已禁用"));
            collaborateBtn.setEnabled(!isSelected);
            focusBtn.setEnabled(!isSelected);
            awayBtn.setEnabled(!isSelected);
        });

        statusPanel.add(autoStateCheckbox);
        statusPanel.add(autoStateLabel);
        statusPanel.add(Box.createVerticalStrut(10));
        statusPanel.add(collaborateBtn);
        statusPanel.add(focusBtn);
        statusPanel.add(awayBtn);

        collaborateBtn.setEnabled(false);
        focusBtn.setEnabled(false);
        awayBtn.setEnabled(false);

        controlPanel.add(statusPanel);
        controlPanel.add(Box.createVerticalStrut(10));

        // 2. 隐私设置
        JPanel privacyPanel = new JPanel();
        privacyPanel.setLayout(new BoxLayout(privacyPanel, BoxLayout.Y_AXIS));
        privacyPanel.setBorder(BorderFactory.createTitledBorder("隐私设置"));

        JCheckBox avatarCheckbox = new JCheckBox("使用虚拟形象", false);
        avatarCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox blurCheckbox = new JCheckBox("启用背景模糊", false);
        blurCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        avatarCheckbox.addActionListener(e -> {
            useVirtualAvatar = avatarCheckbox.isSelected();
            blurCheckbox.setEnabled(!useVirtualAvatar);
            updateVideoDisplay();
            statusLabel.setText("虚拟形象: " + (useVirtualAvatar ? "已启用" : "已禁用"));
        });

        blurCheckbox.addActionListener(e -> {
            useBlurBackground = blurCheckbox.isSelected();
            updateVideoDisplay();
            statusLabel.setText("背景模糊: " + (useBlurBackground ? "已启用" : "已禁用"));
        });

        privacyPanel.add(avatarCheckbox);
        privacyPanel.add(blurCheckbox);

        controlPanel.add(privacyPanel);
        controlPanel.add(Box.createVerticalStrut(10));

        // 3. 音频混合控制
        JPanel audioPanel = new JPanel();
        audioPanel.setLayout(new BoxLayout(audioPanel, BoxLayout.Y_AXIS));
        audioPanel.setBorder(BorderFactory.createTitledBorder("音频混合控制"));

        JLabel userVolumeLabel = new JLabel("用户音量:");
        userVolumeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        userVolumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
        userVolumeSlider.setMajorTickSpacing(20);
        userVolumeSlider.setMinorTickSpacing(5);
        userVolumeSlider.setPaintTicks(true);
        userVolumeSlider.setPaintLabels(true);
        userVolumeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);

        userVolumeSlider.addChangeListener(e -> {
            userVolume = userVolumeSlider.getValue() / 100.0f;
            statusLabel.setText("用户音量: " + userVolume);
        });

        JLabel musicVolumeLabel = new JLabel("背景音乐音量:");
        musicVolumeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        musicVolumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        musicVolumeSlider.setMajorTickSpacing(20);
        musicVolumeSlider.setMinorTickSpacing(5);
        musicVolumeSlider.setPaintTicks(true);
        musicVolumeSlider.setPaintLabels(true);
        musicVolumeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);

        musicVolumeSlider.addChangeListener(e -> {
            musicVolume = musicVolumeSlider.getValue() / 100.0f;
            setMusicVolume(musicVolume);
            statusLabel.setText("背景音乐音量: " + musicVolume);
        });

        audioPanel.add(userVolumeLabel);
        audioPanel.add(userVolumeSlider);
        audioPanel.add(Box.createVerticalStrut(10));
        audioPanel.add(musicVolumeLabel);
        audioPanel.add(musicVolumeSlider);

        controlPanel.add(audioPanel);
        controlPanel.add(Box.createVerticalStrut(10));

        // 4. 手势交流��制
        JPanel gesturePanel = new JPanel();
        gesturePanel.setLayout(new BoxLayout(gesturePanel, BoxLayout.Y_AXIS));
        gesturePanel.setBorder(BorderFactory.createTitledBorder("手势识别"));

        JLabel gestureLabel = new JLabel("检测到的手势: " + lastDetectedGesture);
        gestureLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox gestureCheckbox = new JCheckBox("启用手势识别", true);
        gestureCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        gestureCheckbox.addActionListener(e -> {
            boolean enabled = gestureCheckbox.isSelected();
            gestureLabel.setEnabled(enabled);
            statusLabel.setText("手势识别: " + (enabled ? "已启用" : "已禁用"));
        });

        JPanel gestureButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton thumbsUpBtn = new JButton("👍");
        JButton thinkingBtn = new JButton("🤔");
        JButton celebrateBtn = new JButton("🎉");

        thumbsUpBtn.addActionListener(e -> {
            lastDetectedGesture = "👍 赞同";
            gestureLabel.setText("检测到的手势: " + lastDetectedGesture);
            statusLabel.setText("模拟手势: 赞同");
        });

        thinkingBtn.addActionListener(e -> {
            lastDetectedGesture = "🤔 思考";
            gestureLabel.setText("检测到的手势: " + lastDetectedGesture);
            statusLabel.setText("模拟手势: 思考");
        });

        celebrateBtn.addActionListener(e -> {
            lastDetectedGesture = "🎉 庆祝";
            gestureLabel.setText("检测到的手势: " + lastDetectedGesture);
            statusLabel.setText("模拟手势: 庆祝");
        });

        gestureButtonPanel.add(thumbsUpBtn);
        gestureButtonPanel.add(thinkingBtn);
        gestureButtonPanel.add(celebrateBtn);

        gesturePanel.add(gestureCheckbox);
        gesturePanel.add(gestureLabel);
        gesturePanel.add(new JLabel("模拟手势测试:"));
        gesturePanel.add(gestureButtonPanel);

        controlPanel.add(gesturePanel);

        mainPanel.add(controlPanel, BorderLayout.EAST);

        // 底部状态栏
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel bottomLabel = new JLabel("Vibecoding Helper - 增强版 - 摄像头状态: " +
                (webcam != null ? "已连接" : "未连接"));
        bottomPanel.add(bottomLabel);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
        frame.setLocationRelativeTo(null); // 居中显示
        frame.setVisible(true);

        // 添加窗口关闭事件，确保释放资源
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupResources();
            }
        });

        // 启动视频分析线程
        startVideoAnalysis();

        System.out.println("Vibecoding Helper 界面已成功显示");
    }

    private static void initializeWebcam() {
        try {
            // 获取默认摄像头
            webcam = Webcam.getDefault();

            if (webcam != null) {
                // 设置摄像头分辨率
                webcam.setViewSize(WebcamResolution.VGA.getSize());

                // 打开摄像头
                webcam.open();

                // 创建摄像头显示面板
                webcamPanel = new WebcamPanel(webcam);
                webcamPanel.setFPSDisplayed(true);
                webcamPanel.setImageSizeDisplayed(true);
                webcamPanel.setMirrored(true);

                // 添加到视频面板
                videoPanel.add(webcamPanel, BorderLayout.CENTER);

                statusLabel.setText("摄像头已连接");
            } else {
                // 如果没有找到摄像头，显示占位符
                JLabel placeholderLabel = new JLabel("未检测到摄像头", SwingConstants.CENTER);
                placeholderLabel.setForeground(Color.WHITE);
                placeholderLabel.setFont(new Font("Arial", Font.BOLD, 18));
                videoPanel.add(placeholderLabel, BorderLayout.CENTER);

                statusLabel.setText("未找到摄像头设备");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JLabel errorLabel = new JLabel("摄像头初始化失败: " + e.getMessage(), SwingConstants.CENTER);
            errorLabel.setForeground(Color.RED);
            videoPanel.add(errorLabel, BorderLayout.CENTER);

            statusLabel.setText("摄像头初始化失败");
        }
    }

    private static void startVideoAnalysis() {
        // 创建线程池，定期分析视频
        executor = Executors.newSingleThreadScheduledExecutor();

        // 每500毫秒分析一次
        executor.scheduleAtFixedRate(() -> {
            try {
                if (webcam != null && webcam.isOpen()) {
                    // 获取当前帧
                    currentFrame = webcam.getImage();

                    // 进行视频分析
                    analyzeVideo(currentFrame);

                    // 更新显示
                    updateVideoDisplay();
                }
            } catch (Exception e) {
                System.out.println("视频分析错误: " + e.getMessage());
            }
        }, 1000, 500, TimeUnit.MILLISECONDS);
    }

    private static void analyzeVideo(BufferedImage frame) {
        if (frame == null) return;

        // 模拟视频分析，实际项目中可以接入OpenCV或其他视频分析库
        // 这里使用简单的颜色分析来模拟状态检测

        // 获取中心区域的平均亮度
        int centerX = frame.getWidth() / 2;
        int centerY = frame.getHeight() / 2;
        int sampleSize = 100;
        int totalBrightness = 0;
        int motionDetected = 0;

        // 使用前一帧检测运动
        static BufferedImage prevFrame = null;

        if (prevFrame != null) {
            for (int x = centerX - sampleSize/2; x < centerX + sampleSize/2; x++) {
                for (int y = centerY - sampleSize/2; y < centerY + sampleSize/2; y++) {
                    if (x >= 0 && x < frame.getWidth() && y >= 0 && y < frame.getHeight()) {
                        Color pixelColor = new Color(frame.getRGB(x, y));
                        Color prevPixelColor = new Color(prevFrame.getRGB(x, y));

                        int brightness = (pixelColor.getRed() + pixelColor.getGreen() + pixelColor.getBlue()) / 3;
                        int prevBrightness = (prevPixelColor.getRed() + prevPixelColor.getGreen() + prevPixelColor.getBlue()) / 3;

                        totalBrightness += brightness;

                        // 如果像素变化明显，认为有运动
                        if (Math.abs(brightness - prevBrightness) > 30) {
                            motionDetected++;
                        }
                    }
                }
            }
        }

        // 保存当前帧作为下一次比较
        prevFrame = copyImage(frame);

        // 计算平均亮度
        int pixelCount = sampleSize * sampleSize;
        int avgBrightness = totalBrightness / pixelCount;
        double motionRatio = (double)motionDetected / pixelCount;

        // 根据亮度和运动检测状态
        SwingUtilities.invokeLater(() -> {
            if (avgBrightness < 30) {
                // 很暗，可能离开了
                currentState = UserState.AWAY;
                stateAnalysisLabel.setText("当前状态: 暂时离开 (检测到低亮度)");
            } else if (motionRatio > 0.1) {
                // 运动较多，可能在协作
                currentState = UserState.COLLABORATE;
                stateAnalysisLabel.setText("当前状态: 协作模式 (检测到动作)");
            } else {
                // 亮度正常但运动少，可能在专注
                currentState = UserState.FOCUS;
                stateAnalysisLabel.setText("当前状态: 专注模式 (检测到静止)");
            }

            // 模拟手势检测
            detectGestures(frame);
        });
    }

    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    private static void detectGestures(BufferedImage frame) {
        // 实际项目中应使用机器学习模型进行手势识别
        // 这里我们使用随机模拟来演示功能

        // 每10秒最多触发一次随机手势，以避免频繁误报
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastGestureTime < 10000) {
            return;
        }

        // 1/50的概率触发手势识别
        if (Math.random() < 0.02) {
            String[] gestures = {"👍 赞同", "🤔 思考", "🎉 庆祝"};
            String detectedGesture = gestures[(int)(Math.random() * gestures.length)];

            lastDetectedGesture = detectedGesture;
            lastGestureTime = currentTime;

            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("检测到手势: " + detectedGesture);
            });
        }
    }

    private static void updateVideoDisplay() {
        if (webcam == null || !webcam.isOpen() || webcamPanel == null) {
            return;
        }

        // 根据隐私设置更新视频显示
        if (useVirtualAvatar) {
            // 使用虚拟头像
            webcamPanel.stop();
            webcamPanel.setVisible(false);

            // 显示头像
            JLabel avatarLabel = new JLabel(new ImageIcon(avatarImage));
            videoPanel.add(avatarLabel, BorderLayout.CENTER);

        } else if (useBlurBackground) {
            // 需要实现背景模糊，但简化版只做简单处理
            webcamPanel.setPainter(new BlurredWebcamPainter(webcam));
            webcamPanel.setVisible(true);
            webcamPanel.start();

        } else {
            // 正常显示
            webcamPanel.setPainter(new DefaultPainter());
            webcamPanel.setVisible(true);
            webcamPanel.start();
        }
    }

    // 实现简单的背景模糊画笔
    private static class BlurredWebcamPainter implements WebcamPanel.Painter {
        private Webcam webcam;

        public BlurredWebcamPainter(Webcam webcam) {
            this.webcam = webcam;
        }

        @Override
        public void paintPanel(WebcamPanel panel, Graphics2D g2) {
            if (webcam.isOpen()) {
                BufferedImage image = webcam.getImage();
                if (image != null) {
                    // 简单模糊实现（实际项目中应使用高斯模糊）
                    BufferedImage blurred = new BufferedImage(
                            image.getWidth(), image.getHeight(), image.getType());

                    // 简单模糊处理 - 像素块化
                    int blockSize = 10;
                    for (int x = 0; x < image.getWidth(); x += blockSize) {
                        for (int y = 0; y < image.getHeight(); y += blockSize) {
                            // 获取块的平均颜色
                            int avgR = 0, avgG = 0, avgB = 0;
                            int count = 0;

                            for (int i = 0; i < blockSize; i++) {
                                for (int j = 0; j < blockSize; j++) {
                                    int px = x + i;
                                    int py = y + j;

                                    if (px < image.getWidth() && py < image.getHeight()) {
                                        Color pixel = new Color(image.getRGB(px, py));
                                        avgR += pixel.getRed();
                                        avgG += pixel.getGreen();
                                        avgB += pixel.getBlue();
                                        count++;
                                    }
                                }
                            }

                            if (count > 0) {
                                avgR /= count;
                                avgG /= count;
                                avgB /= count;

                                Color avgColor = new Color(avgR, avgG, avgB);

                                // 使用平均颜色填充块
                                for (int i = 0; i < blockSize; i++) {
                                    for (int j = 0; j < blockSize; j++) {
                                        int px = x + i;
                                        int py = y + j;

                                        if (px < blurred.getWidth() && py < blurred.getHeight()) {
                                            blurred.setRGB(px, py, avgColor.getRGB());
                                        }
                                    }
                                }
                            }
                        }
                    }

                    g2.drawImage(blurred, 0, 0, panel.getWidth(), panel.getHeight(), null);
                }
            }
        }

        @Override
        public void paintImage(WebcamPanel panel, BufferedImage image, Graphics2D g2) {
            // 不需要实现，因为我们已经在paintPanel中处理了图像
        }
    }

    // 默认绘图类
    private static class DefaultPainter implements WebcamPanel.Painter {
        @Override
        public void paintPanel(WebcamPanel panel, Graphics2D g2) {
            // 不需要实现，默认行为
        }

        @Override
        public void paintImage(WebcamPanel panel, BufferedImage image, Graphics2D g2) {
            g2.drawImage(image, 0, 0, panel.getWidth(), panel.getHeight(), null);
        }
    }

    private static void cleanupResources() {
        // 关闭线程池
        if (executor != null) {
            executor.shutdown();
        }

        // 关闭摄像头
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }

        // 停止摄像头面板
        if (webcamPanel != null) {
            webcamPanel.stop();
        }

        // 停止音频
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
            backgroundMusic.close();
        }

        System.out.println("资源已清理");
    }
}
