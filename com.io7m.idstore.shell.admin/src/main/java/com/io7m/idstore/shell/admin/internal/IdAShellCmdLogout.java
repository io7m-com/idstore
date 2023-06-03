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
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QParameterType;
import com.io7m.quarrel.core.QParametersPositionalNone;
import com.io7m.quarrel.core.QParametersPositionalType;
import com.io7m.quarrel.core.QStringType;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.StringsCompleter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.quarrel.core.QCommandStatus.SUCCESS;

/**
 * "logout"
 */

public final class IdAShellCmdLogout implements IdAShellCmdType
{
  private final IdAClientSynchronousType client;
  private final QCommandMetadata metadata;

  /**
   * Construct a command.
   *
   * @param inClient The client
   */

  public IdAShellCmdLogout(
    final IdAClientSynchronousType inClient)
  {
    this.client =
      Objects.requireNonNull(inClient, "client");

    this.metadata =
      new QCommandMetadata(
        "logout",
        new QStringType.QConstant("Log out."),
        Optional.empty()
      );
  }

  @Override
  public Completer completer()
  {
    return new StringsCompleter(
      this.onListNamedParameters()
        .stream()
        .map(QParameterType::name)
        .toList()
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of();
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    this.client.disconnect();
    return SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }

  @Override
  public String toString()
  {
    return "[%s]".formatted(this.getClass().getSimpleName());
  }
}
