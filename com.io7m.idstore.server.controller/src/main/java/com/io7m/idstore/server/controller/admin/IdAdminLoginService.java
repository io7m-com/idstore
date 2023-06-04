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

package com.io7m.idstore.server.controller.admin;

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.events.IdEventAdminLoggedIn;
import com.io7m.idstore.server.service.events.IdEventAdminLoginAuthenticationFailed;
import com.io7m.idstore.server.service.events.IdEventAdminLoginRateLimitExceeded;
import com.io7m.idstore.server.service.events.IdEventServiceType;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitAdminLoginServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionAdminService;
import com.io7m.repetoir.core.RPServiceType;
import com.io7m.seltzer.api.SStructuredErrorType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.BANNED;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.RATE_LIMIT_EXCEEDED;

/**
 * A service that handles the logic for admin logins.
 */

public final class IdAdminLoginService implements RPServiceType
{
  private final IdServerClock clock;
  private final IdServerStrings strings;
  private final IdSessionAdminService sessions;
  private final IdEventServiceType events;
  private final IdRateLimitAdminLoginServiceType rateLimit;

  /**
   * A service that handles the logic for admin logins.
   *
   * @param inClock          The clock
   * @param inStrings        The string resources
   * @param inSessions       A session service
   * @param inRateLimit      The rate limit service
   * @param inEvents         The event service
   */

  public IdAdminLoginService(
    final IdServerClock inClock,
    final IdServerStrings inStrings,
    final IdSessionAdminService inSessions,
    final IdRateLimitAdminLoginServiceType inRateLimit,
    final IdEventServiceType inEvents)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.sessions =
      Objects.requireNonNull(inSessions, "inSessions");
    this.events =
      Objects.requireNonNull(inEvents, "inEvents");
    this.rateLimit =
      Objects.requireNonNull(inRateLimit, "inRateLimit");
  }

  /**
   * Try logging in. Create a new session if logging in succeeds, or raise an
   * exception if the login cannot proceed for any reason (invalid credentials,
   * banned user, etc).
   *
   * @param transaction A database transaction
   * @param requestId   The ID of the request
   * @param remoteHost  The remote remoteHost attempting to log in
   * @param username    The username
   * @param password    The password
   * @param metadata    The request metadata
   *
   * @return A login record
   *
   * @throws IdCommandExecutionFailure On errors
   */

  public IdAdminLoggedIn adminLogin(
    final IdDatabaseTransactionType transaction,
    final UUID requestId,
    final String remoteHost,
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
      this.checkRateLimit(requestId, remoteHost, username);

      final var admins =
        transaction.queries(IdDatabaseAdminsQueriesType.class);
      final var user =
        admins.adminGetForNameRequire(new IdName(username));

      this.checkBan(requestId, admins, user);
      this.checkPassword(requestId, remoteHost, password, user);

      admins.adminLogin(user.id(), metadata);
      this.events.emit(new IdEventAdminLoggedIn(user.id()));

      final var session = this.sessions.createSession(user.id());
      return new IdAdminLoggedIn(session, user.withRedactedPassword());
    } catch (final IdDatabaseException e) {
      if (Objects.equals(e.errorCode(), ADMIN_NONEXISTENT)) {
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

  private void checkRateLimit(
    final UUID requestId,
    final String remoteHost,
    final String username)
    throws IdCommandExecutionFailure
  {
    if (!this.rateLimit.isAllowedByRateLimit(remoteHost)) {
      this.events.emit(
        new IdEventAdminLoginRateLimitExceeded(remoteHost, username)
      );

      throw new IdCommandExecutionFailure(
        this.strings.format("loginRateLimited"),
        RATE_LIMIT_EXCEEDED,
        Map.of(),
        Optional.empty(),
        requestId,
        400
      );
    }
  }

  private void checkPassword(
    final UUID requestId,
    final String remoteHost,
    final String password,
    final IdAdmin user)
    throws IdPasswordException, IdCommandExecutionFailure
  {
    final var ok =
      user.password()
        .check(this.clock.clock(), password);

    if (!ok) {
      this.events.emit(
        new IdEventAdminLoginAuthenticationFailed(remoteHost, user.id())
      );

      throw new IdCommandExecutionFailure(
        this.strings.format("errorInvalidUsernamePassword"),
        AUTHENTICATION_ERROR,
        Map.of(),
        Optional.empty(),
        requestId,
        401
      );
    }
  }

  private void checkBan(
    final UUID requestId,
    final IdDatabaseAdminsQueriesType admins,
    final IdAdmin user)
    throws IdDatabaseException, IdCommandExecutionFailure
  {
    final var banOpt =
      admins.adminBanGet(user.id());

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
    if (cause instanceof final SStructuredErrorType<?> struct) {
      return new IdCommandExecutionFailure(
        this.strings.format("errorInvalidUsernamePassword"),
        cause,
        AUTHENTICATION_ERROR,
        struct.attributes(),
        struct.remediatingAction(),
        requestId,
        401
      );
    }
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
    return "Admin login service.";
  }

  @Override
  public String toString()
  {
    return "[IdAdminLoginService 0x%s]"
      .formatted(Integer.toUnsignedString(this.hashCode(), 16));
  }
}
