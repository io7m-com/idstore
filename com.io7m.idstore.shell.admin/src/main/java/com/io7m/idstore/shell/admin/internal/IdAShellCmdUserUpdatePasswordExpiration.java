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
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdatePasswordExpiration;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdatePasswordExpiration;
import com.io7m.idstore.protocol.admin.IdAPasswordExpirationSetType;
import com.io7m.idstore.protocol.admin.IdAResponseAdminUpdate;
import com.io7m.idstore.protocol.admin.IdAResponseUserUpdate;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QException;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * "user-update-password-expiration"
 */

public final class IdAShellCmdUserUpdatePasswordExpiration
  extends IdAShellCmdAbstract<IdACommandUserUpdatePasswordExpiration, IdAResponseUserUpdate>
{
  private static final QParameterNamed1<UUID> USER_ID =
    new QParameterNamed1<>(
      "--user",
      List.of(),
      new QConstant("The user ID."),
      Optional.empty(),
      UUID.class
    );

  private static final QParameterNamed1<IdAPasswordExpirationSetType> EXPIRES =
    new QParameterNamed1<>(
      "--expires",
      List.of(),
      new QConstant("The password expiration."),
      Optional.empty(),
      IdAPasswordExpirationSetType.class
    );

  /**
   * Construct a command.
   *
   * @param inClient The client
   */

  public IdAShellCmdUserUpdatePasswordExpiration(
    final IdAClientSynchronousType inClient)
  {
    super(
      inClient,
      new QCommandMetadata(
        "user-update-password-expiration",
        new QConstant("Update an user's password expiration."),
        Optional.empty()
      ),
      IdACommandUserUpdatePasswordExpiration.class,
      IdAResponseUserUpdate.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(USER_ID, EXPIRES);
  }

  @Override
  protected IdACommandUserUpdatePasswordExpiration onCreateCommand(
    final QCommandContextType context)
    throws QException
  {
    return new IdACommandUserUpdatePasswordExpiration(
      context.parameterValue(USER_ID),
      context.parameterValue(EXPIRES)
    );
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseUserUpdate response)
    throws QException
  {
    final var output =
      context.output();

    final var expires =
      response.user()
        .password()
        .expires();

    if (expires.isPresent()) {
      final var time = expires.get();
      output.printf("The password will expire at %s.%n", time);
    } else {
      output.println("The password will not expire.");
    }
    output.flush();
  }
}
