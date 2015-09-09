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
package net.beaconpe.jraklib.server;

import net.beaconpe.jraklib.Binary;
import net.beaconpe.jraklib.JRakLib;
import net.beaconpe.jraklib.protocol.EncapsulatedPacket;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Handler that provides easy communication with the server.
 */
public class ServerHandler {
    protected JRakLibServer server;
    protected ServerInstance instance;

    public ServerHandler(JRakLibServer server, ServerInstance instance){
        this.server = server;
        this.instance = instance;
    }

    public void sendEncapsulated(String identifier, EncapsulatedPacket packet){
        byte flags = JRakLib.PRIORITY_NORMAL;
        sendEncapsulated(identifier, packet, flags);
    }

    public void sendEncapsulated(String identifier, EncapsulatedPacket packet, byte flags){
        ByteBuffer bb = ByteBuffer.allocate(3+identifier.getBytes().length+packet.getTotalLength(true));
        bb.put(JRakLib.PACKET_ENCAPSULATED).put((byte) identifier.getBytes().length).put(identifier.getBytes()).put(flags).put(packet.toBinary(true));
        server.pushMainToThreadPacket(Arrays.copyOf(bb.array(), bb.position()));
        bb = null;
    }

    public void sendRaw(String address, short port, byte[] payload){
        ByteBuffer bb = ByteBuffer.allocate(4+address.getBytes().length+payload.length);
        bb.put(JRakLib.PACKET_RAW).put((byte) address.getBytes().length).put(address.getBytes()).put(Binary.writeShort(port)).put(payload);
        server.pushMainToThreadPacket(bb.array());
    }

    public void closeSession(String identifier, String reason){
        ByteBuffer bb = ByteBuffer.allocate(3+identifier.getBytes().length+reason.getBytes().length);
        bb.put(JRakLib.PACKET_CLOSE_SESSION).put((byte) identifier.getBytes().length).put(identifier.getBytes()).put((byte) reason.getBytes().length).put(reason.getBytes());
        server.pushMainToThreadPacket(bb.array());
    }

    public void sendOption(String name, String value){
        ByteBuffer bb = ByteBuffer.allocate(2+name.getBytes().length+value.getBytes().length);
        bb.put(JRakLib.PACKET_SET_OPTION).put((byte) name.getBytes().length).put(name.getBytes()).put(value.getBytes());
        server.pushMainToThreadPacket(bb.array());
    }

    public void blockAddress(String address, int timeout){
        ByteBuffer bb = ByteBuffer.allocate(6+address.getBytes().length);
        bb.put(JRakLib.PACKET_BLOCK_ADDRESS).put((byte) address.getBytes().length).put(address.getBytes()).put(Binary.writeInt(timeout));
        server.pushMainToThreadPacket(bb.array());
    }

    public void shutdown(){
        server.shutdown();
        server.pushMainToThreadPacket(new byte[] {JRakLib.PACKET_SHUTDOWN});
        //TODO: Find a way to kill server after sleep.
    }

    public void emergencyShutdown(){
        server.shutdown();
        server.pushMainToThreadPacket(new byte[] {0x7f}); //JRakLib::PACKET_EMERGENCY_SHUTDOWN
    }

    protected void invalidSession(String identifier){
        ByteBuffer bb = ByteBuffer.allocate(2+identifier.getBytes().length);
        bb.put(JRakLib.PACKET_INVALID_SESSION).put((byte) identifier.getBytes().length).put(identifier.getBytes());
        server.pushMainToThreadPacket(bb.array());
    }

    public boolean handlePacket(){
        byte[] packet = server.readThreadToMainPacket();
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
                instance.handleEncapsulated(identifier, EncapsulatedPacket.fromBinary(buffer, true), flags);
            } else if(id == JRakLib.PACKET_RAW){
                int len = packet[offset++];
                String address = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                int port = Binary.readShort(Binary.subbytes(packet, offset, 2));
                offset += 2;
                byte[] payload = Binary.subbytes(packet, offset);
                instance.handleRaw(address, port, payload);
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
                len = packet[offset++];
                String address = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                int port = Binary.readShort(Binary.subbytes(packet, offset, 2));
                offset += 2;
                long clientID = Binary.readLong(Binary.subbytes(packet, offset, 8));
                instance.openSession(identifier, address, port, clientID);
            } else if(id == JRakLib.PACKET_CLOSE_SESSION){
                int len = packet[offset++];
                String identifier = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                len = packet[offset++];
                String reason = new String(Binary.subbytes(packet, offset, len));
                instance.closeSession(identifier, reason);
            } else if(id == JRakLib.PACKET_INVALID_SESSION){
                int len = packet[offset++];
                String identifier = new String(Binary.subbytes(packet, offset, len));
                instance.closeSession(identifier, "Invalid session.");
            } else if(id == JRakLib.PACKET_ACK_NOTIFICATION){
                int len = packet[offset++];
                String identifier = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                int identifierACK = Binary.readInt(Binary.subbytes(packet, offset, 4));
                instance.notifyACK(identifier, identifierACK);
            } else if(id == JRakLib.PACKET_EXCEPTION_CAUGHT){
                int len = packet[offset++];
                String message = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                len = Binary.readShort(Binary.subbytes(packet, offset, 2));
                String className = new String(Binary.subbytes(packet, offset, len));
                instance.exceptionCaught(className, message);
            }
            return true;
        }
        return false;
    }
}
