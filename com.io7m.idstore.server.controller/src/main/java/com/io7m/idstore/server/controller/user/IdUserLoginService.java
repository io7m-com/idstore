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
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitUserLoginServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionUserService;
import com.io7m.idstore.server.service.telemetry.api.IdEventServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdEventUserLoggedIn;
import com.io7m.idstore.server.service.telemetry.api.IdEventUserLoginAuthenticationFailed;
import com.io7m.idstore.server.service.telemetry.api.IdEventUserLoginRateLimitExceeded;
import com.io7m.idstore.strings.IdStringConstants;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.repetoir.core.RPServiceType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.BANNED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.RATE_LIMIT_EXCEEDED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.idstore.strings.IdStringConstants.BANNED_NO_EXPIRE;
import static com.io7m.idstore.strings.IdStringConstants.ERROR_INVALID_USERNAME_PASSWORD;
import static com.io7m.idstore.strings.IdStringConstants.LOGIN_RATE_LIMITED;
import static com.io7m.idstore.strings.IdStringConstants.WAIT_DURATION;

/**
 * A service that handles the logic for user logins.
 */

public final class IdUserLoginService implements RPServiceType
{
  private final IdServerClock clock;
  private final IdStrings strings;
  private final IdSessionUserService sessions;
  private final IdServerConfigurationService configurations;
  private final IdRateLimitUserLoginServiceType rateLimit;
  private final IdEventServiceType events;

  /**
   * A service that handles the logic for user logins.
   *
   * @param inClock          The clock
   * @param inStrings        The string resources
   * @param inSessions       A session service
   * @param inConfigurations A configuration service
   * @param inRateLimit      The rate limit
   * @param inEvents         The event service
   */

  public IdUserLoginService(
    final IdServerClock inClock,
    final IdStrings inStrings,
    final IdSessionUserService inSessions,
    final IdServerConfigurationService inConfigurations,
    final IdRateLimitUserLoginServiceType inRateLimit,
    final IdEventServiceType inEvents)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.sessions =
      Objects.requireNonNull(inSessions, "inSessions");
    this.configurations =
      Objects.requireNonNull(inConfigurations, "inConfigurations");
    this.rateLimit =
      Objects.requireNonNull(inRateLimit, "inRateLimit");
    this.events =
      Objects.requireNonNull(inEvents, "inEvents");
  }

  /**
   * Try logging in. Create a new session if logging in succeeds, or raise an
   * exception if the login cannot proceed for any reason (invalid credentials,
   * banned user, etc).
   *
   * @param transaction A database transaction
   * @param messageId   The ID of the request
   * @param remoteHost  The remote remoteHost
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
    final UUID messageId,
    final String remoteHost,
    final String username,
    final String password,
    final Map<String, String> metadata)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(transaction, "transaction");
    Objects.requireNonNull(messageId, "messageId");
    Objects.requireNonNull(username, "username");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(metadata, "metadata");

    try {
      this.checkRateLimit(messageId, remoteHost, username);

      final var users =
        transaction.queries(IdDatabaseUsersQueriesType.class);
      final var user =
        users.userGetForNameRequire(new IdName(username));

      this.checkBan(messageId, users, user);
      this.checkPassword(messageId, remoteHost, password, user);

      users.userLogin(
        user.id(),
        metadata,
        this.configurations.configuration()
          .history()
          .userLoginHistoryLimit()
      );

      this.events.emit(new IdEventUserLoggedIn(user.id()));

      final var session = this.sessions.createSession(user.id());
      return new IdUserLoggedIn(session, user.withRedactedPassword());
    } catch (final IdDatabaseException e) {
      if (Objects.equals(e.errorCode(), USER_NONEXISTENT)) {
        throw this.authenticationFailed(messageId, e);
      }
      throw new IdCommandExecutionFailure(
        e.getMessage(),
        e,
        e.errorCode(),
        e.attributes(),
        e.remediatingAction(),
        messageId,
        500
      );
    } catch (final IdPasswordException e) {
      throw new IdCommandExecutionFailure(
        e.getMessage(),
        e,
        e.errorCode(),
        e.attributes(),
        e.remediatingAction(),
        messageId,
        500
      );
    }
  }

  private void checkPassword(
    final UUID requestId,
    final String remoteHost,
    final String password,
    final IdUser user)
    throws IdPasswordException, IdCommandExecutionFailure
  {
    final var ok = user.password().check(this.clock.clock(), password);
    if (!ok) {
      this.events.emit(
        new IdEventUserLoginAuthenticationFailed(remoteHost, user.id())
      );

      throw new IdCommandExecutionFailure(
        this.strings.format(ERROR_INVALID_USERNAME_PASSWORD),
        AUTHENTICATION_ERROR,
        Map.of(),
        Optional.empty(),
        requestId,
        401
      );
    }
  }

  private void checkRateLimit(
    final UUID requestId,
    final String remoteHost,
    final String username)
    throws IdCommandExecutionFailure
  {
    if (!this.rateLimit.isAllowedByRateLimit(remoteHost)) {
      this.events.emit(
        new IdEventUserLoginRateLimitExceeded(remoteHost, username)
      );

      throw new IdCommandExecutionFailure(
        this.strings.format(LOGIN_RATE_LIMITED),
        RATE_LIMIT_EXCEEDED,
        Map.of(
          this.strings.format(WAIT_DURATION),
          this.rateLimit.waitTime().toString()
        ),
        Optional.empty(),
        requestId,
        400
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
        this.strings.format(BANNED_NO_EXPIRE, ban.reason()),
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
        this.strings.format(IdStringConstants.BANNED, ban.reason(), timeExpires),
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
      this.strings.format(ERROR_INVALID_USERNAME_PASSWORD),
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

  @Override
  public String toString()
  {
    return "[IdUserLoginService 0x%s]"
      .formatted(Integer.toUnsignedString(this.hashCode(), 16));
  }
}
