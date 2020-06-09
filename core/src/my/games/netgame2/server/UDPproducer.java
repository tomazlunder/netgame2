package my.games.netgame2.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;

public  class UDPproducer implements Runnable{
    protected DatagramSocket socket = null;
    protected boolean listen = true;

    private BlockingQueue<Message> blockingQueue;

    public UDPproducer(DatagramSocket socket, BlockingQueue<Message> bq) throws IOException {
        this.socket = socket;

        this.blockingQueue = bq;
    }

    public void run() {
        byte[] buf = new byte[512];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        while (listen) {
            //System.out.println("[UDPproducer] Waiting for message");
            try {
                // receive request
                socket.receive(packet);

                // figure out response
                String packetData = new String(packet.getData());
                String[] preParts = packetData.split(Pattern.quote("!")); // Packet ends with !
                String[] parts = preParts[0].split(Pattern.quote(",")); // Split on period.

                Message message = new Message();

                message.id = Integer.parseInt(parts[0].trim());
                message.sourceAddress = packet.getAddress().getHostAddress();
                message.sourcePort = packet.getPort();
                message.sourceName = packet.getAddress().getCanonicalHostName();

                if(parts.length > 1){
                    message.data = Arrays.copyOfRange(parts,1,parts.length);
                }

                //Blocks till it can insert
                blockingQueue.put(message);

                //System.out.println("[UDPproducer] Received message, added to queue");

            } catch (Exception e) {
                e.printStackTrace();
                listen = false;
            }
        }
        socket.close();
    }
}
