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
import io.netty.buffer.ByteBuf;
import net.beaconpe.jraklib.Logger;
import net.beaconpe.jraklib.protocol.EncapsulatedPacket;
import net.beaconpe.jraklib.server.JRakLibServer;
import net.beaconpe.jraklib.server.ServerHandler;
import net.beaconpe.jraklib.server.ServerInstance;

/**
 * Created by jython234 on 9/11/2015.
 *
 * @author RedstoneLamp Team
 */
public class TestServer {

    public static void main(String[] args) {
        JRakLibServer server = new JRakLibServer(new Logger() {
            @Override
            public void notice(String message) {
                System.out.println("NOTICE: "+message);
            }

            @Override
            public void critical(String message) {
                System.err.println("CRITICAL: "+message);
            }

            @Override
            public void emergency(String message) {
                System.err.println("EMERGENCY: "+message);
            }
        }, 19132, "0.0.0.0");
        ServerHandler handler = new ServerHandler(server, new Interface());
        handler.sendOption("name", "MCPE;Test Server;34;0.12.1;0;20");
        while(true) {
            handler.handlePacket();
        }
    }

    public static class Interface implements ServerInstance{

        @Override
        public void openSession(String identifier, String address, int port, long clientID) {
            System.out.println("open session");
        }

        @Override
        public void closeSession(String identifier, String reason) {

        }

        @Override
        public void handleEncapsulated(String identifier, EncapsulatedPacket packet, int flags) {

        }

        @Override
        public void handleRaw(String address, int port, ByteBuf payload) {

        }

        @Override
        public void notifyACK(String identifier, int identifierACK) {

        }

        @Override
        public void exceptionCaught(String clazz, String message) {

        }

        @Override
        public void handleOption(String option, String value) {

        }
    }

}
