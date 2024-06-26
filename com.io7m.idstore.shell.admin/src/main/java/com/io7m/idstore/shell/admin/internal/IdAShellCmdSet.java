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

import com.io7m.idstore.shell.admin.internal.formatting.IdAFormatterPretty;
import com.io7m.idstore.shell.admin.internal.formatting.IdAFormatterRaw;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QParameterNamed01;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.io7m.quarrel.core.QCommandStatus.SUCCESS;

/**
 * "set"
 */

public final class IdAShellCmdSet extends IdAShellCmdAbstract
{
  enum Formatter
  {
    RAW,
    PRETTY
  }

  private static final QParameterNamed01<Boolean> TERMINATE_ON_ERRORS =
    new QParameterNamed01<>(
      "--terminate-on-errors",
      List.of(),
      new QConstant(
        "Terminate execution on the first command that returns an error."),
      Optional.empty(),
      Boolean.class
    );

  private static final QParameterNamed01<Formatter> FORMATTER =
    new QParameterNamed01<>(
      "--formatter",
      List.of(),
      new QConstant(
        "Set the shell formatter."),
      Optional.empty(),
      Formatter.class
    );

  private static final QParameterNamed01<Duration> LOGIN_TIMEOUT =
    new QParameterNamed01<>(
      "--login-timeout",
      List.of(),
      new QConstant(
        "Set the login timeout value."),
      Optional.empty(),
      Duration.class
    );

  private static final QParameterNamed01<Duration> COMMAND_TIMEOUT =
    new QParameterNamed01<>(
      "--command-timeout",
      List.of(),
      new QConstant(
        "Set the command timeout value."),
      Optional.empty(),
      Duration.class
    );

  /**
   * Construct a command.
   *
   * @param inServices The service directory
   */

  public IdAShellCmdSet(
    final RPServiceDirectoryType inServices)
  {
    super(
      inServices,
      new QCommandMetadata(
        "set",
        new QConstant("Set shell options."),
        Optional.empty()
      )
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(
      TERMINATE_ON_ERRORS,
      FORMATTER,
      LOGIN_TIMEOUT,
      COMMAND_TIMEOUT
    );
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
  {
    context.parameterValue(FORMATTER)
      .ifPresent(r -> {
        switch (r) {
          case RAW -> {
            this.options()
              .setFormatter(new IdAFormatterRaw(this.terminal()));
          }
          case PRETTY -> {
            this.options()
              .setFormatter(new IdAFormatterPretty(this.terminal()));
          }
        }
      });

    context.parameterValue(LOGIN_TIMEOUT)
        .ifPresent(x -> this.options().setLoginTimeout(x));

    context.parameterValue(COMMAND_TIMEOUT)
      .ifPresent(x -> this.options().setCommandTimeout(x));

    context.parameterValue(TERMINATE_ON_ERRORS)
      .ifPresent(x -> {
        this.options().terminateOnErrors().set(x.booleanValue());
      });
    return SUCCESS;
  }
}
