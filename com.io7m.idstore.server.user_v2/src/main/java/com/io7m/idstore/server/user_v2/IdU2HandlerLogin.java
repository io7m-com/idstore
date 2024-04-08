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


package com.io7m.idstore.server.user_v2;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.protocol.user.cb.IdUCB2Messages;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUserLoggedIn;
import com.io7m.idstore.server.controller.user.IdUserLoginService;
import com.io7m.idstore.server.http.IdHTTPCookieDeclaration;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctional;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.configuration.IdServerConfigurationService;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimitExceeded;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimits;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.webserver.http.ServerRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.API_MISUSE_ERROR;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHost;
import static com.io7m.idstore.model.IdLoginMetadataStandard.userAgent;
import static com.io7m.idstore.model.IdUserDomain.USER;
import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_CLIENT;
import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_SERVER;
import static com.io7m.idstore.server.http.IdHTTPHandlerCoreFixedDelay.withFixedDelay;
import static com.io7m.idstore.server.http.IdHTTPHandlerCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;
import static com.io7m.idstore.server.user_v2.IdU2HandlerCoreMaintenanceAware.withMaintenanceAwareness;
import static com.io7m.idstore.server.user_v2.IdU2HandlerCoreTransactional.withTransaction;
import static com.io7m.idstore.strings.IdStringConstants.COMMAND_NOT_HERE;

/**
 * The v1 login servlet.
 */

public final class IdU2HandlerLogin extends IdHTTPHandlerFunctional
{
  /**
   * The v1 login servlet.
   *
   * @param services The services
   */

  public IdU2HandlerLogin(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPHandlerFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var limits =
      services.requireService(IdRequestLimits.class);
    final var messages =
      services.requireService(IdUCB2Messages.class);
    final var strings =
      services.requireService(IdStrings.class);
    final var logins =
      services.requireService(IdUserLoginService.class);

    final var configuration =
      services.requireService(IdServerConfigurationService.class);

    final var delay =
      configuration.configuration()
        .rateLimit()
        .userLoginDelay();

    final var sessionDuration =
      configuration.configuration()
        .sessions()
        .userSessionExpiration();

    final var transactional =
      withTransaction(services, (request, info, transaction) -> {
        return execute(
          strings,
          limits,
          messages,
          logins,
          request,
          info,
          transaction,
          sessionDuration
        );
      });

    final var fixedDelay =
      withFixedDelay(services, delay, transactional);

    final var maintenanceAware =
      withMaintenanceAwareness(services, fixedDelay);

    return withInstrumentation(services, USER, maintenanceAware);
  }

  private static IdHTTPResponseType execute(
    final IdStrings strings,
    final IdRequestLimits limits,
    final IdUCB2Messages messages,
    final IdUserLoginService logins,
    final ServerRequest request,
    final IdHTTPRequestInformation information,
    final IdDatabaseTransactionType transaction,
    final Duration sessionDuration)
  {
    final IdUCommandLogin login;
    try {
      login = readLoginCommand(strings, limits, messages, request);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    } catch (final IdException e) {
      setSpanErrorCode(e.errorCode());
      return IdU2Errors.errorResponseOf(messages, information, BLAME_CLIENT, e);
    }

    final var meta = new HashMap<>(login.metadata());
    meta.put(remoteHost(), information.remoteAddress());
    meta.put(userAgent(), information.userAgent());

    final IdUserLoggedIn loggedIn;
    try {
      loggedIn = logins.userLogin(
        transaction,
        information.requestId(),
        information.remoteAddress(),
        login.userName().value(),
        login.password(),
        meta
      );
    } catch (final IdCommandExecutionFailure e) {
      setSpanErrorCode(e.errorCode());
      return IdU2Errors.errorResponseOf(messages, information, e);
    }

    try {
      transaction.commit();
    } catch (final IdDatabaseException e) {
      setSpanErrorCode(e.errorCode());
      return IdU2Errors.errorResponseOf(messages, information, BLAME_SERVER, e);
    }

    final var sessionCookie =
      new IdHTTPCookieDeclaration(
        "IDSTORE_USER_API_SESSION",
        loggedIn.session().id().value(),
        sessionDuration
      );

    return new IdHTTPResponseFixedSize(
      200,
      Set.of(sessionCookie),
      IdUCB2Messages.contentType(),
      messages.serialize(
        new IdUResponseLogin(
          UUID.randomUUID(),
          login.messageId(),
          loggedIn.user().withRedactedPassword()
        )
      )
    );
  }

  private static IdUCommandLogin readLoginCommand(
    final IdStrings strings,
    final IdRequestLimits limits,
    final IdUCB2Messages messages,
    final ServerRequest request)
    throws IOException, IdRequestLimitExceeded, IdProtocolException
  {
    try (var input = limits.boundedMaximumInput(request, 1024)) {
      final var data = input.readAllBytes();
      final var message = messages.parse(data);
      if (message instanceof final IdUCommandLogin login) {
        return login;
      }
    }

    throw new IdProtocolException(
      strings.format(COMMAND_NOT_HERE),
      API_MISUSE_ERROR,
      Map.of(),
      Optional.empty()
    );
  }
}
