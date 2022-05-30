package com.example;

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
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Hello world!
 *
 */
public class Server {
    static ArrayList<String> onlineUsers = new ArrayList<String>();

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

    public static void main(String[] args) {
        ServerSocket listener = getServerSock(1234);

        Socket socket;
        try {
            while (true) {
                socket = listener.accept();

                final BufferedWriter serverWriter = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), "UTF8"));

                final BufferedReader serverReader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), "UTF8"));

                Thread thread = new Thread() {
                    public void run() {
                        while (true) {
                            try {
                                String username = serverReader.readLine();
                                String password = serverReader.readLine();
                                if (usernameExists(username)) {
                                    if (authenticate(username, password)) {
                                        onlineUsers.add(username);
                                        serverWriter.write("OK");
                                        serverWriter.newLine();
                                        serverWriter.flush();
                                        continue;
                                    }
                                }

                                serverWriter.write("NOT OK");
                                serverWriter.newLine();
                                serverWriter.flush();
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