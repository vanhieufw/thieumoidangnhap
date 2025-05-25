package com.movie.network;

import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;

public class SocketClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String serverAddress;
    private int serverPort;
    private volatile boolean isConnected; // Thread-safe flag
    private volatile boolean shouldStop; // Control stopping

    public SocketClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.isConnected = false;
        this.shouldStop = false;
    }

    public void start() {
        new Thread(() -> {
            try {
                socket = new Socket(serverAddress, serverPort);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                synchronized (this) {
                    isConnected = true;
                    notifyAll(); // Notify waiting threads
                }
                listenForMessages();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Không thể kết nối đến server: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                synchronized (this) {
                    isConnected = false;
                    notifyAll();
                }
            }
        }).start();
    }

    public void waitForConnection() throws InterruptedException {
        synchronized (this) {
            if (!isConnected) {
                wait(6000); // Wait up to 6 seconds
            }
        }
    }

    public void sendMessage(String message) {
        if (isConnected && out != null && !shouldStop) {
            out.println(message);
        } else {
            System.err.println("Không thể gửi tin nhắn: Kết nối không hợp lệ.");
        }
    }

    public void stop() {
        shouldStop = true;
        isConnected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Socket client đã đóng: " + socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return isConnected && !shouldStop;
    }

    public void addMessageListener(MessageListener listener) {
        new Thread(() -> listenForMessages(listener)).start();
    }

    private void listenForMessages() {
        listenForMessages(null);
    }

    private void listenForMessages(MessageListener listener) {
        try {
            String message;
            while (isConnected && !shouldStop && (message = in.readLine()) != null) {
                if (listener != null) {
                    listener.onMessage(message);
                }
            }
        } catch (IOException e) {
            if (!shouldStop) { // Only print stack trace if not intentionally stopped
                e.printStackTrace();
            }
            isConnected = false;
        } finally {
            if (!shouldStop) {
                stop();
            }
        }
    }

    public interface MessageListener {
        void onMessage(String message);
    }
}