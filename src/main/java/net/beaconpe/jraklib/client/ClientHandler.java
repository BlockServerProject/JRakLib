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
package net.beaconpe.jraklib.client;

import net.beaconpe.jraklib.Binary;
import net.beaconpe.jraklib.JRakLib;
import net.beaconpe.jraklib.protocol.EncapsulatedPacket;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A handler class for handling the client.
 *
 * @author jython234
 */
public class ClientHandler {
    protected JRakLibClient client;
    protected ClientInstance instance;

    public ClientHandler(JRakLibClient client, ClientInstance instance){
        this.client = client;
        this.instance = instance;
    }

    public void sendEncapsulated(EncapsulatedPacket packet){
        byte flags = JRakLib.PRIORITY_NORMAL;
        sendEncapsulated("", packet, flags);
    }

    public void sendEncapsulated(String identifier, EncapsulatedPacket packet, byte flags){
        ByteBuffer bb = ByteBuffer.allocate(3+identifier.getBytes().length+packet.getTotalLength(true));
        bb.put(JRakLib.PACKET_ENCAPSULATED).put((byte) identifier.getBytes().length).put(identifier.getBytes()).put(flags).put(packet.toBinary(true));
        client.pushMainToThreadPacket(Arrays.copyOf(bb.array(), bb.position()));
        bb = null;
    }

    public void sendRaw(byte[] payload){
        sendRaw(client.getServerIP(), (short) client.getServerPort(), payload);
    }

    public void sendRaw(String address, short port, byte[] payload){
        ByteBuffer bb = ByteBuffer.allocate(4+address.getBytes().length+payload.length);
        bb.put(JRakLib.PACKET_RAW).put((byte) address.getBytes().length).put(address.getBytes()).put(Binary.writeShort(port)).put(payload);
        client.pushMainToThreadPacket(bb.array());
    }

    public void sendOption(String name, String value){
        ByteBuffer bb = ByteBuffer.allocate(2+name.getBytes().length+value.getBytes().length);
        bb.put(JRakLib.PACKET_SET_OPTION).put((byte) name.getBytes().length).put(name.getBytes()).put(value.getBytes());
        client.pushMainToThreadPacket(bb.array());
    }

    public void disconnectFromServer(){
        shutdown();
    }

    public void shutdown(){
        client.shutdown();
        client.pushMainToThreadPacket(new byte[] {JRakLib.PACKET_SHUTDOWN});
        //TODO: Find a way to kill client after sleep.
    }

    public void emergencyShutdown(){
        client.shutdown();
        client.pushMainToThreadPacket(new byte[] {0x7f}); //JRakLib::PACKET_EMERGENCY_SHUTDOWN
    }

    public boolean handlePacket(){
        byte[] packet = client.readThreadToMainPacket();
        if(packet == null){
            return false;
        }
        if(packet.length > 0){
            byte id = packet[0];
            int offset = 1;
            if(id == JRakLib.PACKET_ENCAPSULATED){
                int len = packet[offset++];
                String identifier = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                byte flags = packet[offset++];
                byte[] buffer = Binary.subbytes(packet, offset);
                instance.handleEncapsulated(EncapsulatedPacket.fromBinary(buffer, true), flags);
            } else if(id == JRakLib.PACKET_RAW){
                int len = packet[offset++];
                String address = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                int port = Binary.readShort(Binary.subbytes(packet, offset, 2));
                offset += 2;
                byte[] payload = Binary.subbytes(packet, offset);
                instance.handleRaw(payload);
            } else if(id == JRakLib.PACKET_SET_OPTION){
                int len = packet[offset++];
                String name = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                String value = new String(Binary.subbytes(packet, offset));
                instance.handleOption(name, value);
            } else if(id == JRakLib.PACKET_OPEN_SESSION){
                int len = packet[offset++];
                String identifier = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                long serverID = Binary.readLong(Binary.subbytes(packet, offset, 8));
                instance.connectionOpened(serverID);
            } else if(id == JRakLib.PACKET_CLOSE_SESSION){
                int len = packet[offset++];
                String identifier = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                len = packet[offset++];
                String reason = new String(Binary.subbytes(packet, offset, len));
                instance.connectionClosed(reason);
            }
            return true;
        }
        return false;
    }
}
