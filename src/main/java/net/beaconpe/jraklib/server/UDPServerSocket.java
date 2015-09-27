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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.beaconpe.jraklib.Logger;
import java.io.Closeable;
import java.io.IOException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.PlatformDependent;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ThreadFactory;
import net.beaconpe.jraklib.protocol.Packet;

/**
 * A UDP Server socket.
 */
public class UDPServerSocket implements Closeable
{

    protected final Logger logger;
    protected Bootstrap bootstrap;
    public static final Base BASE = new Base();
    public static SessionManager sm;
    public EventLoopGroup eventLoops;
    private static boolean epoll;
    private final Collection<Channel> listeners = new HashSet<>();
    public static final ChannelInitializer<Channel> SERVER_CHILD = new ChannelInitializer<Channel>()
    {
        @Override
        protected void initChannel(Channel ch) throws Exception
        {
            BASE.initChannel(ch);
        }
    };

    public UDPServerSocket(JRakLibServer server, final InetSocketAddress socketAddress, final int port, Logger mainLogger)
    {
        this.logger = mainLogger;
        sm = new SessionManager(server, this);
        System.setProperty("java.net.preferIPv4Stack", "true"); // Minecraft does not support IPv6
        System.setProperty("io.netty.selectorAutoRebuildThreshold", "0"); // Seems to cause to stop accepting connections
        if (System.getProperty("io.netty.leakDetectionLevel") == null)
        {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED); // Eats performance
        }
        if (!PlatformDependent.isWindows())
        {
            if (epoll = Epoll.isAvailable())
            {
                logger.notice("Epoll is working, utilising it!");
            } else
            {
                logger.notice("Epoll is not working, falling back to NIO: " + getServerChannel().getName());
            }
        }
        eventLoops = newEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty IO Thread #%1$d").build());
        ChannelFutureListener listener = new ChannelFutureListener()
        {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception
            {
                if (future.isSuccess())
                {
                    listeners.add(future.channel());
                    logger.notice("JRakLib Listening on " + socketAddress);
                } else
                {
                    logger.critical("**** FAILED TO BIND TO " + socketAddress + ":" + port + "!");
                    logger.critical("Perhaps a server is already running on that port?");
                    future.cause().printStackTrace(); // TODO: Logs more versatible.
                    //System.exit(1);
                }
            }
        };
        bootstrap = new Bootstrap();
        try {
            bootstrap
                    .channel(getServerChannel())
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(SERVER_CHILD)
                    .group(eventLoops)
                    .localAddress(socketAddress)
                    .bind()
                    .addListener(listener)
            .sync().channel().closeFuture().await(); // its necessary ?
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void writePacket(Packet packet, ChannelHandlerContext ctx, InetSocketAddress dest) throws IOException
    {
        //System.out.println("SEND Packet (" + Hex.getHexString(packet.getID()) + "): " + packet.getID() + " - " + packet.getClass().getSimpleName() + " -> " + Hex.bytebufToHexString(packet.buffer.copy()));
        DatagramPacket dp = new DatagramPacket(packet.buffer, dest);
        ctx.write(dp);
        ctx.flush();
    }

    public void writeRaw(ByteBuf buffer, ChannelHandlerContext ctx, InetSocketAddress dest) throws IOException
    {
        //System.out.println("SEND Raw -> " + Hex.bytebufToHexString(buffer.copy()));
        DatagramPacket dp = new DatagramPacket(buffer, dest);
        ctx.write(dp);
        ctx.flush();
    }

    public final static class Base extends ChannelInitializer<Channel>
    {

        @Override
        public void initChannel(Channel ch) throws Exception
        {
            try
            {
                ch.config().setOption(ChannelOption.IP_TOS, 0x18);
            } catch (ChannelException ex)
            {
                // IP_TOS is not supported (Windows XP / Windows Server 2003)
            }
            ch.config().setAllocator(PooledByteBufAllocator.DEFAULT); // TODO: stress test with and without.
            //ch.pipeline().addLast("timeout", new ReadTimeoutHandler(1, TimeUnit.MINUTES)); // TODO: config
            ch.pipeline().addLast("inbound-boss", new UDPServerHandler());
            ch.pipeline().get(UDPServerHandler.class).setSessionManager(sm);
        }
    };

    public static Class<? extends DatagramChannel> getServerChannel()
    {
        return epoll ? EpollDatagramChannel.class : NioDatagramChannel.class;
    }

    public static EventLoopGroup newEventLoopGroup(int threads, ThreadFactory factory)
    {
        return epoll ? new EpollEventLoopGroup(threads, factory) : new NioEventLoopGroup(threads, factory);
    }

    public Bootstrap getBootstrap()
    {
        return bootstrap;
    }

    @Override
    public void close()
    {
        eventLoops.shutdownGracefully();
    }
}
