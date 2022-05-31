package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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

            serverClient2Writer.write(onlineUserIPs.get(client1Index));
            serverClient2Writer.newLine();
            serverClient2Writer.write(client1Port);
            serverClient2Writer.newLine();
            serverClient2Writer.flush();

            int client2Port = Integer.parseInt(serverClient2Reader.readLine());
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

                clientSockets.add(newClientSocket);

                final BufferedWriter serverWriter = new BufferedWriter(
                        new OutputStreamWriter(newClientSocket.getOutputStream(), "UTF8"));

                final BufferedReader serverReader = new BufferedReader(
                        new InputStreamReader(newClientSocket.getInputStream(), "UTF8"));

                Thread thread = new Thread() {
                    public void run() {
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

                        createChatSession(newClientSocket, serverWriter, serverReader, username);
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