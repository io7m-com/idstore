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
import com.io7m.idstore.model.IdUserDomain;
import com.io7m.idstore.protocol.admin.IdACommandType;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.server.controller.admin.IdACommandContext;
import com.io7m.idstore.server.controller.admin.IdACommandExecutor;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctional;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimitExceeded;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimits;
import com.io7m.idstore.server.service.sessions.IdSessionAdmin;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.webserver.http.ServerRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.API_MISUSE_ERROR;
import static com.io7m.idstore.protocol.admin.IdAResponseBlame.BLAME_CLIENT;
import static com.io7m.idstore.protocol.admin.IdAResponseBlame.BLAME_SERVER;
import static com.io7m.idstore.server.admin_v1.IdA1Errors.errorResponseOf;
import static com.io7m.idstore.server.admin_v1.IdA1HandlerCoreAuthenticated.withAuthentication;
import static com.io7m.idstore.server.admin_v1.IdA1HandlerCoreTransactional.withTransaction;
import static com.io7m.idstore.server.http.IdHTTPHandlerCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;
import static com.io7m.idstore.strings.IdStringConstants.COMMAND_NOT_HERE;

/**
 * The v1 command handler.
 */

public final class IdA1HandlerCommand extends IdHTTPHandlerFunctional
{
  /**
   * The v1 command servlet.
   *
   * @param services The services
   */

  public IdA1HandlerCommand(
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
      services.requireService(IdACB1Messages.class);
    final var strings =
      services.requireService(IdStrings.class);
    final var telemetry =
      services.requireService(IdServerTelemetryServiceType.class);

    return (request, information) -> {
      return withInstrumentation(
        services,
        IdUserDomain.ADMIN,
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
                    telemetry,
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

  private static IdHTTPResponseType execute(
    final RPServiceDirectoryType services,
    final ServerRequest request,
    final IdHTTPRequestInformation information,
    final IdACB1Messages messages,
    final IdServerTelemetryServiceType telemetry,
    final IdRequestLimits limits,
    final IdStrings strings,
    final IdSessionAdmin session,
    final IdAdmin user,
    final IdDatabaseTransactionType transaction)
  {
    try (var input =
           limits.boundedMaximumInput(request, 1048576L)) {

      final var message =
        parseMessage(telemetry, messages, input);

      if (message instanceof final IdACommandType<?> command) {
        return executeCommand(
          services,
          information,
          messages,
          telemetry,
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
          strings.format(COMMAND_NOT_HERE),
          API_MISUSE_ERROR,
          Map.of(),
          Optional.empty()
        )
      );

    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    } catch (final IdRequestLimitExceeded | IdProtocolException e) {
      setSpanErrorCode(e.errorCode());
      return errorResponseOf(messages, information, BLAME_CLIENT, e);
    } catch (final IdDatabaseException e) {
      setSpanErrorCode(e.errorCode());
      return errorResponseOf(messages, information, BLAME_SERVER, e);
    }
  }

  private static IdHTTPResponseType executeCommand(
    final RPServiceDirectoryType services,
    final IdHTTPRequestInformation information,
    final IdACB1Messages messages,
    final IdServerTelemetryServiceType telemetry,
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
      setSpanErrorCode(e.errorCode());
      return errorResponseOf(messages, information, e);
    }

    if (result instanceof final IdAResponseError error) {
      setSpanErrorCode(error.errorCode());
      return new IdHTTPResponseFixedSize(
        switch (error.blame()) {
          case BLAME_SERVER -> 500;
          case BLAME_CLIENT -> 400;
        },
        Set.of(),
        IdACB1Messages.contentType(),
        messages.serialize(error)
      );
    }

    commit(telemetry, transaction);
    return new IdHTTPResponseFixedSize(
      200,
      Set.of(),
      IdACB1Messages.contentType(),
      messages.serialize(result)
    );
  }

  private static IdAMessageType parseMessage(
    final IdServerTelemetryServiceType telemetry,
    final IdACB1Messages messages,
    final InputStream input)
    throws IOException, IdProtocolException
  {
    final var parseSpan =
      telemetry.tracer()
        .spanBuilder("ParseMessage")
        .startSpan();

    try (var ignored = parseSpan.makeCurrent()) {
      final var data = parseMessageReadData(telemetry, input);
      return parseMessageDeserialize(telemetry, messages, data);
    } finally {
      parseSpan.end();
    }
  }

  private static IdAMessageType parseMessageDeserialize(
    final IdServerTelemetryServiceType telemetry,
    final IdACB1Messages messages,
    final byte[] data)
    throws IdProtocolException
  {
    final var readSpan =
      telemetry.tracer()
        .spanBuilder("Deserialize")
        .startSpan();

    try (var ignored = readSpan.makeCurrent()) {
      return messages.parse(data);
    } finally {
      readSpan.end();
    }
  }

  private static byte[] parseMessageReadData(
    final IdServerTelemetryServiceType telemetry,
    final InputStream input)
    throws IOException
  {
    final var readSpan =
      telemetry.tracer()
        .spanBuilder("Read")
        .startSpan();

    try (var ignored = readSpan.makeCurrent()) {
      return input.readAllBytes();
    } finally {
      readSpan.end();
    }
  }

  private static void commit(
    final IdServerTelemetryServiceType telemetry,
    final IdDatabaseTransactionType transaction)
    throws IdDatabaseException
  {
    final var commitSpan =
      telemetry.tracer()
        .spanBuilder("Commit")
        .startSpan();

    try (var ignored = commitSpan.makeCurrent()) {
      transaction.commit();
    } finally {
      commitSpan.end();
    }
  }
}
