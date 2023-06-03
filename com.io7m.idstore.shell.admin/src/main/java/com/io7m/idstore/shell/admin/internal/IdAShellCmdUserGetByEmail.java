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
import com.io7m.idstore.protocol.admin.IdACommandUserGetByEmail;
import com.io7m.idstore.protocol.admin.IdAResponseUserGet;
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
 * "user-get-by-email"
 */

public final class IdAShellCmdUserGetByEmail
  extends IdAShellCmdAbstract<IdACommandUserGetByEmail, IdAResponseUserGet>
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

  public IdAShellCmdUserGetByEmail(
    final IdAClientSynchronousType inClient)
  {
    super(
      inClient,
      new QCommandMetadata(
        "user-get-by-email",
        new QConstant("Retrieve a user by email address."),
        Optional.empty()
      ),
      IdACommandUserGetByEmail.class,
      IdAResponseUserGet.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(EMAIL);
  }

  @Override
  protected IdACommandUserGetByEmail onCreateCommand(
    final QCommandContextType context)
  {
    return new IdACommandUserGetByEmail(context.parameterValue(EMAIL));
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseUserGet response)
    throws QException
  {
    final var userOpt = response.user();
    if (userOpt.isEmpty()) {
      throw new QException(
        "User does not exist.",
        "user-nonexistent",
        Map.ofEntries(
          Map.entry("Email", context.parameterValue(EMAIL).toString())
        ),
        Optional.empty(),
        List.of()
      );
    }

    IdAShellCmdUserGet.formatUser(userOpt.get(), context.output());
  }
}
