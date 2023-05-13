/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.idstore.shell.admin;

import com.io7m.idstore.admin_client.IdAClients;
import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.shell.admin.internal.IdAShell;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminBanCreate;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminBanDelete;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminBanGet;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminCreate;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminEmailAdd;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminEmailRemove;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminGet;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminGetByEmail;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchBegin;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchByEmailBegin;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchByEmailNext;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchByEmailPrevious;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchNext;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchPrevious;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAuditSearchBegin;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAuditSearchNext;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAuditSearchPrevious;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdHelp;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdLogin;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdLogout;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdSelf;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdSet;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserBanCreate;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserBanDelete;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserBanGet;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserCreate;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserEmailAdd;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserEmailRemove;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserGet;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserGetByEmail;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserLoginHistory;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserSearchBegin;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserSearchByEmailBegin;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserSearchByEmailNext;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserSearchByEmailPrevious;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserSearchNext;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserSearchPrevious;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdVersion;
import com.io7m.idstore.shell.admin.internal.IdAShellOptions;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The basic shell.
 */

public final class IdAShells implements IdAShellFactoryType
{
  /**
   * The basic shell.
   */

  public IdAShells()
  {

  }

  @Override
  public IdAShellType create(
    final IdAShellConfiguration configuration)
    throws IOException
  {
    final var client =
      new IdAClients()
        .openSynchronousClient(
          new IdAClientConfiguration(configuration.locale())
        );
    final var terminal =
      configuration.terminal()
        .orElseGet(() -> {
          try {
            return TerminalBuilder.terminal();
          } catch (final IOException e) {
            throw new UncheckedIOException(e);
          }
        });
    final var writer =
      terminal.writer();

    final var options =
      new IdAShellOptions(
        new AtomicBoolean(false)
      );

    final var commands =
      List.of(
        new IdAShellCmdAdminBanCreate(client),
        new IdAShellCmdAdminBanDelete(client),
        new IdAShellCmdAdminBanGet(client),
        new IdAShellCmdAdminCreate(client),
        new IdAShellCmdAdminEmailAdd(client),
        new IdAShellCmdAdminEmailRemove(client),
        new IdAShellCmdAdminGet(client),
        new IdAShellCmdAdminGetByEmail(client),
        new IdAShellCmdAdminSearchBegin(client),
        new IdAShellCmdAdminSearchByEmailBegin(client),
        new IdAShellCmdAdminSearchByEmailNext(client),
        new IdAShellCmdAdminSearchByEmailPrevious(client),
        new IdAShellCmdAdminSearchNext(client),
        new IdAShellCmdAdminSearchPrevious(client),
        new IdAShellCmdAuditSearchBegin(client),
        new IdAShellCmdAuditSearchNext(client),
        new IdAShellCmdAuditSearchPrevious(client),
        new IdAShellCmdHelp(),
        new IdAShellCmdLogin(client),
        new IdAShellCmdLogout(client),
        new IdAShellCmdSelf(client),
        new IdAShellCmdSet(options),
        new IdAShellCmdUserBanCreate(client),
        new IdAShellCmdUserBanDelete(client),
        new IdAShellCmdUserBanGet(client),
        new IdAShellCmdUserCreate(client),
        new IdAShellCmdUserEmailAdd(client),
        new IdAShellCmdUserEmailRemove(client),
        new IdAShellCmdUserGet(client),
        new IdAShellCmdUserGetByEmail(client),
        new IdAShellCmdUserLoginHistory(client),
        new IdAShellCmdUserSearchBegin(client),
        new IdAShellCmdUserSearchByEmailBegin(client),
        new IdAShellCmdUserSearchByEmailNext(client),
        new IdAShellCmdUserSearchByEmailPrevious(client),
        new IdAShellCmdUserSearchNext(client),
        new IdAShellCmdUserSearchPrevious(client),
        new IdAShellCmdVersion()
      );

    final var commandsNamed =
      commands.stream()
        .collect(Collectors.toMap(
          e -> e.metadata().name(),
          Function.identity())
        );

    final var history =
      new DefaultHistory();
    final var parser =
      new DefaultParser();

    final var completer =
      new AggregateCompleter(
        commands.stream()
          .map(c -> {
            return new ArgumentCompleter(
              new StringsCompleter(c.metadata().name()),
              c.completer()
            );
          })
          .collect(Collectors.toList())
      );

    final var reader =
      LineReaderBuilder.builder()
        .appName("idstore")
        .terminal(terminal)
        .completer(completer)
        .parser(parser)
        .history(history)
        .build();

    return new IdAShell(
      client,
      options,
      terminal,
      writer,
      commandsNamed,
      reader
    );
  }
}
