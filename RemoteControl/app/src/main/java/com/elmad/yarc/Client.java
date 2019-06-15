package com.elmad.yarc;

import android.os.AsyncTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;


public class Client implements Runnable {

    private InetAddress serverAddress;
    private int serverPort;
    private DatagramSocket clientSock;
//    private DataOutputStream outToServer;
//    private DataInputStream inFromServer;
    private byte[] buffer;
    private boolean synced;
    private final Object lock;

    Client (String address, int port) {
        try {
            serverAddress = InetAddress.getByName(address);
        } catch (Exception e) {
            System.err.println("Could not get address: " + e.toString());
            serverAddress = null;
        }
        serverPort = port;
        lock = new Object();
        clientSock = null;
        buffer = null;
        synced = false;
    }

    @Override
    public void run() {
        try {
            clientSock = new DatagramSocket();
//            outToServer = new DataOutputStream(clientSock.getOutputStream());
//            inFromServer = new DataInputStream(clientSock.getInputStream());
            System.out.println("Synced with: " +  serverAddress);
            synced = true;
            MainActivity.setStatus();
        } catch (Exception e) {
            System.err.println("Could not open socket: " + e.toString());
            synced = false;
        }
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (lock){
                try {
                    lock.wait();
                    if (buffer != null) {
                        System.out.println("Sending beautiful message: " + buffer.toString());
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                                serverAddress, serverPort);
                        clientSock.send(packet);
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
                System.err.println("Error while buffering message");
            }
        }

    }

    public boolean isSynced() {
        return synced;
    }

}
