package server;

import network.TCPConnection;
import network.TCPConnectionListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static network.TCPConnection.SERVERPORT;

public class ChatServer implements TCPConnectionListener {
    public static void main(String[] args) {
        new ChatServer();
    }
    public static ArrayList<TCPConnection> connections = new ArrayList<TCPConnection>();
    private ChatServer(){
        System.out.println("Server running...");
        try(ServerSocket serverSocket = new ServerSocket(SERVERPORT);) {
            while(true)
            {
                try{
                    new TCPConnection(this,serverSocket.accept());
                }catch (IOException e)
                {
                    System.out.println("TCPConnection exception: "+e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public synchronized void onConnectionReady(TCPConnection tcpConnection) {
        connections.add(tcpConnection);
        sendStrToAllConnections("Client connected: " + tcpConnection);
    }
    @Override
    public synchronized void onReceiveString(TCPConnection tcpConnection, String value) {
        sendStrToAllConnections(value);
    }
    @Override
    public void onReceiveAudio(TCPConnection tcpConnection, String nickname, byte[] data) {
        sendAudioToAllConnections(nickname, data);
    }
    @Override
    public void onReceiveFile(TCPConnection tcpConnection, String nickname, byte[] data) {
        sendFileToAllConnections(nickname,data);
    }
    @Override
    public synchronized void onDisconnect(TCPConnection tcpConnection) {
        connections.remove(tcpConnection);

        sendStrToAllConnections("Client disconnected: " + tcpConnection);
    }
    @Override
    public synchronized void onException(TCPConnection tcpConnection, Exception e) {
        System.out.println("TCPConnection exception: " + e);
    }

    private void sendStrToAllConnections(String value) {
        System.out.println(value);
        final int cnt = connections.size();
        for (int i = 0; i < cnt; i++) connections.get(i).sendString(value);
    }
    private void sendAudioToAllConnections(String nickname,byte[] voice) {
        final int cnt = connections.size();
        for (int i = 0; i < cnt; i++) connections.get(i).sendAudio(nickname, voice);
    }
    private void sendFileToAllConnections(String msg, byte[] voice){
        final int cnt = connections.size();
        for (int i = 0; i < cnt; i++) connections.get(i).sendAudio(msg, voice);
    }
}
