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

import static io.netty.buffer.Unpooled.buffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import net.beaconpe.jraklib.Binary;
import net.beaconpe.jraklib.JRakLib;
import net.beaconpe.jraklib.Logger;
import net.beaconpe.jraklib.protocol.*;
import net.beaconpe.jraklib.protocol.DataPackets.*;
import static net.beaconpe.jraklib.JRakLib.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Manager for managing sessions.
 */
public class SessionManager
{

    protected Map<Byte, Class<? extends Packet>> packetPool = new ConcurrentHashMap<>();
    protected JRakLibServer server;
    protected UDPServerSocket socket;
    protected int receiveBytes = 0;
    protected int sendBytes = 0;
    protected Map<String, Session> sessions = new ConcurrentHashMap<>();
    protected String name = "";
    protected int packetLimit = 1000;
    protected boolean shutdown = false;
    protected int ticks = 0;
    protected long lastMeasure;
    protected Map<String, Long> block = new ConcurrentHashMap<>();
    protected Map<String, Integer> ipSec = new ConcurrentHashMap<>();
    public boolean portChecking = false;
    private long serverID;

    public SessionManager(JRakLibServer server, UDPServerSocket socket)
    {
        this.server = server;
        this.socket = socket;
        registerPackets();
        serverID = new Random().nextLong();
    }

    public int getPort()
    {
        return server.getPort();
    }

    public Logger getLogger()
    {
        return server.getLogger();
    }

    public void run()
    {
        try
        {
            tickProcessor();
        } catch (Exception e)
        {
            streamException(e);
        }
    }

    public void tickProcessor() throws IOException
    {
        lastMeasure = System.currentTimeMillis();
        /*while (!shutdown)
         {*/
        long start = System.currentTimeMillis();
        /*int max = 5000;
         while (receivePacket())
         {
         max = max - 1;
         }
         while (receiveStream(null));*/
        long time = System.currentTimeMillis() - start;
        if (time < 50)
        { //20 ticks per second (1000 / 20)
            sleepUntil(System.currentTimeMillis() + (50 - time));
        }
        tick();
        //}
    }

    private void tick() throws IOException
    {
        long time = System.currentTimeMillis();
        for (Session session : sessions.values())
        {
            session.update(time);
        }
        for (String address : ipSec.keySet())
        {
            if (ipSec.get(address) >= packetLimit)
            {
                blockAddress(address);
            }
        }
        ipSec.clear();
        if ((ticks & 0b1111) == 0)
        {
            double diff = Math.max(0.005d, time - lastMeasure);
            streamOption("bandwith", "up:" + (sendBytes / diff) + ",down:" + (receiveBytes / diff)); //TODO: Fix this stuff
            lastMeasure = time;
            sendBytes = 0;
            receiveBytes = 0;
            if (block.values().size() > 0)
            {
                long now = System.currentTimeMillis();
                for (String address : block.keySet())
                {
                    if (block.get(address) <= now)
                    {
                        block.remove(address);
                    } else
                    {
                        break;
                    }
                }
            }
        }
        ticks = ticks + 1;
    }

    public boolean receivePacket(ChannelHandlerContext ctx, DatagramPacket packet) throws IOException
    {
        if (packet == null)
        {
            return false;
        }
        int len = packet.content().readableBytes();
        if (len > 0)
        {
            InetSocketAddress source = packet.sender();
            receiveBytes += len;
            if (block.containsKey(source.toString()))
            {
                return true;
            }
            if (ipSec.containsKey(source.toString()))
            {
                ipSec.put(source.toString(), ipSec.get(source.toString()) + 1);
            } else
            {
                ipSec.put(source.toString(), 1);
            }
            Packet pkt = getPacketFromPool(packet.content().getByte(0));
            if (pkt != null)
            {
                //System.out.println("Packet -> (" + JRakLib.getHexString(pkt.getID()) + "): " + pkt.getID() + " - " + pkt.getClass().getSimpleName());
                pkt.buffer = packet.content().copy();
                getSession(ctx, getAddressFromString(source.toString()), packet.sender().getPort()).handlePacket(pkt);
                return true;
            } else if (packet.content() != null)
            {
                streamRaw(source, packet.content().copy());
                return true;
            } else
            {
                getLogger().notice("Dropped packet: " + Arrays.toString(packet.content().array()));
                return false;
            }
        }
        return false;
    }

    public void sendPacket(Packet packet, ChannelHandlerContext ctx, String dest, int port) throws IOException
    {
        packet.encode();
        sendBytes += packet.buffer.readableBytes();
        socket.writePacket(packet, ctx, new InetSocketAddress(dest, port));
    }

    public void streamEncapsulated(Session session, EncapsulatedPacket packet)
    {
        streamEncapsulated(session, packet, JRakLib.PRIORITY_NORMAL);
    }

    public void streamEncapsulated(Session session, EncapsulatedPacket packet, byte flags)
    {
        String id = session.getAddress() + ":" + session.getPort();
        ByteBuf bb = buffer(3 + id.getBytes().length + packet.getTotalLength(true));
        bb.writeByte(JRakLib.PACKET_ENCAPSULATED).writeByte((byte) id.getBytes().length).writeBytes(id.getBytes()).writeByte(flags).writeBytes(packet.toBinary(true));
        server.pushThreadToMainPacket(bb);
    }

    public void streamRaw(InetSocketAddress address, ByteBuf payload)
    {
        String dest;
        int port;
        if (address.toString().contains("/"))
        {
            dest = address.toString().split(Pattern.quote("/"))[1].split(Pattern.quote(":"))[0];
            port = Integer.parseInt(address.toString().split(Pattern.quote("/"))[1].split(Pattern.quote(":"))[1]);
        } else
        {
            dest = address.toString().split(Pattern.quote(":"))[0];
            port = Integer.parseInt(address.toString().split(Pattern.quote(":"))[1]);
        }
        streamRaw(dest, port, payload);
    }

    public void streamRaw(String address, int port, ByteBuf payload)
    {
        ByteBuf bb = buffer(4 + address.getBytes().length + payload.readableBytes());
        bb.writeByte(JRakLib.PACKET_RAW).writeByte((byte) address.getBytes().length).writeBytes(address.getBytes()).writeBytes(Binary.writeShort((short) port)).writeBytes(payload);
        server.pushThreadToMainPacket(bb);
    }

    protected void streamClose(String identifier, String reason)
    {
        ByteBuf bb = buffer(3 + identifier.getBytes().length + reason.getBytes().length);
        bb.writeByte(JRakLib.PACKET_CLOSE_SESSION).writeByte((byte) identifier.getBytes().length).writeBytes(identifier.getBytes()).writeByte((byte) reason.getBytes().length).writeBytes(reason.getBytes());
        server.pushThreadToMainPacket(bb);
    }

    protected void streamInvalid(String identifier)
    {
        ByteBuf bb = buffer(2 + identifier.getBytes().length);
        bb.writeByte(JRakLib.PACKET_INVALID_SESSION).writeByte((byte) identifier.getBytes().length).writeBytes(identifier.getBytes());
        server.pushThreadToMainPacket(bb);
    }

    protected void streamOpen(Session session)
    {
        String identifier = session.getAddress() + ":" + session.getPort();
        ByteBuf bb = buffer(13 + identifier.getBytes().length + session.getAddress().getBytes().length);
        bb.writeByte(JRakLib.PACKET_OPEN_SESSION).writeByte((byte) identifier.getBytes().length).writeBytes(identifier.getBytes()).writeByte((byte) session.getAddress().getBytes().length).writeBytes(session.getAddress().getBytes()).writeBytes(Binary.writeShort((short) session.getPort())).writeBytes(Binary.writeLong(session.getID()));
        server.pushThreadToMainPacket(bb);
    }

    protected void streamACK(String identifier, int identifierACK)
    {
        ByteBuf bb = buffer(6 + identifier.getBytes().length);
        bb.writeByte(JRakLib.PACKET_ACK_NOTIFICATION).writeByte((byte) identifier.getBytes().length).writeBytes(identifier.getBytes()).writeBytes(Binary.writeInt(identifierACK));
        server.pushThreadToMainPacket(bb);
    }

    protected void streamOption(String name, String value)
    {
        ByteBuf bb = buffer(2 + name.getBytes().length + value.getBytes().length);
        bb.writeByte(JRakLib.PACKET_SET_OPTION).writeByte((byte) name.getBytes().length).writeBytes(name.getBytes()).writeBytes(value.getBytes());
        server.pushThreadToMainPacket(bb);
    }

    protected void streamException(Exception e)
    {
        ByteBuf bb = buffer(5 + e.getMessage().getBytes().length + e.getClass().getName().getBytes().length);
        bb.writeByte(JRakLib.PACKET_EXCEPTION_CAUGHT).writeByte((byte) e.getMessage().getBytes().length).writeBytes(e.getMessage().getBytes()).writeBytes(Binary.writeUnsignedShort(e.getClass().getName().getBytes().length)).writeBytes(e.getClass().getName().getBytes());
        server.pushThreadToMainPacket(bb);
    }

    public boolean receiveStream() throws IOException
    {
        ByteBuf packet = server.readMainToThreadPacket();
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
                if (sessions.containsKey(identifier))
                {
                    byte flags = packet.getByte(offset++);
                    ByteBuf buffer = Binary.subbytes(packet, offset);
                    sessions.get(identifier).addEncapsulatedToQueue(EncapsulatedPacket.fromBinary(buffer, true), flags);
                } else
                {
                    streamInvalid(identifier);
                }
            } else if (id == JRakLib.PACKET_RAW)
            {
                int len = packet.getByte(offset++);
                String address = Binary.subbytesString(packet, offset, len);
                offset += len;
                int port = Binary.readShort(Binary.subbytes(packet, offset, 2));
                offset += 2;
                ByteBuf payload = Binary.subbytes(packet, offset);
                //socket.writePacket(payload, ctx, new InetSocketAddress(address, port));
                System.out.println("TODO RAW STREAM: " + JRakLib.bytebufToHexString(payload));
            } else if (id == JRakLib.PACKET_CLOSE_SESSION)
            {
                int len = packet.getByte(offset++);//[offset++]
                String identifier = Binary.subbytesString(packet, offset, len);
                if (sessions.containsKey(identifier))
                {
                    removeSession(sessions.get(identifier));
                } else
                {
                    streamInvalid(identifier);
                }
            } else if (id == JRakLib.PACKET_INVALID_SESSION)
            {
                int len = packet.getByte(offset++);
                String identifier = Binary.subbytesString(packet, offset, len);
                if (sessions.containsKey(identifier))
                {
                    removeSession(sessions.get(identifier));
                }
            } else if (id == JRakLib.PACKET_SET_OPTION)
            {
                int len = packet.getByte(offset++);
                String name = Binary.subbytesString(packet, offset, len);
                offset += len;
                String value = Binary.subbytesString(packet, offset);
                switch (name)
                {
                    case "name":
                        this.name = value;
                        break;
                    case "portChecking":
                        portChecking = Boolean.parseBoolean(value);
                        break;
                    case "packetLimit":
                        packetLimit = Integer.parseInt(value);
                        break;
                }
            } else if (id == JRakLib.PACKET_BLOCK_ADDRESS)
            {
                int len = packet.getByte(offset++);
                String address = Binary.subbytesString(packet, offset, len);
                offset += len;
                int timeout = Binary.readInt(Binary.subbytes(packet, offset, 4));
                blockAddress(address, timeout);
            } else if (id == JRakLib.PACKET_SHUTDOWN)
            {
                for (Session session : sessions.values())
                {
                    removeSession(session);
                }
                socket.close();
                shutdown = true;
            } else if (id == JRakLib.PACKET_EMERGENCY_SHUTDOWN)
            {
                shutdown = true;
            } else
            {
                return false;
            }
            return true;
        }
        return false;
    }

    public void blockAddress(String address)
    {
        blockAddress(address, 30000);
    }

    /**
     * Block an address (no packets will be handled from this address), for the specified timeout.
     *
     * @param address The address of the client in the format of ([IP]:[Port]).
     * @param timeout The timeout value in milliseconds. If -1, the address will be blocked forever.
     */
    public void blockAddress(String address, int timeout)
    {
        long _final = System.currentTimeMillis() + timeout;
        if (!block.containsKey(address) || timeout == -1)
        {
            if (timeout == -1)
            {
                _final = Long.MAX_VALUE;
            } else
            {
                getLogger().notice("[JRakLib Thread #" + Thread.currentThread().getId() + "] Blocked " + address + " for " + timeout + " milliseconds");
            }
            block.put(address, _final);
        } else if (block.get(address) < _final)
        {
            block.put(address, _final);
        }
    }

    public Session getSession(ChannelHandlerContext ctx, String ip, int port)
    {
        String id = ip + ":" + port;
        if (!sessions.containsKey(id))
        {
            sessions.put(id, new Session(this, ctx, ip, port));
        }
        return sessions.get(id);
    }

    public void removeSession(Session session) throws IOException
    {
        removeSession(session, "unknown");
    }

    public void removeSession(Session session, String reason) throws IOException
    {
        String id = session.getAddress() + ":" + session.getPort();
        if (sessions.containsKey(id))
        {
            sessions.get(id).resetSession();
            sessions.get(id).close();
            sessions.remove(id);
            streamClose(id, reason);
        }
    }

    public void openSession(Session session)
    {
        streamOpen(session);
    }

    public void notifyACK(Session session, int identifierACK)
    {
        streamACK(session.getAddress() + ":" + session.getPort(), identifierACK);
    }

    public String getName()
    {
        return name;
    }

    public long getID()
    {
        return serverID;
    }

    private void registerPacket(byte id, Class<? extends Packet> clazz)
    {
        packetPool.put(id, clazz);
    }

    public Packet getPacketFromPool(byte id)
    {
        if (packetPool.containsKey(id))
        {
            Class<? extends Packet> clazz = packetPool.get(id);
            try
            {
                return clazz.newInstance();
            } catch (InstantiationException e)
            {
                e.printStackTrace();
            } catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void registerPackets()
    {
        registerPacket(UNCONNECTED_PING.ID, UNCONNECTED_PING.class);
        registerPacket(UNCONNECTED_PING_OPEN_CONNECTIONS.ID, UNCONNECTED_PING_OPEN_CONNECTIONS.class);
        registerPacket(OPEN_CONNECTION_REQUEST_1.ID, OPEN_CONNECTION_REQUEST_1.class);
        registerPacket(OPEN_CONNECTION_REPLY_1.ID, OPEN_CONNECTION_REPLY_1.class);
        registerPacket(OPEN_CONNECTION_REQUEST_2.ID, OPEN_CONNECTION_REQUEST_2.class);
        registerPacket(OPEN_CONNECTION_REPLY_2.ID, OPEN_CONNECTION_REPLY_2.class);
        registerPacket(UNCONNECTED_PONG.ID, UNCONNECTED_PONG.class);
        registerPacket(ADVERTISE_SYSTEM.ID, ADVERTISE_SYSTEM.class);
        registerPacket(DATA_PACKET_0.ID, DATA_PACKET_0.class);
        registerPacket(DATA_PACKET_1.ID, DATA_PACKET_1.class);
        registerPacket(DATA_PACKET_2.ID, DATA_PACKET_2.class);
        registerPacket(DATA_PACKET_3.ID, DATA_PACKET_3.class);
        registerPacket(DATA_PACKET_4.ID, DATA_PACKET_4.class);
        registerPacket(DATA_PACKET_5.ID, DATA_PACKET_5.class);
        registerPacket(DATA_PACKET_6.ID, DATA_PACKET_6.class);
        registerPacket(DATA_PACKET_7.ID, DATA_PACKET_7.class);
        registerPacket(DATA_PACKET_8.ID, DATA_PACKET_8.class);
        registerPacket(DATA_PACKET_9.ID, DATA_PACKET_9.class);
        registerPacket(DATA_PACKET_A.ID, DATA_PACKET_A.class);
        registerPacket(DATA_PACKET_B.ID, DATA_PACKET_B.class);
        registerPacket(DATA_PACKET_C.ID, DATA_PACKET_C.class);
        registerPacket(DATA_PACKET_D.ID, DATA_PACKET_D.class);
        registerPacket(DATA_PACKET_E.ID, DATA_PACKET_E.class);
        registerPacket(DATA_PACKET_F.ID, DATA_PACKET_F.class);
        registerPacket(NACK.ID, NACK.class);
        registerPacket(ACK.ID, ACK.class);
    }
}
