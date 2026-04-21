package org.ps5jb.client.payloads.autoloader;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

final class LocalElfSender {
    private final String host;
    private final int port;
    private final int connectTimeoutMs;

    LocalElfSender(String host, int port, int connectTimeoutMs) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
    }

    boolean isListenerAlive() {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    int sendFile(File elfFile) throws IOException {
        byte[] payload = FileIO.readAllBytes(elfFile);
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            OutputStream out = socket.getOutputStream();
            out.write(payload);
            out.flush();
            return payload.length;
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
