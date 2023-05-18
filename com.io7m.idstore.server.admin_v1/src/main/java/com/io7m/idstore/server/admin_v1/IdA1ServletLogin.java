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


package com.io7m.idstore.server.admin_v1;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.protocol.admin.IdACommandLogin;
import com.io7m.idstore.protocol.admin.IdAResponseLogin;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.admin.IdAdminLoginService;
import com.io7m.idstore.server.controller.admin.IdAdminLoggedIn;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimitExceeded;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimits;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHost;
import static com.io7m.idstore.model.IdLoginMetadataStandard.userAgent;
import static com.io7m.idstore.protocol.admin.IdAResponseBlame.BLAME_CLIENT;
import static com.io7m.idstore.protocol.admin.IdAResponseBlame.BLAME_SERVER;
import static com.io7m.idstore.server.admin_v1.IdA1ServletCoreTransactional.withTransaction;
import static com.io7m.idstore.server.http.IdHTTPServletCoreInstrumented.withInstrumentation;

/**
 * The v1 login servlet.
 */

public final class IdA1ServletLogin extends IdHTTPServletFunctional
{
  /**
   * The v1 login servlet.
   *
   * @param services The services
   */

  public IdA1ServletLogin(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPServletFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var limits =
      services.requireService(IdRequestLimits.class);
    final var messages =
      services.requireService(IdACB1Messages.class);
    final var strings =
      services.requireService(IdServerStrings.class);
    final var logins =
      services.requireService(IdAdminLoginService.class);

    return (request, information) -> {
      return withInstrumentation(
        services,
        (req0, info0) -> {
          return withTransaction(
            services,
            (req1, info1, transaction) -> {
              return execute(
                strings,
                limits,
                messages,
                logins,
                req1,
                info1,
                transaction
              );
            }).execute(req0, info0);
        }).execute(request, information);
    };
  }

  private static IdHTTPServletResponseType execute(
    final IdServerStrings strings,
    final IdRequestLimits limits,
    final IdACB1Messages messages,
    final IdAdminLoginService logins,
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information,
    final IdDatabaseTransactionType transaction)
  {
    final IdACommandLogin login;
    try {
      login = readLoginCommand(strings, limits, messages, request);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    } catch (final IdException e) {
      return IdA1Errors.errorResponseOf(messages, information, BLAME_CLIENT, e);
    }

    final var meta = new HashMap<>(login.metadata());
    meta.put(remoteHost(), information.remoteAddress());
    meta.put(userAgent(), information.userAgent());

    final IdAdminLoggedIn loggedIn;
    try {
      loggedIn = logins.adminLogin(
        transaction,
        information.requestId(),
        login.userName().value(),
        login.password(),
        meta
      );
    } catch (final IdCommandExecutionFailure e) {
      return IdA1Errors.errorResponseOf(messages, information, e);
    }

    try {
      transaction.commit();
    } catch (final IdDatabaseException e) {
      return IdA1Errors.errorResponseOf(messages, information, BLAME_SERVER, e);
    }

    request.getSession(true)
      .setAttribute("ID", loggedIn.session().id());

    return new IdHTTPServletResponseFixedSize(
      200,
      IdACB1Messages.contentType(),
      messages.serialize(
        new IdAResponseLogin(
          information.requestId(),
          loggedIn.admin().withRedactedPassword()
        )
      )
    );
  }

  private static IdACommandLogin readLoginCommand(
    final IdServerStrings strings,
    final IdRequestLimits limits,
    final IdACB1Messages messages,
    final HttpServletRequest request)
    throws IOException, IdRequestLimitExceeded, IdProtocolException
  {
    try (var input = limits.boundedMaximumInput(request, 1024)) {
      final var data = input.readAllBytes();
      final var message = messages.parse(data);
      if (message instanceof final IdACommandLogin login) {
        return login;
      }
    }

    throw new IdProtocolException(
      strings.format("commandNotHere"),
      IdStandardErrorCodes.PROTOCOL_ERROR,
      Map.of(),
      Optional.empty()
    );
  }
}