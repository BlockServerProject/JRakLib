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

/**
 * SERVER_HANDSHAKE (Encapsulated, 0x10)
 */
public class SERVER_HANDSHAKE_DataPacket extends Packet{
    public static byte ID = 0x10;
    public InetSocketAddress address;
    public InetSocketAddress[] systemAddresses = new InetSocketAddress[] {
            new InetSocketAddress("127.0.0.1", 0),
            new InetSocketAddress("0.0.0.0", 0),
            new InetSocketAddress("0.0.0.0", 0),
            new InetSocketAddress("0.0.0.0", 0),
            new InetSocketAddress("0.0.0.0", 0),
            new InetSocketAddress("0.0.0.0", 0),
            new InetSocketAddress("0.0.0.0", 0),
            new InetSocketAddress("0.0.0.0", 0),
            new InetSocketAddress("0.0.0.0", 0),
            new InetSocketAddress("0.0.0.0", 0),
    };

    public long sendPing;
    public long sendPong;


    public byte getID() {
        return 0x10;
    }

    @Override
    protected void _encode() {
        putAddress(address.getHostString(), address.getPort(), (byte) 4);
        putShort((short) 0);
        for(InetSocketAddress a : systemAddresses){
            putAddress(a.getHostString(), a.getPort(), (byte) 4);
        }
        putLong(sendPing);
        putLong(sendPong);
    }

    @Override
    protected void _decode() {
        address = getAddress();
        getShort();
        systemAddresses = new InetSocketAddress[10];
        for(int i = 0; i < 10; i++){
            systemAddresses[i] = getAddress();
        }
        sendPing = getLong();
        sendPong = getLong();
    }
}
