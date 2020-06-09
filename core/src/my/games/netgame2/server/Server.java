package my.games.netgame2.server;

import com.badlogic.gdx.math.Vector2;
import my.games.netgame2.game.GameClass;
import my.games.netgame2.game.Player;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Server {

    static ArrayList<GameClass> games;
    static ArrayList<Client> allClients;
    static ArrayList<Client> lfgQueue;
    static ArrayList<Client> currentMatchmaking;

    static HashMap<String, Integer> pingMap;
    static HashMap<String, Client> fullAddressClientMap;
    static HashMap<Player, Client> playerClientMap;
    static HashMap<Client,Player> clientPlayerMap;

    public static BlockingQueue<Message> blockingQueue;

    public static long time;
    public static long elapsed;

    public static long lastUpdatedGames;
    public static long lastRanMatchmaking;
    public static long MATCHMAKING_FREQUENCY = 5000;
    public static long GAME_FREQUENCY = 16;

    static int numGameInfoAck;

    private static DatagramSocket socket;

    private static byte[] buf;

    static int gameID;
    static long tick = 0;



    public static void main(String[] args) throws IOException {
        games = new ArrayList<>();
        allClients = new ArrayList<>();
        lfgQueue = new ArrayList<>();
        currentMatchmaking = new ArrayList<>();
        fullAddressClientMap = new HashMap<>();
        playerClientMap = new HashMap<>();
        clientPlayerMap = new HashMap<>();
        pingMap = new HashMap<>();
        socket = new DatagramSocket(NetworkConstants.SERVER_OUT_PORT);
        gameID = 0;

        blockingQueue = new ArrayBlockingQueue<Message>(128);
        UDPproducer udpProducer = new UDPproducer(blockingQueue, NetworkConstants.SERVER_IN_PORT);
        new Thread(udpProducer).start();
        System.out.println("Spawned UDP producer thread");

        while (true){
            serverLoop();
        }
    }

    public static void serverLoop(){
        time = System.currentTimeMillis();

        if(time - lastRanMatchmaking >= MATCHMAKING_FREQUENCY && currentMatchmaking.size() == 0){
            lastRanMatchmaking = time;
            matchMaking();
        }

        if(time - lastUpdatedGames >= GAME_FREQUENCY){
            float deltaTime = (time-lastUpdatedGames)/1000f;
            runGames(deltaTime);
            lastUpdatedGames = time;
        }

        respondToMessages();
    }

    private static void matchMaking(){
        try {
            if(lfgQueue.size() >= 2) {
                numGameInfoAck = 0;
                Client client1 = lfgQueue.remove(0);
                Client client2 = lfgQueue.remove(0);

                currentMatchmaking.add(client1);
                currentMatchmaking.add(client2);

                //GAME_INFO for client1
                String packetString = "" + MessageTypes.GAME_INFO + "," + "0" + "!";
                buf = packetString.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, client1.ip, client1.portIN);
                socket.send(packet);
                client1.lastPacket = packet;


                //GAME_INFO for client2
                packetString = "" + MessageTypes.GAME_INFO + "," + "1" + "!";
                buf = packetString.getBytes();
                packet = new DatagramPacket(buf, buf.length, client1.ip, client2.portIN);
                socket.send(packet);
                client2.lastPacket = packet;

                System.out.println("Matchmaking: Sent GAME_INFO to both clients");
            }
            else{
                //System.out.println("Matchmaking: Not enough players");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runGames(float deltaTime){
        tick++;
        ArrayList<GameClass> gamesToRemove = new ArrayList<>();

        for(GameClass game : games){
            game.update(deltaTime);

            //Update clients on game state
            if(game.state != game.PAUSED && tick%1==0){
                try {
                    Client c1 = playerClientMap.get(game.player1);
                    Client c2 = playerClientMap.get(game.player2);

                    //GAME_STATE for both clients
                    String packetString = "" + MessageTypes.GAME_STATE
                            + "," + game.ball.position.x
                            + "," + game.ball.position.y
                            + "," + game.player1.position.x
                            + "," + game.player1.position.y
                            + "," + game.player2.position.x
                            + "," + game.player2.position.y
                            + "!";
                    buf = packetString.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, c1.ip, c1.portIN);
                    socket.send(packet);
                    packet = new DatagramPacket(buf, buf.length, c2.ip, c2.portIN);
                    socket.send(packet);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //If one of the players just scored...
            if(game.justScored != -1){
                try {
                    Client c1 = playerClientMap.get(game.player1);
                    Client c2 = playerClientMap.get(game.player2);

                    String packetString = "" + MessageTypes.GAME_SCORED
                            + "," + game.justScored //Id of player that scored
                            + "!";
                    buf = packetString.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, c1.ip, c1.portIN);
                    socket.send(packet);
                    packet = new DatagramPacket(buf, buf.length, c2.ip, c2.portIN);
                    socket.send(packet);
                    System.out.println("[GAME"+game.gameID+"Sent GAME_SCORED " + game.justScored);

                } catch (IOException e){
                    e.printStackTrace();
                }
            }

            //Add game to list of removals
            if(game.state == game.ENDED){
                gamesToRemove.add(game);
            }
        }

        //Remove finished games
        games.removeAll(gamesToRemove);
    }

    private static void respondToMessages(){
            //TODO: Min time to poll, max time to poll, while loop till max time to poll
        try {
            Message message = blockingQueue.poll(5, TimeUnit.MILLISECONDS);

            if(message == null){
                int a = 3;
                return;
            }

            int port = message.sourcePort;
            String address = message.sourceAddress;
            InetAddress inetAddress = InetAddress.getByName(address);

            String fullAddress = address+":"+port;
            Client client;

            switch (message.id){
                case MessageTypes.PING:
                    System.out.println("Received PING: ("+address+":"+port+")");
                    pingMap.put(address,port);


                    break;
                case MessageTypes.LFG:{
                    if(!fullAddressClientMap.containsKey(fullAddress)) {
                        System.out.println("Received LFG: ("+address+":"+port+")");

                        int clientInPort = Integer.parseInt(message.data[0].trim());
                        clientInPort = pingMap.get(address); //TODO: This is a bit hacky for now

                        String playerName = message.data[1].trim();

                        client = new Client(inetAddress, port, clientInPort, playerName);
                        client.state = Client.RECEIVED_LFG;
                        allClients.add(client);
                        fullAddressClientMap.put(fullAddress, client);
                        lfgQueue.add(client);

                        System.out.println("Added new client: (" + address + ":" + port + "," + clientInPort + "," + playerName + ")");
                    } else{
                        client = fullAddressClientMap.get(fullAddress);
                    }

                    //Respond with LFG_ACK
                    String packetString = "" + MessageTypes.LFG_ACK + "!";
                    buf = packetString.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf,buf.length,client.ip, client.portIN);
                    socket.send(packet);
                    client.lastPacket = packet;

                    System.out.println("Sent LFG_ACK ("+client.ip.toString() +":"+client.portIN+")");
                    int a = 3;
                    break;}

                case MessageTypes.GAME_INFO_ACK:
                    //System.out.println("Received GAME_INFO_ACK");
                    //
                    client = fullAddressClientMap.get(fullAddress);
                    client.retryTimer = 0;
                    client.state = Client.IN_GAME;

                    numGameInfoAck++;
                    if(numGameInfoAck == 2){
                        Player player1 = new Player(0,false);
                        Player player2 = new Player(1,false);
                        playerClientMap.put(player1,currentMatchmaking.get(0));
                        playerClientMap.put(player2,currentMatchmaking.get(1));
                        clientPlayerMap.put(currentMatchmaking.get(0), player1);
                        clientPlayerMap.put(currentMatchmaking.get(1), player2);
                        GameClass game = new GameClass(player1,player2);
                        game.gameID = gameID;
                        gameID++;
                        games.add(game);

                        currentMatchmaking = new ArrayList<>();
                    }
                    break;
                case MessageTypes.PLAYER_POS:
                    //System.out.println("Received PLAYER_POS");
                    try {
                        Vector2 newPosition = new Vector2(Float.parseFloat(message.data[0].trim()), Float.parseFloat(message.data[1].trim()));

                        client = fullAddressClientMap.get(fullAddress);
                        Player player = clientPlayerMap.get(client);

                        player.position = newPosition;
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }
            }
        } catch (InterruptedException e) {
            //e.printStackTrace();
        } catch (UnknownHostException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    private static class Client{
        //public static final int RECEIVED_PING = 0;
        public static final int RECEIVED_LFG = 1;
        public static final int RECEIVED_GAMEINFO_ACK = 2;
        public static final int IN_GAME = 3;

        public int state;
        public DatagramPacket lastPacket;
        public float retryTimer;

        public InetAddress ip;
        public int portOUT;
        public int portIN;
        public String name;

        public Client(InetAddress ip, int portOUT, int portIN, String name){
            this.ip = ip;
            this.portOUT = portOUT;
            this.portIN = portIN;
            this.name = name;

            this.retryTimer = 0;
            //this.state = RECEIVED_PING;
        }
    }
}
