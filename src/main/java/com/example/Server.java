package com.example;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.swing.*;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Hello world!
 *
 */
public class Server {
    public static void writeNewUserToFile(String username, String password) {
        FileWriter fWriter;
        try {
            fWriter = new FileWriter("users.dat", true);
            fWriter.write(username + "\n");
            fWriter.write(DigestUtils.sha1Hex(password) + "\n");
            fWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static boolean passwordsMatched(String password, String repeatPassword) {
        return password.equals(repeatPassword);
    }

    private static boolean usernameExists(String newUsername) {
        Scanner scanner;
        try {
            scanner = new Scanner(new File("users.dat"));
            while (scanner.hasNextLine()) {
                String username = scanner.nextLine();
                if (username.equals(newUsername)) {
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void showCreateUser() {
        final JFrame createUserFrame = new JFrame();
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        createUserFrame.add(usernameLabel);
        final JTextField usernameTxtField = new JTextField();
        usernameTxtField.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        createUserFrame.add(usernameTxtField);
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        createUserFrame.add(passwordLabel);
        final JPasswordField passwordField = new JPasswordField();
        passwordField.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        createUserFrame.add(passwordField);
        JLabel repeatPasswordLabel = new JLabel("Nhập lại password");
        repeatPasswordLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        createUserFrame.add(repeatPasswordLabel);
        final JPasswordField repeatPasswordField = new JPasswordField();
        repeatPasswordField.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        createUserFrame.add(repeatPasswordField);
        JPanel buttonsPanel = new JPanel();
        JButton confirmBtn = new JButton("Xác nhận");
        confirmBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameTxtField.getText();
                String password = String.valueOf(passwordField.getPassword());
                String repeatPassword = String.valueOf(repeatPasswordField.getPassword());
                if (username.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
                    JOptionPane.showMessageDialog(createUserFrame, "Chưa điển đầy đủ các trường thông tin!", "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (usernameExists(username)) {
                    JOptionPane.showMessageDialog(createUserFrame, "Tên tài khoản đã tồn tại!", "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (passwordsMatched(password, repeatPassword)) {
                    writeNewUserToFile(usernameTxtField.getText(), password);
                    JOptionPane.showMessageDialog(createUserFrame, "Thành công!", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } else
                    JOptionPane.showMessageDialog(createUserFrame, "2 mật khẩu không trùng khớp!", "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
            }

        });
        buttonsPanel.add(confirmBtn);
        JButton exitBtn = new JButton("Thoát");
        exitBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                createUserFrame.dispose();
            }

        });
        buttonsPanel.add(exitBtn);
        buttonsPanel.setLayout(new FlowLayout());
        buttonsPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        createUserFrame.add(buttonsPanel);
        createUserFrame.setLayout(new BoxLayout(createUserFrame.getContentPane(), BoxLayout.Y_AXIS));
        createUserFrame.setTitle("Tạo người dùng mới");
        createUserFrame.pack();
        createUserFrame.setVisible(true);
    }

    public static boolean authenticate(String loginUsername, String loginPassword) {
        Scanner scanner;
        try {
            scanner = new Scanner(new File("users.dat"));
            while (scanner.hasNextLine()) {
                String username = scanner.nextLine();
                if (username.equals(loginUsername)) {
                    String password = scanner.nextLine();
                    if (password.equals(DigestUtils.sha1Hex(loginPassword)))
                        return true;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void showLogin() {
        final JFrame loginFrame = new JFrame();
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        loginFrame.add(usernameLabel);
        final JTextField usernameTxtField = new JTextField();
        usernameTxtField.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        loginFrame.add(usernameTxtField);
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        loginFrame.add(passwordLabel);
        final JPasswordField passwordField = new JPasswordField();
        passwordField.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        loginFrame.add(passwordField);

        JPanel buttonsPanel = new JPanel();
        JButton confirmBtn = new JButton("Đăng nhập");
        confirmBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameTxtField.getText(), password = String.valueOf(passwordField.getPassword());
                if (authenticate(username, password)) {
                    loginFrame.dispose();
                } else {
                    JOptionPane.showMessageDialog(loginFrame, "Tên tài khoản hoặc mật khẩu không đúng!", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }

        });
        buttonsPanel.add(confirmBtn);
        JButton exitBtn = new JButton("Thoát");
        exitBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                loginFrame.dispose();
            }

        });
        buttonsPanel.add(exitBtn);
        buttonsPanel.setLayout(new FlowLayout());
        buttonsPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        loginFrame.add(buttonsPanel);

        loginFrame.setTitle("Đăng nhập");
        loginFrame.setLayout(new BoxLayout(loginFrame.getContentPane(), BoxLayout.Y_AXIS));
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.pack();
        loginFrame.setVisible(true);
    }

    public static void showChat(final Socket socket, final BufferedWriter writer, final BufferedReader reader,
            String title) {
        final JFrame chatFrame = new JFrame();
        final JTextArea chatlogTxtArea = new JTextArea(30, 30);
        chatlogTxtArea.setEditable(false);
        chatlogTxtArea.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        JScrollPane chatlogScrollPane = new JScrollPane(chatlogTxtArea);
        chatFrame.add(chatlogScrollPane);

        JPanel jPanel = new JPanel();
        final JTextArea inputTxtArea = new JTextArea(5, 30);
        inputTxtArea.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        final JScrollPane inputTxtScrollPane = new JScrollPane(inputTxtArea);
        inputTxtScrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jPanel.add(inputTxtScrollPane);
        JButton sendBtn = new JButton("Gửi");
        sendBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    writer.write(inputTxtArea.getText() + "\n");
                    writer.flush();
                    chatlogTxtArea.append("You: " + inputTxtArea.getText() + "\n");
                    inputTxtArea.setText("");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        });
        sendBtn.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jPanel.add(sendBtn);
        jPanel.setLayout(new FlowLayout());
        jPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        chatFrame.add(jPanel);
        chatFrame.setLayout(new BoxLayout(chatFrame.getContentPane(), BoxLayout.Y_AXIS));
        chatFrame.setTitle(title);
        chatFrame.pack();
        chatFrame.setVisible(true);
        chatFrame.addWindowListener(new WindowListener() {
            public void windowClosing(WindowEvent e) {
                try {
                    writer.write("END CHAT SESSION");
                    writer.newLine();
                    writer.flush();

                    socket.close();
                    writer.close();
                    reader.close();
                    chatFrame.dispose();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        });

        String responseLine;
        try {
            while ((responseLine = reader.readLine()) != null) {
                if (responseLine.equals("END CHAT SESSION")) {
                    socket.close();
                    writer.close();
                    reader.close();
                    chatFrame.dispose();
                }
                chatlogTxtArea.append("Other: " + responseLine + "\n");
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public static ServerSocket getServerSock(int portnum) {
        try {
            return new ServerSocket(portnum);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Socket getClientSock(String ip, int portnum) {

        try {
            return new Socket(ip, portnum);
        } catch (UnknownHostException e) {
            System.err.println("The IP address of the host could not be determined!");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static void main(String[] args) {
        ServerSocket listener = getServerSock(1234);

        Socket serverSocket;
        try {
            while (true) {
                serverSocket = listener.accept();

                BufferedWriter serverWriter = new BufferedWriter(
                        new OutputStreamWriter(serverSocket.getOutputStream(), "UTF8"));

                BufferedReader serverReader = new BufferedReader(
                        new InputStreamReader(serverSocket.getInputStream(), "UTF8"));

                showChat(serverSocket, serverWriter, serverReader, "Server");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}