/*
   JRakLib networking library.
   This software is not affiliated with RakNet or Jenkins Software LLC.
   This software is a port of PocketMine/RakLib <https://github.com/PocketMine/RakLib>.
   All credit goes to the PocketMine Project (http://pocketmine.net)
 
   Copyright (C) 2015 BlockServerProject & PocketMine team

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.beaconpe.jraklib.server;

import net.beaconpe.jraklib.Logger;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * JRakLib server.
 */
public class JRakLibServer extends Thread{
    protected InetSocketAddress _interface;

    protected Logger logger;
    protected boolean shutdown = false;

    protected Deque<byte[]> externalQueue;
    protected Deque<byte[]> internalQueue;

    public JRakLibServer(Logger logger, int port, String _interface){
        if(port < 1 || port > 65536){
            throw new IllegalArgumentException("Invalid port range.");
        }
        this._interface = new InetSocketAddress(_interface, port);
        this.logger = logger;
        this.shutdown = false;

        externalQueue = new ConcurrentLinkedDeque<>();
        internalQueue = new ConcurrentLinkedDeque<>();

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

    public Deque<byte[]> getExternalQueue(){
        return externalQueue;
    }

    public Deque<byte[]> getInternalQueue(){
        return internalQueue;
    }

    public void pushMainToThreadPacket(byte[] bytes){
        internalQueue.addLast(bytes);
    }

    public byte[] readMainToThreadPacket(){
        if(!internalQueue.isEmpty()) {
            return internalQueue.pop();
        }
        return null;
    }

    public void pushThreadToMainPacket(byte[] bytes){
        externalQueue.addLast(bytes);
    }

    public byte[] readThreadToMainPacket(){
        if(!externalQueue.isEmpty()) {
            externalQueue.pop();
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
