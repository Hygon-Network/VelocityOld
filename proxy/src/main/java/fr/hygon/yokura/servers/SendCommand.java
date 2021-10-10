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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class SendCommand implements SimpleCommand {
  @Override
  public void execute(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();
    final String[] args = invocation.arguments();

    if (args.length == 0) {
      source.sendMessage(Component.text("Please specify a server.")
          .color(TextColor.color(255, 0, 0)));
    } else if (args.length == 1) {
      String serverName = args[0];
      if (!ServerManager.getServerProcesses().containsKey(serverName)) {
        source.sendMessage(Component.text("The server " + serverName + " does not exists.")
            .color(TextColor.color(255, 0, 0)));
        return;
      }
      
      source.sendMessage(Component.text("Please specify a command."));
    } else {
      String serverName = args[0];
      if (!ServerManager.getServerProcesses().containsKey(serverName)) {
        source.sendMessage(Component.text("The server " + serverName + " does not exist.")
            .color(TextColor.color(255, 0, 0)));
        return;
      }

      StringBuilder stringBuffer = new StringBuilder();
      for (int i = 1; i < args.length; i++) {
        stringBuffer.append(args[i]);
        stringBuffer.append(" ");
      }
      stringBuffer.append("\n");
      OutputStream serverInput = ServerManager.getServerProcesses().get(serverName)
          .getServerProcess().getOutputStream();
      try {
        serverInput.write(stringBuffer.toString().getBytes(StandardCharsets.UTF_8));
        serverInput.flush();
      } catch (IOException exception) {
        source.sendMessage(Component.text("Couldn't send the command.")
            .color(TextColor.color(255, 0, 0)));
        return;
      }

      source.sendMessage(Component.text("Commend sent.").color(TextColor.color(0, 255, 0)));
    }
  }
  
  @Override
  public List<String> suggest(final SimpleCommand.Invocation invocation) {
    final String[] currentArgs = invocation.arguments();

    if (currentArgs.length == 0) {
      return new ArrayList<>(ServerManager.getServerProcesses().keySet());
    }

    return new ArrayList<>();
  }

  @Override
  public boolean hasPermission(final SimpleCommand.Invocation invocation) {
    return true;
  }
}
