/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.idstore.server.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.LayoutBase;

import java.util.Locale;

import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_REQUEST_ID;
import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_REQUEST_REMOTE_HOST;
import static com.io7m.idstore.server.logging.IdLoggingHTTPRequestLayout.MDC_REQUEST_REMOTE_PORT;

/**
 * A dynamic logging pattern based on MDC values.
 */

public final class IdLoggingPatternLayout extends LayoutBase<ILoggingEvent>
{
  /**
   * A dynamic logging pattern based on MDC values.
   */

  public IdLoggingPatternLayout()
  {

  }

  @Override
  public String doLayout(
    final ILoggingEvent event)
  {
    final var s = new StringBuilder(128);
    s.append(event.getLevel().toString().toLowerCase(Locale.ROOT));
    s.append(": ");

    s.append(event.getLoggerName());
    s.append(": ");

    final var mdc = event.getMDCPropertyMap();

    final var clientHost = mdc.get(MDC_REQUEST_REMOTE_HOST);
    if (clientHost != null) {
      s.append("[client ");
      s.append(clientHost);
      final var clientPort = mdc.get(MDC_REQUEST_REMOTE_PORT);
      if (clientPort != null) {
        s.append(':');
        s.append(clientPort);
      }
      s.append(']');
      s.append(' ');
    }

    final var clientRequest = mdc.get(MDC_REQUEST_ID);
    if (clientRequest != null) {
      s.append("[request ");
      s.append(clientRequest);
      s.append(']');
      s.append(' ');
    }

    s.append(event.getFormattedMessage());

    {
      final var throwable = event.getThrowableProxy();
      if (throwable != null) {
        s.append(this.formatThrowable(throwable));
      }
    }

    s.append(System.lineSeparator());
    return s.toString();
  }

  private String formatThrowable(
    final IThrowableProxy throwable)
  {
    final var text = new StringBuilder();
    text.append(throwable.getClassName());
    text.append(": ");
    text.append(throwable.getMessage());
    text.append(System.lineSeparator());

    for (final var e : throwable.getStackTraceElementProxyArray()) {
      text.append("  ");
      text.append(e.getSTEAsString());
      text.append(System.lineSeparator());
    }

    final var cause = throwable.getCause();
    if (cause != null) {
      text.append(this.formatThrowable(cause));
    }
    return text.toString();
  }
}
