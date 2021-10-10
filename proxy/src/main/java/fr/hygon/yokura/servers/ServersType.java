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

package fr.hygon.yokura.servers;

public enum ServersType {
  HUB("hub", 2, 512, 512);

  private final String folder;
  private final int maxAvailableServers;
  private final int minRam; //In megabytes
  private final int maxRam; //In megabytes

  ServersType(String folder, int maxAvailableServers, int minRam, int maxRam) {
    this.folder = folder;
    this.maxAvailableServers = maxAvailableServers;
    this.minRam = minRam;
    this.maxRam = maxRam;
  }

  public String getFolder() {
    return folder;
  }

  public int getMinAvailableServers() {
    return maxAvailableServers;
  }

  public int getMinRam() {
    return minRam;
  }

  public int getMaxRam() {
    return maxRam;
  }
}
