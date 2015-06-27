/*
   JRakLib networking library.
   This software is not affiliated with RakNet or Jenkins Software LLC.
   This software is a port of PocketMine/RakLib <https://github.com/PocketMine/RakLib>.
   All credit goes to the PocketMine Project (http://pocketmine.net)
 
   Copyright (C) 2015  JRakLib Project

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

import com.sun.deploy.util.ArrayUtil;
import net.beaconpe.jraklib.Binary;
import org.apache.commons.lang3.ArrayUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

/**
 * Base class for all Packets.
 */
public abstract class Packet {

    protected int offset = 0;
    protected int length;
    public byte[] buffer;
    public ByteBuffer sendBuffer;
    public int sendTime;

    public abstract byte getID();

    protected byte[] get(int len){
        if(len < 0){
            offset = buffer.length - 1;
            return new byte[] {};
        } else {
            ByteBuffer bb = ByteBuffer.allocate(len);
            while(len > 0){
                bb.put(buffer[offset]);
                len = len - 1;
                offset = offset + 1;
            }
            return bb.array();
        }
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
            return Binary.readShort(get(2));
        } else {
            return Binary.readUnsignedShort(get(2));
        }
    }

    protected short getShort(){
        return (short) getShort(true);
    }

    protected int getLTriad(){
        return Binary.readLTriad(get(3));
    }

    protected int getByte(boolean signed){
        offset = offset + 1;
        return Binary.readByte(buffer[offset], signed);
    }

    protected byte getByte(){
        return (byte) getByte(true);
    }

    protected String getString(){
        byte[] d = get(getShort());
        return new String(d);
    }

    protected InetSocketAddress getAddress(){
        byte version = getByte();
        if(version == 4){
            String address = ((~getByte()) & 0xff) +"."+ ((~getByte()) & 0xff) +"."+ ((~getByte()) & 0xff) +"."+ ((~getByte()) & 0xff);
            int port = getShort();
            return new InetSocketAddress(address, port);
        } else {
            //TODO: IPv6
            throw new UnsupportedOperationException("IPv6: Not implemented");
        }
    }

    protected boolean feof(){
        try{
            byte d = buffer[offset];
            return true;
        } catch (ArrayIndexOutOfBoundsException e){
            return false;
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
        sendBuffer = ByteBuffer.allocate(1024*1024);
        putByte(getID());
        _encode();
        buffer = ArrayUtils.subarray(sendBuffer.array(), 0, sendBuffer.position());
    }

    public void decode(){
        offset = 1;
        _decode();
    }

    public void clean(){
        buffer = null;
        sendBuffer = null;
        offset = 0;
        sendTime = -1;
    }
}
