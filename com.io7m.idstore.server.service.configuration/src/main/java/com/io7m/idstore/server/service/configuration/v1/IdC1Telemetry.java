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
import com.io7m.idstore.server.api.IdServerOpenTelemetryConfiguration;
import org.xml.sax.Attributes;

import java.util.Map;
import java.util.Optional;

import static com.io7m.idstore.server.service.configuration.v1.IdC1Names.qName;
import static java.util.Map.entry;

final class IdC1Telemetry
  implements BTElementHandlerType<Object, IdServerOpenTelemetryConfiguration>
{
  private String serviceName;
  private Optional<IdServerOpenTelemetryConfiguration.IdLogs> logs;
  private Optional<IdServerOpenTelemetryConfiguration.IdMetrics> metrics;
  private Optional<IdServerOpenTelemetryConfiguration.IdTraces> traces;

  IdC1Telemetry(
    final BTElementParsingContextType context)
  {
    this.logs = Optional.empty();
    this.metrics = Optional.empty();
    this.traces = Optional.empty();
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.serviceName =
      attributes.getValue("LogicalServiceName");
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      entry(qName("Logs"), IdC1TelemetryLogs::new),
      entry(qName("Metrics"), IdC1TelemetryMetrics::new),
      entry(qName("Traces"), IdC1TelemetryTraces::new)
    );
  }

  @Override
  public IdServerOpenTelemetryConfiguration onElementFinished(
    final BTElementParsingContextType context)
  {
    return new IdServerOpenTelemetryConfiguration(
      this.serviceName,
      this.logs,
      this.metrics,
      this.traces
    );
  }
}
