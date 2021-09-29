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

package fr.hygon.yokura.broker.packets;

import fr.hygon.yokura.broker.Message;
import fr.hygon.yokura.broker.handlers.PubSubManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;

public class MessagePacket implements Packet {
  private final Message message;

  public MessagePacket() {
    this.message = null;
  }

  public MessagePacket(Message message) {
    this.message = message;
  }

  @Override
  public void read(ChannelHandlerContext ctx, ByteBuf in) {
    int channelLength = in.readInt();
    String channel = (String) in.readCharSequence(channelLength, StandardCharsets.UTF_8);

    byte[] message = new byte[in.readableBytes()];
    in.readBytes(message);

    PubSubManager.handleMessage(channel, message);
  }

  @Override
  public void write(ByteBuf out) {
    out.writeInt(message.getChannel().length());
    out.writeCharSequence(message.getChannel(), StandardCharsets.UTF_8);

    out.writeBytes(Unpooled.wrappedBuffer(message.getMessage()));
  }
}
