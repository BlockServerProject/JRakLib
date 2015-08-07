/**
 * JRakLib is not affiliated with Jenkins Software LLC or RakNet.
 * This software is a port of RakLib https://github.com/PocketMine/RakLib.

 * This file is part of JRakLib.
 *
 * JRakLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JRakLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JRakLib.  If not, see <http://www.gnu.org/licenses/>.
 */
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
