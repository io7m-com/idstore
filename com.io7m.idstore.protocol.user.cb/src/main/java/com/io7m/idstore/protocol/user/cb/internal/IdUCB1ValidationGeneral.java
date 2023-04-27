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


package com.io7m.idstore.protocol.user.cb.internal;

import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned32;
import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned64;
import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned8;
import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBOptionType;
import com.io7m.cedarbridge.runtime.api.CBSome;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithms;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user.cb.IdU1Password;
import com.io7m.idstore.protocol.user.cb.IdU1TimestampUTC;
import com.io7m.idstore.protocol.user.cb.IdU1UUID;
import com.io7m.idstore.protocol.user.cb.IdU1User;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;

/**
 * Functions to translate between the core command set and the User v1
 * Cedarbridge encoding command set.
 */

public final class IdUCB1ValidationGeneral
{
  private IdUCB1ValidationGeneral()
  {

  }

  public static IdU1UUID toWireUUID(
    final UUID uuid)
  {
    return new IdU1UUID(
      new CBIntegerUnsigned64(uuid.getMostSignificantBits()),
      new CBIntegerUnsigned64(uuid.getLeastSignificantBits())
    );
  }

  public static UUID fromWireUUID(
    final IdU1UUID uuid)
  {
    return new UUID(
      uuid.fieldMsb().value(),
      uuid.fieldLsb().value()
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
    final CBOptionType<IdU1Password> fieldPassword)
    throws IdPasswordException
  {
    if (fieldPassword instanceof CBSome<IdU1Password> some) {
      return Optional.of(
        fromWirePassword(some.value())
      );
    }
    return Optional.empty();
  }

  public static IdPassword fromWirePassword(
    final IdU1Password fieldPassword)
    throws IdPasswordException
  {
    return new IdPassword(
      IdPasswordAlgorithms.parse(fieldPassword.fieldAlgorithm().value()),
      fieldPassword.fieldHash().value(),
      fieldPassword.fieldSalt().value()
    );
  }

  public static OffsetDateTime fromWireTimestamp(
    final IdU1TimestampUTC t)
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

  public static IdU1TimestampUTC toWireTimestamp(
    final OffsetDateTime t)
  {
    return new IdU1TimestampUTC(
      new CBIntegerUnsigned32(Integer.toUnsignedLong(t.getYear())),
      new CBIntegerUnsigned8(t.getMonthValue()),
      new CBIntegerUnsigned8(t.getDayOfMonth()),
      new CBIntegerUnsigned8(t.getHour()),
      new CBIntegerUnsigned8(t.getMinute()),
      new CBIntegerUnsigned8(t.getSecond()),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(t.getNano() / 1000))
    );
  }

  public static IdU1Password toWirePassword(
    final IdPassword password)
  {
    return new IdU1Password(
      new CBString(password.algorithm().identifier()),
      new CBString(password.hash()),
      new CBString(password.salt())
    );
  }

  public static IdU1User toWireUser(
    final IdUser user)
  {
    return new IdU1User(
      toWireUUID(user.id()),
      new CBString(user.idName().value()),
      new CBString(user.realName().value()),
      toWireEmails(user.emails()),
      toWireTimestamp(user.timeCreated()),
      toWireTimestamp(user.timeUpdated()),
      toWirePassword(user.password())
    );
  }

  private static CBList<CBString> toWireEmails(
    final IdNonEmptyList<IdEmail> emails)
  {
    return new CBList<>(
      emails.toList()
        .stream()
        .map(IdEmail::value)
        .map(CBString::new).toList()
    );
  }

  public static IdUser fromWireUser(
    final IdU1User user)
    throws IdProtocolException, IdPasswordException
  {
    return new IdUser(
      fromWireUUID(user.fieldId()),
      new IdName(user.fieldIdName().value()),
      new IdRealName(user.fieldRealName().value()),
      fromWireEmails(user.fieldEmails()),
      fromWireTimestamp(user.fieldTimeCreated()),
      fromWireTimestamp(user.fieldTimeUpdated()),
      fromWirePassword(user.fieldPassword())
    );
  }
}
