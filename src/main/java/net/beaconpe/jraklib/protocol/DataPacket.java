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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a RakNet Custom Packet.
 */
public abstract class DataPacket extends Packet{
    public List<Object> packets = new ArrayList<>();
    public int seqNumber = -1;

    @Override
    protected void _encode() {
        putLTriad(seqNumber);
        for(Object o : packets){
            if(o instanceof EncapsulatedPacket){
                put(((EncapsulatedPacket) o).toBinary());
            } else {
                put((byte[]) o);
            }
        }
    }

    public int length(){
        int len = 4;
        for(Object o : packets){
            if(o instanceof EncapsulatedPacket){
                len = len + ((EncapsulatedPacket) o).getTotalLength();
            } else {
                len = len + ((byte[]) o).length;
            }
        }
        return len;
    }

    @Override
    protected void _decode() {
        seqNumber = getLTriad();

        while(!feof()){
            int offset = 0;
            byte[] data = Binary.subbytes(buffer, this.offset);
            if(data.length < 1){
                break;
            }
            EncapsulatedPacket packet = EncapsulatedPacket.fromBinary(data, false, offset);
            offset = packet.offset;
            this.offset = this.offset + packet.offset;
            if(packet.buffer.length == 0){
                break;
            }
            packets.add(packet);
        }
    }

    public void clean(){
        packets.clear();
        seqNumber = -1;
        super.clean();
    }
}
