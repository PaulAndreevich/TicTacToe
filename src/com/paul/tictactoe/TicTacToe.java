package com.paul.tictactoe;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import static com.sun.scenario.effect.impl.prism.PrEffectHelper.render;
import static java.time.Clock.tick;


public class TicTacToe implements Runnable{

    private String ip = "localhost";
    private int port = 22222;
    private Scanner scanner = new Scanner(System.in);
    private JFrame frame;
    private final int WIDTH = 526;
    private final int HEIGHT = 527;
    private Thread thread;

    private Painter painter;
    private Socket socket;  //connect to the server
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    private ServerSocket serverSocket;
    private BufferedImage board;
    private BufferedImage redX;
    private BufferedImage blueX;
    private BufferedImage blueCircle;
    private BufferedImage redCircle;

    private String[] spaces= new String[9];

    private boolean yourTurn= false;
    private boolean circle = true;
    private boolean accepted = false;
    private boolean unableToConnectWithOpponent = false;
    private boolean won = false;
    private boolean enemywon = false;
    private boolean tie = false;

    private int lenghtOfSpace = 160;
    private int errors = 0;
    private int firstSpot = -1;
    private int secondSpot = -1;

    private Font font = new Font("Verdana",Font.BOLD,32);
    private Font smallerfont = new Font("Verdana", Font.BOLD, 27);
    private Font largerfont = new Font("Verdana",Font.BOLD, 50);

    private String waitingstring = "Waiting for another player";
    private String unableToCommunicateWithOpponentString = "Unable to communicate with opponent";
    private String wonstring  = "You won!";
    private String enemywonString = "Opponent won!";
    private String tieString = "It's a Tie!";

    private int[][] wins = new int[][]{
            {0,1,2}, {3,4,5}, {6,7,8},
            {0,3,6}, {1,4,7}, {2,5,8},
            {0,4,8}, {2,4,6}
    };

    /*
    0,1,2
    3,4,5
    6,7,8
     */
    public TicTacToe() {
        System.out.println("Please input the IP:");
        ip = scanner.nextLine();
        System.out.println("Please input the port:");
        port = scanner.nextInt();
        while (port < 1 || port > 65535) {
            System.out.println("Port you have entered is invalid, please import another port");
            port = scanner.nextInt();
        }

        loadImages();

        painter = new Painter();
        painter.setPreferredSize(new Dimension(WIDTH,HEIGHT));

        if (!connect()) initializeServer();

        frame = new JFrame();
        frame.setTitle("Tic-Tac-Toe");
        frame.setContentPane(painter);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);

        thread = new Thread(this, "TicTacToe");
        thread.start();
    }

    public void run(){
        while(true){
            tick();
            painter.repaint();

            if(!circle && !accepted){
                listenForServerRequest();

            }
        }
    }

    private void render(Graphics g) {
        g.drawImage(board, 0,0, null);
        if (unableToConnectWithOpponent) {
            g.setColor(Color.RED);
            g.setFont(smallerfont);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(unableToCommunicateWithOpponentString);
            g.drawString(unableToCommunicateWithOpponentString,WIDTH/2 - stringWidth/2,HEIGHT/2);
            return;
        }

        if (accepted){
            for(int i = 0;i< spaces.length; i++){
                if (spaces[i] != null) {
                    if (spaces[i].equals("X")) {
                        if (circle) {
                            g.drawImage(redX, (i % 3) * lenghtOfSpace + 10 * (i % 3), (int) (i / 3) * lenghtOfSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(blueX, (i % 3) * lenghtOfSpace + 10 * (i % 3), (int) (i / 3) * lenghtOfSpace + 10 * (int) (i / 3), null);
                        }
                    } else if (spaces[i].equals("O")) {
                        if (circle) {
                            g.drawImage(blueCircle, (i % 3) * lenghtOfSpace + 10 * (i % 3), (int) (i / 3) * lenghtOfSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(redCircle, (i % 3) * lenghtOfSpace + 10 * (i % 3), (int) (i / 3) * lenghtOfSpace + 10 * (int) (i / 3), null);
                        }
                    }
                }
            }
            if (won || enemywon){
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(10));
                g.setColor(Color.BLACK);
                g.drawLine(firstSpot % 3 * lenghtOfSpace + 10 * firstSpot % 3 + lenghtOfSpace / 2, (int) (firstSpot / 3) * lenghtOfSpace + 10 * (int)(firstSpot / 3) + lenghtOfSpace / 2, secondSpot % 3 * lenghtOfSpace + 10 * secondSpot % 3 + lenghtOfSpace / 2, (int) (secondSpot / 3) * lenghtOfSpace + 10 * (int) (secondSpot / 3) + lenghtOfSpace / 2);

                g.setColor(Color.RED);
                g.setFont(largerfont);
                if(won){
                    int stringWidth = g2.getFontMetrics().stringWidth(wonstring);
                    g.drawString(wonstring, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);

                } else if (enemywon){
                    int stringWidth = g2.getFontMetrics().stringWidth(enemywonString);
                    g.drawString(enemywonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);

                }
            }
            if (tie){
                Graphics2D g2 = (Graphics2D) g;
                g.setColor(Color.BLACK);
                g.setFont(largerfont);
                int stringWidth = g2.getFontMetrics().stringWidth(tieString);
                g2.drawString(tieString, WIDTH/2 - stringWidth/ 2, HEIGHT / 2);
            }
        } else {
            g.setColor(Color.RED);
            g.setFont(font);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(waitingstring);
            g.drawString(waitingstring, WIDTH / 2  - stringWidth / 2, HEIGHT /2);

        }
    }

    private void listenForServerRequest(){
        Socket socket = null;
        try{
            socket = serverSocket.accept(); //will be blocked until a connection is made
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            accepted = true;
            System.out.println("CLIENT HAS REQUESTED TO JOIN");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void tick(){
        if (errors >= 10) unableToConnectWithOpponent = true;

        if(!yourTurn && !unableToConnectWithOpponent){
            try{
                int space = dataInputStream.readInt();
                if (circle) spaces[space] = "X";
                else spaces[space] = "O";
                checkForEnemyWin();
                checkForTie();
                yourTurn = true;
            }catch (IOException e){
                e.printStackTrace();
                errors++;

            }
        }
    }

    private void checkForWin(){
        for (int i = 0; i < wins.length; i++){
            if (spaces[wins[i][0]] != null && spaces[wins[i][1]] != null && spaces[wins[i][2]] != null) {
                if (circle) {
                    if (spaces[wins[i][0]].equals("O") && spaces[wins[i][1]].equals("O") && spaces[wins[i][2]].equals("O")) {
                        firstSpot = wins[i][0];
                        secondSpot = wins[i][2];
                        won = true;
                    }
                } else {
                    if (spaces[wins[i][0]].equals("X") && spaces[wins[i][1]].equals("X") && spaces[wins[i][2]].equals("X")) {
                        firstSpot = wins[i][0];
                        secondSpot = wins[i][2];
                        won = true;
                    }
                }
            }
        }
    }

    private void checkForEnemyWin(){
        for (int i = 0; i < wins.length; i++){
            if (spaces[wins[i][0]] != null && spaces[wins[i][1]] != null && spaces[wins[i][2]] != null) {
                if (circle) {
                    if (spaces[wins[i][0]].equals("X") && spaces[wins[i][1]].equals("X") && spaces[wins[i][2]].equals("X")) {
                        firstSpot = wins[i][0];
                        secondSpot = wins[i][2];
                        enemywon = true;
                    }
                } else {
                    if (spaces[wins[i][0]].equals("O") && spaces[wins[i][1]].equals("O") && spaces[wins[i][2]].equals("O")) {
                        firstSpot = wins[i][0];
                        secondSpot = wins[i][2];
                        enemywon = true;
                    }
                }
            }
        }
    }

    private void checkForTie(){
        for(int i = 0; i < spaces.length;i++){
            if(spaces[i] == null)
                return;
        }
        tie = true;
    }

    private void loadImages(){

        try {
            board = ImageIO.read(new File("/Users/admin/IdeaProjects/TicTacToe/out/TicTacToe.jar/board.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            board = ImageIO.read(new FileInputStream("/Users/admin/IdeaProjects/TicTacToe/out/TicTacToe.jar/board.png"));
            redCircle = ImageIO.read(new FileInputStream("/Users/admin/IdeaProjects/TicTacToe/out/TicTacToe.jar/redCircle.png"));
            blueX = ImageIO.read(new FileInputStream("/Users/admin/IdeaProjects/TicTacToe/out/TicTacToe.jar/blueX.png"));
            blueCircle = ImageIO.read(new FileInputStream("/Users/admin/IdeaProjects/TicTacToe/out/TicTacToe.jar/blueCircle.png"));
            redX = ImageIO.read(new FileInputStream("/Users/admin/IdeaProjects/TicTacToe/out/TicTacToe.jar/redX.png"));
            System.out.println("All loaded");
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private boolean connect() {
        try {
            socket = new Socket(ip, port);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            accepted = true;
        } catch (IOException e) {
            System.out.println("Unable to connect to the address: " + ip + ":" + port + " | Starting a server");
            return false;
        }
        System.out.println("Successfully connected to the server.");
        return true;
    }

    private void initializeServer() {
        try {
            serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
        } catch (Exception e) {
            e.printStackTrace();
        }
        yourTurn = true;
        circle = false;
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        TicTacToe ticTacToe = new TicTacToe();

    }

    private class Painter extends JPanel implements MouseListener {

        public Painter(){
            setFocusable(true);
            requestFocus();
            setBackground(Color.WHITE);
            addMouseListener(this);
        }

        @Override
        public void paintComponent(Graphics g){
            super.paintComponent(g);
            render(g);
            Toolkit.getDefaultToolkit().sync();
        }
        public void setPreferredSize(Dimension dimension) {
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (accepted) {
                if (yourTurn && !unableToConnectWithOpponent && !won && !enemywon) {
                    int x = e.getX() / lenghtOfSpace;
                    int y = e.getY() / lenghtOfSpace;
                    y *= 3;
                    int position = x + y;

                    if (spaces[position] == null) {
                        if (!circle) spaces[position] = "X";
                        else spaces[position] = "O";
                        yourTurn = false;
                        repaint();
                        Toolkit.getDefaultToolkit().sync();

                        try {
                            dataOutputStream.writeInt(position);
                            dataOutputStream.flush();
                        } catch (IOException e1) {
                            errors++;
                            e1.printStackTrace();
                        }

                        System.out.println("DATA WAS SENT");
                        checkForWin();
                        checkForTie();

                    }
                }
            }
        }


        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }
}
