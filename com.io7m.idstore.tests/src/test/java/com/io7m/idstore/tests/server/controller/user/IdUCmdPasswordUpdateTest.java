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


package com.io7m.idstore.tests.server.controller.user;

import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.IdUCommandPasswordUpdate;
import com.io7m.idstore.protocol.user.IdUResponseUserUpdate;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCmdPasswordUpdate;
import com.io7m.junreachable.UnreachableCodeException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_RESET_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdUCmdPasswordUpdateTest
  extends IdUCmdAbstractContract
{
  /**
   * Users can always update their own passwords.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdate()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser("user0");
    final var context =
      this.createContextAndSession(user0);

    final var newPassword =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("abcd");

    final var user0After =
      new IdUser(
        user0.id(),
        user0.idName(),
        user0.realName(),
        user0.emails(),
        user0.timeCreated(),
        user0.timeUpdated(),
        newPassword
      );

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    when(users.userGetRequire(any()))
      .thenReturn(user0After);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseUsersQueriesType.class))
      .thenReturn(users);

    /* Act. */

    final var handler =
      new IdUCmdPasswordUpdate();
    final var response =
      handler.execute(
        context,
        new IdUCommandPasswordUpdate("abcd", "abcd")
      );

    /* Assert. */

    assertEquals(
      new IdUResponseUserUpdate(context.requestId(), user0After),
      response
    );

    verify(users, this.once())
      .userGetRequire(user0.id());
    verify(users, this.once())
      .userUpdate(
        eq(user0After.id()),
        eq(Optional.empty()),
        eq(Optional.empty()),
        argThat(argument -> {
          try {
            return argument.orElseThrow().check("abcd");
          } catch (final IdPasswordException e) {
            throw new UnreachableCodeException(e);
          }
        })
      );

    verify(transaction)
      .queries(IdDatabaseUsersQueriesType.class);
    verify(transaction)
      .userIdSet(user0.id());

    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Passwords and password confirmations must match.
   *
   * @throws Exception On errors
   */

  @Test
  public void testPasswordConfirmationMismatch()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser("user0");
    final var context =
      this.createContextAndSession(user0);

    final var users =
      mock(IdDatabaseUsersQueriesType.class);

    final var transaction =
      this.transaction();

    /* Act. */

    final var handler = new IdUCmdPasswordUpdate();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new IdUCommandPasswordUpdate("abcd", "wrong")
        );
      });

    /* Assert. */

    assertEquals(PASSWORD_RESET_MISMATCH, ex.errorCode());

    verifyNoMoreInteractions(users);
    verifyNoMoreInteractions(transaction);
  }
}
