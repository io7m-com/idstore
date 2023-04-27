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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.sessions.IdSessionUserService;
import com.io7m.repetoir.core.RPServiceType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.BANNED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;

/**
 * A service that handles the logic for user logins.
 */

public final class IdUserLoginService implements RPServiceType
{
  private final IdServerClock clock;
  private final IdServerStrings strings;
  private final IdSessionUserService sessions;
  private final IdServerConfigurationService configurations;

  /**
   * A service that handles the logic for user logins.
   *
   * @param inClock          The clock
   * @param inStrings        The string resources
   * @param inSessions       A session service
   * @param inConfigurations A configuration service
   */

  public IdUserLoginService(
    final IdServerClock inClock,
    final IdServerStrings inStrings,
    final IdSessionUserService inSessions,
    final IdServerConfigurationService inConfigurations)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.sessions =
      Objects.requireNonNull(inSessions, "inSessions");
    this.configurations =
      Objects.requireNonNull(inConfigurations, "inConfigurations");
  }

  /**
   * A record of a user logging in.
   *
   * @param session The created session
   * @param user    The user
   */

  public record IdUserLoggedIn(
    IdSessionUser session,
    IdUser user)
  {
    /**
     * A record of a user logging in.
     */

    public IdUserLoggedIn
    {
      Objects.requireNonNull(session, "session");
      Objects.requireNonNull(user, "user");
    }
  }

  /**
   * Try logging in. Create a new session if logging in succeeds, or raise an
   * exception if the login cannot proceed for any reason (invalid credentials,
   * banned user, etc).
   *
   * @param transaction A database transaction
   * @param requestId   The ID of the request
   * @param username    The username
   * @param password    The password
   * @param metadata    The request metadata
   *
   * @return A login record
   *
   * @throws IdCommandExecutionFailure On errors
   */

  public IdUserLoggedIn userLogin(
    final IdDatabaseTransactionType transaction,
    final UUID requestId,
    final String username,
    final String password,
    final Map<String, String> metadata)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(transaction, "transaction");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(username, "username");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(metadata, "metadata");

    try {
      final var users =
        transaction.queries(IdDatabaseUsersQueriesType.class);
      final var user =
        users.userGetForNameRequire(new IdName(username));

      this.checkBan(requestId, users, user);

      final var ok =
        user.password().check(password);

      if (!ok) {
        throw new IdCommandExecutionFailure(
          this.strings.format("errorInvalidUsernamePassword"),
          AUTHENTICATION_ERROR,
          Map.of(),
          Optional.empty(),
          requestId,
          401
        );
      }

      users.userLogin(
        user.id(),
        metadata,
        this.configurations.configuration()
          .history()
          .userLoginHistoryLimit()
      );

      final var session = this.sessions.createSession(user.id());
      return new IdUserLoggedIn(session, user.withRedactedPassword());
    } catch (final IdDatabaseException e) {
      if (Objects.equals(e.errorCode(), USER_NONEXISTENT)) {
        throw this.authenticationFailed(requestId, e);
      }
      throw new IdCommandExecutionFailure(
        e.getMessage(),
        e,
        e.errorCode(),
        e.attributes(),
        e.remediatingAction(),
        requestId,
        500
      );
    } catch (final IdPasswordException e) {
      throw new IdCommandExecutionFailure(
        e.getMessage(),
        e,
        e.errorCode(),
        e.attributes(),
        e.remediatingAction(),
        requestId,
        500
      );
    }
  }

  private void checkBan(
    final UUID requestId,
    final IdDatabaseUsersQueriesType users,
    final IdUser user)
    throws IdDatabaseException, IdCommandExecutionFailure
  {
    final var banOpt =
      users.userBanGet(user.id());

    /*
     * If there's no ban, allow the login.
     */

    if (banOpt.isEmpty()) {
      return;
    }

    final var ban = banOpt.get();
    final var expiresOpt = ban.expires();

    /*
     * If there's no expiration on the ban, deny the login.
     */

    if (expiresOpt.isEmpty()) {
      throw new IdCommandExecutionFailure(
        this.strings.format("bannedNoExpire", ban.reason()),
        BANNED,
        Map.of(),
        Optional.empty(),
        requestId,
        403
      );
    }

    /*
     * If the current time is before the expiration date, deny the login.
     */

    final var timeExpires = expiresOpt.get();
    final var timeNow = this.clock.now();

    if (timeNow.compareTo(timeExpires) < 0) {
      throw new IdCommandExecutionFailure(
        this.strings.format("banned", ban.reason(), timeExpires),
        BANNED,
        Map.of(),
        Optional.empty(),
        requestId,
        403
      );
    }
  }

  private IdCommandExecutionFailure authenticationFailed(
    final UUID requestId,
    final Exception cause)
  {
    return new IdCommandExecutionFailure(
      this.strings.format("errorInvalidUsernamePassword"),
      cause,
      AUTHENTICATION_ERROR,
      Map.of(),
      Optional.empty(),
      requestId,
      401
    );
  }

  @Override
  public String description()
  {
    return "User login service.";
  }
}
