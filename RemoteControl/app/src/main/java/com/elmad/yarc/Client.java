package com.elmad.yarc;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.net.Socket;



public class Client extends AsyncTask<Void, Void, Void> {

    public String serverAddress;
    public int serverPort;
    public Socket clientSock;
    public DataOutputStream outToServer;
    private boolean connected;

    Client (String address, int port) {
        serverAddress = address;
        serverPort = port;
        clientSock = null;
        connected = false;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        try {
            clientSock = new Socket(serverAddress, serverPort);
            outToServer = new DataOutputStream(clientSock.getOutputStream());
            System.out.println("Connected to: " +  serverAddress);
            connected = true;
            MainActivity.setStatus();
        } catch (Exception e) {
            System.err.println("could not open socket: " + e.toString());
            connected = false;
        }
        return null;
    }

    public void sendMessage(String message) {
        byte[] buffer = new byte[1024];
        buffer = message.getBytes();
        try {
            outToServer.write(buffer);
        } catch (Exception e) {
            System.err.println("could not send message to server: " + e.toString());
        }
    }

    public boolean isConnected() {
        return connected;
    }

}
