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
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;

/**
 * A logging pattern specifically for HTTP requests.
 *
 * @see IdServerRequestLog
 */

public final class IdLoggingHTTPRequestLayout extends LayoutBase<ILoggingEvent>
{
  public static final String MDC_REQUEST_DOMAIN = "request.domain";
  public static final String MDC_REQUEST_ID = "request.id";
  public static final String MDC_REQUEST_METHOD = "request.method";
  public static final String MDC_REQUEST_REMOTE_HOST = "request.remote_host";
  public static final String MDC_REQUEST_REMOTE_PORT = "request.remote_port";
  public static final String MDC_REQUEST_URI = "request.uri";
  public static final String MDC_REQUEST_USER_AGENT = "request.user_agent";
  public static final String MDC_RESPONSE_CONTENT_LENGTH = "request.content_length";
  public static final String MDC_RESPONSE_CONTENT_TYPE = "request.content_type";
  public static final String MDC_RESPONSE_DURATION = "response.duration";
  public static final String MDC_RESPONSE_HTTP_STATUS = "response.status";

  private static final List<String> MDC_FIELDS =
    List.of(
      MDC_REQUEST_DOMAIN,
      MDC_REQUEST_ID,
      MDC_REQUEST_METHOD,
      MDC_REQUEST_REMOTE_HOST,
      MDC_REQUEST_REMOTE_PORT,
      MDC_REQUEST_URI,
      MDC_REQUEST_USER_AGENT,
      MDC_RESPONSE_CONTENT_LENGTH,
      MDC_RESPONSE_CONTENT_TYPE,
      MDC_RESPONSE_DURATION,
      MDC_RESPONSE_HTTP_STATUS
    );

  private final JsonMapper mapper;

  /**
   * A logging pattern specifically for HTTP requests.
   *
   * @see IdServerRequestLog
   */

  public IdLoggingHTTPRequestLayout()
  {
    this.mapper =
      JsonMapper.builder()
        .enable(USE_BIG_INTEGER_FOR_INTS)
        .enable(ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SORT_PROPERTIES_ALPHABETICALLY)
        .build();
  }

  private static void addMdc(
    final Map<String, String> mdc,
    final String mdcKey,
    final String outputKey,
    final ObjectNode obj)
  {
    final var value = mdc.get(mdcKey);
    if (value != null) {
      obj.put(outputKey, value);
    }
  }

  @Override
  public String doLayout(
    final ILoggingEvent event)
  {
    final var obj =
      this.mapper.createObjectNode();
    final var mdc =
      event.getMDCPropertyMap();

    final var time =
      Instant.ofEpochMilli(event.getTimeStamp());
    final var timeOff =
      OffsetDateTime.ofInstant(time, ZoneOffset.UTC);

    obj.put("time", timeOff.toString());

    for (final var field : MDC_FIELDS) {
      addMdc(mdc, field, field, obj);
    }

    try {
      return this.mapper.writeValueAsString(obj) + '\n';
    } catch (final JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
