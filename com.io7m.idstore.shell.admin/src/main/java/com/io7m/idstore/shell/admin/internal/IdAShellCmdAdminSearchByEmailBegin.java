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
import com.io7m.idstore.model.IdAdminColumn;
import com.io7m.idstore.model.IdAdminColumnOrdering;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailBegin;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QParameterNamed01;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QParametersPositionalNone;
import com.io7m.quarrel.core.QParametersPositionalType;
import com.io7m.quarrel.core.QStringType.QConstant;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.io7m.idstore.shell.admin.internal.IdAShellCmdAdminSearchBegin.formatAdminPage;

/**
 * "admin-search-by-email-begin"
 */

public final class IdAShellCmdAdminSearchByEmailBegin
  extends IdAShellCmdAbstract<IdACommandAdminSearchByEmailBegin, IdAResponseAdminSearchByEmailBegin>
{
  private static final QParameterNamed1<OffsetDateTime> CREATED_FROM =
    new QParameterNamed1<>(
      "--created-from",
      List.of(),
      new QConstant("Return admins created later than this date."),
      Optional.of(IdTimeRange.largest().timeLower()),
      OffsetDateTime.class
    );

  private static final QParameterNamed1<OffsetDateTime> CREATED_TO =
    new QParameterNamed1<>(
      "--created-to",
      List.of(),
      new QConstant("Return admins created earlier than this date."),
      Optional.of(IdTimeRange.largest().timeUpper()),
      OffsetDateTime.class
    );

  private static final QParameterNamed1<OffsetDateTime> UPDATED_FROM =
    new QParameterNamed1<>(
      "--updated-from",
      List.of(),
      new QConstant("Return admins updated later than this date."),
      Optional.of(IdTimeRange.largest().timeLower()),
      OffsetDateTime.class
    );

  private static final QParameterNamed1<OffsetDateTime> UPDATED_TO =
    new QParameterNamed1<>(
      "--updated-to",
      List.of(),
      new QConstant("Return admins updated earlier than this date."),
      Optional.of(IdTimeRange.largest().timeUpper()),
      OffsetDateTime.class
    );

  private static final QParameterNamed01<String> QUERY =
    new QParameterNamed01<>(
      "--email",
      List.of(),
      new QConstant("Match admin emails against this query text."),
      Optional.empty(),
      String.class
    );

  /**
   * Construct a command.
   *
   * @param inClient The client
   */

  public IdAShellCmdAdminSearchByEmailBegin(
    final IdAClientSynchronousType inClient)
  {
    super(
      inClient,
      new QCommandMetadata(
        "admin-search-by-email-begin",
        new QConstant("Begin searching for admins by email."),
        Optional.empty()
      ),
      IdACommandAdminSearchByEmailBegin.class,
      IdAResponseAdminSearchByEmailBegin.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(
      CREATED_FROM,
      CREATED_TO,
      UPDATED_FROM,
      UPDATED_TO,
      QUERY
    );
  }

  @Override
  protected IdACommandAdminSearchByEmailBegin onCreateCommand(
    final QCommandContextType context)
  {
    final var parameters =
      new IdAdminSearchByEmailParameters(
        new IdTimeRange(
          context.parameterValue(CREATED_FROM),
          context.parameterValue(CREATED_TO)
        ),
        new IdTimeRange(
          context.parameterValue(UPDATED_FROM),
          context.parameterValue(UPDATED_TO)
        ),
        context.parameterValue(QUERY).orElse(""),
        new IdAdminColumnOrdering(IdAdminColumn.BY_IDNAME, true),
        10
      );

    return new IdACommandAdminSearchByEmailBegin(parameters);
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseAdminSearchByEmailBegin response)
  {
    formatAdminPage(response.page(), context.output());
  }
}
