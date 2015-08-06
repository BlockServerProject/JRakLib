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
package net.beaconpe.jraklib.protocol;

import net.beaconpe.jraklib.JRakLib;

import java.net.InetSocketAddress;

/**
 * OPEN_CONNECTION_REPLY_2 (Not encapsulated, 0x08)
 */
public class OPEN_CONNECTION_REPLY_2 extends Packet{
    public static byte ID = 0x08;
    public long serverID;
    public InetSocketAddress clientAddress;
    public short mtuSize;


    public byte getID() {
        return 0x08;
    }

    @Override
    protected void _encode() {
        put(JRakLib.MAGIC);
        putLong(serverID);
        putAddress(clientAddress.getHostString(), clientAddress.getPort(), (byte) 4);
        putShort(mtuSize);
        putByte((byte) 0x00); //server security
    }

    @Override
    protected void _decode() {
        offset = offset + 15;
        serverID = getLong();
        clientAddress = getAddress();
        mtuSize = getShort();
        //server security
    }
}
