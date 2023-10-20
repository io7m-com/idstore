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
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreTransactionalType;
import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.webserver.http.ServerRequest;

import java.util.Objects;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.protocol.admin.IdAResponseBlame.BLAME_SERVER;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;

/**
 * A servlet core that executes the given core with a database transaction.
 */

public final class IdA1HandlerCoreTransactional
  implements IdHTTPHandlerFunctionalCoreType
{
  private final IdHTTPHandlerFunctionalCoreTransactionalType core;
  private final IdDatabaseType database;
  private final IdACB1Messages messages;

  private IdA1HandlerCoreTransactional(
    final RPServiceDirectoryType services,
    final IdHTTPHandlerFunctionalCoreTransactionalType inCore)
  {
    Objects.requireNonNull(services, "services");

    this.core =
      Objects.requireNonNull(inCore, "core");
    this.database =
      services.requireService(IdDatabaseType.class);
    this.messages =
      services.requireService(IdACB1Messages.class);
  }

  /**
   * @param inServices The services
   * @param inCore     The core
   *
   * @return A servlet core that executes the given core with a database transaction
   */

  public static IdHTTPHandlerFunctionalCoreType withTransaction(
    final RPServiceDirectoryType inServices,
    final IdHTTPHandlerFunctionalCoreTransactionalType inCore)
  {
    return new IdA1HandlerCoreTransactional(inServices, inCore);
  }

  @Override
  public IdHTTPResponseType execute(
    final ServerRequest request,
    final IdHTTPRequestInformation information)
  {
    try (var connection = this.database.openConnection(IDSTORE)) {
      try (var transaction = connection.openTransaction()) {
        return this.core.executeTransactional(
          request,
          information,
          transaction
        );
      }
    } catch (final IdDatabaseException e) {
      setSpanErrorCode(e.errorCode());
      return IdA1Errors.errorResponseOf(
        this.messages,
        information,
        BLAME_SERVER,
        e);
    }
  }
}
