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
 * OPEN_CONNECTION_REQUEST_1 (Not encapsulated, 0x05)
 */
public class OPEN_CONNECTION_REQUEST_1 extends Packet{
    public static byte ID = 0x05;
    public byte protocol = JRakLib.PROTOCOL;
    public short mtuSize;


    public byte getID() {
        return 0x05;
    }

    @Override
    protected void _encode() {
        put(JRakLib.MAGIC);
        putByte(protocol);
        put(new byte[mtuSize - 18]);
    }

    @Override
    protected void _decode() {
        offset = offset + 15; //Magic
        protocol = getByte();
        mtuSize = (short) (get().length - 18);
    }
}
