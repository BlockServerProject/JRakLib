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
package net.beaconpe.jraklib;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * JRakLib Constants Class.
 */
public abstract class JRakLib {
    public static final String VERSION = "1.1.1-SNAPSHOT";
    public static final byte PROTOCOL = 7;
    public static final byte[] MAGIC = new byte[]{
            0x00, (byte) 0xff, (byte) 0xff, 0x00,
            (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe,
            (byte) 0xfd, (byte) 0xfd, (byte) 0xfd, (byte) 0xfd,
            0x12, 0x34, 0x56, 0x78 };

    public static final byte PRIORITY_NORMAL = 0;
    public static final byte PRIORITY_IMMEDIATE = 1;

    public static final byte FLAG_NEED_ACK = 0b00001000;
    
    /*
     * ENCAPSULATED payload:
     * byte (identifier length)
     * byte[] (identifier)
     * byte (flags, last 3 bits, priority)
     * payload (binary internal EncapsulatedPacket)
     */
    public static final byte PACKET_ENCAPSULATED = 0x01;
    /*
     * OPEN_SESSION payload:
     * byte (identifier length)
     * byte[] (identifier)
     * byte (address length)
     * byte[] (address)
     * short (port)
     * long (clientID)
     */
    public static final byte PACKET_OPEN_SESSION = 0x02;
    /*
     * CLOSE_SESSION payload:
     * byte (identifier length)
     * byte[] (identifier)
     * string (reason)
     */
    public static final byte PACKET_CLOSE_SESSION = 0x03;
    /*
     * INVALID_SESSION payload:
     * byte (identifier length)
     * byte[] (identifier)
     */
    public static final byte PACKET_INVALID_SESSION = 0x04;
    /* SEND_QUEUE payload:
     * byte (identifier length)
     * byte[] (identifier)
     */
    public static final byte PACKET_SEND_QUEUE = 0x05;
    /*
     * ACK_NOTIFICATION payload:
     * byte (identifier length)
     * byte[] (identifier)
     * int (identifierACK)
     */
    public static final byte PACKET_ACK_NOTIFICATION = 0x06;
    /*
     * SET_OPTION payload:
     * byte (option name length)
     * byte[] (option name)
     * byte[] (option value)
     */
    public static final byte PACKET_SET_OPTION = 0x07;
    /*
     * RAW payload:
     * byte (address length)
     * byte[] (address from/to)
     * short (port)
     * byte[] (payload)
     */
    public static final byte PACKET_RAW = 0x08;
    /*
     * RAW payload:
     * byte (address length)
     * byte[] (address)
     * int (timeout)
     */
    public static final byte PACKET_BLOCK_ADDRESS = 0x09;
    /*
     * No payload
     *
     * Sends the disconnect message, removes sessions correctly, closes sockets.
     */
    public static final byte PACKET_SHUTDOWN = 0x7e;
    /*
     * No payload
     *
     * Leaves everything as-is and halts, other Threads can be in a post-crash condition.
     */
    public static final byte PACKET_EMERGENCY_SHUTDOWN = 0x7f;

    /**
     * RAW payload:
     * byte (message length)
     * byte[] message
     * ushort (class length)
     * byte[] class message
     */
    public static final byte PACKET_EXCEPTION_CAUGHT = 0x7d;

    public static void sleepUntil(long time) {
        while (true) {
            if (Instant.now().toEpochMilli() >= time) {
                break;
            }
        }
    }

    public static String getAddressFromString(String address){
        if(address.contains("/")){
            return address.split(Pattern.quote("/"))[1].split(Pattern.quote(":"))[0];
        } else {
            return address.split(Pattern.quote(":"))[0];
        }
    }

    public static int getPortFromString(String address){
        if(address.contains("/")){
            return Integer.parseInt(address.split(Pattern.quote("/"))[1].split(Pattern.quote(":"))[1]);
        } else {
            return Integer.parseInt(address.split(Pattern.quote(":"))[1]);
        }
    }
}
