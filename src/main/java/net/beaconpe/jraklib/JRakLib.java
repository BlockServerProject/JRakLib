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

import io.netty.buffer.ByteBuf;
import java.util.regex.Pattern;

/**
 * JRakLib Constants Class.
 */
public abstract class JRakLib
{

    public static final String VERSION = "1.2-SNAPSHOT";
    public static final byte PROTOCOL = 7;
    public static final byte[] MAGIC = new byte[]
    {
        0x00, (byte) 0xff, (byte) 0xff, 0x00,
        (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe,
        (byte) 0xfd, (byte) 0xfd, (byte) 0xfd, (byte) 0xfd,
        0x12, 0x34, 0x56, 0x78
    };
    public static final byte PRIORITY_NORMAL = 0;
    public static final byte PRIORITY_IMMEDIATE = 1;
    public static final byte FLAG_NEED_ACK = 0b00001000;
    /*
     * Internal Packet:
     * int32 (length without this field)
     * byte (packet ID)
     * payload
     */
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
     * RAW payload: byte (message length) byte[] message ushort (class length) byte[] class message
     */
    public static final byte PACKET_EXCEPTION_CAUGHT = 0x7d;

    public static void sleepUntil(long time)
    {
        while (true)
        {
            if (System.currentTimeMillis() >= time)
            {
                break;
            }
        }
    }

    public static String getAddressFromString(String address)
    {
        if (address.contains("/"))
        {
            return address.split(Pattern.quote("/"))[1].split(Pattern.quote(":"))[0];
        } else
        {
            return address.split(Pattern.quote(":"))[0];
        }
    }

    public static int getPortFromString(String address)
    {
        if (address.contains("/"))
        {
            return Integer.parseInt(address.split(Pattern.quote("/"))[1].split(Pattern.quote(":"))[1]);
        } else
        {
            return Integer.parseInt(address.split(Pattern.quote(":"))[1]);
        }
    }

    public static String bytebufToHexString(ByteBuf bb)
    {
        byte[] bytes = new byte[bb.readableBytes()];
        for (int i = 0; i < bb.readableBytes(); i++)
        {
            bytes[i] = bb.getByte(i);
        }
        return bytesToHexString(bytes);
    }

    public static String bytesToHexString(byte[] data)
    {
        StringBuilder sb = new StringBuilder();
        String sTemp;
        for (int i = 0; i < data.length; i++)
        {
            sTemp = Integer.toHexString(0xFF & data[i]);
            if (sTemp.length() < 2)
            {
                sb.append(0);
            }
            sb.append(sTemp.toUpperCase()).append(", ");
        }
        return sb.toString();
    }

    public static String getHexString(byte b)
    {
        String hex = Integer.toHexString(0xFF & b);
        String hexString = "";
        if (hex.length() == 1)
        {
            hexString += "0";
        }
        hexString += hex;
        return hexString;
    }
}
