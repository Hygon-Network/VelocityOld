/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package fr.hygon.yokura.broker.handlers;

import fr.hygon.yokura.broker.packets.Packet;
import fr.hygon.yokura.broker.packets.Packets;
import fr.hygon.yokura.broker.packets.RegisterClientPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.UUID;

public class Broker {
  public static final UUID CLIENT_UUID = UUID.randomUUID();

  private final Object locker = new Object();
  public boolean isConnected = false;
  public boolean hasFinished = false;

  private static Channel brokerChannel = null;

  /**
   * Connects the proxy to the broker server.
   * @param host the broker host
   * @param port the broker port
   * @return true if the connection was successful, false if it wasn't
   */
  public boolean connect(String host, int port) {
    Thread brokerThread = new Thread(() -> {
      EventLoopGroup group = new NioEventLoopGroup();
      try {
        Bootstrap clientBootstrap = new Bootstrap();

        clientBootstrap.group(group);
        clientBootstrap.channel(NioSocketChannel.class);
        clientBootstrap.remoteAddress(new InetSocketAddress(host, port));
        clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
          protected void initChannel(SocketChannel socketChannel) {
            socketChannel.pipeline().addLast(new ClientHandler());
          }
        });
        ChannelFuture channelFuture = clientBootstrap.connect();
        channelFuture.addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            brokerChannel = future.channel();
            sendPacket(new RegisterClientPacket());
          }

          synchronized (locker) {
            isConnected = future.isSuccess();
            hasFinished = true;
            locker.notify();
          }
        });
        channelFuture.sync();
        channelFuture.channel().closeFuture().sync();
      } catch (InterruptedException exception) {
        exception.printStackTrace();
      } finally {
        try {
          group.shutdownGracefully().sync();
        } catch (InterruptedException exception) {
          exception.printStackTrace();
        }
      }
    });
    brokerThread.setName("Broker Thread");
    brokerThread.start();

    synchronized (locker) {
      while (!hasFinished) {
        try {
          locker.wait();
        } catch (InterruptedException exception) {
          exception.printStackTrace();
        }
      }
    }

    return isConnected;
  }

  /**
   * Sends a packet to the broker.
   * @param packet the packet
   */
  public static void sendPacket(Packet packet) {
    ByteBuf byteBuf = Unpooled.buffer();
    byteBuf.writeInt(Packets.getIdByPacket(packet));

    byteBuf.writeLong(CLIENT_UUID.getMostSignificantBits());
    byteBuf.writeLong(CLIENT_UUID.getLeastSignificantBits());

    packet.write(byteBuf);
    brokerChannel.writeAndFlush(byteBuf);
  }
}
