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

package com.io7m.idstore.protocol.user.cb;

import com.io7m.cedarbridge.runtime.api.CBMap;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.cedarbridge.runtime.api.CBUUID;
import com.io7m.cedarbridge.runtime.convenience.CBMaps;
import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolMessageValidatorType;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddBegin;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddDeny;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddPermit;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemoveBegin;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemoveDeny;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemovePermit;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUCommandPasswordUpdate;
import com.io7m.idstore.protocol.user.IdUCommandRealnameUpdate;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUCommandUserSelf;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.protocol.user.IdUResponseBlame;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddBegin;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddDeny;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddPermit;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemoveBegin;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemoveDeny;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemovePermit;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.protocol.user.IdUResponseUserSelf;
import com.io7m.idstore.protocol.user.IdUResponseUserUpdate;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.io7m.cedarbridge.runtime.api.CBOptionType.fromOptional;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.protocol.user.cb.internal.IdUCB2ValidationGeneral.fromWireUser;
import static com.io7m.idstore.protocol.user.cb.internal.IdUCB2ValidationGeneral.toWireUser;
import static java.util.Map.entry;

/**
 * Functions to translate between the core command set and the User v2
 * Cedarbridge encoding command set.
 */

public final class IdUCB2Validation
  implements IdProtocolMessageValidatorType<IdUMessageType, ProtocolIdUv2Type>
{
  /**
   * Functions to translate between the core command set and the User v2
   * Cedarbridge encoding command set.
   */

  public IdUCB2Validation()
  {

  }

  private static ProtocolIdUv2Type toWireResponse(
    final IdUResponseType response)
  {
    return switch (response) {
      case final IdUResponseError c -> toWireResponseError(c);
      case final IdUResponseLogin c -> toWireResponseLogin(c);
      case final IdUResponseUserSelf c -> toWireResponseUserSelf(c);
      case final IdUResponseUserUpdate c -> toWireResponseUserUpdate(c);
      case final IdUResponseEmailRemovePermit c ->
        toWireResponseEmailRemovePermit(c);
      case final IdUResponseEmailRemoveDeny c ->
        toWireResponseEmailRemoveDeny(c);
      case final IdUResponseEmailRemoveBegin c ->
        toWireResponseEmailRemoveBegin(c);
      case final IdUResponseEmailAddPermit c -> toWireResponseEmailAddPermit(c);
      case final IdUResponseEmailAddDeny c -> toWireResponseEmailAddDeny(c);
      case final IdUResponseEmailAddBegin c -> toWireResponseEmailAddBegin(c);
    };
  }

  private static IdU1ResponseEmailRemovePermit toWireResponseEmailRemovePermit(
    final IdUResponseEmailRemovePermit c)
  {
    return new IdU1ResponseEmailRemovePermit(
      new CBUUID(c.messageId()),
      new CBUUID(c.correlationId())
    );
  }

  private static IdU1ResponseEmailRemoveDeny toWireResponseEmailRemoveDeny(
    final IdUResponseEmailRemoveDeny c)
  {
    return new IdU1ResponseEmailRemoveDeny(
      new CBUUID(c.messageId()),
      new CBUUID(c.correlationId())
    );
  }

  private static IdU1ResponseEmailRemoveBegin toWireResponseEmailRemoveBegin(
    final IdUResponseEmailRemoveBegin c)
  {
    return new IdU1ResponseEmailRemoveBegin(
      new CBUUID(c.messageId()),
      new CBUUID(c.correlationId())
    );
  }

  private static IdU1ResponseEmailAddPermit toWireResponseEmailAddPermit(
    final IdUResponseEmailAddPermit c)
  {
    return new IdU1ResponseEmailAddPermit(
      new CBUUID(c.messageId()),
      new CBUUID(c.correlationId())
    );
  }

  private static IdU1ResponseEmailAddDeny toWireResponseEmailAddDeny(
    final IdUResponseEmailAddDeny c)
  {
    return new IdU1ResponseEmailAddDeny(
      new CBUUID(c.messageId()),
      new CBUUID(c.correlationId())
    );
  }

  private static IdU1ResponseEmailAddBegin toWireResponseEmailAddBegin(
    final IdUResponseEmailAddBegin c)
  {
    return new IdU1ResponseEmailAddBegin(
      new CBUUID(c.messageId()),
      new CBUUID(c.correlationId())
    );
  }

  private static IdU1ResponseUserSelf toWireResponseUserSelf(
    final IdUResponseUserSelf c)
  {
    return new IdU1ResponseUserSelf(
      new CBUUID(c.messageId()),
      new CBUUID(c.correlationId()),
      toWireUser(c.user())
    );
  }

  private static IdU1ResponseUserUpdate toWireResponseUserUpdate(
    final IdUResponseUserUpdate c)
  {
    return new IdU1ResponseUserUpdate(
      new CBUUID(c.messageId()),
      new CBUUID(c.correlationId()),
      toWireUser(c.user())
    );
  }

  private static IdU1ResponseError toWireResponseError(
    final IdUResponseError error)
  {
    return new IdU1ResponseError(
      new CBUUID(error.messageId()),
      new CBUUID(error.correlationId()),
      new CBString(error.errorCode().id()),
      new CBString(error.message()),
      CBMaps.ofMapString(error.attributes()),
      fromOptional(error.remediatingAction().map(CBString::new)),
      fromBlame(error.blame())
    );
  }

  private static IdU1ResponseBlame fromBlame(
    final IdUResponseBlame blame)
  {
    return switch (blame) {
      case BLAME_SERVER -> new IdU1ResponseBlame.BlameServer();
      case BLAME_CLIENT -> new IdU1ResponseBlame.BlameClient();
    };
  }

  private static IdU1ResponseLogin toWireResponseLogin(
    final IdUResponseLogin login)
  {
    return new IdU1ResponseLogin(
      new CBUUID(login.messageId()),
      new CBUUID(login.correlationId()),
      toWireUser(login.user())
    );
  }

  private static ProtocolIdUv2Type convertToWireCommand(
    final IdUCommandType<?> command)
  {
    return switch (command) {
      case final IdUCommandLogin c -> toWireCommandLogin(c);
      case final IdUCommandUserSelf c -> toWireCommandUserSelf(c);
      case final IdUCommandEmailAddBegin c -> toWireCommandEmailAddBegin(c);
      case final IdUCommandEmailAddPermit c -> toWireCommandEmailAddPermit(c);
      case final IdUCommandEmailAddDeny c -> toWireCommandEmailAddDeny(c);
      case final IdUCommandEmailRemoveBegin c ->
        toWireCommandEmailRemoveBegin(c);
      case final IdUCommandEmailRemovePermit c ->
        toWireCommandEmailRemovePermit(c);
      case final IdUCommandEmailRemoveDeny c -> toWireCommandEmailRemoveDeny(c);
      case final IdUCommandRealnameUpdate c -> toWireCommandRealnameUpdate(c);
      case final IdUCommandPasswordUpdate c -> toWireCommandPasswordUpdate(c);
    };
  }

  private static IdU1CommandRealnameUpdate toWireCommandRealnameUpdate(
    final IdUCommandRealnameUpdate c)
  {
    return new IdU1CommandRealnameUpdate(
      new CBUUID(c.messageId()),
      new CBString(c.realName().value()));
  }

  private static IdU1CommandPasswordUpdate toWireCommandPasswordUpdate(
    final IdUCommandPasswordUpdate c)
  {
    return new IdU1CommandPasswordUpdate(
      new CBUUID(c.messageId()),
      new CBString(c.password()),
      new CBString(c.passwordConfirm())
    );
  }

  private static IdU1CommandEmailAddBegin toWireCommandEmailAddBegin(
    final IdUCommandEmailAddBegin c)
  {
    return new IdU1CommandEmailAddBegin(
      new CBUUID(c.messageId()),
      new CBString(c.email().value()));
  }

  private static IdU1CommandEmailAddPermit toWireCommandEmailAddPermit(
    final IdUCommandEmailAddPermit c)
  {
    return new IdU1CommandEmailAddPermit(
      new CBUUID(c.messageId()),
      new CBString(c.token().value()));
  }

  private static IdU1CommandEmailAddDeny toWireCommandEmailAddDeny(
    final IdUCommandEmailAddDeny c)
  {
    return new IdU1CommandEmailAddDeny(
      new CBUUID(c.messageId()),
      new CBString(c.token().value()));
  }

  private static IdU1CommandEmailRemoveBegin toWireCommandEmailRemoveBegin(
    final IdUCommandEmailRemoveBegin c)
  {
    return new IdU1CommandEmailRemoveBegin(
      new CBUUID(c.messageId()),
      new CBString(c.email().value()));
  }

  private static IdU1CommandEmailRemovePermit toWireCommandEmailRemovePermit(
    final IdUCommandEmailRemovePermit c)
  {
    return new IdU1CommandEmailRemovePermit(
      new CBUUID(c.messageId()),
      new CBString(c.token().value()));
  }

  private static IdU1CommandEmailRemoveDeny toWireCommandEmailRemoveDeny(
    final IdUCommandEmailRemoveDeny c)
  {
    return new IdU1CommandEmailRemoveDeny(
      new CBUUID(c.messageId()),
      new CBString(c.token().value()));
  }

  private static IdU1CommandUserSelf toWireCommandUserSelf(
    final IdUCommandUserSelf c)
  {
    return new IdU1CommandUserSelf(new CBUUID(c.messageId()));
  }

  private static IdU1CommandLogin toWireCommandLogin(
    final IdUCommandLogin login)
  {
    return new IdU1CommandLogin(
      new CBUUID(login.messageId()),
      new CBString(login.userName().value()),
      new CBString(login.password()),
      toWireLoginMetadata(login.metadata())
    );
  }

  private static CBMap<CBString, CBString> toWireLoginMetadata(
    final Map<String, String> metadata)
  {
    return new CBMap<>(
      metadata.entrySet()
        .stream()
        .map(e -> entry(new CBString(e.getKey()), new CBString(e.getValue())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
    );
  }

  private static IdUResponseError fromWireResponseError(
    final IdU1ResponseError error)
    throws IdProtocolException
  {
    return new IdUResponseError(
      error.fieldMessageId().value(),
      error.fieldCorrelationId().value(),
      error.fieldMessage().value(),
      new IdErrorCode(error.fieldErrorCode().value()),
      CBMaps.toMapString(error.fieldAttributes()),
      error.fieldRemediatingAction()
        .asOptional()
        .map(CBString::value),
      fromWireBlame(error.fieldBlame())
    );
  }

  private static IdUResponseBlame fromWireBlame(
    final IdU1ResponseBlame blame)
  {
    return switch (blame) {
      case final IdU1ResponseBlame.BlameClient m ->
        IdUResponseBlame.BLAME_CLIENT;
      case final IdU1ResponseBlame.BlameServer m ->
        IdUResponseBlame.BLAME_SERVER;
    };
  }

  private static IdUResponseLogin fromWireResponseLogin(
    final IdU1ResponseLogin login)
    throws IdProtocolException, IdPasswordException
  {
    return new IdUResponseLogin(
      login.fieldMessageId().value(),
      login.fieldCorrelationId().value(),
      fromWireUser(login.fieldUser())
    );
  }

  private static IdUCommandLogin fromWireCommandLogin(
    final IdU1CommandLogin login)
  {
    return new IdUCommandLogin(
      login.fieldMessageId().value(),
      new IdName(login.fieldUserName().value()),
      login.fieldPassword().value(),
      fromWireCommandLoginMetadata(login.fieldMetadata())
    );
  }

  private static Map<String, String> fromWireCommandLoginMetadata(
    final CBMap<CBString, CBString> map)
  {
    return map.values()
      .entrySet()
      .stream()
      .map(e -> Map.entry(e.getKey().value(), e.getValue().value()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public ProtocolIdUv2Type convertToWire(
    final IdUMessageType message)
    throws IdProtocolException
  {
    return switch (message) {
      case final IdUCommandType<?> command -> convertToWireCommand(command);
      case final IdUResponseType response -> toWireResponse(response);
    };
  }

  @Override
  public IdUMessageType convertFromWire(
    final ProtocolIdUv2Type message)
    throws IdProtocolException
  {
    try {

      /*
       * Commands.
       */

      return switch (message) {
        case final IdU1CommandLogin c -> fromWireCommandLogin(c);
        case final IdU1CommandEmailAddBegin c ->
          fromWireCommandEmailAddBegin(c);
        case final IdU1CommandEmailAddPermit c ->
          fromWireCommandEmailAddPermit(c);
        case final IdU1CommandEmailAddDeny c -> fromWireCommandEmailAddDeny(c);
        case final IdU1CommandEmailRemoveBegin c ->
          fromWireCommandEmailRemoveBegin(c);
        case final IdU1CommandEmailRemovePermit c ->
          fromWireCommandEmailRemovePermit(c);
        case final IdU1CommandEmailRemoveDeny c ->
          fromWireCommandEmailRemoveDeny(c);
        case final IdU1CommandUserSelf c -> fromWireCommandUserSelf(c);
        case final IdU1CommandRealnameUpdate c ->
          fromWireCommandRealnameUpdate(c);
        case final IdU1CommandPasswordUpdate c ->
          fromWireCommandPasswordUpdate(c);

        /*
         * Responses.
         */

        case final IdU1ResponseLogin c -> fromWireResponseLogin(c);
        case final IdU1ResponseError c -> fromWireResponseError(c);
        case final IdU1ResponseUserSelf c -> fromWireResponseUserSelf(c);
        case final IdU1ResponseUserUpdate c -> fromWireResponseUserUpdate(c);
        case final IdU1ResponseEmailAddBegin c ->
          fromWireResponseEmailAddBegin(c);
        case final IdU1ResponseEmailAddPermit c ->
          fromWireResponseEmailAddPermit(c);
        case final IdU1ResponseEmailAddDeny c ->
          fromWireResponseEmailAddDeny(c);
        case final IdU1ResponseEmailRemoveBegin c ->
          fromWireResponseEmailRemoveBegin(c);
        case final IdU1ResponseEmailRemovePermit c ->
          fromWireResponseEmailRemovePermit(c);
        case final IdU1ResponseEmailRemoveDeny c ->
          fromWireResponseEmailRemoveDeny(c);
      };

    } catch (final Exception e) {
      throw new IdProtocolException(
        Objects.requireNonNullElse(
          e.getMessage(),
          e.getClass().getSimpleName()),
        e,
        PROTOCOL_ERROR,
        Map.of(),
        Optional.empty()
      );
    }
  }

  private static IdUCommandRealnameUpdate fromWireCommandRealnameUpdate(
    final IdU1CommandRealnameUpdate c)
  {
    return new IdUCommandRealnameUpdate(
      c.fieldMessageId().value(),
      new IdRealName(c.fieldName().value())
    );
  }

  private static IdUCommandPasswordUpdate fromWireCommandPasswordUpdate(
    final IdU1CommandPasswordUpdate c)
  {
    return new IdUCommandPasswordUpdate(
      c.fieldMessageId().value(),
      c.fieldPassword().value(),
      c.fieldPasswordConfirm().value()
    );
  }

  private static IdUCommandEmailAddBegin fromWireCommandEmailAddBegin(
    final IdU1CommandEmailAddBegin c)
  {
    return new IdUCommandEmailAddBegin(
      c.fieldMessageId().value(),
      new IdEmail(c.fieldEmail().value()));
  }

  private static IdUCommandEmailAddPermit fromWireCommandEmailAddPermit(
    final IdU1CommandEmailAddPermit c)
  {
    return new IdUCommandEmailAddPermit(
      c.fieldMessageId().value(),
      new IdToken(c.fieldToken().value()));
  }

  private static IdUCommandEmailAddDeny fromWireCommandEmailAddDeny(
    final IdU1CommandEmailAddDeny c)
  {
    return new IdUCommandEmailAddDeny(
      c.fieldMessageId().value(),
      new IdToken(c.fieldToken().value()));
  }

  private static IdUCommandEmailRemoveBegin fromWireCommandEmailRemoveBegin(
    final IdU1CommandEmailRemoveBegin c)
  {
    return new IdUCommandEmailRemoveBegin(
      c.fieldMessageId().value(),
      new IdEmail(c.fieldEmail().value()));
  }

  private static IdUCommandEmailRemovePermit fromWireCommandEmailRemovePermit(
    final IdU1CommandEmailRemovePermit c)
  {
    return new IdUCommandEmailRemovePermit(
      c.fieldMessageId().value(),
      new IdToken(c.fieldToken().value()));
  }

  private static IdUCommandEmailRemoveDeny fromWireCommandEmailRemoveDeny(
    final IdU1CommandEmailRemoveDeny c)
  {
    return new IdUCommandEmailRemoveDeny(
      c.fieldMessageId().value(),
      new IdToken(c.fieldToken().value()));
  }

  private static IdUResponseEmailAddBegin fromWireResponseEmailAddBegin(
    final IdU1ResponseEmailAddBegin c)
  {
    return new IdUResponseEmailAddBegin(
      c.fieldMessageId().value(),
      c.fieldCorrelationId().value()
    );
  }

  private static IdUResponseEmailAddPermit fromWireResponseEmailAddPermit(
    final IdU1ResponseEmailAddPermit c)
  {
    return new IdUResponseEmailAddPermit(
      c.fieldMessageId().value(),
      c.fieldCorrelationId().value()
    );
  }

  private static IdUResponseEmailAddDeny fromWireResponseEmailAddDeny(
    final IdU1ResponseEmailAddDeny c)
  {
    return new IdUResponseEmailAddDeny(
      c.fieldMessageId().value(),
      c.fieldCorrelationId().value()
    );
  }

  private static IdUResponseEmailRemoveBegin fromWireResponseEmailRemoveBegin(
    final IdU1ResponseEmailRemoveBegin c)
  {
    return new IdUResponseEmailRemoveBegin(
      c.fieldMessageId().value(),
      c.fieldCorrelationId().value()
    );
  }

  private static IdUResponseEmailRemovePermit fromWireResponseEmailRemovePermit(
    final IdU1ResponseEmailRemovePermit c)
  {
    return new IdUResponseEmailRemovePermit(
      c.fieldMessageId().value(),
      c.fieldCorrelationId().value()
    );
  }

  private static IdUResponseEmailRemoveDeny fromWireResponseEmailRemoveDeny(
    final IdU1ResponseEmailRemoveDeny c)
  {
    return new IdUResponseEmailRemoveDeny(
      c.fieldMessageId().value(),
      c.fieldCorrelationId().value()
    );
  }

  private static IdUCommandUserSelf fromWireCommandUserSelf(
    final IdU1CommandUserSelf c)
  {
    return new IdUCommandUserSelf(
      c.fieldMessageId().value()
    );
  }

  private static IdUResponseUserSelf fromWireResponseUserSelf(
    final IdU1ResponseUserSelf c)
    throws IdProtocolException, IdPasswordException
  {
    return new IdUResponseUserSelf(
      c.fieldMessageId().value(),
      c.fieldCorrelationId().value(),
      fromWireUser(c.fieldUser())
    );
  }

  private static IdUResponseUserUpdate fromWireResponseUserUpdate(
    final IdU1ResponseUserUpdate c)
    throws IdProtocolException, IdPasswordException
  {
    return new IdUResponseUserUpdate(
      c.fieldMessageId().value(),
      c.fieldCorrelationId().value(),
      fromWireUser(c.fieldUser())
    );
  }
}
