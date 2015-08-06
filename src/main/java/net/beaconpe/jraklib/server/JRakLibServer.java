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

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

/**
 * JRakLib server.
 */
public class JRakLibServer extends Thread{
    protected InetSocketAddress _interface;

    protected Logger logger;
    protected boolean shutdown = false;

    protected List<byte[]> externalQueue;
    protected List<byte[]> internalQueue;

    public JRakLibServer(Logger logger, int port, String _interface){
        if(port < 1 || port > 65536){
            throw new IllegalArgumentException("Invalid port range.");
        }
        this._interface = new InetSocketAddress(_interface, port);
        this.logger = logger;
        this.shutdown = false;

        externalQueue = new LinkedList<>();
        internalQueue = new LinkedList<>();

        start();
    }

    public boolean isShutdown(){
        return shutdown == true;
    }

    public void shutdown(){
        shutdown = true;
    }

    public int getPort(){
        return _interface.getPort();
    }

    public String getInterface(){
        return _interface.getHostString();
    }

    public Logger getLogger(){
        return logger;
    }

    public List<byte[]> getExternalQueue(){
        return externalQueue;
    }

    public List<byte[]> getInternalQueue(){
        return internalQueue;
    }

    public void pushMainToThreadPacket(byte[] bytes){
        internalQueue.add(0, bytes);
    }

    public byte[] readMainToThreadPacket(){
        if(!internalQueue.isEmpty()) {
            byte[] data = internalQueue.get(internalQueue.size() - 1);
            internalQueue.remove(data);
            return data;
        }
        return null;
    }

    public void pushThreadToMainPacket(byte[] bytes){
        externalQueue.add(0, bytes);
    }

    public byte[] readThreadToMainPacket(){
        if(!externalQueue.isEmpty()) {
            byte[] data = externalQueue.get(externalQueue.size() - 1);
            externalQueue.remove(data);
            return data;
        }
        return null;
    }

    private class ShutdownHandler extends Thread{
        public void run(){
            if(shutdown != true){
                logger.emergency("[RakLib Thread #"+getId()+"] RakLib crashed!");
            }
        }
    }

    public void run(){
        setName("JRakLib Thread #"+getId());
        Runtime.getRuntime().addShutdownHook(new ShutdownHandler());
        UDPServerSocket socket = new UDPServerSocket(logger, _interface.getPort(), _interface.getHostString());
        new SessionManager(this, socket);
    }
}
