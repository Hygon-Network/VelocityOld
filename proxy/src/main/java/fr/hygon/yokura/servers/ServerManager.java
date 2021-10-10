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

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import fr.hygon.yokura.broker.BrokerPubSub;
import fr.hygon.yokura.broker.Message;
import fr.hygon.yokura.broker.handlers.PubSubManager;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerManager {
  private static final Logger LOGGER = LogManager.getLogger(ServerProcess.class);
  private static final Map<String, ServerProcess> serverProcesses =
      Collections.synchronizedMap(new HashMap<>());

  private int serverId = 0;
  private final VelocityServer velocityServer;
  private final VelocityConfiguration velocityConfiguration;

  public ServerManager(VelocityServer velocityServer) {
    this.velocityServer = velocityServer;
    this.velocityConfiguration = velocityServer.getConfiguration();
  }

  /**
   * Starts the server manager.
   */
  public void start() {
    startPubSubs();
    verifyAvailableServers();
  }

  /**
   * Shutdown all the servers.
   */
  public void shutdown() {
    System.out.println("Stopping the server manager...");
    for (ServerProcess serverProcess : serverProcesses.values()) {
      if (serverProcess.getServerProcess() != null) {
        serverProcess.getServerProcess().destroy();
      }
    }
  }

  private void startPubSubs() {
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        String newForwardingSecret = generateRandomString();
        velocityConfiguration.setForwardingSecret(newForwardingSecret
            .getBytes(StandardCharsets.UTF_8));

        PubSubManager.publish(new Message("FORWARDING_SECRET_UPDATE",
            newForwardingSecret.getBytes(StandardCharsets.UTF_8)));
      }
    }, 0, 5000);

    BrokerPubSub serverStatusUpdate = new BrokerPubSub() {
      @Override
      public void onReceive(String channel, byte[] message) {
        if (channel.equals("SERVER_STATUS_UPDATE")) {
          ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(message);
          DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

          try {
            String serverId = dataInputStream.readUTF();
            Status status = Status.valueOf(dataInputStream.readUTF());
            serverProcesses.get(serverId).setStatus(status);
          } catch (IOException exception) {
            exception.printStackTrace();
          }

          verifyAvailableServers();
        }
      }
    };
    serverStatusUpdate.registerChannel("SERVER_STATUS_UPDATE");
  }

  /**
   * Verify if the proxy needs to create more servers. If it needs to, it will create them.
   */
  public void verifyAvailableServers() {

    HashMap<ServersType, Integer> availableServers = new HashMap<>();
    for (ServerProcess servers : serverProcesses.values()) {
      if (servers.getStatus() == Status.STARTING || servers.getStatus() == Status.WAITING_PLAYERS) {
        availableServers.merge(servers.getServerType(), 1, Integer::sum);
      }
    }

    for (ServersType serversType : ServersType.values()) {
      while (availableServers.getOrDefault(serversType, 0) < serversType.getMinAvailableServers()) {
        ServerProcess serverProcess =
            new ServerProcess(this, velocityServer, serversType, serverId);
        serverProcess.startServer();
        availableServers.merge(serversType, 1, Integer::sum);
        serverId++;
      }
    }
  }

  public static Logger getLogger() {
    return LOGGER;
  }

  public static Map<String, ServerProcess> getServerProcesses() {
    return serverProcesses;
  }

  private static String generateRandomString() {
    String chars = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz1234567890";
    StringBuilder builder = new StringBuilder();
    Random rnd = new SecureRandom();
    for (int i = 0; i < 32; i++) {
      builder.append(chars.charAt(rnd.nextInt(chars.length())));
    }
    return builder.toString();
  }
}