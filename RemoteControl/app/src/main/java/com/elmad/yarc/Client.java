package com.elmad.yarc;

import android.os.AsyncTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;


public class Client implements Runnable {

    private String serverAddress;
    private int serverPort;
    private Socket clientSock;
    private DataOutputStream outToServer;
    private DataInputStream inFromServer;
    private byte[] buffer;
    private boolean connected;
    private final Object lock;

    Client (String address, int port) {
        serverAddress = address;
        serverPort = port;
        lock = new Object();
        clientSock = null;
        buffer = null;
        connected = false;
    }

    @Override
    public void run() {
        try {
            clientSock = new Socket(serverAddress, serverPort);
            outToServer = new DataOutputStream(clientSock.getOutputStream());
            inFromServer = new DataInputStream(clientSock.getInputStream());
            System.out.println("Connected to: " +  serverAddress);
            connected = true;
            MainActivity.setStatus();
        } catch (Exception e) {
            System.err.println("could not open socket: " + e.toString());
            connected = false;
        }
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (lock){
                try {
                    lock.wait();
                    if (buffer != null) {
                        System.out.println("Sending beautiful message: " + buffer.toString());
                        outToServer.write(buffer);
                    }
                } catch (Exception e) {
                    System.err.println("could not send message to server: " + e.toString());
                }
                buffer = null;
            }
        }
//            System.out.println("Waiting for server...");
//            try {
//                byte[] buffer = new byte[1024];
//                while(inFromServer.read(buffer) != -1) {
//                    System.out.println("Server says: " + Arrays.toString(buffer));
//                }
//            } catch (Exception e) {
//                System.err.println("could not read from server: " + e.toString());
//            }
//        }
    }

    public void sendMessage(String message) {
        buffer = new byte[24];
        buffer = message.getBytes();
        synchronized (lock) {
            try {
                lock.notifyAll();
            } catch (Exception e) {
                System.err.println("error while buffering message");
            }
        }

    }

    public boolean isConnected() {
        return connected;
    }

}
