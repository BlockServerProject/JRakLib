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

import net.beaconpe.jraklib.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

/**
 * A UDP Server socket.
 */
public class UDPServerSocket implements Closeable{
    protected Logger logger;
    protected DatagramSocket socket;

    public UDPServerSocket(Logger logger, int port, String _interface){
        this.logger = logger;
        try {
            socket = new DatagramSocket(new InetSocketAddress(_interface, port));
            socket.setBroadcast(true);
            socket.setSendBufferSize(1024 * 1024 * 8);
            socket.setReceiveBufferSize(1024 * 1024 * 8);
            socket.setSoTimeout(1);
        } catch(SocketException e){
            logger.critical("**** FAILED TO BIND TO "+_interface+":"+port+"!");
            logger.critical("Perhaps a server is already running on that port?");
            System.exit(1);
        }
    }

    public DatagramSocket getSocket(){
        return socket;
    }

    public void close(){
        socket.close();
    }

    public DatagramPacket readPacket() throws IOException {
        DatagramPacket dp = new DatagramPacket(new byte[65535], 65535);
        try {
            socket.receive(dp);
            dp.setData(Arrays.copyOf(dp.getData(), dp.getLength()));
            return dp;
        } catch (SocketTimeoutException e) {
            return null;
        }
    }

    public void writePacket(byte[] buffer, InetSocketAddress dest) throws IOException {
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length, dest);
        socket.send(dp);
    }

    public void setSendBuffer(int size) throws SocketException {
        socket.setSendBufferSize(size);
    }

    public void setRecvBuffer(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }
}
