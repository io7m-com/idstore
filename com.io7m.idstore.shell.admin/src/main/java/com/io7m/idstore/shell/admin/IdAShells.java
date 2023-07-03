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
import com.io7m.idstore.admin_client.api.IdAClientSynchronousType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.shell.admin.internal.IdAShell;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminBanCreate;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminBanDelete;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminBanGet;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminCreate;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminEmailAdd;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminEmailRemove;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminGet;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminGetByEmail;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminMaintenanceModeSet;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchBegin;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchByEmailBegin;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchByEmailNext;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchByEmailPrevious;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchNext;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchPrevious;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminUpdatePasswordExpiration;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAuditSearchBegin;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAuditSearchNext;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdAuditSearchPrevious;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdHelp;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdLogin;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdLogout;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdMailTest;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdSelf;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdSet;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdType;
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
import com.io7m.idstore.shell.admin.internal.IdAShellCmdUserUpdatePasswordExpiration;
import com.io7m.idstore.shell.admin.internal.IdAShellCmdVersion;
import com.io7m.idstore.shell.admin.internal.IdAShellOptions;
import com.io7m.idstore.shell.admin.internal.IdAShellTerminalHolder;
import com.io7m.repetoir.core.RPServiceDirectory;
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
    throws IdException
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
      new IdAShellOptions(terminal);

    final var services = new RPServiceDirectory();
    services.register(IdAClientSynchronousType.class, client);
    services.register(IdAShellOptions.class, options);
    services.register(
      IdAShellTerminalHolder.class,
      new IdAShellTerminalHolder(terminal)
    );

    final List<IdAShellCmdType> commands =
      List.of(
        new IdAShellCmdAdminBanCreate(services),
        new IdAShellCmdAdminBanDelete(services),
        new IdAShellCmdAdminBanGet(services),
        new IdAShellCmdAdminCreate(services),
        new IdAShellCmdAdminEmailAdd(services),
        new IdAShellCmdAdminEmailRemove(services),
        new IdAShellCmdAdminGet(services),
        new IdAShellCmdAdminGetByEmail(services),
        new IdAShellCmdAdminMaintenanceModeSet(services),
        new IdAShellCmdAdminSearchBegin(services),
        new IdAShellCmdAdminSearchByEmailBegin(services),
        new IdAShellCmdAdminSearchByEmailNext(services),
        new IdAShellCmdAdminSearchByEmailPrevious(services),
        new IdAShellCmdAdminSearchNext(services),
        new IdAShellCmdAdminSearchPrevious(services),
        new IdAShellCmdAdminUpdatePasswordExpiration(services),
        new IdAShellCmdAuditSearchBegin(services),
        new IdAShellCmdAuditSearchNext(services),
        new IdAShellCmdAuditSearchPrevious(services),
        new IdAShellCmdHelp(services),
        new IdAShellCmdLogin(services),
        new IdAShellCmdLogout(services),
        new IdAShellCmdMailTest(services),
        new IdAShellCmdSelf(services),
        new IdAShellCmdSet(services),
        new IdAShellCmdUserBanCreate(services),
        new IdAShellCmdUserBanDelete(services),
        new IdAShellCmdUserBanGet(services),
        new IdAShellCmdUserCreate(services),
        new IdAShellCmdUserEmailAdd(services),
        new IdAShellCmdUserEmailRemove(services),
        new IdAShellCmdUserGet(services),
        new IdAShellCmdUserGetByEmail(services),
        new IdAShellCmdUserLoginHistory(services),
        new IdAShellCmdUserSearchBegin(services),
        new IdAShellCmdUserSearchByEmailBegin(services),
        new IdAShellCmdUserSearchByEmailNext(services),
        new IdAShellCmdUserSearchByEmailPrevious(services),
        new IdAShellCmdUserSearchNext(services),
        new IdAShellCmdUserSearchPrevious(services),
        new IdAShellCmdUserUpdatePasswordExpiration(services),
        new IdAShellCmdVersion(services)
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
      services,
      terminal,
      writer,
      commandsNamed,
      reader
    );
  }
}
