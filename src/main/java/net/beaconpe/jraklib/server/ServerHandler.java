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

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.buffer;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import net.beaconpe.jraklib.Binary;
import net.beaconpe.jraklib.JRakLib;
import net.beaconpe.jraklib.protocol.EncapsulatedPacket;

/**
 * Handler that provides easy communication with the server.
 */
public class ServerHandler
{

    protected JRakLibServer server;
    protected ServerInstance instance;

    public ServerHandler(JRakLibServer server, ServerInstance instance)
    {
        this.server = server;
        this.instance = instance;
    }

    public void sendEncapsulated(String identifier, EncapsulatedPacket packet)
    {
        byte flags = JRakLib.PRIORITY_NORMAL;
        sendEncapsulated(identifier, packet, flags);
    }

    public void sendEncapsulated(String identifier, EncapsulatedPacket packet, byte flags)
    {
        ByteBuf bb = buffer(3 + identifier.getBytes().length + packet.getTotalLength(true));
        bb.writeByte(JRakLib.PACKET_ENCAPSULATED).writeByte((byte) identifier.getBytes().length).writeBytes(identifier.getBytes()).writeByte(flags).writeBytes(packet.toBinary(true));
        server.pushMainToThreadPacket(bb.copy());
        bb = null;
    }

    public void sendRaw(String address, short port, byte[] payload)
    {
        ByteBuf bb = buffer(4 + address.getBytes().length + payload.length);
        bb.writeByte(JRakLib.PACKET_RAW).writeByte((byte) address.getBytes().length).writeBytes(address.getBytes()).writeBytes(Binary.writeShort(port)).writeBytes(payload);
        server.pushMainToThreadPacket(bb);
    }

    public void closeSession(String identifier, String reason)
    {
        ByteBuf bb = buffer(3 + identifier.getBytes().length + reason.getBytes().length);
        bb.writeByte(JRakLib.PACKET_CLOSE_SESSION).writeByte((byte) identifier.getBytes().length).writeBytes(identifier.getBytes()).writeByte((byte) reason.getBytes().length).writeBytes(reason.getBytes());
        server.pushMainToThreadPacket(bb);
    }

    public void sendOption(String name, String value)
    {
        ByteBuf bb = buffer(2 + name.getBytes().length + value.getBytes().length);
        bb.writeByte(JRakLib.PACKET_SET_OPTION).writeByte(name.getBytes().length).writeBytes(name.getBytes()).writeBytes(value.getBytes());
        server.pushMainToThreadPacket(bb);
    }

    public void blockAddress(String address, int timeout)
    {
        ByteBuf bb = buffer(6 + address.getBytes().length);
        bb.writeByte(JRakLib.PACKET_BLOCK_ADDRESS).writeByte((byte) address.getBytes().length).writeBytes(address.getBytes()).writeBytes(Binary.writeInt(timeout));
        server.pushMainToThreadPacket(bb);
    }

    public void shutdown()
    {
        server.shutdown();
        server.pushMainToThreadPacket(wrappedBuffer(new byte[]
        {
            JRakLib.PACKET_SHUTDOWN
        }));
        //TODO: Find a way to kill server after sleep.
    }

    public void emergencyShutdown()
    {
        server.shutdown();
        server.pushMainToThreadPacket(wrappedBuffer(new byte[]
        {
            0x7f
        })); //JRakLib::PACKET_EMERGENCY_SHUTDOWN
    }

    protected void invalidSession(String identifier)
    {
        ByteBuf bb = buffer(2 + identifier.getBytes().length);
        bb.writeByte(JRakLib.PACKET_INVALID_SESSION).writeByte((byte) identifier.getBytes().length).writeBytes(identifier.getBytes());
        server.pushMainToThreadPacket(bb);
    }

    public boolean handlePacket()
    {
        ByteBuf packet = server.readThreadToMainPacket();
        if (packet == null)
        {
            return false;
        }
        if (packet.readableBytes() > 0)
        {
            byte id = packet.getByte(0);
            int offset = 1;
            if (id == JRakLib.PACKET_ENCAPSULATED)
            {
                int len = packet.getByte(offset++);
                String identifier = Binary.subbytesString(packet, offset, len);
                offset += len;
                byte flags = packet.getByte(offset++);
                ByteBuf buffer = Binary.subbytes(packet, offset);
                instance.handleEncapsulated(identifier, EncapsulatedPacket.fromBinary(buffer, true), flags);
            } else if (id == JRakLib.PACKET_RAW)
            {
                int len = packet.getByte(offset++);
                String address = Binary.subbytesString(packet, offset, len);
                offset += len;
                int port = Binary.readShort(Binary.subbytes(packet, offset, 2));
                offset += 2;
                ByteBuf payload = Binary.subbytes(packet, offset);
                instance.handleRaw(address, port, payload);
            } else if (id == JRakLib.PACKET_SET_OPTION)
            {
                int len = packet.getByte(offset++);
                String name = Binary.subbytesString(packet, offset, len);
                offset += len;
                String value = Binary.subbytesString(packet, offset);
                instance.handleOption(name, value);
            } else if (id == JRakLib.PACKET_OPEN_SESSION)
            {
                int len = packet.getByte(offset++);
                String identifier = Binary.subbytesString(packet, offset, len);
                offset += len;
                len = packet.getByte(offset++);
                String address = Binary.subbytesString(packet, offset, len);
                offset += len;
                int port = Binary.readShort(Binary.subbytes(packet, offset, 2));
                offset += 2;
                long clientID = Binary.readLong(Binary.subbytes(packet, offset, 8));
                instance.openSession(identifier, address, port, clientID);
            } else if (id == JRakLib.PACKET_CLOSE_SESSION)
            {
                int len = packet.getByte(offset++);
                String identifier = Binary.subbytesString(packet, offset, len);
                offset += len;
                len = packet.getByte(offset++);
                String reason = Binary.subbytesString(packet, offset, len);
                instance.closeSession(identifier, reason);
            } else if (id == JRakLib.PACKET_INVALID_SESSION)
            {
                int len = packet.getByte(offset++);
                String identifier = Binary.subbytesString(packet, offset, len);
                instance.closeSession(identifier, "Invalid session.");
            } else if (id == JRakLib.PACKET_ACK_NOTIFICATION)
            {
                int len = packet.getByte(offset++);
                String identifier = Binary.subbytesString(packet, offset, len);
                offset += len;
                int identifierACK = Binary.readInt(Binary.subbytes(packet, offset, 4));
                instance.notifyACK(identifier, identifierACK);
            } else if (id == JRakLib.PACKET_EXCEPTION_CAUGHT)
            {
                int len = packet.getByte(offset++);
                String message = Binary.subbytesString(packet, offset, len);
                offset += len;
                len = Binary.readShort(Binary.subbytes(packet, offset, 2));
                String className = Binary.subbytesString(packet, offset, len);
                instance.exceptionCaught(className, message);
            }
            return true;
        }
        return false;
    }
}
