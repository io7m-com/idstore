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

import com.io7m.idstore.admin_client.api.IdAClientType;
import com.io7m.idstore.shell.admin.internal.formatting.IdAFormatterType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QParameterType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;

import java.util.Objects;

/**
 * The abstract base class for shell commands.
 */

public abstract class IdAShellCmdAbstract
  implements IdAShellCmdType
{
  private final RPServiceDirectoryType services;
  private final QCommandMetadata metadata;

  protected IdAShellCmdAbstract(
    final RPServiceDirectoryType inServices,
    final QCommandMetadata inMetadata)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.metadata =
      Objects.requireNonNull(inMetadata, "metadata");
  }

  @Override
  public final QCommandMetadata metadata()
  {
    return this.metadata;
  }

  protected final IdAClientType client()
  {
    return this.services().requireService(IdAClientType.class);
  }

  protected final Terminal terminal()
  {
    return this.services()
      .requireService(IdAShellTerminalHolder.class)
      .terminal();
  }

  protected final IdAShellOptions options()
  {
    return this.services().requireService(IdAShellOptions.class);
  }

  protected final RPServiceDirectoryType services()
  {
    return this.services;
  }

  protected final IdAFormatterType formatter()
  {
    return this.options().formatter();
  }

  @Override
  public final String toString()
  {
    return "[%s]".formatted(this.getClass().getSimpleName());
  }

  @Override
  public final Completer completer()
  {
    return new StringsCompleter(
      this.onListNamedParameters()
        .stream()
        .map(QParameterType::name)
        .toList()
    );
  }
}
