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


package com.io7m.idstore.server.service.configuration.v1;

import com.io7m.blackthorne.core.BTElementHandlerConstructorType;
import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.blackthorne.core.BTQualifiedName;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerConfigurationFile;
import com.io7m.idstore.server.api.IdServerDatabaseConfiguration;
import com.io7m.idstore.server.api.IdServerHTTPConfiguration;
import com.io7m.idstore.server.api.IdServerHistoryConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerOpenTelemetryConfiguration;
import com.io7m.idstore.server.api.IdServerPasswordExpirationConfiguration;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import com.io7m.idstore.server.api.IdServerSessionConfiguration;

import java.util.Map;
import java.util.Optional;

import static com.io7m.idstore.server.service.configuration.v1.IdC1Names.qName;
import static java.util.Map.entry;

/**
 * The root configuration parser.
 */

public final class IdC1Configuration
  implements BTElementHandlerType<Object, IdServerConfigurationFile>
{
  private IdServerBrandingConfiguration branding;
  private IdServerDatabaseConfiguration database;
  private IdServerHTTPConfiguration http;
  private IdServerHistoryConfiguration history;
  private IdServerMailConfiguration mail;
  private IdServerPasswordExpirationConfiguration passwords;
  private IdServerRateLimitConfiguration rateLimit;
  private IdServerSessionConfiguration sessions;
  private Optional<IdServerOpenTelemetryConfiguration> telemetry;

  /**
   * The root configuration parser.
   *
   * @param context The context
   */

  public IdC1Configuration(
    final BTElementParsingContextType context)
  {
    this.telemetry = Optional.empty();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      entry(qName("Branding"), IdC1Branding::new),
      entry(qName("Database"), IdC1Database::new),
      entry(qName("HTTPServices"), IdC1HTTPServices::new),
      entry(qName("History"), IdC1History::new),
      entry(qName("Mail"), IdC1Mail::new),
      entry(qName("OpenTelemetry"), IdC1Telemetry::new),
      entry(qName("PasswordExpiration"), IdC1PasswordExpiration::new),
      entry(qName("RateLimiting"), IdC1RateLimit::new),
      entry(qName("Sessions"), IdC1Sessions::new)
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    switch (result) {
      case final IdServerBrandingConfiguration c -> {
        this.branding = c;
      }
      case final IdServerMailConfiguration c -> {
        this.mail = c;
      }
      case final IdServerHTTPConfiguration c -> {
        this.http = c;
      }
      case final IdServerDatabaseConfiguration c -> {
        this.database = c;
      }
      case final IdServerHistoryConfiguration c -> {
        this.history = c;
      }
      case final IdServerSessionConfiguration c -> {
        this.sessions = c;
      }
      case final IdServerRateLimitConfiguration c -> {
        this.rateLimit = c;
      }
      case final IdServerPasswordExpirationConfiguration c -> {
        this.passwords = c;
      }
      case final IdServerOpenTelemetryConfiguration c -> {
        this.telemetry = Optional.of(c);
      }
      default -> {
        throw new IllegalArgumentException(
          "Unrecognized element: %s".formatted(result)
        );
      }
    }
  }

  @Override
  public IdServerConfigurationFile onElementFinished(
    final BTElementParsingContextType context)
  {
    return new IdServerConfigurationFile(
      this.branding,
      this.mail,
      this.http,
      this.database,
      this.history,
      this.sessions,
      this.rateLimit,
      this.passwords,
      this.telemetry
    );
  }
}
