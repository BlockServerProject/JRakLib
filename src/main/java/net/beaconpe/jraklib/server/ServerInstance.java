package net.beaconpe.jraklib.server;

import net.beaconpe.jraklib.protocol.EncapsulatedPacket;

/**
 * An interface for communication with the server implementation.
 */
public interface ServerInstance {

    void openSession(String identifier, String address, int port, long clientID);

    void closeSession(String identifier, String reason);

    void handleEncapsulated(String identifier, EncapsulatedPacket packet, int flags);

    void handleRaw(String address, int port, byte[] payload);

    void notifyACK(String identifier, int identifierACK);

    void handleOption(String option, String value);
}
