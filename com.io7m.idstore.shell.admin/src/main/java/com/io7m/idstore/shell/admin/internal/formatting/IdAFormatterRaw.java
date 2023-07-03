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


package com.io7m.idstore.shell.admin.internal.formatting;

import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserSummary;
import org.jline.terminal.Terminal;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A raw formatter.
 */

public final class IdAFormatterRaw implements IdAFormatterType
{
  private final Terminal terminal;

  /**
   * A raw formatter.
   *
   * @param inTerminal The terminal
   */

  public IdAFormatterRaw(
    final Terminal inTerminal)
  {
    this.terminal =
      Objects.requireNonNull(inTerminal, "terminal");
  }

  @Override
  public String toString()
  {
    return "[%s]".formatted(this.getClass().getSimpleName());
  }

  @Override
  public void formatAdmin(
    final IdAdmin admin)
  {
    final var out = this.terminal.writer();
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
  public void formatAdmins(
    final IdPage<IdAdminSummary> page)
  {
    final var out = this.terminal.writer();
    formatPage(page, out);

    out.println("# Admin ID | Name | Real Name");

    for (final var admin : page.items()) {
      out.printf(
        "%-40s %-40s %s%n",
        admin.id(),
        admin.idName().value(),
        admin.realName().value()
      );
    }
    out.flush();
  }

  @Override
  public void formatAudits(
    final IdPage<IdAuditEvent> page)
  {
    final var out = this.terminal.writer();
    formatPage(page, out);

    out.println("# ID | Time | Owner | Type | Message");

    for (final var audit : page.items()) {
      out.printf(
        "%-12s | %-24s | %-36s | %-24s | %s%n",
        Long.toUnsignedString(audit.id()),
        audit.time(),
        audit.owner(),
        audit.type(),
        audit.message()
      );
    }
    out.flush();
  }

  @Override
  public void formatUsers(
    final IdPage<IdUserSummary> page)
  {
    final var out = this.terminal.writer();
    formatPage(page, out);

    out.println("# User ID | Name | Real Name");

    for (final var admin : page.items()) {
      out.printf(
        "%-40s %-40s %s%n",
        admin.id(),
        admin.idName().value(),
        admin.realName().value()
      );
    }
    out.flush();
  }

  @Override
  public void formatLoginHistory(
    final List<IdLogin> histories)
  {
    final var out = this.terminal.writer();
    out.printf("# %-22s | %-15s | %s%n", "Time", "Host", "User Agent");
    for (final var history : histories) {
      out.printf(
        "%-24s |%-16s |%s%n",
        history.time(),
        history.host(),
        history.userAgent()
      );
    }
    out.flush();
  }

  @Override
  public void formatUser(
    final IdUser user)
  {
    final var out = this.terminal.writer();
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

  private static void formatPage(
    final IdPage<?> page,
    final PrintWriter out)
  {
    out.printf(
      "# Page %s of %s, offset %s%n",
      Integer.toUnsignedString(page.pageIndex()),
      Integer.toUnsignedString(page.pageCount()),
      Long.toUnsignedString(page.pageFirstOffset())
    );
  }
}
