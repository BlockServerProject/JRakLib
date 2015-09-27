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

import java.net.InetSocketAddress;
import net.beaconpe.jraklib.JRakLib;

/**
 * OPEN_CONNECTION_REQUEST_2 (Not encapsulated, 0x07)
 */
public class OPEN_CONNECTION_REQUEST_2 extends Packet
{

    public static byte ID = 0x07;
    public long clientID;
    public InetSocketAddress serverAddress;
    public short mtuSize;

    @Override
    public byte getID()
    {
        return 0x07;
    }

    @Override
    protected void _encode()
    {
        put(JRakLib.MAGIC);
        putAddress(serverAddress.getHostString(), serverAddress.getPort(), (byte) 4);
        putShort(mtuSize);
        putLong(clientID);
    }

    @Override
    protected void _decode()
    {
        offset = offset + JRakLib.MAGIC.length;
        serverAddress = getAddress();
        mtuSize = getShort();
        clientID = getLong();
    }
}
