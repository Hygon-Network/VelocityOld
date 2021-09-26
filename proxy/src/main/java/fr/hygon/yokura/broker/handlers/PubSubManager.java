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

import fr.hygon.yokura.broker.BrokerPubSub;
import fr.hygon.yokura.broker.Message;
import fr.hygon.yokura.broker.packets.ChannelPacket;
import fr.hygon.yokura.broker.packets.MessagePacket;

import java.util.ArrayList;

public class PubSubManager {
  private static final ArrayList<BrokerPubSub> pubSubs = new ArrayList<>();

  /**
   * Registers a PubSub runnable to a channel.
   * @param pubSub the runnable
   * @param channel the channel
   */
  public static void registerChannel(BrokerPubSub pubSub, String channel) {
    if (!pubSubs.contains(pubSub)) {
      pubSubs.add(pubSub);
    }

    ChannelPacket channelPacket = new ChannelPacket(ChannelPacket.Actions.REGISTER, channel);
    Broker.sendPacket(channelPacket);
  }

  /**
   * Unregisters a PubSub runnable from a channel.
   * @param pubSub the runnable
   * @param channel the channel
   */
  public static void unregisterChannel(BrokerPubSub pubSub, String channel) {
    if (pubSub.channels.isEmpty()) {
      pubSubs.remove(pubSub);
    }

    boolean isClientStillUsingChannel = false; // Other pubsubs might still use it!
    for (BrokerPubSub brokerPubSubs : pubSubs) {
      if (brokerPubSubs.channels.contains(channel)) {
        isClientStillUsingChannel = true;
        break;
      }
    }

    if (!isClientStillUsingChannel) { // No pubsubs are listening for this channel
      // so we can unregister it
      ChannelPacket channelPacket = new ChannelPacket(ChannelPacket.Actions.UNREGISTER, channel);
      Broker.sendPacket(channelPacket);
    }
  }

  /**
   * Sends a message to the broker.
   * @param message the message to be published
   */
  public static void publish(Message message) {
    MessagePacket messagePacket = new MessagePacket(message);
    Broker.sendPacket(messagePacket);
  }

  /**
   * Handles a message and execute all the concerned PubSub runnables.
   * @param channel the channel the message was received on
   * @param message the message content
   */
  public static void handleMessage(String channel, byte[] message) {
    for (BrokerPubSub brokerPubSub : pubSubs) {
      if (brokerPubSub.channels.contains(channel)) {
        brokerPubSub.onReceive(channel, message);
      }
    }
  }
}
