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

package fr.hygon.yokura.broker;

import fr.hygon.yokura.broker.handlers.PubSubManager;

import java.util.ArrayList;

public abstract class BrokerPubSub {
  public ArrayList<String> channels = new ArrayList<>();

  public void onReceive(String channel, byte[] message) {
  }

  public void registerChannel(String channel) {
    channels.add(channel);
    PubSubManager.registerChannel(this, channel);
  }

  public void unregisterChannel(String channel) {
    channels.remove(channel);
    PubSubManager.unregisterChannel(this, channel);
  }
}
