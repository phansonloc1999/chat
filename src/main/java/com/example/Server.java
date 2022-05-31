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
            int client1Port = Integer.parseInt(serverClient1Reader.readLine());
            int client1Index = onlineUsers.indexOf(client1Username);
            int client2Index = onlineUsers.indexOf(client2Username);
            Socket client2Socket = clientSockets.get(client2Index);
            final BufferedWriter serverClient2Writer = new BufferedWriter(
                    new OutputStreamWriter(client2Socket.getOutputStream(), "UTF8"));

            final BufferedReader serverClient2Reader = new BufferedReader(
                    new InputStreamReader(client2Socket.getInputStream(), "UTF8"));

            serverClient2Writer.write("CHAT REQUEST");
            serverClient2Writer.newLine();
            serverClient2Writer.flush();

            serverClient2Writer.write(onlineUserIPs.get(client1Index));
            serverClient2Writer.newLine();
            serverClient2Writer.flush();

            serverClient1Writer.write(onlineUserIPs.get(client2Index));
            serverClient1Writer.newLine();
            serverClient1Writer.write("1");
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
                        String username, password;
                        while (true) {
                            try {
                                username = serverReader.readLine();
                                password = serverReader.readLine();
                                if (usernameExists(username)) {
                                    if (authenticate(username, password)) {
                                        onlineUsers.add(username);
                                        InetSocketAddress socketAddress = (InetSocketAddress) newClientSocket
                                                .getRemoteSocketAddress();
                                        onlineUserIPs.add(socketAddress.getAddress().getHostAddress());
                                        serverWriter.write("OK");
                                        serverWriter.newLine();
                                        serverWriter.flush();
                                        break;
                                    }
                                }

                                serverWriter.write("NOT OK");
                                serverWriter.newLine();
                                serverWriter.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        // Await chat requests from logged in user
                        while (true) {
                            String chatRequest;
                            try {
                                chatRequest = serverReader.readLine();
                                if (chatRequest.equals("CHAT REQUEST")) {
                                    createChatSession(newClientSocket, serverWriter, serverReader, username);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                thread.start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}