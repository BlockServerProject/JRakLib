package net.beaconpe.jraklib.client;

import net.beaconpe.jraklib.protocol.EncapsulatedPacket;

/**
 * Represents a Connection to a server.
 *
 * @author jython234
 */
public class Connection {

    protected boolean connected;

    protected ConnectionManager manager;

    public Connection(ConnectionManager manager){

    }

    public void update(long time){

    }

    public void handlePacket(byte[] data) {

    }

    public void addEncapsulatedToQueue(EncapsulatedPacket packet, byte flags) {

    }

    protected void onShutdown() {

    }
}
