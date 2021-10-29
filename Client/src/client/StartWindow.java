package client;

import server.ChatServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static network.TCPConnection.SERVERPORT;

public class StartWindow extends JFrame implements ActionListener {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 100;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new StartWindow();
            }
        });


    }
    private final JTextField fieldNickname = new JTextField("guest");
    private final JTextField fieldInputIp = new JTextField("127.0.0.1");
    private static String[] IP = {"", ""};

    private void splitIpPort() {
        if(fieldInputIp.getText().contains(":"))
            IP = fieldInputIp.getText().split(":", 2);
        else{
            IP[0]=fieldInputIp.getText();
            IP[1]="";
        }
    }

    JButton buttonCreate = new JButton("Create");
    class joinListenerAction extends Thread implements ActionListener {
        public synchronized void actionPerformed(ActionEvent e) {
            splitIpPort();
            ClientWindow.main(new String[]{IP[0], IP[1], fieldNickname.getText()});
            buttonCreate.setEnabled(false);
            StartWindow.this.setVisible(false);
        }
    }

    class createListenerAction implements ActionListener {
        public synchronized void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Thread createServerThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ChatServer.main(new String[]{""});
                        }
                    });
                    Thread createClientThread = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            ClientWindow.main(new String[]{"127.0.0.1", String.valueOf(SERVERPORT), fieldNickname.getText()});
                        }
                    });
                    createClientThread.start();
                    createServerThread.start();
                    buttonCreate.setEnabled(false);
                    StartWindow.this.setVisible(false);
                }
            });
        }
    }

    JButton buttonJoin = new JButton("Join");

    public StartWindow() {
        buttonJoin.addActionListener(new joinListenerAction());
        buttonCreate.addActionListener(new createListenerAction());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        add(fieldNickname, BorderLayout.NORTH);
        add(fieldInputIp, BorderLayout.SOUTH);
        add(buttonJoin, BorderLayout.EAST);
        add(buttonCreate, BorderLayout.WEST);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
}
