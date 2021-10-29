package client;

import network.TCPConnection;
import network.TCPConnectionListener;
import server.ChatServer;

import java.awt.event.*;
import java.io.*;
import java.net.URL;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
//import java.util.ListIterator;

import static network.TCPConnection.SERVERPORT;

public class ClientWindow implements ActionListener, TCPConnectionListener {
    private final String path ="data";
    private static String IP_ADDR = "";
    private static int PORT = SERVERPORT;
    private static String username = "";
    private static JFrame jFrame = new JFrame();
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;
    private static final Container log = new Container();
    private static final Container audioButtonsContainer = new Container();
    private final JTextField fieldNickname = new JTextField();
    private final HintTextField fieldInput = new HintTextField("Start communication...");
    private final JScrollPane scrollPane = new JScrollPane(log);
    private TCPConnection connection;
    private byte[] voiceMessage;
    private final String audioPath="\\audios";
    private final String filePath="\\files";
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                IP_ADDR = args[0];
                if (!args[1].equals("")) PORT = Integer.parseInt(args[1]);
                username = args[2];
                new ClientWindow();
            }
        });
    }

    JButton startButton = new JButton("Start   ");
    JButton stopButton = new JButton("Stop   ");
    JButton sendButton = new JButton("Send   ");
    JButton deleteButton = new JButton("Delete");
    JButton chooseFileButton = new JButton("Ch. File");
    class HintTextField extends JTextField implements FocusListener {

        private final String hint;
        private boolean showingHint;

        public HintTextField(final String hint) {
            super(hint);
            this.hint = hint;
            this.showingHint = true;
            super.addFocusListener(this);
        }

        @Override
        public void focusGained(FocusEvent e) {
            if (this.getText().isEmpty()) {
                super.setText("");
                showingHint = false;
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (this.getText().isEmpty()) {
                super.setText(hint);
                showingHint = true;
            }
        }

        @Override
        public String getText() {
            return showingHint ? "" : super.getText();
        }
    }
    private ClientWindow() {
        chooseFileButton.addActionListener(new chooseFileListenerAction());
        startButton.addActionListener(new startListenerAction());
        stopButton.addActionListener(new stopListenerAction());
        sendButton.addActionListener(new sendListenerAction());
        deleteButton.addActionListener(new deleteListenerAction());
        fieldInput.addActionListener(this);

        audioButtonsContainer.add(startButton);
        audioButtonsContainer.add(stopButton);
        audioButtonsContainer.add(deleteButton);
        audioButtonsContainer.add(sendButton);
        audioButtonsContainer.add(chooseFileButton);


        log.setLayout(new BoxLayout(log, BoxLayout.Y_AXIS));
        audioButtonsContainer.setLayout(new BoxLayout(audioButtonsContainer,BoxLayout.Y_AXIS));
        stopButton.setEnabled(false);
        sendButton.setEnabled(false);
        deleteButton.setEnabled(false);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setSize(WIDTH, HEIGHT);
        jFrame.setLocationRelativeTo(null);

        jFrame.add(audioButtonsContainer,BorderLayout.EAST);
        jFrame.add(scrollPane, BorderLayout.CENTER);
        jFrame.add(fieldInput, BorderLayout.SOUTH);
        jFrame.add(fieldNickname, BorderLayout.NORTH);
        fieldNickname.setText(username);
        fieldNickname.setEditable(false);
        DeleteOldData();
        jFrame.setVisible(true);

        try {
            connection = new TCPConnection(this, IP_ADDR, PORT);
        } catch (IOException e) {
            printMessage("Connection exception: " + e);
        }
    }

    class sendListenerAction extends Thread implements ActionListener {
        public synchronized void actionPerformed(ActionEvent e) {
            connection.sendAudio(fieldNickname.getText() + ": ", voiceMessage);
            sendButton.setEnabled(false);
            deleteButton.setEnabled(false);
            startButton.setEnabled(true);
        }
    }
    class startListenerAction extends Thread implements ActionListener {
        public synchronized void actionPerformed(ActionEvent e) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            captureAudio();
        }
    }
    class stopListenerAction extends Thread implements ActionListener {
        public synchronized void actionPerformed(ActionEvent e) {
            stopButton.setEnabled(false);
            deleteButton.setEnabled(true);
            sendButton.setEnabled(true);
            stopCapture = true;
        }
    }
    class deleteListenerAction extends Thread implements ActionListener {
        public synchronized void actionPerformed(ActionEvent e) {
            startButton.setEnabled(true);
            deleteButton.setEnabled(false);
            sendButton.setEnabled(false);
        }
    }
    class chooseFileListenerAction extends Thread implements ActionListener{
        public synchronized void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            int res = fileChooser.showOpenDialog(jFrame);
            if (res == JFileChooser.APPROVE_OPTION) {
                File chFile = fileChooser.getSelectedFile();
                chFile.getName();
                try {
                    byte[] fileBt =  Files.readAllBytes(chFile.toPath());
                    connection.sendFile(fieldNickname.getText()+ ": " +chFile.getName()+"~", fileBt);

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }
        }

    }

    private  void DeleteOldData(){
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(ChatServer.connections.size()==0) {
            try {
                File dir = new File(path);
                File[] dataDirs = dir.listFiles();
                for (int i = 0; i < dataDirs.length; i++) {
                    File[] userDirs = dataDirs[i].listFiles();
                    for (int k = 0; k < userDirs.length; k++) {
                        File[] arrFiles = userDirs[k].listFiles();
                        for (int j = 0; j < arrFiles.length; j++) {
                            if (arrFiles[j].delete()) {
                                System.out.println(arrFiles[j].getAbsolutePath() + " файл удален");
                            } else System.out.println(arrFiles[j].getAbsolutePath() + " не обнаружено");
                        }
                        userDirs[k].delete();
                    }
                    dataDirs[i].delete();
                }
            } catch (NullPointerException e) {
                System.out.println(e);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String message = fieldInput.getText();
        if (message.equals("")) return;
        fieldInput.setText(null);
        connection.sendString(fieldNickname.getText() + ": " + message);
    }
    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {
        printMessage("Connection ready...");
    }
    @Override
    public void onReceiveString(TCPConnection tcpConnection, String value) {
        printMessage(value);
    }
    @Override
    public void onReceiveAudio(TCPConnection tcpConnection, String nickname,byte[] data){
        printVoiceMessage(nickname, data);
    }
    @Override
    public void onReceiveFile(TCPConnection tcpConnection, String nickname, byte[] data) {
        printMessage(nickname);
        downloadFile(nickname, data);
    }
    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
        printMessage("Connection lost");
    }
    @Override
    public void onException(TCPConnection tcpConnection, Exception e) {
        printMessage("Connection exception: " + e);
    }


    private synchronized void printMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JLabel msg = new JLabel(message);
                log.add(msg);
                jFrame.setVisible(false);
                jFrame.setVisible(true);
            }
        });

    }
    private synchronized void printVoiceMessage(String nickname, byte[] voice) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                saveAudio(voice);
                JLabel nick = new JLabel(nickname);
                log.add(nick);
                String new_path = path + "\\" +fieldNickname.getText() + audioPath;
                File dir = new File(new_path);
                File[] arrFiles = dir.listFiles();
                int count = arrFiles.length;
                JButton voiceMsg = new JButton(String.valueOf(count-1));
                voiceMsg.setPreferredSize(new Dimension(400,30));
                log.add(voiceMsg);
                voiceMsg.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        playAudio(Integer.parseInt(voiceMsg.getText()));
                    }
                });
                jFrame.setVisible(false);
                jFrame.setVisible(true);
            }
        });
    }

    boolean stopCapture = false;
    ByteArrayOutputStream byteArrayOutputStream;
    AudioFormat audioFormat;
    TargetDataLine targetDataLine;
    AudioInputStream audioInputStream;
    SourceDataLine sourceDataLine;

    private void playAudio(int id) {
        try{
            //устанавливаем

            byte audioData[] = getAudioFromFile(id);

            InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
            AudioFormat audioFormat = getAudioFormat();
            audioInputStream = new AudioInputStream(byteArrayInputStream, audioFormat, audioData.length/audioFormat.getFrameSize());
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();

            //Создаем поток для проигрывания
            // данных и запускаем его
            // он будет работать пока
            // все записанные данные не проиграются

            Thread playThread = new Thread(new PlayThread());
            playThread.start();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }
    private byte[] getAudioFromFile(int id){
        String new_path = path + "\\" + fieldNickname.getText() + audioPath;
        byte bytes[] = new byte[10000];
        try {
            File dir = new File(new_path); //path указывает на директорию
            File[] arrFiles = dir.listFiles();
            int count = arrFiles.length;
            FileInputStream fis = new FileInputStream(new_path + "\\audio"+id+".txt");
            bytes = fis.readAllBytes();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        return bytes;
    }
    private AudioFormat getAudioFormat(){
        float sampleRate = 8000.0F;
        //8000,11025,16000,22050,44100
        int sampleSizeInBits = 16;
        //8,16
        int channels = 1;
        //1,2
        boolean signed = true;
        //true,false
        boolean bigEndian = false;
        //true,false
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }
    class PlayThread extends Thread{
        byte tempBuffer[] = new byte[10000];

        public void run(){
            try{
                int cnt;
                // цикл пока не вернется -1

                while((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1){
                    if(cnt > 0){
                        //Пишем данные во внутренний буфер канала откуда оно передастся на звуковой выход
                        sourceDataLine.write(tempBuffer, 0, cnt);
                    }
                }

                sourceDataLine.drain();
                sourceDataLine.close();
            }catch (Exception e) {
                System.out.println(e);
                System.exit(0);
            }
        }
    }
    private void captureAudio(){
        try{
            //Установим все для захвата
            audioFormat = getAudioFormat();
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

            //Создаем поток для захвата аудио
            Thread captureThread = new Thread(new CaptureThread());
            captureThread.start();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }
    class CaptureThread extends Thread{

        byte tempBuffer[] = new byte[10000];
        public void run(){
            byteArrayOutputStream = new ByteArrayOutputStream();
            stopCapture = false;
            try{
                while(!stopCapture){
                    int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
                    if(cnt > 0){
                        //Сохраняем данные в выходной поток
                        byteArrayOutputStream.write(tempBuffer, 0, cnt);
                    }
                }
                byteArrayOutputStream.close();
                //saveAudio(byteArrayOutputStream.toByteArray());

                targetDataLine.close();

            }catch (Exception e) {
                System.out.println(e);
                System.exit(0);
            }
            voiceMessage = byteArrayOutputStream.toByteArray();
        }

    }

    private void saveAudio(byte[] voice){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(voice, 0, voice.length);
        String new_path = path + "\\" + fieldNickname.getText();
        String allPath ="";
        try {
            Files.createDirectories(Paths.get(new_path));
            allPath = new_path + audioPath;
            Files.createDirectories(Paths.get(allPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        File dir = new File(allPath);
        File[] arrFiles = dir.listFiles();
        int count = arrFiles.length;
        String newFilePath = allPath + "\\audio" + count + ".txt";

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(newFilePath));
            baos.writeTo(fos);
            fos.close();
        } catch(IOException ioe) {
            // Handle exception here
            ioe.printStackTrace();
        }

    }
    private void downloadFile(String msg, byte[] data){
        String new_path = path + "\\" + fieldNickname.getText();
        String allPath ="";
        try {
            Files.createDirectories(Paths.get(new_path));
            new_path = new_path + filePath;
            Files.createDirectories(Paths.get(new_path));
            allPath = new_path + "\\" + msg.substring(msg.indexOf(":")+1, msg.length()-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileOutputStream fos = new FileOutputStream(allPath)) {
            fos.write(data);
            fos.flush();
        }
        catch (FileNotFoundException ex) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
