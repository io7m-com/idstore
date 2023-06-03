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
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchPrevious;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QParametersPositionalNone;
import com.io7m.quarrel.core.QParametersPositionalType;
import com.io7m.quarrel.core.QStringType.QConstant;

import java.util.List;
import java.util.Optional;

import static com.io7m.idstore.shell.admin.internal.IdAShellCmdAuditSearchBegin.formatAuditPage;

/**
 * "audit-search-previous"
 */

public final class IdAShellCmdAuditSearchPrevious
  extends IdAShellCmdAbstract<IdACommandAuditSearchPrevious, IdAResponseAuditSearchPrevious>
{
  /**
   * Construct a command.
   *
   * @param inClient The client
   */

  public IdAShellCmdAuditSearchPrevious(
    final IdAClientSynchronousType inClient)
  {
    super(
      inClient,
      new QCommandMetadata(
        "audit-search-previous",
        new QConstant("Go to the previous page of audit events."),
        Optional.empty()
      ),
      IdACommandAuditSearchPrevious.class,
      IdAResponseAuditSearchPrevious.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of();
  }

  @Override
  protected IdACommandAuditSearchPrevious onCreateCommand(
    final QCommandContextType context)
  {
    return new IdACommandAuditSearchPrevious();
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseAuditSearchPrevious response)
  {
    formatAuditPage(response.page(), context.output());
  }
}
