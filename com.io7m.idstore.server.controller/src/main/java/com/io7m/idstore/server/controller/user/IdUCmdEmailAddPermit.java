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


package com.io7m.idstore.server.controller.user;

import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddPermit;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddPermit;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.server.security.IdSecUserActionEmailRemovePermit;

import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_FAILED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_NONEXISTENT;
import static com.io7m.idstore.model.IdEmailVerificationOperation.EMAIL_ADD;
import static com.io7m.idstore.model.IdEmailVerificationResolution.PERMITTED;
import static com.io7m.idstore.strings.IdStringConstants.NOT_FOUND;
import static com.io7m.idstore.strings.IdStringConstants.OPERATION_NOT_PERMITTED;

/**
 * IdUCmdEmailAddPermit
 */

public final class IdUCmdEmailAddPermit
  extends IdUCmdAbstract<IdUCommandEmailAddPermit>
{
  /**
   * IdUCmdEmailAddPermit
   */

  public IdUCmdEmailAddPermit()
  {

  }

  @Override
  protected IdUResponseType executeActual(
    final IdUCommandContext context,
    final IdUCommandEmailAddPermit command)
    throws IdException
  {
    final var token = command.token();
    final var user = context.user();
    context.securityCheck(new IdSecUserActionEmailRemovePermit(user));

    final var transaction =
      context.transaction();
    final var emails =
      transaction.queries(IdDatabaseEmailsQueriesType.class);
    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    final var verificationOpt =
      emails.emailVerificationGetPermit(token);

    if (verificationOpt.isEmpty()) {
      throw context.failFormatted(
        command,
        404,
        EMAIL_VERIFICATION_NONEXISTENT,
        NOT_FOUND
      );
    }

    final var verification = verificationOpt.get();
    if (!checkVerification(context, verification, user)) {
      throw context.failFormatted(
        command,
        400,
        EMAIL_VERIFICATION_FAILED,
        OPERATION_NOT_PERMITTED
      );
    }

    transaction.userIdSet(user.id());
    users.userEmailAdd(user.id(), verification.email());
    emails.emailVerificationDelete(token, PERMITTED);

    return new IdUResponseEmailAddPermit(
      UUID.randomUUID(),
      command.messageId()
    );
  }

  private static boolean checkVerification(
    final IdUCommandContext context,
    final IdEmailVerification verification,
    final IdUser user)
  {
    final var verificationUser = verification.user();
    if (!Objects.equals(verificationUser, user.id())) {
      return false;
    }

    final var expires = verification.expires();
    if (context.now().isAfter(expires)) {
      return false;
    }

    final var operation = verification.operation();
    if (operation != EMAIL_ADD) {
      return false;
    }

    return true;
  }
}
