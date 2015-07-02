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

import java.net.InetSocketAddress;

/**
 * CLIENT_HANDSHAKE (Encapsulated, 0x13)
 */
public class CLIENT_HANDSHAKE_DataPacket extends Packet{
    public static byte ID = 0x13;
    public InetSocketAddress address;

    public InetSocketAddress[] systemAddresses;

    public long sendPing;
    public long sendPong;


    public byte getID() {
        return 0x13;
    }

    @Override
    protected void _encode() {
        putAddress(address.getHostString(), address.getPort(), (byte) 4);
        for(InetSocketAddress a : systemAddresses){
            putAddress(a.getHostString(), a.getPort(), (byte) 4);
        }
        putLong(sendPing);
        putLong(sendPong);
    }

    @Override
    protected void _decode() {
        address = getAddress();
        systemAddresses = new InetSocketAddress[10];
        for(int i = 0; i < 10; i++){
            systemAddresses[i] = getAddress();
        }
        sendPing = getLong();
        sendPong = getLong();
    }
}
