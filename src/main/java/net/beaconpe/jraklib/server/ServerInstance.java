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

    void exceptionCaught(String clazz, String message);

    void handleOption(String option, String value);
}
