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
package net.beaconpe.jraklib.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

/**
 *
 * @author beaconpe
 */
public class UDPServerHandler extends SimpleChannelInboundHandler<DatagramPacket>
{

    private SessionManager sm;

    public void setSessionManager(SessionManager sm)
    {
        this.sm = sm;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception
    {
        if (sm != null)
        {
            //System.out.println(" -> " + JRakLib.bytebufToHexString(packet.content().copy()));
            sm.tickProcessor();
            sm.receiveStream();
            sm.receivePacket(ctx, packet);
            sm.receiveStream();
            sm.tickProcessor();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
    {
        try
        {
            sm.tickProcessor();
            sm.receiveStream();
            sm.tickProcessor();
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        cause.printStackTrace();
        // We don't close the channel because we can keep serving requests.
    }
}
