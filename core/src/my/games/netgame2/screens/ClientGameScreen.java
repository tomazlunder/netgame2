package my.games.netgame2.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import my.games.netgame2.MainClass;
import my.games.netgame2.game.Constants;
import my.games.netgame2.game.GameClass;
import my.games.netgame2.game.Player;
import my.games.netgame2.server.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientGameScreen extends GameScreen{

    public final int WAITING_FOR_LFG_ACK = 0;
    public final int WAITING_FOR_GAMEINFO = 1;
    public final int WAITING_FOR_START = 2;
    public final int GAME_ON = 3;
    public final int GAME_PAUSED = 4;
    public final int GAME_ENDED = 5;
    public int connectionState;

    int playerNumber;
    Player thisPlayer;
    UDPproducer udpProducer;
    BlockingQueue<Message> blockingQueue;

    private static DatagramSocket socket;

    private static byte[] buf;

    private int socketOutPort;
    private int socketInPort;

    private float pauseTime;
    private int lastScored;

    //TODO: Change
    private static final String serverAdress = "localhost";

    public ClientGameScreen(MainClass mainClass) throws IOException {
        super(mainClass);
        this.gameClass.state = gameClass.RUNNING;
        connectionState = -1;
        playerNumber = -1;
        lastScored = -1;
        thisPlayer = null;

        blockingQueue = new ArrayBlockingQueue<Message>(128);

        socket = new DatagramSocket();
        socketOutPort = socket.getPort();
        udpProducer = new UDPproducer(blockingQueue);
        new Thread(udpProducer).start();
        socketInPort = udpProducer.getPort();
        System.out.println("Spawned UDP producer thread");

        sendLFG();
    }

    //Not updating gameClass like gameScreen
    @Override
    public void render(float deltaTime) {
        if(connectionState == GAME_PAUSED){
            pauseTime -= deltaTime;
        }

        handleInput(deltaTime);

        draw(deltaTime);
        clientDraw();
        respondToMessages();
    }

    @Override
    public void handleInput(float deltaTime) {
        if(connectionState == GAME_ON) {
            boolean moved = false;
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
                gameClass.movePlayer(thisPlayer, 0, deltaTime);
                moved = true;
            } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
                gameClass.movePlayer(thisPlayer, 1, deltaTime);
                moved = true;
            }

            if(moved){
                updateServer();
            }
        }
    }

    public void updateServer(){
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(serverAdress);
            String packetString = "" + MessageTypes.PLAYER_POS +","+thisPlayer.position.x+","+thisPlayer.position.y + "!";
            buf = packetString.getBytes();
            DatagramPacket packet = new DatagramPacket(buf,buf.length,inetAddress,NetworkConstants.SERVER_IN_PORT);
            socket.send(packet);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Addition stuff client has to draw
    public void clientDraw(){
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        parent.batch.begin();

        //Drawing paused timer
        if(connectionState == GAME_PAUSED && pauseTime > 0){
            parent.fontBig.draw(parent.batch, Integer.toString( (int) pauseTime + 1),width * 0.49f, height * 0.6f);
        }

        if(connectionState == WAITING_FOR_LFG_ACK){
            parent.font.draw(parent.batch, "WAITING_FOR_LFG_ACK",width * 0.2f, height * 0.7f);
        } else if(connectionState == WAITING_FOR_GAMEINFO){
            parent.font.draw(parent.batch, "WAITING_FOR_GAMEINFO",width * 0.2f, height * 0.7f);
        } else if(connectionState == WAITING_FOR_START){
            parent.font.draw(parent.batch, "WAITING_FOR_START",width * 0.2f, height * 0.7f);
        }

        parent.batch.end();
    }

    public void sendLFG(){
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(serverAdress);
            String packetString = "" + MessageTypes.LFG +","+socketInPort+","+"defaultName"+"!";
            buf = packetString.getBytes();
            DatagramPacket packet = new DatagramPacket(buf,buf.length,inetAddress,NetworkConstants.SERVER_IN_PORT);
            socket.send(packet);

            connectionState = WAITING_FOR_LFG_ACK;
            System.out.println("Sent LFG packet");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void respondToMessages(){
        //TODO: Min time to poll, max time to poll, while loop till max time to poll
        try {
            Message message = blockingQueue.poll(10, TimeUnit.MILLISECONDS);

            if(message == null) return;

            int port = message.sourcePort;
            String address = message.sourceAddress;
            InetAddress inetAddress = InetAddress.getByName(address);

            switch (message.id){
                case MessageTypes.LFG_ACK:
                    System.out.println("Received LFG_ACK");
                    connectionState = WAITING_FOR_GAMEINFO;
                    break;
                case MessageTypes.GAME_INFO:
                    System.out.println("Received GAME_INFO");

                    String playerNumberString = message.data[0].trim();
                    this.playerNumber = Integer.parseInt(playerNumberString);
                    if(playerNumber == 0){
                        thisPlayer = gameClass.player1;
                    } else{
                        thisPlayer = gameClass.player2;
                    }

                    connectionState = GAME_PAUSED;
                    pauseTime = Constants.START_PAUSE_TIME;

                    //Respond with GAME_INFO_ACK
                    String packetString = "" + MessageTypes.GAME_INFO_ACK + "!";
                    buf = packetString.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf,buf.length,inetAddress,NetworkConstants.SERVER_IN_PORT);
                    socket.send(packet);

                    System.out.println("Sent GAME_INFO_ACK");
                    break;
                case MessageTypes.GAME_STATE:
                    if(connectionState != GAME_ON ){
                        connectionState = GAME_ON;
                        System.out.println("Received first GAME_STATE .. GAME_ON");
                    }

                    try {
                        Vector2 ballPosition = new Vector2(Float.parseFloat(message.data[0].trim()), Float.parseFloat(message.data[1].trim()));
                        Vector2 p1position = new Vector2(Float.parseFloat(message.data[2].trim()), Float.parseFloat(message.data[3].trim()));
                        Vector2 p2position = new Vector2(Float.parseFloat(message.data[4].trim()), Float.parseFloat(message.data[5].trim()));

                        gameClass.ball.position = ballPosition;
                        if(playerNumber == 0){
                            gameClass.player2.position = p2position;
                        } else {
                            gameClass.player1.position = p1position;
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                    break;
                case MessageTypes.GAME_SCORED:
                    System.out.println("Received GAME_SCORED");
                    pauseTime = Constants.SCORE_PAUSE_TIME;
                    connectionState = GAME_PAUSED;

                    String player = message.data[0].trim();
                    lastScored = Integer.parseInt(player);
                    if(lastScored == 0){
                        gameClass.player2.lives--;
                    } else{
                        gameClass.player1.lives--;
                    }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (UnknownHostException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}