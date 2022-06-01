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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.*;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Hello world!
 *
 */
public class Client {
    private static Socket clientToServerSocket = null;

    private static Socket incomingChatReqSocket = null;

    private static String clientUsername = null;

    private static ArrayList<DatagramSocket> chatSockets = new ArrayList<>();

    private static final String INCOMING_CHAT_REQUEST = "INCOMING CHAT REQUEST";
    private static final String ABORT_CHAT_SESSION = "ABORT CHAT SESSION";

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

                try {
                    final BufferedWriter clientWriter;
                    final BufferedReader clientReader;
                    clientWriter = new BufferedWriter(
                            new OutputStreamWriter(clientToServerSocket.getOutputStream(), "UTF8"));

                    clientReader = new BufferedReader(
                            new InputStreamReader(clientToServerSocket.getInputStream(), "UTF8"));

                    clientWriter.write(username);
                    clientWriter.newLine();
                    clientWriter.write(password);
                    clientWriter.newLine();
                    clientWriter.flush();

                    String authenResult;
                    try {
                        authenResult = clientReader.readLine();
                        if (authenResult.equals("OK")) {
                            clientUsername = username;

                            loginFrame.dispose();

                            showInputUsernameToChat();
                        } else {
                            JOptionPane.showMessageDialog(loginFrame, "Tên tài khoản hoặc mật khẩu không đúng!",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                } catch (UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
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

        Thread thread = new Thread() {
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(incomingChatReqSocket.getInputStream(), "UTF8"));
                    while (true) {
                        String chatRequest = reader.readLine();
                        if (chatRequest.equals(INCOMING_CHAT_REQUEST)) {
                            System.out.println("INCOMING CHAT REQUEST");

                            BufferedWriter writer = new BufferedWriter(
                                    new OutputStreamWriter(incomingChatReqSocket.getOutputStream(), "UTF8"));

                            DatagramSocket chatSocket = createChatSocket();
                            int port = chatSocket.getLocalPort();
                            System.out.println("Sending info: port " + port + " to server");
                            writer.write(Integer.toString(port));
                            writer.newLine();
                            writer.flush();

                            reader = new BufferedReader(
                                    new InputStreamReader(clientToServerSocket.getInputStream(), "UTF8"));
                            String client1IP = reader.readLine();
                            int client1Port = Integer.parseInt(reader.readLine());
                            String client1Username = reader.readLine();
                            System.out.println("Client 1 IP " + client1IP + " and port " + client1Port);
                            showChat(chatSocket, InetAddress.getByName(client1IP), client1Port, client1Username);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
        thread.start();
    }

    public static void showChat(final DatagramSocket socket, final InetAddress otherUserInetAddress,
            final int otherUserPort,
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
                String text = inputTxtArea.getText() + System.lineSeparator();
                DatagramPacket dp = new DatagramPacket(text.getBytes(), text.length(),
                        otherUserInetAddress, otherUserPort);
                try {
                    socket.send(dp);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                chatlogTxtArea.append("You: " + inputTxtArea.getText() + "\n");
                inputTxtArea.setText("");
            }

        });
        sendBtn.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jPanel.add(sendBtn);
        jPanel.setLayout(new FlowLayout());
        jPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        chatFrame.add(jPanel);

        chatFrame.addWindowListener(new WindowListener() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatagramPacket dp = new DatagramPacket(ABORT_CHAT_SESSION.getBytes(), ABORT_CHAT_SESSION.length(),
                        otherUserInetAddress, otherUserPort);
                try {
                    socket.send(dp);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                socket.close();
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

            @Override
            public void windowOpened(WindowEvent e) {
            }
        });
        chatFrame.setLayout(new BoxLayout(chatFrame.getContentPane(), BoxLayout.Y_AXIS));
        chatFrame.setTitle(title);
        chatFrame.pack();
        chatFrame.setVisible(true);

        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    byte[] data = new byte[1024];
                    DatagramPacket dp = new DatagramPacket(data, data.length);
                    try {
                        socket.receive(dp);
                        String str = new String(dp.getData(), 0, dp.getLength());
                        System.out.println(str);

                        if (str.equals(ABORT_CHAT_SESSION)) {
                            socket.close();
                            chatFrame.dispose();
                        }

                        chatlogTxtArea.append("Other: " + str);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
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

    public static void showServerConfig() {
        final JFrame serverConfJFrame = new JFrame();
        JLabel serverIPLabel = new JLabel("Server IP");
        serverIPLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        serverConfJFrame.add(serverIPLabel);
        final JTextField serverIPTxtField = new JTextField();
        serverIPTxtField.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        serverConfJFrame.add(serverIPTxtField);

        JLabel serverPortLabel = new JLabel("Server Port");
        serverPortLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        serverConfJFrame.add(serverPortLabel);
        final JTextField serverPortTxtField = new JTextField();
        serverPortTxtField.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        serverConfJFrame.add(serverPortTxtField);

        JPanel buttonsPanel = new JPanel();
        JButton confirmBtn = new JButton("Xác nhận");
        confirmBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                serverConfJFrame.dispose();

                clientToServerSocket = getClientSock(
                        serverIPTxtField.getText().isEmpty() ? "localhost" : serverIPTxtField.getText(),
                        serverPortTxtField.getText().isEmpty() ? 1234 : Integer.parseInt(serverPortTxtField.getText()));

                incomingChatReqSocket = getClientSock(
                        serverIPTxtField.getText().isEmpty() ? "localhost" : serverIPTxtField.getText(),
                        serverPortTxtField.getText().isEmpty() ? 1234 : Integer.parseInt(serverPortTxtField.getText()));

                if (clientToServerSocket.isConnected() && incomingChatReqSocket.isConnected()) {
                    System.out.println("Successfully connected!");
                    showLogin();
                }
            }

        });
        buttonsPanel.add(confirmBtn);
        JButton exitBtn = new JButton("Thoát");
        exitBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                serverConfJFrame.dispose();
            }

        });
        buttonsPanel.add(exitBtn);
        buttonsPanel.setLayout(new FlowLayout());
        buttonsPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        serverConfJFrame.add(buttonsPanel);

        serverConfJFrame.setLayout(new BoxLayout(serverConfJFrame.getContentPane(), BoxLayout.Y_AXIS));
        serverConfJFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        serverConfJFrame.setTitle("Cấu hình server");
        serverConfJFrame.pack();
        serverConfJFrame.setVisible(true);
    }

    public static void showInputUsernameToChat() {
        try {
            final BufferedWriter clientWriter = new BufferedWriter(
                    new OutputStreamWriter(clientToServerSocket.getOutputStream(), "UTF8"));
            final BufferedReader clientReader = new BufferedReader(
                    new InputStreamReader(clientToServerSocket.getInputStream(), "UTF8"));

            final JFrame inputUsernameToChatFrame = new JFrame();
            JLabel inputUsernameLabel = new JLabel("Username");
            inputUsernameLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            inputUsernameToChatFrame.add(inputUsernameLabel);
            final JTextField inputUsrnameTxt = new JTextField();
            inputUsrnameTxt.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            inputUsernameToChatFrame.add(inputUsrnameTxt);

            JButton confirmBtn = new JButton("Xác nhận");
            confirmBtn.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String usernameToChat = inputUsrnameTxt.getText();

                    try {
                        clientWriter.write("CHAT REQUEST");
                        clientWriter.newLine();
                        clientWriter.write(usernameToChat);
                        clientWriter.newLine();
                        clientWriter.flush();

                        System.out.println("Sending chat request");

                        String result = clientReader.readLine();
                        if (result.equals("OK")) {
                            System.out.println("OK from server");

                            DatagramSocket chatSocket = createChatSocket();
                            clientWriter.write(Integer.toString(chatSocket.getLocalPort()));
                            clientWriter.newLine();
                            clientWriter.flush();
                            System.out.println("Creating chat session");

                            String otherUserIP = clientReader.readLine();
                            System.out.println("Other IP: " + otherUserIP);
                            int otherUserChatPort = Integer.parseInt(clientReader.readLine());
                            System.out.println("Other chat port: " + otherUserChatPort);
                            InetAddress otherUserInetAddress = InetAddress.getByName(otherUserIP);

                            showChat(chatSocket, otherUserInetAddress, otherUserChatPort, usernameToChat);
                        } else
                            JOptionPane.showMessageDialog(inputUsernameToChatFrame,
                                    "Tài khoản không online hoặc không tồn tại", "ERROR", JOptionPane.ERROR_MESSAGE);
                    } catch (UnsupportedEncodingException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            });
            inputUsernameToChatFrame.add(confirmBtn);
            inputUsernameToChatFrame
                    .setLayout(new BoxLayout(inputUsernameToChatFrame.getContentPane(), BoxLayout.Y_AXIS));
            inputUsernameToChatFrame.setTitle("Nhập tên người dùng muốn chat");
            inputUsernameToChatFrame.pack();
            inputUsernameToChatFrame.setVisible(true);
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    protected static DatagramSocket createChatSocket() {
        try {
            DatagramSocket chatSocket = new DatagramSocket();
            chatSockets.add(chatSocket);
            return chatSocket;
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        showServerConfig();
    }
}
