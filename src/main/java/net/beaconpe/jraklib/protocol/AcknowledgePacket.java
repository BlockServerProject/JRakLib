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

import net.beaconpe.jraklib.Binary;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for ACK/NACK.
 */
public abstract class AcknowledgePacket extends Packet{

    public Integer[] packets;

    @Override
    protected void _encode() {
        ByteBuffer payload = ByteBuffer.allocate(1024);
        int count = packets.length;
        int records = 0;

        if(count > 0){
            int pointer = 0;
            int start = packets[0];
            int last = packets[0];

            while(pointer + 1 < count){
                int current = packets[pointer++];
                int diff = current - last;
                if(diff == 1){
                    last = current;
                } else if(diff > 1){ //Forget about duplicated packets (bad queues?)
                    if(start == last){
                        payload.put((byte) 0x01);
                        payload.put(Binary.writeLTriad(start));
                        start = last = current;
                    } else {
                        payload.put((byte) 0x00);
                        payload.put(Binary.writeLTriad(start));
                        payload.put(Binary.writeLTriad(last));
                        start = last = current;
                    }
                    records = records + 1;
                }
            }

            if(start == last){
                payload.put((byte) 0x01);
                payload.put(Binary.writeLTriad(start));
            } else {
                payload.put((byte) 0x00);
                payload.put(Binary.writeLTriad(start));
                payload.put(Binary.writeLTriad(last));
            }
            records = records + 1;
        }
        putShort((short) records);
        put(ArrayUtils.subarray(payload.array(), 0, payload.position()));
    }

    @Override
    protected void _decode() {
        int count = getShort();
        List<Integer> packets = new ArrayList<>();
        int cnt = 0;
        for(int i = 0; i < count && !feof() && cnt < 4096; i++){
            if(getByte() == 0){
                int start = getLTriad();
                int end = getLTriad();
                if((end - start) > 512){
                    end = start + 512;
                }
                for(int c = start; c <= end; c++){
                    cnt = cnt + 1;
                    packets.add(c);
                }
            } else {
                packets.add(getLTriad());
            }
        }
        this.packets = packets.stream().toArray(Integer[]::new);
    }

    public void clean(){
        packets = new Integer[] {};
        super.clean();
    }
}
