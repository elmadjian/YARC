package com.elmad.yarc;

import android.os.AsyncTask;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Client implements Runnable {

    private InetAddress serverAddress;
    private InetAddress broadcast;
    private int serverPort;
    private DatagramSocket clientSock;
    private byte[] buffer;
    private boolean synced;
    private boolean ACK;
    private boolean WoL;
    private final Object lock;

    Client (String address, int port) {
        try {
            serverAddress = InetAddress.getByName(address);
            String baddr = address.replaceAll("(\\d+\\.\\d+\\.\\d+\\.)\\d+", "$1");
            baddr += "255";
            broadcast = InetAddress.getByName(baddr);
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
            clientSock.setSoTimeout(3000);
        } catch (Exception e) {
            System.err.println("Could not open socket: " + e.toString());
        }
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (lock){
                try {
                    lock.wait();
                    if (buffer != null) {
                        if (WoL) {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                                    broadcast, 9);
                            clientSock.send(packet);
                            WoL = false;
                        }
                        else {
                            //System.out.println("Sending beautiful message: " + buffer.toString());
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                                    serverAddress, serverPort);
                            clientSock.send(packet);
                        }
                    }
                    if (ACK) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        clientSock.receive(packet);
                        synced = true;
                        MainActivity.setStatus();
                    }
                } catch (Exception e) {
                    System.err.println("could not reach server: " + e.toString());
                    synced = false;
                    MainActivity.setStatus();
                }
                buffer = null;
            }
        }
    }

    public void sendMessage(String message) {
        if (message.equals("ack\n"))
            ACK = true;
        else
            ACK = false;
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

    public void sendWoL(byte[] bytes) {
        buffer = bytes;
        WoL = true;
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
