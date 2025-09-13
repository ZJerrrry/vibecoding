package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 使用Swing代替JavaFX的简单应用程序
 * Swing在macOS上通常更稳定
 */
public class SwingApp {
    public static void main(String[] args) {
        // 确保在EDT线程中创建和修改Swing组件
        SwingUtilities.invokeLater(() -> {
            try {
                // 设置macOS上的应用程序外观
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Vibecoding Helper");
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            createAndShowGUI();
        });
    }

    private static void createAndShowGUI() {
        // 创建主窗口
        JFrame frame = new JFrame("Vibecoding Helper");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // 创建主面板，使用边界布局
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 创建标题标签
        JLabel titleLabel = new JLabel("Vibecoding Helper", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 创建中央区域 - 模拟视频区域
        JPanel videoPanel = new JPanel();
        videoPanel.setBackground(Color.DARK_GRAY);
        videoPanel.setPreferredSize(new Dimension(640, 480));
        videoPanel.setLayout(new BorderLayout());

        JLabel videoLabel = new JLabel("🎥 摄像头区域", SwingConstants.CENTER);
        videoLabel.setForeground(Color.WHITE);
        videoLabel.setFont(new Font("Arial", Font.BOLD, 18));
        videoPanel.add(videoLabel, BorderLayout.CENTER);

        JLabel statusLabel = new JLabel("应用程序已成功启动", SwingConstants.CENTER);
        statusLabel.setForeground(Color.LIGHT_GRAY);
        videoPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(videoPanel, BorderLayout.CENTER);

        // 创建右侧控制面板
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("控制面板"));

        // 状态控制
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setBorder(BorderFactory.createTitledBorder("状态管理"));

        ButtonGroup statusGroup = new ButtonGroup();
        JRadioButton collaborateBtn = new JRadioButton("协作模式", true);
        JRadioButton focusBtn = new JRadioButton("专注模式");
        JRadioButton awayBtn = new JRadioButton("暂时离开");

        statusGroup.add(collaborateBtn);
        statusGroup.add(focusBtn);
        statusGroup.add(awayBtn);

        statusPanel.add(collaborateBtn);
        statusPanel.add(focusBtn);
        statusPanel.add(awayBtn);

        controlPanel.add(statusPanel);
        controlPanel.add(Box.createVerticalStrut(10));

        // 交互控制
        JPanel interactionPanel = new JPanel();
        interactionPanel.setLayout(new FlowLayout());
        interactionPanel.setBorder(BorderFactory.createTitledBorder("非语言交流"));

        JButton thumbsUpBtn = new JButton("👍");
        JButton thinkingBtn = new JButton("🤔");
        JButton celebrateBtn = new JButton("🎉");

        thumbsUpBtn.addActionListener(e -> {
            statusLabel.setText("发送了赞同表情");
            statusLabel.setForeground(Color.YELLOW);
            // 2秒后恢复
            new Timer(2000, evt -> {
                statusLabel.setText("应用程序已成功启动");
                statusLabel.setForeground(Color.LIGHT_GRAY);
                ((Timer)evt.getSource()).stop();
            }).start();
        });

        thinkingBtn.addActionListener(e -> {
            statusLabel.setText("发送了思考表情");
            statusLabel.setForeground(Color.YELLOW);
            // 2秒后恢复
            new Timer(2000, evt -> {
                statusLabel.setText("应用程序已成功启动");
                statusLabel.setForeground(Color.LIGHT_GRAY);
                ((Timer)evt.getSource()).stop();
            }).start();
        });

        celebrateBtn.addActionListener(e -> {
            statusLabel.setText("发送了庆祝表情");
            statusLabel.setForeground(Color.YELLOW);
            // 2秒后恢复
            new Timer(2000, evt -> {
                statusLabel.setText("应用程序已成功启动");
                statusLabel.setForeground(Color.LIGHT_GRAY);
                ((Timer)evt.getSource()).stop();
            }).start();
        });

        interactionPanel.add(thumbsUpBtn);
        interactionPanel.add(thinkingBtn);
        interactionPanel.add(celebrateBtn);

        controlPanel.add(interactionPanel);

        mainPanel.add(controlPanel, BorderLayout.EAST);

        // 底部状态栏
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel bottomLabel = new JLabel("Vibecoding Helper - 使用Swing实现的稳定版本");
        bottomPanel.add(bottomLabel);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
        frame.setLocationRelativeTo(null); // 居中显示
        frame.setVisible(true);

        System.out.println("Swing界面已成功显示");
    }
}
