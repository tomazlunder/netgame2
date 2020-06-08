package my.games.netgame2.server;

public class Message {
    public int id;
    public String[] data;
    public String sourceAddress;
    public int sourcePort;
    public String sourceName;

    public Message(){
        data = new String[6];
    }
}
