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
import static io.netty.buffer.Unpooled.buffer;
import java.util.ArrayList;
import java.util.List;
import net.beaconpe.jraklib.Binary;

/**
 * Base class for ACK/NACK.
 */
public abstract class AcknowledgePacket extends Packet
{

    public Integer[] packets;

    @Override
    protected void _encode()
    {
        ByteBuf payload = buffer(1024);
        int count = packets.length;
        int records = 0;
        if (count > 0)
        {
            int pointer = 0;
            int start = packets[0];
            int last = packets[0];
            while (pointer + 1 < count)
            {
                int current = packets[pointer++];
                int diff = current - last;
                if (diff == 1)
                {
                    last = current;
                } else if (diff > 1)
                { //Forget about duplicated packets (bad queues?)
                    if (start == last)
                    {
                        payload.writeByte((byte) 0x01);
                        payload.writeBytes(Binary.writeLTriad(start));
                        start = last = current;
                    } else
                    {
                        payload.writeByte((byte) 0x00);
                        payload.writeBytes(Binary.writeLTriad(start));
                        payload.writeBytes(Binary.writeLTriad(last));
                        start = last = current;
                    }
                    records = records + 1;
                }
            }
            if (start == last)
            {
                payload.writeByte((byte) 0x01);
                payload.writeBytes(Binary.writeLTriad(start));
            } else
            {
                payload.writeByte((byte) 0x00);
                payload.writeBytes(Binary.writeLTriad(start));
                payload.writeBytes(Binary.writeLTriad(last));
            }
            records = records + 1;
        }
        putShort((short) records);
        put(payload.copy());
    }

    @Override
    protected void _decode()
    {
        int count = getShort();
        List<Integer> packets = new ArrayList<>();
        int cnt = 0;
        for (int i = 0; i < count && !feof() && cnt < 4096; i++)
        {
            if (getByte() == 0)
            {
                int start = getLTriad();
                int end = getLTriad();
                if ((end - start) > 512)
                {
                    end = start + 512;
                }
                for (int c = start; c <= end; c++)
                {
                    cnt = cnt + 1;
                    packets.add(c);
                }
            } else
            {
                packets.add(getLTriad());
            }
        }
        this.packets = packets.stream().toArray(Integer[]::new);
    }

    @Override
    public void clean()
    {
        packets = new Integer[]
        {
        };
        super.clean();
    }
}
