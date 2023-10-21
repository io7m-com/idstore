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


package com.io7m.idstore.server.service.tls;


import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.idstore.strings.IdStringConstants;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.tls.IdTLSContext;
import com.io7m.idstore.tls.IdTLSStoreConfiguration;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.recordSpanException;

/**
 * The TLS context service.
 */

public final class IdTLSContextService
  implements IdTLSContextServiceType
{
  private final ConcurrentHashMap.KeySetView<IdTLSContext, Boolean> contexts;
  private final IdServerTelemetryServiceType telemetry;
  private final IdStrings strings;

  private IdTLSContextService(
    final IdServerTelemetryServiceType inTelemetry,
    final IdStrings inStrings)
  {
    this.telemetry =
      Objects.requireNonNull(inTelemetry, "telemetry");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.contexts =
      ConcurrentHashMap.newKeySet();
  }

  @Override
  public String toString()
  {
    return "[IdTLSContextService 0x%x]"
      .formatted(Integer.valueOf(this.hashCode()));
  }

  /**
   * @param services The service directory
   *
   * @return A new TLS context service
   */

  public static IdTLSContextServiceType createService(
    final RPServiceDirectoryType services)
  {
    return new IdTLSContextService(
      services.requireService(IdServerTelemetryServiceType.class),
      services.requireService(IdStrings.class)
    );
  }

  @Override
  public IdTLSContext create(
    final String user,
    final IdTLSStoreConfiguration keyStoreConfiguration,
    final IdTLSStoreConfiguration trustStoreConfiguration)
    throws IdException
  {
    try {
      final var newContext =
        IdTLSContext.create(
          user,
          keyStoreConfiguration,
          trustStoreConfiguration
        );
      this.contexts.add(newContext);
      return newContext;
    } catch (final IOException e) {
      throw errorIO(this.strings, e);
    } catch (final GeneralSecurityException e) {
      throw errorSecurity(e);
    }
  }

  @Override
  public void reload()
  {
    final var span =
      this.telemetry.tracer()
        .spanBuilder("ReloadTLSContexts")
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      for (final var context : this.contexts) {
        this.reloadContext(context);
      }
    } finally {
      span.end();
    }
  }

  private void reloadContext(
    final IdTLSContext context)
  {
    final var span =
      this.telemetry.tracer()
        .spanBuilder("ReloadTLSContext")
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      context.reload();
    } catch (final Throwable e) {
      recordSpanException(e);
    } finally {
      span.end();
    }
  }

  @Override
  public String description()
  {
    return "The TLS context service.";
  }

  private static IdException errorIO(
    final IdStrings strings,
    final IOException e)
  {
    return new IdException(
      strings.format(IdStringConstants.ERROR_IO),
      e,
      IdStandardErrorCodes.IO_ERROR,
      Map.of(),
      Optional.empty()
    );
  }

  private static IdException errorSecurity(
    final GeneralSecurityException e)
  {
    return new IdException(
      e.getMessage(),
      e,
      IdStandardErrorCodes.IO_ERROR,
      Map.of(),
      Optional.empty()
    );
  }
}
