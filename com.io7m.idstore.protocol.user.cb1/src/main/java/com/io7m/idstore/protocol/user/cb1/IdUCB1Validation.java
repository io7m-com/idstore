/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.idstore.protocol.user.cb1;

import com.io7m.cedarbridge.runtime.api.CBString;
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
import com.io7m.idstore.protocol.user.IdUCommandRealnameUpdate;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUCommandUserSelf;
import com.io7m.idstore.protocol.user.IdUMessageType;
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

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.protocol.user.cb1.internal.IdUCB1ValidationGeneral.fromWireUUID;
import static com.io7m.idstore.protocol.user.cb1.internal.IdUCB1ValidationGeneral.fromWireUser;
import static com.io7m.idstore.protocol.user.cb1.internal.IdUCB1ValidationGeneral.toWireUUID;
import static com.io7m.idstore.protocol.user.cb1.internal.IdUCB1ValidationGeneral.toWireUser;

/**
 * Functions to translate between the core command set and the User v1
 * Cedarbridge encoding command set.
 */

public final class IdUCB1Validation
  implements IdProtocolMessageValidatorType<IdUMessageType, ProtocolIdU1v1Type>
{
  /**
   * Functions to translate between the core command set and the User v1
   * Cedarbridge encoding command set.
   */

  public IdUCB1Validation()
  {

  }

  private static ProtocolIdU1v1Type toWireResponse(
    final IdUResponseType response)
    throws IdProtocolException
  {
    if (response instanceof IdUResponseError c) {
      return toWireResponseError(c);
    } else if (response instanceof IdUResponseLogin c) {
      return toWireResponseLogin(c);
    } else if (response instanceof IdUResponseUserSelf c) {
      return toWireResponseUserSelf(c);
    } else if (response instanceof IdUResponseUserUpdate c) {
      return toWireResponseUserUpdate(c);
    } else if (response instanceof IdUResponseEmailRemovePermit c) {
      return toWireResponseEmailRemovePermit(c);
    } else if (response instanceof IdUResponseEmailRemoveDeny c) {
      return toWireResponseEmailRemoveDeny(c);
    } else if (response instanceof IdUResponseEmailRemoveBegin c) {
      return toWireResponseEmailRemoveBegin(c);
    } else if (response instanceof IdUResponseEmailAddPermit c) {
      return toWireResponseEmailAddPermit(c);
    } else if (response instanceof IdUResponseEmailAddDeny c) {
      return toWireResponseEmailAddDeny(c);
    } else if (response instanceof IdUResponseEmailAddBegin c) {
      return toWireResponseEmailAddBegin(c);
    }

    throw new IdProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(response)
    );
  }

  private static IdU1ResponseEmailRemovePermit toWireResponseEmailRemovePermit(
    final IdUResponseEmailRemovePermit c)
  {
    return new IdU1ResponseEmailRemovePermit(toWireUUID(c.requestId()));
  }

  private static IdU1ResponseEmailRemoveDeny toWireResponseEmailRemoveDeny(
    final IdUResponseEmailRemoveDeny c)
  {
    return new IdU1ResponseEmailRemoveDeny(toWireUUID(c.requestId()));
  }

  private static IdU1ResponseEmailRemoveBegin toWireResponseEmailRemoveBegin(
    final IdUResponseEmailRemoveBegin c)
  {
    return new IdU1ResponseEmailRemoveBegin(toWireUUID(c.requestId()));
  }

  private static IdU1ResponseEmailAddPermit toWireResponseEmailAddPermit(
    final IdUResponseEmailAddPermit c)
  {
    return new IdU1ResponseEmailAddPermit(toWireUUID(c.requestId()));
  }

  private static IdU1ResponseEmailAddDeny toWireResponseEmailAddDeny(
    final IdUResponseEmailAddDeny c)
  {
    return new IdU1ResponseEmailAddDeny(toWireUUID(c.requestId()));
  }

  private static IdU1ResponseEmailAddBegin toWireResponseEmailAddBegin(
    final IdUResponseEmailAddBegin c)
  {
    return new IdU1ResponseEmailAddBegin(toWireUUID(c.requestId()));
  }

  private static IdU1ResponseUserSelf toWireResponseUserSelf(
    final IdUResponseUserSelf c)
  {
    return new IdU1ResponseUserSelf(
      toWireUUID(c.requestId()),
      toWireUser(c.user())
    );
  }

  private static IdU1ResponseUserUpdate toWireResponseUserUpdate(
    final IdUResponseUserUpdate c)
  {
    return new IdU1ResponseUserUpdate(
      toWireUUID(c.requestId()),
      toWireUser(c.user())
    );
  }

  private static IdU1ResponseError toWireResponseError(
    final IdUResponseError error)
  {
    return new IdU1ResponseError(
      toWireUUID(error.requestId()),
      new CBString(error.errorCode()),
      new CBString(error.message())
    );
  }

  private static IdU1ResponseLogin toWireResponseLogin(
    final IdUResponseLogin login)
  {
    return new IdU1ResponseLogin(toWireUUID(login.requestId()));
  }

  private static ProtocolIdU1v1Type convertToWireCommand(
    final IdUCommandType<?> command)
    throws IdProtocolException
  {
    if (command instanceof IdUCommandLogin c) {
      return toWireCommandLogin(c);
    } else if (command instanceof IdUCommandUserSelf c) {
      return toWireCommandUserSelf(c);
    } else if (command instanceof IdUCommandEmailAddBegin c) {
      return toWireCommandEmailAddBegin(c);
    } else if (command instanceof IdUCommandEmailAddPermit c) {
      return toWireCommandEmailAddPermit(c);
    } else if (command instanceof IdUCommandEmailAddDeny c) {
      return toWireCommandEmailAddDeny(c);
    } else if (command instanceof IdUCommandEmailRemoveBegin c) {
      return toWireCommandEmailRemoveBegin(c);
    } else if (command instanceof IdUCommandEmailRemovePermit c) {
      return toWireCommandEmailRemovePermit(c);
    } else if (command instanceof IdUCommandEmailRemoveDeny c) {
      return toWireCommandEmailRemoveDeny(c);
    } else if (command instanceof IdUCommandRealnameUpdate c) {
      return toWireCommandRealnameUpdate(c);
    }

    throw new IdProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(command)
    );
  }

  private static IdU1CommandRealnameUpdate toWireCommandRealnameUpdate(
    final IdUCommandRealnameUpdate c)
  {
    return new IdU1CommandRealnameUpdate(new CBString(c.realName().value()));
  }

  private static IdU1CommandEmailAddBegin toWireCommandEmailAddBegin(
    final IdUCommandEmailAddBegin c)
  {
    return new IdU1CommandEmailAddBegin(new CBString(c.email().value()));
  }

  private static IdU1CommandEmailAddPermit toWireCommandEmailAddPermit(
    final IdUCommandEmailAddPermit c)
  {
    return new IdU1CommandEmailAddPermit(new CBString(c.token().value()));
  }

  private static IdU1CommandEmailAddDeny toWireCommandEmailAddDeny(
    final IdUCommandEmailAddDeny c)
  {
    return new IdU1CommandEmailAddDeny(new CBString(c.token().value()));
  }

  private static IdU1CommandEmailRemoveBegin toWireCommandEmailRemoveBegin(
    final IdUCommandEmailRemoveBegin c)
  {
    return new IdU1CommandEmailRemoveBegin(new CBString(c.email().value()));
  }

  private static IdU1CommandEmailRemovePermit toWireCommandEmailRemovePermit(
    final IdUCommandEmailRemovePermit c)
  {
    return new IdU1CommandEmailRemovePermit(new CBString(c.token().value()));
  }

  private static IdU1CommandEmailRemoveDeny toWireCommandEmailRemoveDeny(
    final IdUCommandEmailRemoveDeny c)
  {
    return new IdU1CommandEmailRemoveDeny(new CBString(c.token().value()));
  }

  private static IdU1CommandUserSelf toWireCommandUserSelf(
    final IdUCommandUserSelf c)
  {
    return new IdU1CommandUserSelf();
  }

  private static IdU1CommandLogin toWireCommandLogin(
    final IdUCommandLogin login)
  {
    return new IdU1CommandLogin(
      new CBString(login.userName().value()),
      new CBString(login.password())
    );
  }

  private static IdUResponseError fromWireResponseError(
    final IdU1ResponseError error)
  {
    return new IdUResponseError(
      fromWireUUID(error.fieldRequestId()),
      error.fieldErrorCode().value(),
      error.fieldMessage().value()
    );
  }

  private static IdUResponseLogin fromWireResponseLogin(
    final IdU1ResponseLogin login)
  {
    return new IdUResponseLogin(fromWireUUID(login.fieldRequestId()));
  }

  private static IdUCommandLogin fromWireCommandLogin(
    final IdU1CommandLogin login)
  {
    return new IdUCommandLogin(
      new IdName(login.fieldUserName().value()),
      login.fieldPassword().value()
    );
  }

  @Override
  public ProtocolIdU1v1Type convertToWire(
    final IdUMessageType message)
    throws IdProtocolException
  {
    if (message instanceof IdUCommandType<?> command) {
      return convertToWireCommand(command);
    } else if (message instanceof IdUResponseType response) {
      return toWireResponse(response);
    } else {
      throw new IdProtocolException(
        PROTOCOL_ERROR,
        "Unrecognized message: %s".formatted(message)
      );
    }
  }

  @Override
  public IdUMessageType convertFromWire(
    final ProtocolIdU1v1Type message)
    throws IdProtocolException
  {
    try {

      /*
       * Commands.
       */

      if (message instanceof IdU1CommandLogin c) {
        return fromWireCommandLogin(c);
      } else if (message instanceof IdU1CommandEmailAddBegin c) {
        return fromWireCommandEmailAddBegin(c);
      } else if (message instanceof IdU1CommandEmailAddPermit c) {
        return fromWireCommandEmailAddPermit(c);
      } else if (message instanceof IdU1CommandEmailAddDeny c) {
        return fromWireCommandEmailAddDeny(c);
      } else if (message instanceof IdU1CommandEmailRemoveBegin c) {
        return fromWireCommandEmailRemoveBegin(c);
      } else if (message instanceof IdU1CommandEmailRemovePermit c) {
        return fromWireCommandEmailRemovePermit(c);
      } else if (message instanceof IdU1CommandEmailRemoveDeny c) {
        return fromWireCommandEmailRemoveDeny(c);
      } else if (message instanceof IdU1CommandUserSelf c) {
        return fromWireCommandUserSelf(c);
      } else if (message instanceof IdU1CommandRealnameUpdate c) {
        return fromWireCommandRealnameUpdate(c);

        /*
         * Responses.
         */

      } else if (message instanceof IdU1ResponseLogin c) {
        return fromWireResponseLogin(c);
      } else if (message instanceof IdU1ResponseError c) {
        return fromWireResponseError(c);
      } else if (message instanceof IdU1ResponseUserSelf c) {
        return fromWireResponseUserSelf(c);
      } else if (message instanceof IdU1ResponseUserUpdate c) {
        return fromWireResponseUserUpdate(c);
      } else if (message instanceof IdU1ResponseEmailAddBegin c) {
        return fromWireResponseEmailAddBegin(c);
      } else if (message instanceof IdU1ResponseEmailAddPermit c) {
        return fromWireResponseEmailAddPermit(c);
      } else if (message instanceof IdU1ResponseEmailAddDeny c) {
        return fromWireResponseEmailAddDeny(c);
      } else if (message instanceof IdU1ResponseEmailRemoveBegin c) {
        return fromWireResponseEmailRemoveBegin(c);
      } else if (message instanceof IdU1ResponseEmailRemovePermit c) {
        return fromWireResponseEmailRemovePermit(c);
      } else if (message instanceof IdU1ResponseEmailRemoveDeny c) {
        return fromWireResponseEmailRemoveDeny(c);
      }
    } catch (final Exception e) {
      throw new IdProtocolException(PROTOCOL_ERROR, e.getMessage(), e);
    }

    throw new IdProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(message)
    );
  }

  private static IdUCommandRealnameUpdate fromWireCommandRealnameUpdate(
    final IdU1CommandRealnameUpdate c)
  {
    return new IdUCommandRealnameUpdate(new IdRealName(c.fieldName().value()));
  }

  private static IdUCommandEmailAddBegin fromWireCommandEmailAddBegin(
    final IdU1CommandEmailAddBegin c)
  {
    return new IdUCommandEmailAddBegin(new IdEmail(c.fieldEmail().value()));
  }

  private static IdUCommandEmailAddPermit fromWireCommandEmailAddPermit(
    final IdU1CommandEmailAddPermit c)
  {
    return new IdUCommandEmailAddPermit(new IdToken(c.fieldToken().value()));
  }

  private static IdUCommandEmailAddDeny fromWireCommandEmailAddDeny(
    final IdU1CommandEmailAddDeny c)
  {
    return new IdUCommandEmailAddDeny(new IdToken(c.fieldToken().value()));
  }

  private static IdUCommandEmailRemoveBegin fromWireCommandEmailRemoveBegin(
    final IdU1CommandEmailRemoveBegin c)
  {
    return new IdUCommandEmailRemoveBegin(new IdEmail(c.fieldEmail().value()));
  }

  private static IdUCommandEmailRemovePermit fromWireCommandEmailRemovePermit(
    final IdU1CommandEmailRemovePermit c)
  {
    return new IdUCommandEmailRemovePermit(new IdToken(c.fieldToken().value()));
  }

  private static IdUCommandEmailRemoveDeny fromWireCommandEmailRemoveDeny(
    final IdU1CommandEmailRemoveDeny c)
  {
    return new IdUCommandEmailRemoveDeny(new IdToken(c.fieldToken().value()));
  }

  private static IdUResponseEmailAddBegin fromWireResponseEmailAddBegin(
    final IdU1ResponseEmailAddBegin c)
  {
    return new IdUResponseEmailAddBegin(fromWireUUID(c.fieldRequestId()));
  }

  private static IdUResponseEmailAddPermit fromWireResponseEmailAddPermit(
    final IdU1ResponseEmailAddPermit c)
  {
    return new IdUResponseEmailAddPermit(fromWireUUID(c.fieldRequestId()));
  }

  private static IdUResponseEmailAddDeny fromWireResponseEmailAddDeny(
    final IdU1ResponseEmailAddDeny c)
  {
    return new IdUResponseEmailAddDeny(fromWireUUID(c.fieldRequestId()));
  }

  private static IdUResponseEmailRemoveBegin fromWireResponseEmailRemoveBegin(
    final IdU1ResponseEmailRemoveBegin c)
  {
    return new IdUResponseEmailRemoveBegin(fromWireUUID(c.fieldRequestId()));
  }

  private static IdUResponseEmailRemovePermit fromWireResponseEmailRemovePermit(
    final IdU1ResponseEmailRemovePermit c)
  {
    return new IdUResponseEmailRemovePermit(fromWireUUID(c.fieldRequestId()));
  }

  private static IdUResponseEmailRemoveDeny fromWireResponseEmailRemoveDeny(
    final IdU1ResponseEmailRemoveDeny c)
  {
    return new IdUResponseEmailRemoveDeny(fromWireUUID(c.fieldRequestId()));
  }

  private static IdUCommandUserSelf fromWireCommandUserSelf(
    final IdU1CommandUserSelf c)
  {
    return new IdUCommandUserSelf();
  }

  private static IdUResponseUserSelf fromWireResponseUserSelf(
    final IdU1ResponseUserSelf c)
    throws IdProtocolException, IdPasswordException
  {
    return new IdUResponseUserSelf(
      fromWireUUID(c.fieldRequestId()),
      fromWireUser(c.fieldUser())
    );
  }

  private static IdUResponseUserUpdate fromWireResponseUserUpdate(
    final IdU1ResponseUserUpdate c)
    throws IdProtocolException, IdPasswordException
  {
    return new IdUResponseUserUpdate(
      fromWireUUID(c.fieldRequestId()),
      fromWireUser(c.fieldUser())
    );
  }
}
