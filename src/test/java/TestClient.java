import net.beaconpe.jraklib.client.ClientHandler;
import net.beaconpe.jraklib.client.ClientInstance;
import net.beaconpe.jraklib.client.JRakLibClient;
import net.beaconpe.jraklib.protocol.EncapsulatedPacket;

import java.io.IOException;
import java.util.Arrays;

/**
 * Random Testing client thing.
 *
 * @author jython234
 */
public class TestClient {

    public static void main(String[] args){
        try {
            JRakLibClient.PingResponse response = JRakLibClient.pingServer(null, "imcpe.com", 19132, 5, 500);
            System.out.println("ServerID: "+response.serverId+", Name: "+response.name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JRakLibClient client = new JRakLibClient(null, "imcpe.com", 19132);
        ClientHandler handler = new ClientHandler(client, new TestClientInstance());
        while(true){
            handler.handlePacket();
        }
    }

    public static class TestClientInstance implements ClientInstance {

        @Override
        public void connectionOpened(long serverId) {
            System.out.println("Connection opened! ServerID: "+serverId);
        }

        @Override
        public void connectionClosed(String reason) {
            System.out.println("Connection closed, reason: "+reason);
        }

        @Override
        public void handleEncapsulated(EncapsulatedPacket packet, int flags) {
            System.out.println("Encapsulated: "+packet.buffer[0]+", "+flags);
        }

        @Override
        public void handleRaw(byte[] payload) {
            System.out.println("Raw: "+ Arrays.toString(payload));
        }

        @Override
        public void handleOption(String option, String value) {
            System.out.println("Option: "+option+", "+value);
        }
    }

}
