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

package com.io7m.idstore.protocol.admin_v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolMessagesType;
import com.io7m.idstore.services.api.IdServiceType;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.UUID;

import static com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;

/**
 * The Public API v1 message protocol.
 */

public final class IdA1Messages
  implements IdProtocolMessagesType<IdA1MessageType>,
  IdServiceType
{
  /**
   * The JSON schema identifier for the protocol.
   */

  public static final String SCHEMA_ID =
    "https://www.io7m.com/idstore/admin-api-1.json";

  /**
   * The content type for the protocol.
   */

  public static final String CONTENT_TYPE =
    "application/idstore_admin+json";

  private final SimpleDeserializers serializers;
  private final JsonMapper mapper;

  /**
   * The Public API v1 message protocol.
   */

  public IdA1Messages()
  {
    final var builder =
      DmJsonRestrictedDeserializers.builder();

    for (final var c : IdA1CommandType.class.getPermittedSubclasses()) {
      builder.allowClass(c);
    }
    for (final var c : IdA1ResponseType.class.getPermittedSubclasses()) {
      builder.allowClass(c);
    }

    this.serializers =
      builder.allowClass(BigInteger.class)
        .allowClass(IdA1Admin.class)
        .allowClass(IdA1AdminColumn.class)
        .allowClass(IdA1AdminColumnOrdering.class)
        .allowClass(IdA1AdminOrdering.class)
        .allowClass(IdA1AdminPermission.class)
        .allowClass(IdA1AdminSearchByEmailParameters.class)
        .allowClass(IdA1AdminSearchParameters.class)
        .allowClass(IdA1AdminSummary.class)
        .allowClass(IdA1AuditEvent.class)
        .allowClass(IdA1AuditListParameters.class)
        .allowClass(IdA1Ban.class)
        .allowClass(IdA1MessageType.class)
        .allowClass(IdA1Page.class)
        .allowClass(IdA1Password.class)
        .allowClass(IdA1TimeRange.class)
        .allowClass(IdA1User.class)
        .allowClass(IdA1UserColumn.class)
        .allowClass(IdA1UserColumnOrdering.class)
        .allowClass(IdA1UserOrdering.class)
        .allowClass(IdA1UserSearchByEmailParameters.class)
        .allowClass(IdA1UserSearchParameters.class)
        .allowClass(IdA1UserSummary.class)
        .allowClass(String.class)
        .allowClass(URI.class)
        .allowClass(UUID.class)
        .allowClass(boolean.class)
        .allowClass(int.class)
        .allowClass(long.class)
        .allowClassName(listOf(IdA1AdminColumnOrdering.class))
        .allowClassName(listOf(IdA1AdminSummary.class))
        .allowClassName(listOf(IdA1AuditEvent.class))
        .allowClassName(listOf(IdA1UserColumnOrdering.class))
        .allowClassName(listOf(IdA1UserSummary.class))
        .allowClassName(listOf(String.class))
        .allowClassName(setOf(IdA1AdminPermission.class))
        .build();

    this.mapper =
      JsonMapper.builder()
        .enable(USE_BIG_INTEGER_FOR_INTS)
        .enable(ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SORT_PROPERTIES_ALPHABETICALLY)
        .disable(WRITE_DATES_AS_TIMESTAMPS)
        .build();

    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(this.serializers);
    this.mapper.registerModule(simpleModule);
    this.mapper.registerModule(new JavaTimeModule());
    this.mapper.registerModule(new Jdk8Module());
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
  public IdA1MessageType parse(
    final byte[] data)
    throws IdProtocolException
  {
    try {
      return this.mapper.readValue(data, IdA1MessageType.class);
    } catch (final IOException e) {
      throw new IdProtocolException(IO_ERROR, e.getMessage(), e);
    }
  }

  @Override
  public byte[] serialize(
    final IdA1MessageType message)
    throws IdProtocolException
  {
    try {
      return this.mapper.writeValueAsBytes(message);
    } catch (final JsonProcessingException e) {
      throw new IdProtocolException(IO_ERROR, e.getMessage(), e);
    }
  }

  @Override
  public String description()
  {
    return "Admin API 1.0 messages.";
  }

  @Override
  public String toString()
  {
    return "[IdSP1Messages 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
