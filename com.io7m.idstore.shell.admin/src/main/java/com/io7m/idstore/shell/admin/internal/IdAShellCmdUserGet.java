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

import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.admin.IdACommandUserGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserGet;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QException;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * "user-get"
 */

public final class IdAShellCmdUserGet
  extends IdAShellCmdAbstractCR<IdACommandUserGet, IdAResponseUserGet>
{
  private static final QParameterNamed1<UUID> USER_ID =
    new QParameterNamed1<>(
      "--user",
      List.of(),
      new QConstant("The user ID."),
      Optional.empty(),
      UUID.class
    );

  /**
   * Construct a command.
   *
   * @param inServices The service directory
   */

  public IdAShellCmdUserGet(
    final RPServiceDirectoryType inServices)
  {
    super(
      inServices,
      new QCommandMetadata(
        "user-get",
        new QConstant("Retrieve a user."),
        Optional.empty()
      ),
      IdACommandUserGet.class,
      IdAResponseUserGet.class
    );
  }

  static void formatUser(
    final IdUser user,
    final PrintWriter out)
  {
    out.print("User ID: ");
    out.println(user.id());
    out.print("Name: ");
    out.println(user.idName().value());
    out.print("Real Name: ");
    out.println(user.realName().value());
    out.print("Time Created: ");
    out.println(user.timeCreated());
    out.print("Time Updated: ");
    out.println(user.timeUpdated());
    for (final var email : user.emails().toList()) {
      out.print("Email: ");
      out.println(email.value());
    }
    out.flush();
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(USER_ID);
  }

  @Override
  protected IdACommandUserGet onCreateCommand(
    final QCommandContextType context)
  {
    return new IdACommandUserGet(context.parameterValue(USER_ID));
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
          Map.entry("User ID", context.parameterValue(USER_ID).toString())
        ),
        Optional.empty(),
        List.of()
      );
    }

    formatUser(userOpt.get(), context.output());
  }
}
