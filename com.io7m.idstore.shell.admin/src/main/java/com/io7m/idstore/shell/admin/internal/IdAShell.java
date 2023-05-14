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

package com.io7m.idstore.shell.admin.internal;

import com.io7m.idstore.admin_client.api.IdAClientSynchronousType;
import com.io7m.idstore.shell.admin.IdAShellType;
import com.io7m.idstore.shell.admin.IdAShellValueConverters;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandOrGroupType;
import com.io7m.quarrel.core.QCommandParserConfiguration;
import com.io7m.quarrel.core.QCommandParsers;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QErrorFormatting;
import com.io7m.quarrel.core.QException;
import com.io7m.quarrel.core.QLocalization;
import com.io7m.quarrel.core.QLocalizationType;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.MaskingCallback;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.io7m.quarrel.core.QCommandStatus.FAILURE;
import static com.io7m.quarrel.core.QCommandStatus.SUCCESS;

/**
 * The basic shell.
 */

public final class IdAShell implements IdAShellType
{
  private final CloseableCollectionType<ClosingResourceFailedException> resources;
  private final IdAClientSynchronousType client;
  private final LineReader reader;
  private final PrintWriter writer;
  private final QCommandParserConfiguration parserConfiguration;
  private final QCommandParsers parsers;
  private final QLocalizationType localizer;
  private final SortedMap<String, IdAShellCmdType> commandsNamed;
  private final SortedMap<String, QCommandOrGroupType> commandsView;
  private final IdAShellOptions options;
  private final Terminal terminal;
  private volatile QCommandStatus status;

  /**
   * The basic shell.
   *
   * @param inClient        The client
   * @param inOptions       The shell options
   * @param inCommandsNamed The named commands
   * @param inReader        The line reader
   * @param inTerminal      The terminal
   * @param inWriter        The writer
   */

  public IdAShell(
    final IdAClientSynchronousType inClient,
    final IdAShellOptions inOptions,
    final Terminal inTerminal,
    final PrintWriter inWriter,
    final Map<String, IdAShellCmdType> inCommandsNamed,
    final LineReader inReader)
  {
    this.client =
      Objects.requireNonNull(inClient, "client");
    this.options =
      Objects.requireNonNull(inOptions, "options");
    this.terminal =
      Objects.requireNonNull(inTerminal, "terminal");
    this.writer =
      Objects.requireNonNull(inWriter, "writer");
    this.commandsNamed =
      new TreeMap<>(
        Objects.requireNonNull(inCommandsNamed, "commandsNamed")
      );
    this.commandsView =
      new TreeMap<>(this.commandsNamed);
    this.reader =
      Objects.requireNonNull(inReader, "reader");
    this.resources =
      CloseableCollection.create();
    this.parsers =
      new QCommandParsers();
    this.parserConfiguration =
      new QCommandParserConfiguration(
        IdAShellValueConverters.get(),
        QCommandParsers.emptyResources()
      );
    this.localizer =
      QLocalization.create(Locale.getDefault());
    this.status =
      SUCCESS;

    this.resources.add(this.client);
    this.resources.add(this.terminal);
    this.resources.add(this.writer);
    this.resources.add(this.resources);
  }

  @Override
  public void close()
    throws Exception
  {
    this.resources.close();
  }

  @Override
  public Collection<QCommandType> commands()
  {
    return this.commandsNamed.values()
      .stream()
      .map((IdAShellCmdType x) -> (QCommandType) x)
      .toList();
  }

  @Override
  public void run()
  {
    while (true) {
      try {
        this.runForOneLine();
      } catch (final EndOfFileException e) {
        break;
      } catch (final ShellCommandFailed e) {
        this.status = FAILURE;
        if (this.options.terminateOnErrors().get()) {
          break;
        }
      }
    }
  }

  @Override
  public int exitCode()
  {
    return this.status.exitCode();
  }

  private static final class ShellCommandFailed
    extends Exception
  {
    ShellCommandFailed()
    {

    }
  }

  private void runForOneLine()
    throws EndOfFileException, ShellCommandFailed
  {
    String line = null;

    try {
      line = this.reader.readLine(
        "[idstore]# ",
        null,
        (MaskingCallback) null,
        null);
    } catch (final UserInterruptException e) {
      // Ignore
    }

    if (line == null) {
      this.status = SUCCESS;
      return;
    }

    line = line.trim();
    if (line.isEmpty()) {
      this.status = SUCCESS;
      return;
    }

    final var parsed =
      this.reader.getParser()
        .parse(line, 0);

    final var commandName = parsed.word();
    if (!this.commandsNamed.containsKey(commandName)) {
      this.writer.append("Unrecognized command: ");
      this.writer.append(commandName);
      this.writer.println();
      this.writer.flush();
      throw new ShellCommandFailed();
    }

    final var command =
      this.commandsNamed.get(commandName);

    final var arguments =
      parsed.words()
        .stream()
        .skip(1L)
        .toList();

    final var parser =
      this.parsers.create(this.parserConfiguration);

    final QCommandContextType context;
    try {
      context = parser.execute(
        this.commandsView,
        this.writer,
        command,
        arguments
      );
    } catch (final QException ex) {
      this.formatQException(ex);
      throw new ShellCommandFailed();
    }

    try {
      this.status = command.onExecute(context);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (final Exception e) {
      if (e instanceof QException q) {
        this.formatQException(q);
      }

      e.printStackTrace(this.writer);
      this.writer.println();
      this.writer.flush();
      throw new ShellCommandFailed();
    }
  }

  private void formatQException(
    final QException ex)
  {
    QErrorFormatting.format(
      this.localizer, ex, s -> {
        this.writer.append(s);
        this.writer.println();
        this.writer.flush();
      }
    );
    ex.extraErrors().forEach(e -> {
      QErrorFormatting.format(
        this.localizer, e, s -> {
          this.writer.append(s);
          this.writer.println();
          this.writer.flush();
        }
      );
    });
  }
}
