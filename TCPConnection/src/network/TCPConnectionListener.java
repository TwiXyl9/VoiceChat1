package network;

public interface TCPConnectionListener {
    void onConnectionReady(TCPConnection tcpConnection);
    void onReceiveString(TCPConnection tcpConnection,String value);
    void onReceiveAudio(TCPConnection tcpConnection, String nickname, byte[] data);
    void onReceiveFile(TCPConnection tcpConnection, String nickname, byte[] data);
    void onDisconnect(TCPConnection tcpConnection);
    void onException(TCPConnection tcpConnection,Exception e);
}
