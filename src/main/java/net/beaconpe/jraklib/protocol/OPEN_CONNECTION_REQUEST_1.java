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
