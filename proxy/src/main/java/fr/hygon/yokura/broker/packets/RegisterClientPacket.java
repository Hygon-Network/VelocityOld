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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class RegisterClientPacket implements Packet {
  @Override
  public void read(ChannelHandlerContext ctx, ByteBuf in) { // The client won't receive this packet

  }

  @Override
  public void write(ByteBuf out) {
    out.writeInt(Packets.REGISTER_CLIENT_PACKET.getPacketId());
    // The UUID has already been wrote before, so we don't need to rewrite it.
  }
}
