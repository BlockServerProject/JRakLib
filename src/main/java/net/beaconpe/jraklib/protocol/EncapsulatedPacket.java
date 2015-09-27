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

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Represents an encapsulated packet inside a CustomPacket
 */
public class EncapsulatedPacket {
    public byte reliability;
    public boolean hasSplit = false;
    public int length = 0;
    public int messageIndex = -1;
    public int orderIndex = -1;
    public byte orderChannel = -1;
    public int splitCount = -1;
    public short splitID = -1;
    public int splitIndex = -1;
    public byte[] buffer;
    public boolean needACK = false;
    public int identifierACK = -1;

    public int bufferLength;

    protected int offset;

    public static EncapsulatedPacket fromBinary(byte[] binary){
        return fromBinary(binary, false);
    }

    public static EncapsulatedPacket fromBinary(byte[] binary, boolean internal){
        return fromBinary(binary, internal, -1);
    }

    public static EncapsulatedPacket fromBinary(byte[] binary, boolean internal, int offset){
        EncapsulatedPacket packet = new EncapsulatedPacket();
        packet.bufferLength = binary.length;
        byte flags = binary[0];
        packet.reliability = (byte) ((flags & 0b11100000) >> 5);
        packet.hasSplit = (flags & 0b00010000) > 0;
        int length;
        if(internal){
            length = Binary.readInt(Binary.subbytes(binary, 1, 4));
            packet.identifierACK = Binary.readInt(Binary.subbytes(binary, 5, 4));
            offset = 9;
        } else {
            length = (int) Math.ceil(Binary.readShort(Binary.subbytes(binary, 1, 2)) / 8.0);
            offset = 3;
            packet.identifierACK = -1;
        }

        if(packet.reliability > 0){
            if(packet.reliability >= 2 && packet.reliability != 5){
                packet.messageIndex = Binary.readLTriad(Binary.subbytes(binary, offset, 3));
                offset = offset + 3;
            }

            if(packet.reliability <= 4 && packet.reliability != 2){
                packet.orderIndex = Binary.readLTriad(Binary.subbytes(binary, offset, 3));
                offset = offset + 3;
                packet.orderChannel = binary[offset++];
            }
        }

        if(packet.hasSplit){
            packet.splitCount = Binary.readInt(Binary.subbytes(binary, offset, 4));
            offset = offset + 4;
            packet.splitID = (short) Binary.readShort(Binary.subbytes(binary, offset, 2));
            offset = offset + 2;
            packet.splitIndex = Binary.readInt(Binary.subbytes(binary, offset, 4));
            offset = offset + 4;
        }

        packet.buffer = Binary.subbytes(binary, offset, length);
        offset = offset + length;
        packet.offset = offset;
        return packet;
    }

    public int getTotalLength(){
        return getTotalLength(false);
    }

    public int getTotalLength(boolean internal){
        if(internal) {
            return 9 + buffer.length + (messageIndex != -1 ? 3 : 0) + (orderIndex != -1 ? 4 : 0) + (hasSplit ? 10 : 0);
        } else {
            return 3 + buffer.length + (messageIndex != -1 ? 3 : 0) + (orderIndex != -1 ? 4 : 0) + (hasSplit ? 10 : 0);
        }
    }

    public byte[] toBinary(boolean internal){
        int offset = 0;
        ByteBuffer bb = ByteBuffer.allocate(64 * 64 * 64);
        bb.put((byte) ((byte) (reliability << 5) | (hasSplit ? 0b00010000 : 0)));
        if(internal){
            bb.put(Binary.writeInt(buffer.length));
            bb.put(Binary.writeInt(identifierACK));
        } else {
            bb.put(Binary.writeShort((short) (buffer.length << 3)));
        }

        if(reliability > 0){
            if(reliability >= 2 && reliability != 5){
                bb.put(Binary.writeLTriad(messageIndex));
            }
            if(reliability <= 4 && reliability != 2){
                bb.put(Binary.writeLTriad(orderIndex));
                bb.put(Binary.writeByte(orderChannel));
            }
        }

        if(hasSplit){
            bb.put(Binary.writeInt(splitCount));
            bb.put(Binary.writeShort(splitID));
            bb.put(Binary.writeInt(splitIndex));
        }

        bb.put(buffer);
        byte[] data = Arrays.copyOf(bb.array(), bb.position());
        bufferLength = data.length;
        bb = null;
        return data;
    }

    public byte[] toBinary(){
        return toBinary(false);
    }
}
