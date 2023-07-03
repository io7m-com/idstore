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

import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.protocol.admin.IdACommandAdminGet;
import com.io7m.idstore.protocol.admin.IdAResponseAdminGet;
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
import java.util.stream.Collectors;

/**
 * "admin-get"
 */

public final class IdAShellCmdAdminGet
  extends IdAShellCmdAbstractCR<IdACommandAdminGet, IdAResponseAdminGet>
{
  private static final QParameterNamed1<UUID> USER_ID =
    new QParameterNamed1<>(
      "--admin",
      List.of(),
      new QConstant("The admin ID."),
      Optional.empty(),
      UUID.class
    );

  /**
   * Construct a command.
   *
   * @param inServices The service directory
   */

  public IdAShellCmdAdminGet(
    final RPServiceDirectoryType inServices)
  {
    super(
      inServices,
      new QCommandMetadata(
        "admin-get",
        new QConstant("Retrieve an admin."),
        Optional.empty()
      ),
      IdACommandAdminGet.class,
      IdAResponseAdminGet.class
    );
  }

  static void formatAdmin(
    final IdAdmin admin,
    final PrintWriter out)
  {
    out.print("Admin ID: ");
    out.println(admin.id());
    out.print("Name: ");
    out.println(admin.idName().value());
    out.print("Real Name: ");
    out.println(admin.realName().value());
    out.print("Time Created: ");
    out.println(admin.timeCreated());
    out.print("Time Updated: ");
    out.println(admin.timeUpdated());
    for (final var email : admin.emails().toList()) {
      out.print("Email: ");
      out.println(email.value());
    }
    out.print("Permissions: ");
    out.println(
      admin.permissions()
        .impliedPermissions()
        .stream()
        .map(Enum::name)
        .collect(Collectors.joining(" "))
    );
    out.flush();
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(USER_ID);
  }

  @Override
  protected IdACommandAdminGet onCreateCommand(
    final QCommandContextType context)
  {
    return new IdACommandAdminGet(context.parameterValue(USER_ID));
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
          Map.entry("Admin ID", context.parameterValue(USER_ID).toString())
        ),
        Optional.empty(),
        List.of()
      );
    }

    formatAdmin(adminOpt.get(), context.output());
  }
}
