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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientHandler extends ChannelInboundHandlerAdapter {
  private ByteBuf packetBuf;

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    packetBuf = ctx.alloc().buffer(4);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    packetBuf.release();
    packetBuf = null;
  }

  @Override
  public void channelRead(ChannelHandlerContext channelHandlerContext, Object object) {
    ByteBuf byteBuf = (ByteBuf) object;
    packetBuf.writeBytes(byteBuf);
    byteBuf.release();

    if (packetBuf.readableBytes() >= 4) {
      int packetSize = packetBuf.readInt();
      if (packetBuf.readableBytes() >= packetSize) {
        int packetId = packetBuf.readInt();
        Packet packet = Packets.getPacketById(packetId);
        if (packet == null) {
          System.err.println("Received unknown packet with id " + packetId);
          return;
        }
        packet.read(channelHandlerContext, packetBuf);
        packetBuf = channelHandlerContext.alloc().buffer(4);
      } else {
        packetBuf.resetReaderIndex();
        packetBuf.resetWriterIndex();
      }
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable cause) {
    cause.printStackTrace();
  }
}