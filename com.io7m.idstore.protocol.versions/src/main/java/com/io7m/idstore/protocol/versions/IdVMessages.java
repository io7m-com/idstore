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


package com.io7m.idstore.protocol.versions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolMessagesType;
import com.io7m.idstore.services.api.IdServiceType;

import java.io.IOException;
import java.math.BigInteger;

import static com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;

/**
 * The Versioning protocol.
 */

public final class IdVMessages
  implements IdProtocolMessagesType<IdVMessageType>,
  IdServiceType
{
  /**
   * The JSON schema identifier for the protocol.
   */

  public static final String SCHEMA_ID =
    "https://www.io7m.com/idstore/versions-1.json";

  /**
   * The content type for the protocol.
   */

  private static final String CONTENT_TYPE =
    "application/idstore_versions+json";

  private final SimpleDeserializers serializers;
  private final JsonMapper mapper;

  /**
   * The Versioning protocol.
   */

  public IdVMessages()
  {
    this.serializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClass(IdVProtocolSupported.class)
        .allowClass(IdVProtocols.class)
        .allowClass(IdVMessageType.class)
        .allowClass(String.class)
        .allowClass(BigInteger.class)
        .allowClassName(listOf(IdVProtocolSupported.class))
        .build();

    this.mapper =
      JsonMapper.builder()
        .enable(USE_BIG_INTEGER_FOR_INTS)
        .enable(ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SORT_PROPERTIES_ALPHABETICALLY)
        .build();

    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(this.serializers);
    this.mapper.registerModule(simpleModule);
  }

  private static String listOf(
    final Class<?> clazz)
  {
    return "java.util.List<%s>".formatted(clazz.getCanonicalName());
  }

  private static String setOf(
    final Class<?> clazz)
  {
    return "java.util.Set<%s>".formatted(clazz.getCanonicalName());
  }

  private static String mapOf(
    final Class<?> keyClazz,
    final String valClazz)
  {
    return "java.util.Map<%s,%s>"
      .formatted(keyClazz.getCanonicalName(), valClazz);
  }

  /**
   * @return The JSON schema identifier for the protocol.
   */

  public static String schemaId()
  {
    return SCHEMA_ID;
  }

  /**
   * @return The content type for the protocol.
   */

  public static String contentType()
  {
    return CONTENT_TYPE;
  }

  @Override
  public IdVMessageType parse(
    final byte[] data)
    throws IdProtocolException
  {
    try {
      return this.mapper.readValue(data, IdVMessageType.class);
    } catch (final IOException e) {
      throw new IdProtocolException(e.getMessage(), e);
    }
  }

  @Override
  public byte[] serialize(
    final IdVMessageType message)
    throws IdProtocolException
  {
    try {
      return this.mapper.writeValueAsBytes(message);
    } catch (final JsonProcessingException e) {
      throw new IdProtocolException(e.getMessage(), e);
    }
  }

  @Override
  public String description()
  {
    return "Versioning messages service.";
  }

  @Override
  public String toString()
  {
    return "[IdVMessages 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
