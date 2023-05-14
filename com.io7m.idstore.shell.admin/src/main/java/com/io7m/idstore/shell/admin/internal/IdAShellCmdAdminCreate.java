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
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.protocol.admin.IdACommandAdminCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminCreate;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QException;
import com.io7m.quarrel.core.QParameterNamed01;
import com.io7m.quarrel.core.QParameterNamed0N;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QParametersPositionalNone;
import com.io7m.quarrel.core.QParametersPositionalType;
import com.io7m.quarrel.core.QStringType.QConstant;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * "admin-create"
 */

public final class IdAShellCmdAdminCreate
  extends IdAShellCmdAbstract<IdACommandAdminCreate, IdAResponseAdminCreate>
{
  private static final QParameterNamed01<UUID> USER_ID =
    new QParameterNamed01<>(
      "--id",
      List.of(),
      new QConstant("The user ID."),
      Optional.empty(),
      UUID.class
    );

  private static final QParameterNamed1<String> NAME =
    new QParameterNamed1<>(
      "--name",
      List.of(),
      new QConstant("The user name."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> REAL_NAME =
    new QParameterNamed1<>(
      "--real-name",
      List.of(),
      new QConstant("The user's real name."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<IdEmail> EMAIL =
    new QParameterNamed1<>(
      "--email",
      List.of(),
      new QConstant("The email address."),
      Optional.empty(),
      IdEmail.class
    );

  private static final QParameterNamed1<String> PASSWORD =
    new QParameterNamed1<>(
      "--password",
      List.of(),
      new QConstant("The password."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed0N<IdAdminPermission> PERMISSIONS =
    new QParameterNamed0N<>(
      "--permission",
      List.of(),
      new QConstant("A permission to grant the admin."),
      List.of(),
      IdAdminPermission.class
    );

  /**
   * Construct a command.
   *
   * @param inClient The client
   */

  public IdAShellCmdAdminCreate(
    final IdAClientSynchronousType inClient)
  {
    super(
      inClient,
      new QCommandMetadata(
        "admin-create",
        new QConstant("Create an admin."),
        Optional.empty()
      ),
      IdACommandAdminCreate.class,
      IdAResponseAdminCreate.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(USER_ID, NAME, REAL_NAME, EMAIL, PASSWORD, PERMISSIONS);
  }

  @Override
  public QParametersPositionalType onListPositionalParameters()
  {
    return new QParametersPositionalNone();
  }

  @Override
  protected IdACommandAdminCreate onCreateCommand(
    final QCommandContextType context)
    throws Exception
  {
    final var algorithm =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create();

    return new IdACommandAdminCreate(
      context.parameterValue(USER_ID),
      new IdName(context.parameterValue(NAME)),
      new IdRealName(context.parameterValue(REAL_NAME)),
      context.parameterValue(EMAIL),
      algorithm.createHashed(context.parameterValue(PASSWORD)),
      Set.copyOf(context.parameterValues(PERMISSIONS))
    );
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseAdminCreate response)
    throws QException
  {

  }
}
