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

import com.io7m.idstore.protocol.admin.IdACommandAdminBanDelete;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanDelete;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * "admin-ban-delete"
 */

public final class IdAShellCmdAdminBanDelete
  extends IdAShellCmdAbstractCR<IdACommandAdminBanDelete, IdAResponseAdminBanDelete>
{
  private static final QParameterNamed1<UUID> USER_ID =
    new QParameterNamed1<>(
      "--admin",
      List.of(),
      new QConstant("The admin ID."),
      Optional.empty(),
      UUID.class
    );

  /**
   * Construct a command.
   *
   * @param inServices The service directory
   */

  public IdAShellCmdAdminBanDelete(
    final RPServiceDirectoryType inServices)
  {
    super(
      inServices,
      new QCommandMetadata(
        "admin-ban-delete",
        new QConstant("Unban an admin."),
        Optional.empty()
      ),
      IdACommandAdminBanDelete.class,
      IdAResponseAdminBanDelete.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(USER_ID);
  }

  @Override
  protected IdACommandAdminBanDelete onCreateCommand(
    final QCommandContextType context)
  {
    return new IdACommandAdminBanDelete(
      context.parameterValue(USER_ID)
    );
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseAdminBanDelete response)
  {

  }
}
