package my.games.netgame2.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import my.games.netgame2.MainClass;
import my.games.netgame2.game.Constants;
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
    public final int GAME_ON = 3;
    public final int GAME_PAUSED = 4;
    public final int GAME_ENDED = 5;
    public int connectionState;

    int playerNumber;
    Player thisPlayer;
    UDPproducer udpProducer;
    BlockingQueue<Message> blockingQueue;

    private static DatagramSocket socket;

    private DatagramPacket lastPacket;
    private float retryTimer;
    private int numRetries = 0;

    private static byte[] buf;

    private float pauseTime;

    private String nameOpponent;
    int lastScored;

    public ClientGameScreen(MainClass mainClass) throws IOException {
        super(mainClass);
        this.gameClass.state = gameClass.RUNNING;
        connectionState = -1;
        playerNumber = -1;
        lastScored = -1;
        thisPlayer = null;

        blockingQueue = new ArrayBlockingQueue<>(128);

        socket = new DatagramSocket();
        System.out.println("Opened socket [PORT:"+socket.getLocalPort()+"]");

        udpProducer = new UDPproducer(socket,blockingQueue);
        new Thread(udpProducer).start();
        System.out.println("Spawned UDP producer thread");

        sendLFG();
    }

    //Not updating gameClass like gameScreen
    @Override
    public void render(float deltaTime) {
        messageResender(deltaTime);

        if(connectionState == GAME_PAUSED){
            pauseTime -= deltaTime;
        }

        handleInput(deltaTime);

        draw();
        clientDraw();
        respondToMessages();

        if(gameClass.state == gameClass.ENDED){
            updateButton();
        }
    }

    public void messageResender(float deltaTime){
        if(numRetries >= 6){
            System.out.println("Reached maximum resend number. Returning to main menu.");
            parent.changeScreen(parent.MAINMENU);
        }


        if(connectionState == WAITING_FOR_LFG_ACK){
            retryTimer += deltaTime;
        }

        if(retryTimer > NetworkConstants.PACKET_RETRY_TIME){
            retryTimer = 0;
            numRetries ++;
            System.out.println("Retrying to send packet: "+new String(lastPacket.getData()).trim().split("!")[0]);
            try {
                socket.send(lastPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        try {
            InetAddress inetAddress = InetAddress.getByName(NetworkConstants.SERVER_ADDRESS);
            String packetString = "" + MessageTypes.PLAYER_POS +","+thisPlayer.position.x+","+thisPlayer.position.y + "!";
            buf = packetString.getBytes();
            DatagramPacket packet = new DatagramPacket(buf,buf.length,inetAddress,NetworkConstants.SERVER_PORT);
            socket.send(packet);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Addition stuff client has to draw
    public void clientDraw(){
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        parent.batch.begin();

        //Drawing who scored
        if(connectionState == GAME_PAUSED && lastScored != -1){
            String scorer = "";
            if(lastScored == thisPlayer.playerNumber){
                scorer = parent.myUsername;
            }
            else{
                scorer = nameOpponent;
            }
            parent.fontBig.draw(parent.batch, scorer + " scored!",width * 0.18f, height * 0.5f);
        }

        //Drawing paused timer
        if(connectionState == GAME_PAUSED && pauseTime > 0){
            parent.fontBig.draw(parent.batch, Integer.toString( (int) pauseTime + 1),width * 0.49f, height * 0.6f);
        }

        //Drawing state
        if(connectionState == WAITING_FOR_LFG_ACK){
            parent.font.draw(parent.batch, "Connecting to matchmaking",width * 0.2f, height * 0.7f);
        } else if(connectionState == WAITING_FOR_GAMEINFO){
            parent.font.draw(parent.batch, "Looking for game",width * 0.2f, height * 0.7f);
        } else if(connectionState == GAME_ENDED){
            parent.font.draw(parent.batch, "GAME ENDED",width * 0.2f, height * 0.7f);
        }

        parent.batch.end();
    }

    public void sendLFG(){
        try {
            InetAddress inetAddress = InetAddress.getByName(NetworkConstants.SERVER_ADDRESS);
            String packetString = "" + MessageTypes.LFG +","+parent.myUsername+"!";
            buf = packetString.getBytes();
            DatagramPacket packet = new DatagramPacket(buf,buf.length,inetAddress,NetworkConstants.SERVER_PORT);
            socket.send(packet);
            lastPacket = packet;

            connectionState = WAITING_FOR_LFG_ACK;
            System.out.println("Sent LFG packet to "+NetworkConstants.SERVER_ADDRESS+":"+NetworkConstants.SERVER_PORT);
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

            String packetString;

            switch (message.id){
                case MessageTypes.LFG_ACK:
                    System.out.println("Received LFG_ACK");
                    retryTimer = 0;
                    numRetries = 0;

                    connectionState = WAITING_FOR_GAMEINFO;
                    break;
                case MessageTypes.GAME_INFO:
                    System.out.println("Received GAME_INFO");
                    retryTimer = 0;
                    numRetries = 0;

                    String playerNumberString = message.data[0].trim();
                    nameOpponent = message.data[1].trim();

                    this.playerNumber = Integer.parseInt(playerNumberString);
                    if(playerNumber == 0){
                        thisPlayer = gameClass.player1;
                    } else{
                        thisPlayer = gameClass.player2;
                    }

                    connectionState = GAME_PAUSED;
                    pauseTime = Constants.START_PAUSE_TIME;

                    //Respond with GAME_INFO_ACK
                    packetString = "" + MessageTypes.GAME_INFO_ACK + "!";
                    buf = packetString.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf,buf.length,inetAddress,NetworkConstants.SERVER_PORT);
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
                    int p1Lives = Integer.parseInt(message.data[1].trim());
                    int p2Lives = Integer.parseInt(message.data[2].trim());

                    gameClass.player1.lives = p1Lives;
                    gameClass.player2.lives = p2Lives;

                    if(gameClass.player1.lives  == 0 || gameClass.player2.lives == 0){
                        connectionState = GAME_ENDED;
                        gameClass.state = gameClass.ENDED;
                    }

                    packetString = "" + MessageTypes.GAME_SCORED_ACK + "!";
                    buf = packetString.getBytes();
                    packet = new DatagramPacket(buf,buf.length,inetAddress,NetworkConstants.SERVER_PORT);
                    socket.send(packet);
                    break;
                default:
                    System.out.println("Unknown message received");
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
