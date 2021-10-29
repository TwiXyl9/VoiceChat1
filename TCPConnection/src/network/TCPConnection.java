package network;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

public class TCPConnection {
    private final Socket socket;
    private final Thread rxThread;
    private final TCPConnectionListener eventListener;
    //private final BufferedReader in;
    private final DataInputStream in;
    //private final BufferedWriter out;
    private final DataOutputStream out;
    public static final int SERVERPORT = 8187;
    public TCPConnection(TCPConnectionListener eventListener,String ipAddress,int port) throws IOException{
        this(eventListener,new Socket(ipAddress,port));
    }
    public TCPConnection(TCPConnectionListener eventListener, Socket socket) throws IOException {
        this.socket = socket;
        this.eventListener = eventListener;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventListener.onConnectionReady(TCPConnection.this);
                    while (!rxThread.isInterrupted()) {

                        String data = in.readUTF();
                        if (data != null) {
                            if (count(data,":")==1 && data.lastIndexOf(":")==data.length()-2) {
                                long fileSize = in.readLong();
                                byte[] buf = new byte[10000];
                                ByteArrayOutputStream totalBuf = new ByteArrayOutputStream();
                                int count = 0;
                                int total = 0;
                                while ((count = in.read(buf)) != -1) {
                                    total += count;
                                    totalBuf.write(buf);
                                    if (total == fileSize) {
                                        break;
                                    }
                                }

                                eventListener.onReceiveAudio(TCPConnection.this, data, totalBuf.toByteArray());
                            }
                            else{
                                if(data.contains("~")){
                                    long fileSize = in.readLong();
                                    byte[] buf = new byte[10000];
                                    ByteArrayOutputStream totalBuf = new ByteArrayOutputStream();
                                    int count = 0;
                                    int total = 0;
                                    while ((count = in.read(buf)) != -1) {
                                        total += count;
                                        totalBuf.write(buf);
                                        if (total == fileSize) {
                                            break;
                                        }
                                    }
                                    eventListener.onReceiveFile(TCPConnection.this,data,totalBuf.toByteArray());
                                }
                                else {
                                    eventListener.onReceiveString(TCPConnection.this, data);
                                }
                            }
                        }

                    }
                } catch (IOException e) {
                    eventListener.onException(TCPConnection.this, e);
                } finally {
                    eventListener.onDisconnect(TCPConnection.this);
                }
            }
        });
        rxThread.start();
    }
    public static int count(String str, String target) {
        return (str.length() - str.replace(target, "").length()) / target.length();
    }

    public synchronized void sendString(String value) {
        try {
            out.writeUTF(value);
            out.flush();
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
            disconnect();
        }
    }
    public synchronized void sendAudio(String nickname, byte[] voice) {
        try {
            out.writeUTF(nickname);
            out.writeLong(voice.length);
            out.write(voice);
            out.flush();
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
            disconnect();
        }
    }
    public synchronized void sendFile(String msg, byte[] voice) {
        try {
            out.writeUTF(msg);
            out.writeLong(voice.length);
            out.write(voice);
            out.flush();
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
            disconnect();
        }
    }
    public synchronized void disconnect() {
        rxThread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
        }
    }

    @Override
    public String toString() {
        return "TCPConnection:" +socket.getInetAddress() + ":"+socket.getPort();
    }
}
