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
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.protocol.admin.IdACommandType;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.admin.IdACommandContext;
import com.io7m.idstore.server.controller.admin.IdACommandExecutor;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimitExceeded;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimits;
import com.io7m.idstore.server.service.sessions.IdSessionAdmin;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.protocol.admin.IdAResponseBlame.BLAME_CLIENT;
import static com.io7m.idstore.protocol.admin.IdAResponseBlame.BLAME_SERVER;
import static com.io7m.idstore.server.admin_v1.IdA1Errors.errorResponseOf;
import static com.io7m.idstore.server.admin_v1.IdA1ServletCoreAuthenticated.withAuthentication;
import static com.io7m.idstore.server.admin_v1.IdA1ServletCoreTransactional.withTransaction;
import static com.io7m.idstore.server.http.IdHTTPServletCoreInstrumented.withInstrumentation;

/**
 * The v1 command servlet.
 */

public final class IdA1ServletCommand extends IdHTTPServletFunctional
{
  /**
   * The v1 command servlet.
   *
   * @param services The services
   */

  public IdA1ServletCommand(
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

    return (request, information) -> {
      return withInstrumentation(
        services,
        (req0, info0) -> {
          return withAuthentication(
            services,
            (req1, info1, session, user) -> {
              return withTransaction(
                services,
                (req2, info2, transaction) -> {
                  return execute(
                    services,
                    req2,
                    info2,
                    messages,
                    limits,
                    strings,
                    session,
                    user,
                    transaction
                  );
                }).execute(req1, info1);
            }).execute(req0, info0);
        }).execute(request, information);
    };
  }

  private static IdHTTPServletResponseType execute(
    final RPServiceDirectoryType services,
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information,
    final IdACB1Messages messages,
    final IdRequestLimits limits,
    final IdServerStrings strings,
    final IdSessionAdmin session,
    final IdAdmin user,
    final IdDatabaseTransactionType transaction)
  {
    try (var input =
           limits.boundedMaximumInput(request, 1048576)) {
      final var data =
        input.readAllBytes();
      final var message =
        messages.parse(data);
      if (message instanceof final IdACommandType<?> command) {
        return executeCommand(
          services,
          information,
          messages,
          session,
          user,
          command,
          transaction
        );
      }

      return errorResponseOf(
        messages,
        information,
        BLAME_CLIENT,
        new IdProtocolException(
          strings.format("commandNotHere"),
          PROTOCOL_ERROR,
          Map.of(),
          Optional.empty()
        )
      );

    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    } catch (final IdRequestLimitExceeded | IdProtocolException e) {
      return errorResponseOf(messages, information, BLAME_CLIENT, e);
    } catch (final IdDatabaseException e) {
      return errorResponseOf(messages, information, BLAME_SERVER, e);
    }
  }

  private static IdHTTPServletResponseType executeCommand(
    final RPServiceDirectoryType services,
    final IdHTTPServletRequestInformation information,
    final IdACB1Messages messages,
    final IdSessionAdmin session,
    final IdAdmin user,
    final IdACommandType<?> command,
    final IdDatabaseTransactionType transaction)
    throws IdDatabaseException
  {
    final var executor =
      new IdACommandExecutor();

    final var context =
      new IdACommandContext(
        services,
        information.requestId(),
        transaction,
        session,
        information.remoteAddress(),
        information.userAgent(),
        user
      );

    final IdAResponseType result;
    try {
      result = executor.execute(context, command);
    } catch (final IdCommandExecutionFailure e) {
      return errorResponseOf(messages, information, e);
    }

    if (result instanceof final IdAResponseError error) {
      Span.current().setAttribute("idstore.errorCode", error.errorCode().id());
      return new IdHTTPServletResponseFixedSize(
        switch (error.blame()) {
          case BLAME_SERVER -> 500;
          case BLAME_CLIENT -> 400;
        },
        IdACB1Messages.contentType(),
        messages.serialize(error)
      );
    }

    transaction.commit();
    return new IdHTTPServletResponseFixedSize(
      200,
      IdACB1Messages.contentType(),
      messages.serialize(result)
    );
  }
}