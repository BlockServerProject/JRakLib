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

import net.beaconpe.jraklib.Binary;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Base class for all Packets.
 */
public abstract class Packet {

    protected int offset = 0;
    protected int length;
    public byte[] buffer;
    protected ByteBuffer sendBuffer;
    public long sendTime;

    public abstract byte getID();

    protected byte[] get(int len){
        if(len < 0){
            offset = buffer.length - 1;
            return new byte[] {};
        } else {
            /*
            ByteBuffer bb = ByteBuffer.allocate(len);
            while(len > 0 && !feof()){
                offset = offset + 1;
                if(buffer.length == offset){
                    offset = offset - 1;
                }
                bb.put(buffer[offset]);
                len = len - 1;
            }
            return bb.array();
            */
            byte[] bytes = Binary.subbytes(buffer, offset, len);
            offset = offset + bytes.length;
            return bytes;
        }
    }

    public void setBuffer(byte[] buffer, int offset){
        this.buffer = buffer;
        this.offset = offset;
    }

    public int getOffset(){
        return offset;
    }

    protected byte[] get(){
        return Binary.subbytes(buffer, offset);
    }

    protected long getLong(boolean signed){
        if(signed){
            return Binary.readLong(get(8));
        } else {
            return Binary.readLong(get(8)) & 0xFF;
        }
    }

    protected long getLong(){
        return getLong(true);
    }

    protected int getInt(){
        return Binary.readInt(get(4));
    }

    protected int getShort(boolean signed){
        if(signed){
            return Binary.readSignedShort(get(2));
        } else {
            return Binary.readShort(get(2));
        }
    }

    protected short getShort(){
        return (short) getShort(true);
    }

    protected int getLTriad(){
        return Binary.readLTriad(get(3));
    }

    protected int getByte(boolean signed){
        int b = Binary.readByte(buffer[offset], signed);
        offset = offset + 1;
        return b;
    }

    protected byte getByte(){
        return (byte) getByte(true);
    }

    protected String getString(){
        byte[] d = get(getShort());
        return new String(d);
    }

    protected InetSocketAddress getAddress(){
        int version = getByte();
        if(version == 4){
            String address = ((~getByte()) & 0xff) +"."+ ((~getByte()) & 0xff) +"."+ ((~getByte()) & 0xff) +"."+ ((~getByte()) & 0xff);
            int port = getShort(false);
            return new InetSocketAddress(address, port);
        } else {
            //TODO: IPv6
            return new InetSocketAddress("0.0.0.0", 0);
        }
    }

    protected boolean feof(){
        try{
            byte d = buffer[offset];
            return false;
        } catch (ArrayIndexOutOfBoundsException e){
            return true;
        }
    }

    protected void put(byte[] bytes){
        sendBuffer.put(bytes);
    }

    protected void putLong(long l){
        sendBuffer.put(Binary.writeLong(l));
    }

    protected void putInt(int i){
        sendBuffer.put(Binary.writeInt(i));
    }

    protected void putShort(short s){
        sendBuffer.put(Binary.writeShort(s));
    }

    protected void putLTriad(int t){
        sendBuffer.put(Binary.writeLTriad(t));
    }

    protected void putByte(byte b){
        sendBuffer.put(b);
    }

    protected void putString(String s){
        putShort((short) s.getBytes().length);
        put(s.getBytes());
    }

    protected void putAddress(String addr, int port, byte version){
        if(!addr.contains(Pattern.quote("."))){
            try {
                addr = InetAddress.getByName(addr).getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        putByte(version);
        if(version == 4){
            for (String section : addr.split(Pattern.quote("."))){
                putByte((byte) ((byte) ~(Integer.parseInt(section)) & 0xFF));
            }
            putShort((short) port);
        }
    }

    protected abstract void _encode();
    protected abstract void _decode();

    public void encode(){
        sendBuffer = ByteBuffer.allocate(64 * 64 * 64);
        putByte(getID());
        _encode();
        buffer = Arrays.copyOf(sendBuffer.array(), sendBuffer.position());
    }

    public void decode(){
        getByte();
        _decode();
    }

    public void clean(){
        buffer = null;
        sendBuffer = null;
        offset = 0;
        sendTime = -1;
    }
}
