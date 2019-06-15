package com.elmad.yarc;

import android.os.AsyncTask;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Client implements Runnable {

    private InetAddress serverAddress;
    private int serverPort;
    private DatagramSocket clientSock;
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
        while (!synced) {
            try {
                clientSock = new DatagramSocket();
                System.out.println("Synced with: " +  serverAddress);
                synced = true;
                MainActivity.setStatus();
            } catch (Exception e) {
                System.err.println("Could not open socket: " + e.toString());
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                System.err.println("Could not make thread sleep: " + e.toString());
            }
        }
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (lock){
                try {
                    lock.wait();
                    if (buffer != null) {
                        //System.out.println("Sending beautiful message: " + buffer.toString());
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
