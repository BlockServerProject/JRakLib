/*
   JRakLib networking library.
   This software is not affiliated with RakNet or Jenkins Software LLC.
   This software is a port of PocketMine/RakLib <https://github.com/PocketMine/RakLib>.
   All credit goes to the PocketMine Project (http://pocketmine.net)
 
   Copyright (C) 2015  BlockServerProject

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
import java.util.List;

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

        externalQueue = new ArrayList<>();
        internalQueue = new ArrayList<>();

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
        internalQueue.add(bytes);
    }

    public byte[] readMainToThreadPacket(){
        byte[] d = internalQueue.get(0);
        internalQueue.remove(d);
        return d;
    }

    public void pushThreadToMainPacket(byte[] bytes){
        externalQueue.add(bytes);
    }

    public byte[] readThreadToMainPacket(){
        byte[] d = externalQueue.get(0);
        externalQueue.remove(d);
        return d;
    }

    private class ShutdownHandler extends Thread{
        public void run(){
            if(shutdown != true){
                logger.emergency("[RakLib Thread #"+getId()+"] RakLib crashed!");
            }
        }
    }

    public void run(){
        Runtime.getRuntime().addShutdownHook(new ShutdownHandler());
        UDPServerSocket socket = new UDPServerSocket(logger, _interface.getPort(), _interface.getHostString());

    }
}
