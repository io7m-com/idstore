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
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddDeny;
import com.io7m.idstore.protocol.user.IdUResponseEmailAddDeny;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.server.security.IdSecUserActionEmailAddDeny;

import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_FAILED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_NONEXISTENT;
import static com.io7m.idstore.model.IdEmailVerificationOperation.EMAIL_ADD;
import static com.io7m.idstore.model.IdEmailVerificationResolution.DENIED;

/**
 * IdUCmdEmailAddDeny
 */

public final class IdUCmdEmailAddDeny
  extends IdUCmdAbstract<IdUCommandEmailAddDeny>
{
  /**
   * IdUCmdEmailAddDeny
   */

  public IdUCmdEmailAddDeny()
  {

  }

  @Override
  protected IdUResponseType executeActual(
    final IdUCommandContext context,
    final IdUCommandEmailAddDeny command)
    throws IdException
  {
    final var token = command.token();
    final var user = context.user();
    context.securityCheck(new IdSecUserActionEmailAddDeny(user));

    final var transaction =
      context.transaction();
    final var emails =
      transaction.queries(IdDatabaseEmailsQueriesType.class);

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

    transaction.userIdSet(user.id());
    emails.emailVerificationDelete(token, DENIED);
    return new IdUResponseEmailAddDeny(context.requestId());
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
