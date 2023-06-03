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

import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QParameterNamed01;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;
import org.jline.builtins.Completers;
import org.jline.reader.Completer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.quarrel.core.QCommandStatus.SUCCESS;

/**
 * "set"
 */

public final class IdAShellCmdSet implements IdAShellCmdType
{
  private static final QParameterNamed01<Boolean> TERMINATE_ON_ERRORS =
    new QParameterNamed01<>(
      "--terminate-on-errors",
      List.of(),
      new QConstant(
        "Terminate execution on the first command that returns an error."),
      Optional.empty(),
      Boolean.class
    );

  private final QCommandMetadata metadata;
  private final IdAShellOptions options;

  /**
   * Construct a command.
   *
   * @param inOptions The shell options
   */

  public IdAShellCmdSet(
    final IdAShellOptions inOptions)
  {
    this.options =
      Objects.requireNonNull(inOptions, "options");
    this.metadata =
      new QCommandMetadata(
        "set",
        new QConstant("Set shell options."),
        Optional.empty()
      );
  }

  @Override
  public Completer completer()
  {
    return new Completers.OptionCompleter(List.of(), 1);
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(TERMINATE_ON_ERRORS);
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
  {
    context.parameterValue(TERMINATE_ON_ERRORS)
      .ifPresent(x -> {
        this.options.terminateOnErrors().set(x.booleanValue());
      });
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
