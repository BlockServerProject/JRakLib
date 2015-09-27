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

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.buffer;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import net.beaconpe.jraklib.Binary;
import net.beaconpe.jraklib.JRakLib;
import net.beaconpe.jraklib.protocol.EncapsulatedPacket;

/**
 * A handler class for handling the client.
 */
public class ClientHandler
{

    protected JRakLibClient client;
    protected ClientInstance instance;

    public ClientHandler(JRakLibClient client, ClientInstance instance)
    {
        this.client = client;
        this.instance = instance;
    }

    public void sendEncapsulated(EncapsulatedPacket packet)
    {
        byte flags = JRakLib.PRIORITY_NORMAL;
        sendEncapsulated("", packet, flags);
    }

    public void sendEncapsulated(String identifier, EncapsulatedPacket packet, byte flags)
    {
        ByteBuf bb = buffer(3 + identifier.getBytes().length + packet.getTotalLength(true));
        bb.writeByte(JRakLib.PACKET_ENCAPSULATED).writeByte((byte) identifier.getBytes().length).writeBytes(identifier.getBytes()).writeByte(flags).writeBytes(packet.toBinary(true));
        client.pushMainToThreadPacket(bb.copy());
        bb = null;
    }

    public void sendRaw(ByteBuf payload)
    {
        sendRaw(client.getServerIP(), (short) client.getServerPort(), payload);
    }

    public void sendRaw(String address, short port, ByteBuf payload)
    {
        ByteBuf bb = buffer(4 + address.getBytes().length + payload.readableBytes());
        bb.writeByte(JRakLib.PACKET_RAW).writeByte((byte) address.getBytes().length).writeBytes(address.getBytes()).writeBytes(Binary.writeShort(port)).writeBytes(payload);
        client.pushMainToThreadPacket(bb.copy());
    }

    public void sendOption(String name, String value)
    {
        ByteBuf bb = buffer(2 + name.getBytes().length + value.getBytes().length);
        bb.writeByte(JRakLib.PACKET_SET_OPTION).writeByte((byte) name.getBytes().length).writeBytes(name.getBytes()).writeBytes(value.getBytes());
        client.pushMainToThreadPacket(bb.copy());
    }

    public void disconnectFromServer()
    {
        shutdown();
    }

    public void shutdown()
    {
        client.shutdown();
        client.pushMainToThreadPacket(wrappedBuffer(new byte[]
        {
            JRakLib.PACKET_SHUTDOWN
        }));
        //TODO: Find a way to kill client after sleep.
    }

    public void emergencyShutdown()
    {
        client.shutdown();
        client.pushMainToThreadPacket(wrappedBuffer(new byte[]
        {
            0x7f
        })); //JRakLib::PACKET_EMERGENCY_SHUTDOWN
    }

    public boolean handlePacket()
    {
        ByteBuf packet = client.readThreadToMainPacket();
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
                instance.handleEncapsulated(EncapsulatedPacket.fromBinary(buffer, true), flags);
            } else if (id == JRakLib.PACKET_RAW)
            {
                int len = packet.getByte(offset++);
                String address = Binary.subbytesString(packet, offset, len);
                offset += len;
                int port = Binary.readShort(Binary.subbytes(packet, offset, 2));
                offset += 2;
                ByteBuf payload = Binary.subbytes(packet, offset);
                instance.handleRaw(payload);
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
                long serverID = Binary.readLong(Binary.subbytes(packet, offset, 8));
                instance.connectionOpened(serverID);
            } else if (id == JRakLib.PACKET_CLOSE_SESSION)
            {
                int len = packet.getByte(offset++);
                String identifier = Binary.subbytesString(packet, offset, len);
                offset += len;
                len = packet.getByte(offset++);
                String reason = Binary.subbytesString(packet, offset, len);
                instance.connectionClosed(reason);
            }
            return true;
        }
        return false;
    }
}
