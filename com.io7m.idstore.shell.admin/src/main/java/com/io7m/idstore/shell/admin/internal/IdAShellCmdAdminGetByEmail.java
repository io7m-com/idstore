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
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.protocol.admin.IdACommandAdminGetByEmail;
import com.io7m.idstore.protocol.admin.IdAResponseAdminGet;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QException;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QParametersPositionalNone;
import com.io7m.quarrel.core.QParametersPositionalType;
import com.io7m.quarrel.core.QStringType.QConstant;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * "admin-get-by-email"
 */

public final class IdAShellCmdAdminGetByEmail
  extends IdAShellCmdAbstract<IdACommandAdminGetByEmail, IdAResponseAdminGet>
{
  private static final QParameterNamed1<IdEmail> EMAIL =
    new QParameterNamed1<>(
      "--email",
      List.of(),
      new QConstant("The email address."),
      Optional.empty(),
      IdEmail.class
    );

  /**
   * Construct a command.
   *
   * @param inClient The client
   */

  public IdAShellCmdAdminGetByEmail(
    final IdAClientSynchronousType inClient)
  {
    super(
      inClient,
      new QCommandMetadata(
        "admin-get-by-email",
        new QConstant("Retrieve an admin by email address."),
        Optional.empty()
      ),
      IdACommandAdminGetByEmail.class,
      IdAResponseAdminGet.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(EMAIL);
  }

  @Override
  public QParametersPositionalType onListPositionalParameters()
  {
    return new QParametersPositionalNone();
  }

  @Override
  protected IdACommandAdminGetByEmail onCreateCommand(
    final QCommandContextType context)
  {
    return new IdACommandAdminGetByEmail(context.parameterValue(EMAIL));
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseAdminGet response)
    throws QException
  {
    final var adminOpt = response.admin();
    if (adminOpt.isEmpty()) {
      throw new QException(
        "Admin does not exist.",
        "admin-nonexistent",
        Map.ofEntries(
          Map.entry("Email", context.parameterValue(EMAIL).toString())
        ),
        Optional.empty(),
        List.of()
      );
    }

    IdAShellCmdAdminGet.formatAdmin(adminOpt.get(), context.output());
  }
}
