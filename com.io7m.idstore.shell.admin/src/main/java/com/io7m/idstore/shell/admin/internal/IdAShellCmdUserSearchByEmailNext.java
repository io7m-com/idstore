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
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailNext;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QParametersPositionalNone;
import com.io7m.quarrel.core.QParametersPositionalType;
import com.io7m.quarrel.core.QStringType.QConstant;

import java.util.List;
import java.util.Optional;

import static com.io7m.idstore.shell.admin.internal.IdAShellCmdUserSearchBegin.formatUserPage;

/**
 * "user-search-by-email-next"
 */

public final class IdAShellCmdUserSearchByEmailNext
  extends IdAShellCmdAbstract<IdACommandUserSearchByEmailNext, IdAResponseUserSearchByEmailNext>
{
  /**
   * Construct a command.
   *
   * @param inClient The client
   */

  public IdAShellCmdUserSearchByEmailNext(
    final IdAClientSynchronousType inClient)
  {
    super(
      inClient,
      new QCommandMetadata(
        "user-search-by-email-next",
        new QConstant("Go to the next page of users."),
        Optional.empty()
      ),
      IdACommandUserSearchByEmailNext.class,
      IdAResponseUserSearchByEmailNext.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of();
  }

  @Override
  public QParametersPositionalType onListPositionalParameters()
  {
    return new QParametersPositionalNone();
  }

  @Override
  protected IdACommandUserSearchByEmailNext onCreateCommand(
    final QCommandContextType context)
  {
    return new IdACommandUserSearchByEmailNext();
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseUserSearchByEmailNext response)
  {
    formatUserPage(response.page(), context.output());
  }
}
