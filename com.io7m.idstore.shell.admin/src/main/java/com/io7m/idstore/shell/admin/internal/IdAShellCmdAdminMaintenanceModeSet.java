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

import com.io7m.idstore.protocol.admin.IdACommandMaintenanceModeSet;
import com.io7m.idstore.protocol.admin.IdAResponseMaintenanceModeSet;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QException;
import com.io7m.quarrel.core.QParameterNamed01;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * "maintenance-mode"
 */

public final class IdAShellCmdAdminMaintenanceModeSet
  extends IdAShellCmdAbstractCR<IdACommandMaintenanceModeSet, IdAResponseMaintenanceModeSet>
{
  private static final QParameterNamed01<String> SET =
    new QParameterNamed01<>(
      "--set",
      List.of(),
      new QConstant("Set maintenance mode with the given message."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed01<String> UNSET =
    new QParameterNamed01<>(
      "--unset",
      List.of(),
      new QConstant("Unset maintenance mode."),
      Optional.empty(),
      String.class
    );

  /**
   * Construct a command.
   *
   * @param inServices The service directory
   */

  public IdAShellCmdAdminMaintenanceModeSet(
    final RPServiceDirectoryType inServices)
  {
    super(
      inServices,
      new QCommandMetadata(
        "maintenance-mode",
        new QConstant("Enable/disable maintenance mode."),
        Optional.empty()
      ),
      IdACommandMaintenanceModeSet.class,
      IdAResponseMaintenanceModeSet.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(
      SET,
      UNSET
    );
  }

  @Override
  protected IdACommandMaintenanceModeSet onCreateCommand(
    final QCommandContextType context)
    throws QException
  {
    final var set = context.parameterValue(SET);
    if (set.isPresent()) {
      return new IdACommandMaintenanceModeSet(set);
    }

    final var unset = context.parameterValue(UNSET);
    if (unset.isPresent()) {
      return new IdACommandMaintenanceModeSet(Optional.empty());
    }

    throw new QException(
      "Must specify one of --set or --unset.",
      "usage",
      Map.of(),
      Optional.empty(),
      List.of()
    );
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseMaintenanceModeSet response)
  {
    context.output()
      .println(response.message());
  }
}
