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

import com.io7m.idstore.admin_client.api.IdAClientCredentials;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.admin_client.api.IdAClientSynchronousType;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QParameterPositional;
import com.io7m.quarrel.core.QParameterType;
import com.io7m.quarrel.core.QParametersPositionalType;
import com.io7m.quarrel.core.QParametersPositionalTyped;
import com.io7m.quarrel.core.QStringType.QConstant;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.StringsCompleter;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.quarrel.core.QCommandStatus.SUCCESS;

/**
 * "login"
 */

public final class IdAShellCmdLogin implements IdAShellCmdType
{
  private static final QParameterPositional<String> SERVER =
    new QParameterPositional<>(
      "server",
      new QConstant("The server address."),
      String.class
    );

  private static final QParameterPositional<String> USERNAME =
    new QParameterPositional<>(
      "username",
      new QConstant("The username."),
      String.class
    );

  private static final QParameterPositional<String> PASSWORD =
    new QParameterPositional<>(
      "password",
      new QConstant("The password."),
      String.class
    );

  private final IdAClientSynchronousType client;
  private final QCommandMetadata metadata;

  /**
   * Construct a command.
   *
   * @param inClient The client
   */

  public IdAShellCmdLogin(
    final IdAClientSynchronousType inClient)
  {
    this.client =
      Objects.requireNonNull(inClient, "client");

    this.metadata =
      new QCommandMetadata(
        "login",
        new QConstant("Log in."),
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
  public QParametersPositionalType onListPositionalParameters()
  {
    return new QParametersPositionalTyped(List.of(
      SERVER,
      USERNAME,
      PASSWORD
    ));
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    final var server =
      context.parameterValue(SERVER);
    final var userName =
      context.parameterValue(USERNAME);
    final var password =
      context.parameterValue(PASSWORD);

    final var credentials =
      new IdAClientCredentials(
        userName,
        password,
        new URI(server),
        Map.of()
      );

    this.client.loginOrElseThrow(credentials, IdAClientException::ofError);
    return SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
