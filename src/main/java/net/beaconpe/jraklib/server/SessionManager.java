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
import net.beaconpe.jraklib.Logger;

import net.beaconpe.jraklib.protocol.*;
import net.beaconpe.jraklib.protocol.DataPackets.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static net.beaconpe.jraklib.JRakLib.*;

/**
 * Manager for managing sessions.
 */
public class SessionManager{
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

    public SessionManager(JRakLibServer server, UDPServerSocket socket){
        this.server = server;
        this.socket = socket;
        registerPackets();

        serverID = new Random().nextLong();

        run();
    }

    public int getPort(){
        return server.getPort();
    }

    public Logger getLogger(){
        return server.getLogger();
    }

    public void run(){
        try {
            tickProcessor();
        } catch (Exception e) {
            streamException(e);
        }
    }

    private void tickProcessor() throws IOException {
        lastMeasure = Instant.now().toEpochMilli();

        while(!shutdown){
            long start = Instant.now().toEpochMilli();
            int max = 5000;
            while(receivePacket()){
                max = max - 1;
            }
            while(receiveStream());
            long time = Instant.now().toEpochMilli() - start;
            if(time < 50){ //20 ticks per second (1000 / 20)
                sleepUntil(Instant.now().toEpochMilli()+(50 - time));
            }
            tick();
        }
    }

    private void tick() throws IOException {
        long time = Instant.now().toEpochMilli();
        for(Session session: sessions.values()){
            session.update(time);
        }

        for(String address : ipSec.keySet()){
            if(ipSec.get(address) >= packetLimit){
                blockAddress(address);
            }
        }
        ipSec.clear();

        if((ticks & 0b1111) == 0){
            double diff = Math.max(0.005d, time - lastMeasure);
            streamOption("bandwith", "up:"+(sendBytes / diff)+",down:"+(receiveBytes / diff)); //TODO: Fix this stuff
            lastMeasure = time;
            sendBytes = 0;
            receiveBytes = 0;

            if(block.values().size() > 0){
                long now = Instant.now().toEpochMilli();
                for(String address: block.keySet()){
                    if(block.get(address) <= now){
                        block.remove(address);
                    } else {
                        break;
                    }
                }
            }
        }
        ticks = ticks + 1;
    }

    private boolean receivePacket() throws IOException {
        DatagramPacket packet = socket.readPacket();
        if(packet == null) {
            return false;
        }
        int len = packet.getLength();
        if(len > 0){
            SocketAddress source = packet.getSocketAddress();
            receiveBytes += len;
            if(block.containsKey(source.toString())){
                return true;
            }

            if(ipSec.containsKey(source.toString())){
                ipSec.put(source.toString(), ipSec.get(source.toString()) + 1);
            } else {
                ipSec.put(source.toString(), 1);
            }

            Packet pkt = getPacketFromPool(packet.getData()[0]);
            if(pkt != null){
                pkt.buffer = packet.getData();
                getSession(getAddressFromString(source.toString()), packet.getPort()).handlePacket(pkt);
                return true;
            } else if (packet.getData() != null){
                streamRaw(source, packet.getData());
                return true;
            } else {
                getLogger().notice("Dropped packet: "+ Arrays.toString(packet.getData()));
                return false;
            }
        }
        return false;
    }

    public void sendPacket(Packet packet, String dest, int port) throws IOException {
        packet.encode();
        sendBytes += packet.buffer.length;
        socket.writePacket(packet.buffer, new InetSocketAddress(dest, port));
    }

    public void streamEncapsulated(Session session, EncapsulatedPacket packet){
        streamEncapsulated(session, packet, JRakLib.PRIORITY_NORMAL);
    }

    public void streamEncapsulated(Session session, EncapsulatedPacket packet, byte flags){
        String id = session.getAddress() + ":" + session.getPort();
        ByteBuffer bb = ByteBuffer.allocate(3+id.getBytes().length+packet.getTotalLength(true));
        bb.put(JRakLib.PACKET_ENCAPSULATED).put((byte) id.getBytes().length).put(id.getBytes()).put(flags).put(packet.toBinary(true));
        server.pushThreadToMainPacket(bb.array());
    }

    public void streamRaw(SocketAddress address, byte[] payload){
        String dest;
        int port;
        if(address.toString().contains("/")) {
            dest = address.toString().split(Pattern.quote("/"))[1].split(Pattern.quote(":"))[0];
            port = Integer.parseInt(address.toString().split(Pattern.quote("/"))[1].split(Pattern.quote(":"))[1]);
        } else {
            dest = address.toString().split(Pattern.quote(":"))[0];
            port = Integer.parseInt(address.toString().split(Pattern.quote(":"))[1]);
        }
        streamRaw(dest, port, payload);
    }

    public void streamRaw(String address, int port, byte[] payload){
        ByteBuffer bb = ByteBuffer.allocate(4 + address.getBytes().length + payload.length);
        bb.put(JRakLib.PACKET_RAW).put((byte) address.getBytes().length).put(address.getBytes()).put(Binary.writeShort((short) port)).put(payload);
        server.pushThreadToMainPacket(bb.array());
    }

    protected void streamClose(String identifier, String reason){
        ByteBuffer bb = ByteBuffer.allocate(3 + identifier.getBytes().length + reason.getBytes().length);
        bb.put(JRakLib.PACKET_CLOSE_SESSION).put((byte) identifier.getBytes().length).put(identifier.getBytes()).put((byte) reason.getBytes().length).put(reason.getBytes());
        server.pushThreadToMainPacket(bb.array());
    }

    protected void streamInvalid(String identifier){
        ByteBuffer bb = ByteBuffer.allocate(2+identifier.getBytes().length);
        bb.put(JRakLib.PACKET_INVALID_SESSION).put((byte) identifier.getBytes().length).put(identifier.getBytes());
        server.pushThreadToMainPacket(bb.array());
    }

    protected void streamOpen(Session session){
        String identifier = session.getAddress() + ":" + session.getPort();
        ByteBuffer bb = ByteBuffer.allocate(13 + identifier.getBytes().length+session.getAddress().getBytes().length);
        bb.put(JRakLib.PACKET_OPEN_SESSION).put((byte) identifier.getBytes().length).put(identifier.getBytes()).put((byte) session.getAddress().getBytes().length).put(session.getAddress().getBytes()).put(Binary.writeShort((short) session.getPort())).put(Binary.writeLong(session.getID()));
        server.pushThreadToMainPacket(bb.array());
    }

    protected void streamACK(String identifier, int identifierACK){
        ByteBuffer bb = ByteBuffer.allocate(6+identifier.getBytes().length);
        bb.put(JRakLib.PACKET_ACK_NOTIFICATION).put((byte) identifier.getBytes().length).put(identifier.getBytes()).put(Binary.writeInt(identifierACK));
        server.pushThreadToMainPacket(bb.array());
    }

    protected void streamOption(String name, String value){
        ByteBuffer bb = ByteBuffer.allocate(2+name.getBytes().length+value.getBytes().length);
        bb.put(JRakLib.PACKET_SET_OPTION).put((byte) name.getBytes().length).put(name.getBytes()).put(value.getBytes());
        server.pushThreadToMainPacket(bb.array());
    }

    protected void streamException(Exception e) {
        ByteBuffer bb = ByteBuffer.allocate(5+e.getMessage().getBytes().length+e.getClass().getName().getBytes().length);
        bb.put(JRakLib.PACKET_EXCEPTION_CAUGHT).put((byte) e.getMessage().getBytes().length).put(e.getMessage().getBytes()).put(Binary.writeUnsignedShort(e.getClass().getName().getBytes().length)).put(e.getClass().getName().getBytes());
        server.pushThreadToMainPacket(bb.array());
    }

    public boolean receiveStream() throws IOException {
        byte[] packet = server.readMainToThreadPacket();
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
                if(sessions.containsKey(identifier)){
                    byte flags = packet[offset++];
                    byte[] buffer = Binary.subbytes(packet, offset);
                    sessions.get(identifier).addEncapsulatedToQueue(EncapsulatedPacket.fromBinary(buffer, true), flags);
                } else {
                    streamInvalid(identifier);
                }
            } else if(id == JRakLib.PACKET_RAW){
                int len = packet[offset++];
                String address = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                int port = Binary.readShort(Binary.subbytes(packet, offset, 2));
                offset += 2;
                byte[] payload = Binary.subbytes(packet, offset);
                socket.writePacket(payload, new InetSocketAddress(address, port));
            } else if(id == JRakLib.PACKET_CLOSE_SESSION){
                int len = packet[offset++];
                String identifier = new String(Binary.subbytes(packet, offset, len));
                if(sessions.containsKey(identifier)){
                    removeSession(sessions.get(identifier));
                } else {
                    streamInvalid(identifier);
                }
            } else if(id == JRakLib.PACKET_INVALID_SESSION){
                int len = packet[offset++];
                String identifier = new String(Binary.subbytes(packet, offset, len));
                if(sessions.containsKey(identifier)){
                    removeSession(sessions.get(identifier));
                }
            } else if(id == JRakLib.PACKET_SET_OPTION){
                int len = packet[offset++];
                String name = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                String value = new String(Binary.subbytes(packet, offset));
                switch(name){
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
            } else if(id == JRakLib.PACKET_BLOCK_ADDRESS){
                int len = packet[offset++];
                String address = new String(Binary.subbytes(packet, offset, len));
                offset += len;
                int timeout = Binary.readInt(Binary.subbytes(packet, offset, 4));
                blockAddress(address, timeout);
            } else if(id == JRakLib.PACKET_SHUTDOWN){
                for(Session session: sessions.values()){
                    removeSession(session);
                }

                socket.close();
                shutdown = true;
            } else if(id == JRakLib.PACKET_EMERGENCY_SHUTDOWN){
                shutdown = true;
            } else {
                return false;
            }
            return true;
        }
        return false;
    }

    public void blockAddress(String address){
        blockAddress(address, 30000);
    }

    /**
     * Block an address (no packets will be handled from this address), for the specified timeout.
     * @param address The address of the client in the format of ([IP]:[Port]).
     * @param timeout The timeout value in milliseconds. If -1, the address will be blocked forever.
     */
    public void blockAddress(String address, int timeout){
        long _final = Instant.now().toEpochMilli() + timeout;
        if(!block.containsKey(address) || timeout == -1){
            if(timeout == -1){
                _final = Long.MAX_VALUE;
            } else {
                getLogger().notice("[JRakLib Thread #"+Thread.currentThread().getId()+"] Blocked "+address+" for "+timeout+" milliseconds");
            }
            block.put(address, _final);
        } else if(block.get(address) < _final){
            block.put(address, _final);
        }
    }

    public Session getSession(String ip, int port){
        String id = ip + ":" + port;
        if(!sessions.containsKey(id)){
            sessions.put(id, new Session(this, ip, port));
        }

        return sessions.get(id);
    }

    public void removeSession(Session session) throws IOException {
        removeSession(session, "unknown");
    }

    public void removeSession(Session session, String reason) throws IOException {
        String id = session.getAddress() + ":" + session.getPort();
        if(sessions.containsKey(id)){
            sessions.get(id).close();
            sessions.remove(id);
            streamClose(id, reason);
        }
    }

    public void openSession(Session session){
        streamOpen(session);
    }

    public void notifyACK(Session session, int identifierACK){
        streamACK(session.getAddress() + ":" + session.getPort(), identifierACK);
    }

    public String getName(){
        return name;
    }

    public long getID(){
        return serverID;
    }

    private void registerPacket(byte id, Class<? extends Packet> clazz) {
        packetPool.put(id, clazz);
    }

    public Packet getPacketFromPool(byte id) {
        if(packetPool.containsKey(id)){
            Class<? extends Packet> clazz = packetPool.get(id);
            try {
                return clazz.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void registerPackets(){
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
