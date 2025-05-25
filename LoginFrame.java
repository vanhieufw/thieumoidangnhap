package com.movie.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import com.movie.bus.CustomerBUS;
import com.movie.bus.AdminBUS;
import com.movie.util.PasswordEncrypter;
import com.movie.network.SocketClient;

public class LoginFrame extends JFrame {
    private final CustomerBUS customerBUS = new CustomerBUS();
    private final AdminBUS adminBUS = new AdminBUS();
    private boolean isAdminLogin;

    public LoginFrame() {
        initUI();
    }

    private void initUI() {
        setTitle("Đăng nhập hệ thống");
        setSize(300, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(245, 245, 245)); // Xám nhạt #F5F5F5
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titleLabel = new JLabel("ĐĂNG NHẬP HỆ THỐNG");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(0, 0, 0)); // Đen #000000
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        JRadioButton adminRadio = new JRadioButton("Quản trị viên");
        JRadioButton userRadio = new JRadioButton("Người dùng");
        adminRadio.setForeground(Color.WHITE); // Chữ trắng
        userRadio.setForeground(Color.WHITE);  // Chữ trắng
        ButtonGroup group = new ButtonGroup();
        group.add(adminRadio);
        group.add(userRadio);
        adminRadio.setSelected(true); // Mặc định chọn Quản trị viên
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        radioPanel.setBackground(new Color(74, 74, 74)); // Xám đậm #4A4A4A
        radioPanel.add(adminRadio); // Quản trị viên bên trái
        radioPanel.add(userRadio);  // Người dùng bên phải
        gbc.gridwidth = 2;
        gbc.gridy = 1;
        mainPanel.add(radioPanel, gbc);

        JTextField usernameField = new JTextField(15);
        usernameField.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Tên đăng nhập"));
        usernameField.setBackground(Color.WHITE);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        mainPanel.add(usernameField, gbc);

        JPasswordField passwordField = new JPasswordField(15);
        passwordField.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Mật khẩu"));
        passwordField.setBackground(Color.WHITE);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridy = 3;
        mainPanel.add(passwordField, gbc);

        JButton loginButton = new JButton("Đăng nhập");
        loginButton.setBackground(new Color(59, 130, 246)); // Xanh dương #3B82F6
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 16));
        loginButton.setPreferredSize(new Dimension(150, 40));
        gbc.gridy = 4;
        mainPanel.add(loginButton, gbc);

        JButton registerButton = new JButton("Đăng ký người dùng");
        registerButton.setBackground(new Color(59, 130, 246)); // Xanh dương #3B82F6
        registerButton.setForeground(Color.WHITE);
        registerButton.setFont(new Font("Arial", Font.BOLD, 16));
        registerButton.setPreferredSize(new Dimension(150, 40));
        gbc.gridy = 5;
        mainPanel.add(registerButton, gbc);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            isAdminLogin = adminRadio.isSelected();
            if (authenticate(username, password)) {
                SocketClient client = new SocketClient("localhost", 5000);
                client.start();
                int attempts = 20;
                while (attempts > 0 && !client.isConnected()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    attempts--;
                }
                if (client.isConnected()) {
                    client.sendMessage("Login: " + username);
                } else {
                    JOptionPane.showMessageDialog(null, "Không thể kết nối đến server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (isAdminLogin) {
                    new AdminFrame().setVisible(true);
                } else {
                    int customerId = customerBUS.getCustomerIdByUsername(username);
                    if (customerId != -1) {
                        new UserFrame(customerId).setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(null, "Không thể lấy CustomerID!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                dispose();
            } else {
                JOptionPane.showMessageDialog(null, "Đăng nhập thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerButton.addActionListener(e -> {
            new RegisterFrame(this).setVisible(true);
        });

        add(mainPanel);
        setVisible(true);
    }

    private boolean authenticate(String username, String password) {
        if (isAdminLogin) {
            return adminBUS.validateAdmin(username, password);
        } else {
            return customerBUS.validateUserPlain(username, password);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}