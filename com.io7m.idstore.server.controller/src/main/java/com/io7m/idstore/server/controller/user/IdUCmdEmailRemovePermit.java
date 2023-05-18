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
import com.io7m.idstore.model.IdEmailVerificationOperation;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemovePermit;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemovePermit;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.server.security.IdSecUserActionEmailRemovePermit;

import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_FAILED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_NONEXISTENT;
import static com.io7m.idstore.model.IdEmailVerificationResolution.PERMITTED;

/**
 * IdUCmdEmailRemovePermit
 */

public final class IdUCmdEmailRemovePermit
  extends IdUCmdAbstract<IdUCommandEmailRemovePermit>
{
  /**
   * IdUCmdEmailRemovePermit
   */

  public IdUCmdEmailRemovePermit()
  {

  }

  @Override
  protected IdUResponseType executeActual(
    final IdUCommandContext context,
    final IdUCommandEmailRemovePermit command)
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
      emails.emailVerificationGet(token);

    if (verificationOpt.isEmpty()) {
      throw context.failFormatted(
        404,
        EMAIL_VERIFICATION_NONEXISTENT,
        "notFound"
      );
    }

    final var verification = verificationOpt.get();
    if (!checkVerification(context, verification, user)) {
      throw context.failFormatted(
        400,
        EMAIL_VERIFICATION_FAILED,
        "operationNotPermitted"
      );
    }

    if (user.emails().size() == 1) {
      throw context.failFormatted(
        400,
        EMAIL_VERIFICATION_FAILED,
        "emailRemoveLast"
      );
    }

    transaction.userIdSet(user.id());
    users.userEmailRemove(user.id(), verification.email());
    emails.emailVerificationDelete(token, PERMITTED);

    return new IdUResponseEmailRemovePermit(context.requestId());
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
    if (operation != IdEmailVerificationOperation.EMAIL_REMOVE) {
      return false;
    }

    return true;
  }
}
