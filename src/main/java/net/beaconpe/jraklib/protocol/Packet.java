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

import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import net.beaconpe.jraklib.Binary;
import static io.netty.buffer.Unpooled.buffer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * Base class for all Packets.
 */
public abstract class Packet
{

    protected int offset = 0;
    protected int length;
    public ByteBuf buffer;
    public ByteBuf sendBuffer;
    public long sendTime;

    public abstract byte getID();

    protected ByteBuf get(int len)
    {
        if (len < 0)
        {
            offset = buffer.readableBytes() - 1;
            return buffer(0);
        } else
        {
            ByteBuf bytes = Binary.subbytes(buffer.copy(), offset, len);
            offset = offset + bytes.readableBytes();
            return bytes;
        }
    }

    public void setBuffer(ByteBuf buffer, int offset)
    {
        this.buffer = buffer;
        this.offset = offset;
    }

    public int getOffset()
    {
        return offset;
    }

    protected ByteBuf get()
    {
        return Binary.subbytes(buffer, offset);
    }

    protected long getLong(boolean signed)
    {
        if (signed)
        {
            return Binary.readLong(get(8));
        } else
        {
            return Binary.readLong(get(8)) & 0xFF;
        }
    }

    protected long getLong()
    {
        return getLong(true);
    }

    protected int getInt()
    {
        return Binary.readInt(get(4));
    }

    protected int getShort(boolean signed)
    {
        if (signed)
        {
            return Binary.readSignedShort(get(2));
        } else
        {
            return Binary.readShort(get(2));
        }
    }

    protected short getShort()
    {
        return (short) getShort(true);
    }

    protected int getLTriad()
    {
        return Binary.readLTriad(get(3));
    }

    protected int getByte(boolean signed)
    {
        int b = Binary.readByte(buffer.getByte(offset), signed);
        offset = offset + 1;
        return b;
    }

    protected byte getByte()
    {
        return (byte) getByte(true);
    }

    protected String getString()
    {
        ByteBuf bb = get(getShort());
        byte[] d = new byte[bb.readableBytes()];
        bb.copy().readBytes(d);
        return new String(d);
    }

    protected InetSocketAddress getAddress()
    {
        int version = getByte();
        if (version == 4)
        {
            String address = ((~getByte()) & 0xff) + "." + ((~getByte()) & 0xff) + "." + ((~getByte()) & 0xff) + "." + ((~getByte()) & 0xff);
            int port = getShort(false);
            return new InetSocketAddress(address, port);
        } else
        {
            //TODO: IPv6
            return new InetSocketAddress("0.0.0.0", 0);
        }
    }

    protected boolean feof()
    {
        return buffer.readableBytes() == offset;
    }

    protected void put(ByteBuf bytes)
    {
        sendBuffer.writeBytes(bytes);
    }

    protected void put(byte[] bytes)
    {
        sendBuffer.writeBytes(bytes);
    }

    protected void putLong(long l)
    {
        sendBuffer.writeBytes(Binary.writeLong(l));
    }

    protected void putInt(int i)
    {
        sendBuffer.writeBytes(Binary.writeInt(i));
    }

    protected void putShort(short s)
    {
        sendBuffer.writeBytes(Binary.writeShort(s));
    }

    protected void putLTriad(int t)
    {
        sendBuffer.writeBytes(Binary.writeLTriad(t));
    }

    protected void putByte(byte b)
    {
        sendBuffer.writeByte(b);
    }

    protected void putString(String s)
    {
        putShort((short) s.getBytes().length);
        put(s.getBytes());
    }

    protected void putAddress(String addr, int port, byte version)
    {
        if (!addr.contains(Pattern.quote(".")))
        {
            try
            {
                addr = InetAddress.getByName(addr).getHostAddress();
            } catch (UnknownHostException e)
            {
                e.printStackTrace();
            }
        }
        putByte(version);
        if (version == 4)
        {
            for (String section : addr.split(Pattern.quote(".")))
            {
                putByte((byte) ((byte) ~(Integer.parseInt(section)) & 0xFF));
            }
            putShort((short) port);
        }
    }

    protected abstract void _encode();

    protected abstract void _decode();

    public void encode()
    {
        sendBuffer = buffer(64 * 64 * 64);
        putByte(getID());
        _encode();
        buffer = sendBuffer.copy();
    }

    public void decode()
    {
        getByte();
        _decode();
    }

    public void clean()
    {
        buffer = null;
        sendBuffer = null;
        offset = 0;
        sendTime = -1;
    }
}
