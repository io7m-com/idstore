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
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdAuditSearchParameters;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchBegin;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QParameterNamed01;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QParametersPositionalNone;
import com.io7m.quarrel.core.QParametersPositionalType;
import com.io7m.quarrel.core.QStringType.QConstant;

import java.io.PrintWriter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * "audit-search-begin"
 */

public final class IdAShellCmdAuditSearchBegin
  extends IdAShellCmdAbstract<IdACommandAuditSearchBegin, IdAResponseAuditSearchBegin>
{
  private static final QParameterNamed1<OffsetDateTime> TIME_FROM =
    new QParameterNamed1<>(
      "--time-from",
      List.of(),
      new QConstant("Return audit events later than this date."),
      Optional.of(IdTimeRange.largest().timeLower()),
      OffsetDateTime.class
    );

  private static final QParameterNamed1<OffsetDateTime> TIME_TO =
    new QParameterNamed1<>(
      "--time-to",
      List.of(),
      new QConstant("Return audit events earlier than this date."),
      Optional.of(IdTimeRange.largest().timeUpper()),
      OffsetDateTime.class
    );

  private static final QParameterNamed01<String> OWNER =
    new QParameterNamed01<>(
      "--owner",
      List.of(),
      new QConstant("Filter events by owner."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed01<String> TYPE =
    new QParameterNamed01<>(
      "--type",
      List.of(),
      new QConstant("Filter events by type."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed01<String> MESSAGE =
    new QParameterNamed01<>(
      "--message",
      List.of(),
      new QConstant("Filter events by message."),
      Optional.empty(),
      String.class
    );

  /**
   * Construct a command.
   *
   * @param inClient The client
   */

  public IdAShellCmdAuditSearchBegin(
    final IdAClientSynchronousType inClient)
  {
    super(
      inClient,
      new QCommandMetadata(
        "audit-search-begin",
        new QConstant("Begin searching for audits."),
        Optional.empty()
      ),
      IdACommandAuditSearchBegin.class,
      IdAResponseAuditSearchBegin.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(
      TIME_FROM,
      TIME_TO,
      OWNER,
      TYPE,
      MESSAGE
    );
  }

  @Override
  public QParametersPositionalType onListPositionalParameters()
  {
    return new QParametersPositionalNone();
  }

  @Override
  protected IdACommandAuditSearchBegin onCreateCommand(
    final QCommandContextType context)
  {
    final var parameters =
      new IdAuditSearchParameters(
        new IdTimeRange(
          context.parameterValue(TIME_FROM),
          context.parameterValue(TIME_TO)
        ),
        context.parameterValue(OWNER),
        context.parameterValue(TYPE),
        context.parameterValue(MESSAGE),
        100
      );

    return new IdACommandAuditSearchBegin(parameters);
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseAuditSearchBegin response)
  {
    formatAuditPage(response.page(), context.output());
  }

  static void formatAuditPage(
    final IdPage<IdAuditEvent> page,
    final PrintWriter w)
  {
    w.printf(
      "# Page %s of %s, offset %s%n",
      Integer.toUnsignedString(page.pageIndex()),
      Integer.toUnsignedString(page.pageCount()),
      Long.toUnsignedString(page.pageFirstOffset())
    );
    w.println("# ID | Time | Owner | Type | Message");

    for (final var audit : page.items()) {
      w.printf(
        "%-12s | %-24s | %-36s | %-24s | %s%n",
        Long.toUnsignedString(audit.id()),
        audit.time(),
        audit.owner(),
        audit.type(),
        audit.message()
      );
    }
    w.flush();
  }
}
