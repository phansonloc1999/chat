package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Hello world!
 *
 */
public class Server {
    static ArrayList<String> onlineUsers = new ArrayList<>();

    static ArrayList<String> onlineUserIPs = new ArrayList<>();

    static ArrayList<String> chatPorts = new ArrayList<>();

    static ArrayList<Socket> clientSockets = new ArrayList<>();

    static ArrayList<Socket> chatRequestSockets = new ArrayList<>();

    private static final String CLIENT_LOGGING_OFF = "CLIENT LOGGING OFF";

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

    public static ServerSocket getServerSock(int portnum) {
        try {
            return new ServerSocket(portnum);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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

    private static void createChatSession(Socket clientSocket, BufferedWriter serverClient1Writer,
            BufferedReader serverClient1Reader, String client1Username) {
        String client2Username;
        while (true) {
            try {
                client2Username = serverClient1Reader.readLine(); // Get username that client1 wants to chat
                System.out.println("Username to chat: " + client2Username);

                if (onlineUsers.contains(client2Username)) {
                    System.out.println("Username online, creating chat session");

                    serverClient1Writer.write("OK");
                    serverClient1Writer.newLine();
                    serverClient1Writer.flush();
                    break;
                } else {
                    serverClient1Writer.write("NOT OK");
                    serverClient1Writer.newLine();
                    serverClient1Writer.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            System.out.println("Client 1 " + client1Username + " Client 2 " + client2Username);
            int client1Port = Integer.parseInt(serverClient1Reader.readLine());
            System.out.println("Port of client 1: " + client1Port);
            int client1Index = onlineUsers.indexOf(client1Username);
            int client2Index = onlineUsers.indexOf(client2Username);
            Socket client2Socket = clientSockets.get(client2Index);
            final BufferedWriter serverClient2Writer = new BufferedWriter(
                    new OutputStreamWriter(client2Socket.getOutputStream(), "UTF8"));

            Socket chatRequestSocketClient2 = chatRequestSockets.get(client2Index);
            BufferedWriter chatRequestClient2Writer = new BufferedWriter(
                    new OutputStreamWriter(chatRequestSocketClient2.getOutputStream(), "UTF8"));
            System.out.println("Clients exchange ip and port information");
            chatRequestClient2Writer.write("INCOMING CHAT REQUEST");
            chatRequestClient2Writer.newLine();
            chatRequestClient2Writer.flush();

            BufferedReader chatRequestClient2Reader = new BufferedReader(
                    new InputStreamReader(chatRequestSocketClient2.getInputStream(), "UTF8"));
            System.out.println("Receiving port of client 2");
            String client2Port = chatRequestClient2Reader.readLine();
            System.out.println("Port of client 2 is " + client2Port);

            serverClient2Writer.write(onlineUserIPs.get(client1Index));
            serverClient2Writer.newLine();
            serverClient2Writer.write(Integer.toString(client1Port));
            serverClient2Writer.newLine();
            serverClient2Writer.write(client1Username);
            serverClient2Writer.newLine();
            serverClient2Writer.flush();

            serverClient1Writer.write(onlineUserIPs.get(client2Index));
            serverClient1Writer.newLine();
            serverClient1Writer.write(client2Port);
            serverClient1Writer.newLine();
            serverClient1Writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        ServerSocket listener = getServerSock(1234);

        try {
            while (true) {
                final Socket newClientSocket = listener.accept();
                final Socket chatRequestSocket = listener.accept();
                chatRequestSockets.add(chatRequestSocket);
                clientSockets.add(newClientSocket);

                Thread thread = new Thread() {
                    public void run() {

                        BufferedWriter serverWriter = null;
                        try {
                            serverWriter = new BufferedWriter(
                                    new OutputStreamWriter(newClientSocket.getOutputStream(), "UTF8"));
                        } catch (UnsupportedEncodingException e1) {
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }

                        BufferedReader serverReader = null;
                        try {
                            serverReader = new BufferedReader(
                                    new InputStreamReader(newClientSocket.getInputStream(), "UTF8"));
                        } catch (UnsupportedEncodingException e1) {
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }

                        // Authentication
                        String option, username, password;
                        while (true) {
                            try {
                                option = serverReader.readLine();
                                username = serverReader.readLine();
                                password = serverReader.readLine();
                                if (option.equals("SIGN IN")) {
                                    if (usernameExists(username)) {
                                        if (authenticate(username, password)) {
                                            System.out.println(username + " logged on");

                                            onlineUsers.add(username);
                                            InetSocketAddress socketAddress = (InetSocketAddress) newClientSocket
                                                    .getRemoteSocketAddress();
                                            onlineUserIPs.add(socketAddress.getAddress().getHostAddress());
                                            serverWriter.write("OK");
                                            serverWriter.newLine();
                                            serverWriter.flush();
                                            break;
                                        }
                                    } else {
                                        serverWriter.write("NOT OK");
                                        serverWriter.newLine();
                                        serverWriter.flush();
                                    }
                                } else if (option.equals("REGISTER")) {
                                    if (!usernameExists(username)) {
                                        writeNewUserToFile(username, password);
                                        serverWriter.write("OK");
                                        serverWriter.newLine();
                                        serverWriter.flush();
                                    } else {
                                        serverWriter.write("NOT OK");
                                        serverWriter.newLine();
                                        serverWriter.flush();
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        while (true) {
                            String fromClient;
                            try {
                                fromClient = serverReader.readLine();
                                if (fromClient.equals("CHAT REQUEST")) {
                                    createChatSession(newClientSocket, serverWriter, serverReader, username);
                                }
                                if (fromClient.equals(CLIENT_LOGGING_OFF)) {
                                    String loggedOffUsername = serverReader.readLine();
                                    int index = onlineUsers.indexOf(loggedOffUsername);
                                    clientSockets.remove(index);
                                    System.out.println(loggedOffUsername + " logged off");
                                    return;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                thread.start();
            }
        } catch (

        IOException e) {
            e.printStackTrace();
        }
    }
}