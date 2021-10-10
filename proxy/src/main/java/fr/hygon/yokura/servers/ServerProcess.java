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

import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

public class ServerProcess {
  private final VelocityServer velocityServer;
  private final VelocityConfiguration.Yokura yokuraConfig;
  private final ServerManager serverManager;

  private final ServersType serverType;
  private final String serverFullId;

  private Status status = Status.STARTING;
  private ServerInfo serverInfo;
  private Process serverProcess;

  /**
   * Manager for a server.
   * @param serverManager the class that manages the server
   * @param velocityServer the velocity
   * @param serverType the type of server it needs to be
   * @param serverId the ID of the server
   */
  public ServerProcess(ServerManager serverManager, VelocityServer velocityServer,
                       ServersType serverType, int serverId) {
    this.serverManager = serverManager;
    this.velocityServer = velocityServer;
    this.yokuraConfig = velocityServer.getConfiguration().getYokuraConfig();
    this.serverType = serverType;
    this.serverFullId = serverType.getFolder() + serverId;
  }

  /**
   * Starts the server.
   */
  public void startServer() {
    if (serverProcess != null) {
      return;
    }

    ServerManager.getServerProcesses().put(serverFullId, this);

    new Thread(() -> {
      deleteDirectory(new File(yokuraConfig.getServersTempFolder()
          + File.separator + serverFullId));
      try {
        copyFolder(
            Paths.get(yokuraConfig.getServersFolder() + File.separator + serverType.getFolder()),
            Paths.get(yokuraConfig.getServersTempFolder() + File.separator + serverFullId),
            StandardCopyOption.COPY_ATTRIBUTES);
      } catch (IOException exception) {
        ServerManager.getLogger().error("Couldn't copy server files for the server "
            + serverFullId + ".", exception);
        removeServerAndVerify();
        return;
      }

      // We obtain a port that isn't used by any program on the OS.
      int port;
      try {
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();
      } catch (IOException exception) {
        ServerManager.getLogger().error("Couldn't obtain a port.", exception);
        removeServerAndVerify();
        return;
      }

      ProcessBuilder processBuilder = new ProcessBuilder(
          "java",
          "-Xms" + serverType.getMinRam() + "M",
          "-Xmx" + serverType.getMaxRam() + "M",
          "-jar", yokuraConfig.getServersTempFolder() + File.separator
          + serverFullId + File.separator + "yokura.jar",
          "-p", String.valueOf(port),
          "--network-server-name", serverFullId,
          "nogui"
      );
      processBuilder.directory(new File(yokuraConfig.getServersTempFolder()
          + File.separator + serverFullId));

      try {
        serverProcess = processBuilder.start();
      } catch (IOException exception) {
        ServerManager.getLogger().error("Couldn't start the server "
            + serverFullId + ".", exception);
        removeServerAndVerify();
        return;
      }

      ServerManager.getLogger().info("Created a " + serverType
          + " server with ID " + serverFullId + ".");

      serverInfo = new ServerInfo(serverFullId, new InetSocketAddress("localhost", port));
      velocityServer.registerServer(serverInfo);

      if (serverType == ServersType.HUB) {
        velocityServer.getConfiguration().getAttemptConnectionOrder().add(serverFullId);
      }

      try {
        int exitCode = serverProcess.waitFor();
        if (exitCode != 130 && exitCode != 143) { // process either returns 130 or
          // 143 when velocity is shutting down
          removeServerAndVerify();
        }
      } catch (InterruptedException ignored) {
        // Ignored
      }
    }).start();
  }

  public Status getStatus() {
    return status;
  }

  public ServersType getServerType() {
    return serverType;
  }

  protected Process getServerProcess() {
    return serverProcess;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  private void removeServerAndVerify() {
    velocityServer.getConfiguration().getAttemptConnectionOrder().remove(serverFullId);

    if (!deleteDirectory(new File(yokuraConfig.getServersTempFolder()
        + File.separator + serverFullId))) {
      ServerManager.getLogger().warn("Could not delete the " + serverFullId
          + " server directory...");
    }

    ServerManager.getServerProcesses().remove(serverFullId);
    serverManager.verifyAvailableServers();
  }

  /**
   * Copy a folder recursively.
   *
   * @param source  the source folder
   * @param target  the destination folder
   * @param options copy options
   * @throws IOException if the program cannot write
   */
  private void copyFolder(Path source, Path target, CopyOption... options) throws IOException {
    Files.walkFileTree(source, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        Files.createDirectories(target.resolve(source.relativize(dir)));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
          throws IOException {
        Files.copy(file, target.resolve(source.relativize(file)), options);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private boolean deleteDirectory(File directoryToBeDeleted) {
    File[] allContents = directoryToBeDeleted.listFiles();
    if (allContents != null) {
      for (File file : allContents) {
        deleteDirectory(file);
      }
    }
    return directoryToBeDeleted.delete();
  }
}
