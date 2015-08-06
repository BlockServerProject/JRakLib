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

/**
 * UNCONNECTED_PING (Not encapsulated, 0x01)
 */
public class UNCONNECTED_PING extends Packet{
    public static byte ID = 0x01;
    public long pingId;


    public byte getID() {
        return 0x01;
    }

    @Override
    protected void _encode() {
        putLong(pingId);
        put(JRakLib.MAGIC);
    }

    @Override
    protected void _decode() {
        pingId = getLong();
        //magic
    }
}
