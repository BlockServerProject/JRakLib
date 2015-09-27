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
import static io.netty.buffer.Unpooled.buffer;
import java.util.Arrays;

/**
 * Binary Utility class for writing/reading.
 */
public class Binary
{

    /**
     * Reads a 3-byte little-endian number
     * @param bytes
     * @return integer
     */
    public static int readLTriad(ByteBuf bytes)
    {
        return (bytes.getByte(0) & 0xFF) | ((bytes.getByte(1) & 0xFF) << 8) | ((bytes.getByte(2) & 0x0F) << 16);
    }

    /**
     * Writes a 3-byte little-endian number
     * @param triad
     * @return triad bytes
     */
    public static byte[] writeLTriad(int triad)
    {
        byte b1, b2, b3;
        b3 = (byte) (triad & 0xFF);
        b2 = (byte) ((triad >> 8) & 0xFF);
        b1 = (byte) ((triad >> 16) & 0xFF);
        return new byte[]
        {
            b3, b2, b1
        };
    }

    /**
     * Reads a signed byte as a boolean
     * @param b The raw byte
     * @return The boolean
     */
    public static boolean readBool(byte b)
    {
        return b == 0;
    }

    /**
     * Writes a signed boolean as a byte
     * @param b The boolean
     * @return Boolean as a byte
     */
    public static byte writeBool(boolean b)
    {
        if (b)
        {
            return 0x01;
        } else
        {
            return 0x00;
        }
    }

    /**
     * Reads a signed/unsigned byte
     * @param b The raw byte
     * @param signed If the byte is signed
     * @return Signed/unsigned byte as int.
     */
    public static int readByte(byte b, boolean signed)
    {
        if (signed)
        {
            return b;
        } else
        {
            return b & 0xFF;
        }
    }

    /**
     * Writes a signed/unsigned byte.
     * @param b Raw byte
     * @return The byte.
     */
    public static byte writeByte(byte b)
    {
        return b;
    }

    /**
     * Reads an unsigned 16 bit big-endian number.
     * @param bytes Raw bytes
     * @return The unsigned short.
     */
    public static int readShort(ByteBuf bytes)
    {
        return ((bytes.getByte(0) << 8) & 0x0000ff00) | (bytes.getByte(1) & 0x000000ff);
    }

    /**
     * Reads a signed 16 bit big-endian number.
     * @param bytes Raw bytes
     * @return The signed short.
     */
    public static short readSignedShort(ByteBuf bytes)
    {
        return bytes.getShort(0);
    }

    /**
     * Writes a signed 16 bit big-endian number.
     * @param s The short
     * @return Short as a byte array
     */
    public static ByteBuf writeShort(short s)
    {
        return buffer(0).writeShort(s); //2
    }

    /**
     * Writes an unsigned 16 bit big-endian number.
     * @param s The unsigned short (integer)
     * @return Short as a byte array
     */
    public static ByteBuf writeUnsignedShort(int s)
    {
        ByteBuf bb = buffer(0); //2
        bb.writeByte((byte) ((s >> 8) & 0xff));
        bb.writeByte((byte) (s & 0xff));
        return bb;
    }

    /**
     * Reads a signed 32 bit big-endian number.
     * @param bytes Raw bytes
     * @return The integer.
     */
    public static int readInt(ByteBuf bytes)
    {
        return bytes.getInt(0);
    }

    /**
     * Writes a signed 32 bit big-endian number.
     * @param i The integer.
     * @return Integer as a byte array
     */
    public static ByteBuf writeInt(int i)
    {
        return buffer(0).writeInt(i); //4
    }

    /**
     * Reads a signed 32 bit big-endian floating point number.
     * @param bytes Raw bytes
     * @return The float
     */
    public static float readFloat(ByteBuf bytes)
    {
        return bytes.getFloat(0);
    }

    /**
     * Writes a signed 32 bit big-endian floating point number.
     * @param f The float.
     * @return The float as a byte array
     */
    public static ByteBuf writeFloat(float f)
    {
        return buffer(0).writeFloat(f); //4
    }

    /**
     * Reads a signed 64 bit big-endian double precision number.
     * @param bytes Raw bytes
     * @return The double.
     */
    public static double readDouble(ByteBuf bytes)
    {
        return bytes.getDouble(0);
    }

    /**
     * Writes a signed 64 bit big-endian double precision number.
     * @param d The double.
     * @return The double as a byte array
     */
    public static ByteBuf writeDouble(double d)
    {
        return buffer(0).writeDouble(d); //8
    }

    /**
     * Reads a signed 64 bit big-endian number.
     * @param bytes Raw bytes
     * @return The long
     */
    public static long readLong(ByteBuf bytes)
    {
        return bytes.getLong(0);
    }

    /**
     * Writes a signed 64 bit big-endian number.
     * @param l The long.
     * @return The long as a byte array.
     */
    public static ByteBuf writeLong(long l)
    {
        return buffer(0).writeLong(l); //8
    }

    public static String subbytesString(ByteBuf bytes, int start, int length)
    {
        ByteBuf bb = Binary.subbytes(bytes, start, length);
        byte[] d = new byte[bb.readableBytes()];
        bb.getBytes(0, d, 0, bb.readableBytes());
        return new String(d);
    }

    public static String subbytesString(ByteBuf bytes, int start)
    {
        return subbytesString(bytes, start, bytes.readableBytes() - start);
    }

    public static ByteBuf subbytes(ByteBuf bytes, int start, int length)
    {
        return bytes.copy().slice(start, length);
    }

    public static ByteBuf subbytes(ByteBuf bytes, int start)
    {
        return subbytes(bytes, start, bytes.readableBytes() - start);
    }

    public static byte[][] splitbytes(ByteBuf bytes, int chunkSize)
    {
        byte[][] splits = new byte[1024][chunkSize];
        int chunks = 0;
        for (int i = 0; i < bytes.readableBytes(); i += chunkSize)
        {
            if ((bytes.readableBytes() - i) > chunkSize)
            {
                splits[chunks] = Arrays.copyOfRange(bytes.array(), i, i + chunkSize);
            } else
            {
                splits[chunks] = Arrays.copyOfRange(bytes.array(), i, bytes.readableBytes());
            }
            chunks++;
        }
        splits = Arrays.copyOf(splits, chunks);
        return splits;
    }
}
