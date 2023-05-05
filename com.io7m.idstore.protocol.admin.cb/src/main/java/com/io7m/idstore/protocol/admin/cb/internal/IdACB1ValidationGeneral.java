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


package com.io7m.idstore.protocol.admin.cb.internal;

import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned32;
import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned64;
import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned8;
import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBOptionType;
import com.io7m.cedarbridge.runtime.api.CBSerializableType;
import com.io7m.cedarbridge.runtime.api.CBSome;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.cedarbridge.runtime.api.CBUUID;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithms;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.protocol.admin.cb.IdA1AuditEvent;
import com.io7m.idstore.protocol.admin.cb.IdA1Ban;
import com.io7m.idstore.protocol.admin.cb.IdA1Login;
import com.io7m.idstore.protocol.admin.cb.IdA1Page;
import com.io7m.idstore.protocol.admin.cb.IdA1Password;
import com.io7m.idstore.protocol.admin.cb.IdA1TimeRange;
import com.io7m.idstore.protocol.admin.cb.IdA1TimestampUTC;
import com.io7m.idstore.protocol.api.IdProtocolException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.io7m.cedarbridge.runtime.api.CBOptionType.fromOptional;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.model.IdOptional.mapPartial;

/**
 * Functions to translate between the core command set and the Admin v1
 * Cedarbridge encoding command set.
 */

public final class IdACB1ValidationGeneral
{
  private IdACB1ValidationGeneral()
  {

  }

  public static <A, B extends CBSerializableType> IdA1Page<B> toWirePage(
    final IdPage<A> page,
    final Function<A, B> f)
  {
    return new IdA1Page<>(
      new CBList<>(page.items().stream().map(f).toList()),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(page.pageIndex())),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(page.pageCount())),
      new CBIntegerUnsigned64(page.pageFirstOffset())
    );
  }

  public static IdA1Ban toWireBan(
    final IdBan ban)
  {
    return new IdA1Ban(
      new CBUUID(ban.user()),
      new CBString(ban.reason()),
      fromOptional(
        ban.expires().map(IdACB1ValidationGeneral::toWireTimestamp)
      )
    );
  }

  public static IdA1TimestampUTC toWireTimestamp(
    final OffsetDateTime t)
  {
    return new IdA1TimestampUTC(
      new CBIntegerUnsigned32(Integer.toUnsignedLong(t.getYear())),
      new CBIntegerUnsigned8(t.getMonthValue()),
      new CBIntegerUnsigned8(t.getDayOfMonth()),
      new CBIntegerUnsigned8(t.getHour()),
      new CBIntegerUnsigned8(t.getMinute()),
      new CBIntegerUnsigned8(t.getSecond()),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(t.getNano() / 1000))
    );
  }

  public static IdA1TimeRange toWireTimeRange(
    final IdTimeRange timeRange)
  {
    return new IdA1TimeRange(
      toWireTimestamp(timeRange.timeLower()),
      toWireTimestamp(timeRange.timeUpper())
    );
  }

  public static IdA1Password toWirePassword(
    final IdPassword password)
  {
    return new IdA1Password(
      new CBString(password.algorithm().identifier()),
      new CBString(password.hash()),
      new CBString(password.salt())
    );
  }

  public static <A extends CBSerializableType, B> IdPage<B> fromWirePage(
    final IdA1Page<A> page,
    final Function<A, B> f)
  {
    return new IdPage<>(
      page.fieldItems().values().stream().map(f).toList(),
      (int) page.fieldPageIndex().value(),
      (int) page.fieldPageCount().value(),
      page.fieldPageFirstOffset().value()
    );
  }

  public static IdNonEmptyList<IdEmail> fromWireEmails(
    final CBList<CBString> fieldEmails)
    throws IdProtocolException
  {
    final var es = fieldEmails.values();
    if (es.isEmpty()) {
      throw new IdProtocolException(
        "Admin emails list is empty!",
        PROTOCOL_ERROR,
        Map.of(),
        Optional.of("Provide at least one admin email address.")
      );
    }

    final var emails = new ArrayList<>(fieldEmails.values());
    final var email0 = new IdEmail(emails.remove(0).value());
    return new IdNonEmptyList<>(
      email0,
      emails.stream().map(CBString::value).map(IdEmail::new).toList()
    );
  }

  public static Optional<IdPassword> fromWirePasswordOptional(
    final CBOptionType<IdA1Password> fieldPassword)
    throws IdPasswordException
  {
    if (fieldPassword instanceof CBSome<IdA1Password> some) {
      return Optional.of(
        fromWirePassword(some.value())
      );
    }
    return Optional.empty();
  }

  public static IdTimeRange fromWireTimeRange(
    final IdA1TimeRange t)
  {
    return new IdTimeRange(
      fromWireTimestamp(t.fieldLower()),
      fromWireTimestamp(t.fieldUpper())
    );
  }

  public static IdPassword fromWirePassword(
    final IdA1Password fieldPassword)
    throws IdPasswordException
  {
    return new IdPassword(
      IdPasswordAlgorithms.parse(fieldPassword.fieldAlgorithm().value()),
      fieldPassword.fieldHash().value(),
      fieldPassword.fieldSalt().value()
    );
  }

  public static IdBan fromWireBan(
    final IdA1Ban fieldBan)
  {
    return new IdBan(
      fieldBan.fieldUser().value(),
      fieldBan.fieldReason().value(),
      mapPartial(
        fieldBan.fieldExpires().asOptional(),
        IdACB1ValidationGeneral::fromWireTimestamp)
    );
  }

  public static OffsetDateTime fromWireTimestamp(
    final IdA1TimestampUTC t)
  {
    return OffsetDateTime.of(
      (int) (t.fieldYear().value() & 0xffffffffL),
      t.fieldMonth().value(),
      t.fieldDay().value(),
      t.fieldHour().value(),
      t.fieldMinute().value(),
      t.fieldSecond().value(),
      (int) (t.fieldMillisecond().value() * 1000L),
      ZoneOffset.UTC
    );
  }

  public static IdA1AuditEvent toWireAuditEvent(
    final IdAuditEvent e)
  {
    return new IdA1AuditEvent(
      new CBIntegerUnsigned64(e.id()),
      new CBUUID(e.owner()),
      toWireTimestamp(e.time()),
      new CBString(e.type()),
      new CBString(e.message())
    );
  }

  public static IdLogin fromWireLogin(
    final IdA1Login i)
  {
    return new IdLogin(
      i.fieldUser().value(),
      fromWireTimestamp(i.fieldTime()),
      i.fieldHost().value(),
      i.fieldAgent().value()
    );
  }

  public static IdAuditEvent fromWireAuditEvent(
    final IdA1AuditEvent i)
  {
    return new IdAuditEvent(
      i.fieldId().value(),
      i.fieldOwner().value(),
      fromWireTimestamp(i.fieldTime()),
      i.fieldType().value(),
      i.fieldMessage().value()
    );
  }

  public static IdA1Login toWireLogin(
    final IdLogin i)
  {
    return new IdA1Login(
      new CBUUID(i.userId()),
      toWireTimestamp(i.time()),
      new CBString(i.host()),
      new CBString(i.userAgent())
    );
  }
}
