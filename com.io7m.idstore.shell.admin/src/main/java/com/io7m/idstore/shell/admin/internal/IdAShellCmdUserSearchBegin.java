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

import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdUserColumn;
import com.io7m.idstore.model.IdUserColumnOrdering;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchBegin;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QParameterNamed01;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * "user-search-begin"
 */

public final class IdAShellCmdUserSearchBegin
  extends IdAShellCmdAbstractCR<IdACommandUserSearchBegin, IdAResponseUserSearchBegin>
{
  private static final QParameterNamed1<OffsetDateTime> CREATED_FROM =
    new QParameterNamed1<>(
      "--created-from",
      List.of(),
      new QConstant("Return users created later than this date."),
      Optional.of(IdTimeRange.largest().timeLower()),
      OffsetDateTime.class
    );

  private static final QParameterNamed1<OffsetDateTime> CREATED_TO =
    new QParameterNamed1<>(
      "--created-to",
      List.of(),
      new QConstant("Return users created earlier than this date."),
      Optional.of(IdTimeRange.largest().timeUpper()),
      OffsetDateTime.class
    );

  private static final QParameterNamed1<OffsetDateTime> UPDATED_FROM =
    new QParameterNamed1<>(
      "--updated-from",
      List.of(),
      new QConstant("Return users updated later than this date."),
      Optional.of(IdTimeRange.largest().timeLower()),
      OffsetDateTime.class
    );

  private static final QParameterNamed1<OffsetDateTime> UPDATED_TO =
    new QParameterNamed1<>(
      "--updated-to",
      List.of(),
      new QConstant("Return users updated earlier than this date."),
      Optional.of(IdTimeRange.largest().timeUpper()),
      OffsetDateTime.class
    );

  private static final QParameterNamed01<String> QUERY =
    new QParameterNamed01<>(
      "--query",
      List.of(),
      new QConstant("Match users against this query text."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<Integer> LIMIT =
    new QParameterNamed1<>(
      "--limit",
      List.of(),
      new QConstant("The maximum number of results per page."),
      Optional.of(Integer.valueOf(10)),
      Integer.class
    );

  /**
   * Construct a command.
   *
   * @param inServices The service directory
   */

  public IdAShellCmdUserSearchBegin(
    final RPServiceDirectoryType inServices)
  {
    super(
      inServices,
      new QCommandMetadata(
        "user-search-begin",
        new QConstant("Begin searching for users."),
        Optional.empty()
      ),
      IdACommandUserSearchBegin.class,
      IdAResponseUserSearchBegin.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(
      CREATED_FROM,
      CREATED_TO,
      LIMIT,
      UPDATED_FROM,
      UPDATED_TO,
      QUERY
    );
  }

  @Override
  protected IdACommandUserSearchBegin onCreateCommand(
    final QCommandContextType context)
  {
    final var parameters =
      new IdUserSearchParameters(
        new IdTimeRange(
          context.parameterValue(CREATED_FROM),
          context.parameterValue(CREATED_TO)
        ),
        new IdTimeRange(
          context.parameterValue(UPDATED_FROM),
          context.parameterValue(UPDATED_TO)
        ),
        context.parameterValue(QUERY),
        new IdUserColumnOrdering(IdUserColumn.BY_IDNAME, true),
        context.parameterValue(LIMIT).intValue()
      );

    return new IdACommandUserSearchBegin(parameters);
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseUserSearchBegin response)
    throws Exception
  {
    this.formatter().formatUsers(response.page());
  }
}
