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
import com.io7m.tabla.core.TTableRendererType;
import com.io7m.tabla.core.TTableType;
import com.io7m.tabla.core.Tabla;
import org.jline.terminal.Terminal;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

import static com.io7m.tabla.core.TColumnWidthConstraint.atLeastContentOrHeader;
import static com.io7m.tabla.core.TConstraintHardness.SOFT_CONSTRAINT;
import static com.io7m.tabla.core.TTableWidthConstraintType.tableWidthExact;

/**
 * A pretty formatter.
 */

public final class IdAFormatterPretty implements IdAFormatterType
{
  private final Terminal terminal;
  private final TTableRendererType tableRenderer;

  /**
   * A pretty formatter.
   *
   * @param inTerminal The terminal
   */

  public IdAFormatterPretty(
    final Terminal inTerminal)
  {
    this.terminal =
      Objects.requireNonNull(inTerminal, "terminal");
    this.tableRenderer =
      Tabla.framedUnicodeRenderer();
  }

  @Override
  public String toString()
  {
    return "[%s]".formatted(this.getClass().getSimpleName());
  }

  private int width()
  {
    var width = Math.max(0, this.terminal.getWidth() - 8);
    if (width == 0) {
      width = 100;
    }
    return width;
  }

  @Override
  public void formatAdmin(
    final IdAdmin admin)
    throws Exception
  {
    final var builder =
      Tabla.builder()
        .setWidthConstraint(tableWidthExact(this.width(), SOFT_CONSTRAINT))
        .declareColumn("Attribute", atLeastContentOrHeader())
        .declareColumn("Value", atLeastContentOrHeader());

    builder.addRow()
      .addCell("ID")
      .addCell(admin.id().toString());

    builder.addRow()
      .addCell("Name")
      .addCell(admin.idName().value());

    builder.addRow()
      .addCell("Real Name")
      .addCell(admin.realName().value());

    builder.addRow()
      .addCell("Time Created")
      .addCell(admin.timeCreated().toString());

    builder.addRow()
      .addCell("Time Updated")
      .addCell(admin.timeUpdated().toString());

    for (final var email : admin.emails().toList()) {
      builder.addRow()
        .addCell("Email")
        .addCell(email.value());
    }

    for (final var permission : admin.permissions().impliedPermissions()) {
      builder.addRow()
        .addCell("Permission")
        .addCell(permission.name());
    }

    this.renderTable(builder.build());
  }

  @Override
  public void formatAdmins(
    final IdPage<IdAdminSummary> page)
    throws Exception
  {
    final var out = this.terminal.writer();
    formatPage(page, out);

    final var builder =
      Tabla.builder()
        .setWidthConstraint(tableWidthExact(this.width(), SOFT_CONSTRAINT))
        .declareColumn("ID", atLeastContentOrHeader())
        .declareColumn("Name", atLeastContentOrHeader())
        .declareColumn("Real Name", atLeastContentOrHeader());

    for (final var item : page.items()) {
      builder.addRow()
        .addCell(item.id().toString())
        .addCell(item.idName().value())
        .addCell(item.realName().value());
    }

    this.renderTable(builder.build());
  }

  @Override
  public void formatAudits(
    final IdPage<IdAuditEvent> page)
    throws Exception
  {
    final var out = this.terminal.writer();
    formatPage(page, out);

    final var builder =
      Tabla.builder()
        .setWidthConstraint(tableWidthExact(this.width(), SOFT_CONSTRAINT))
        .declareColumn("ID", atLeastContentOrHeader())
        .declareColumn("Time", atLeastContentOrHeader())
        .declareColumn("Owner", atLeastContentOrHeader())
        .declareColumn("Type", atLeastContentOrHeader())
        .declareColumn("Message", atLeastContentOrHeader());

    for (final var audit : page.items()) {
      builder.addRow()
        .addCell(Long.toUnsignedString(audit.id()))
        .addCell(audit.time().toString())
        .addCell(audit.owner().toString())
        .addCell(audit.type())
        .addCell(audit.message());
    }

    this.renderTable(builder.build());
  }

  @Override
  public void formatUsers(
    final IdPage<IdUserSummary> page)
    throws Exception
  {
    final var out = this.terminal.writer();
    formatPage(page, out);

    final var builder =
      Tabla.builder()
        .setWidthConstraint(tableWidthExact(this.width(), SOFT_CONSTRAINT))
        .declareColumn("ID", atLeastContentOrHeader())
        .declareColumn("Name", atLeastContentOrHeader())
        .declareColumn("Real Name", atLeastContentOrHeader());

    for (final var item : page.items()) {
      builder.addRow()
        .addCell(item.id().toString())
        .addCell(item.idName().value())
        .addCell(item.realName().value());
    }

    this.renderTable(builder.build());
  }

  @Override
  public void formatLoginHistory(
    final List<IdLogin> history)
    throws Exception
  {
    final var builder =
      Tabla.builder()
        .setWidthConstraint(tableWidthExact(this.width(), SOFT_CONSTRAINT))
        .declareColumn("Time", atLeastContentOrHeader())
        .declareColumn("Host", atLeastContentOrHeader())
        .declareColumn("User Agent", atLeastContentOrHeader());

    for (final var login : history) {
      builder.addRow()
        .addCell(login.time().toString())
        .addCell(login.host())
        .addCell(login.userAgent());
    }

    this.renderTable(builder.build());
  }

  @Override
  public void formatUser(
    final IdUser user)
    throws Exception
  {
    final var builder =
      Tabla.builder()
        .setWidthConstraint(tableWidthExact(this.width(), SOFT_CONSTRAINT))
        .declareColumn("Attribute", atLeastContentOrHeader())
        .declareColumn("Value", atLeastContentOrHeader());

    builder.addRow()
      .addCell("ID")
      .addCell(user.id().toString());

    builder.addRow()
      .addCell("Name")
      .addCell(user.idName().value());

    builder.addRow()
      .addCell("Real Name")
      .addCell(user.realName().value());

    builder.addRow()
      .addCell("Time Created")
      .addCell(user.timeCreated().toString());

    builder.addRow()
      .addCell("Time Updated")
      .addCell(user.timeUpdated().toString());

    for (final var email : user.emails().toList()) {
      builder.addRow()
        .addCell("Email")
        .addCell(email.value());
    }

    this.renderTable(builder.build());
  }

  private static void formatPage(
    final IdPage<?> page,
    final PrintWriter out)
  {
    out.printf(
      " Page %s of %s, offset %s%n",
      Integer.toUnsignedString(page.pageIndex()),
      Integer.toUnsignedString(page.pageCount()),
      Long.toUnsignedString(page.pageFirstOffset())
    );
  }

  private void renderTable(
    final TTableType table)
  {
    final var lines =
      this.tableRenderer.renderLines(table);

    final var writer = this.terminal.writer();
    for (final var line : lines) {
      writer.println(line);
    }
  }
}
