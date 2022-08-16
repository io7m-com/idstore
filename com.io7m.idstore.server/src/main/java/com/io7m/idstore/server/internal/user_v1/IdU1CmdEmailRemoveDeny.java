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


package com.io7m.idstore.server.internal.user_v1;

import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailRemoveDeny;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseEmailRemoveDeny;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseType;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutorType;
import com.io7m.idstore.server.security.IdSecPolicyResultDenied;
import com.io7m.idstore.server.security.IdSecUserActionEmailAddDeny;
import com.io7m.idstore.server.security.IdSecurity;
import com.io7m.idstore.server.security.IdSecurityException;

import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_FAILED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.model.IdEmailVerificationOperation.EMAIL_REMOVE;
import static com.io7m.idstore.model.IdEmailVerificationResolution.DENIED;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * IdU1CmdEmailRemoveDeny
 */

public final class IdU1CmdEmailRemoveDeny
  implements IdCommandExecutorType<
  IdU1CommandContext, IdU1CommandEmailRemoveDeny, IdU1ResponseType>
{
  /**
   * IdU1CmdEmailRemoveDeny
   */

  public IdU1CmdEmailRemoveDeny()
  {

  }

  @Override
  public IdU1ResponseType execute(
    final IdU1CommandContext context,
    final IdU1CommandEmailRemoveDeny command)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    try {
      final var token =
        new IdToken(command.token());

      final var user = context.user();
      if (IdSecurity.check(new IdSecUserActionEmailAddDeny(user))
        instanceof IdSecPolicyResultDenied denied) {
        throw context.fail(
          FORBIDDEN_403,
          SECURITY_POLICY_DENIED,
          denied.message()
        );
      }

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

      return new IdU1ResponseEmailRemoveDeny(context.requestId());
    } catch (final IdSecurityException e) {
      throw context.failSecurity(e);
    } catch (final IdDatabaseException e) {
      throw context.failDatabase(e);
    }
  }

  private static boolean checkVerification(
    final IdU1CommandContext context,
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
    if (operation != EMAIL_REMOVE) {
      return false;
    }

    return true;
  }
}
