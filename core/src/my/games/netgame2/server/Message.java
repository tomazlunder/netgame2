package my.games.netgame2.server;

public class Message {
    public String sourceAddress;
    public int sourcePort;
    public String sourceName;

    public int id;
    public String[] data;

    public Message(){
        data = new String[6];
    }
}
