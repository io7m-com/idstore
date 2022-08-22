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

package com.io7m.idstore.protocol.user_v1;

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

/**
 * The user API v1 message protocol.
 */

public final class IdU1Messages
  implements IdProtocolMessagesType<IdU1MessageType>,
  IdServiceType
{
  /**
   * The JSON schema identifier for the protocol.
   */

  public static final String SCHEMA_ID =
    "https://www.io7m.com/idstore/user-api-1.json";

  /**
   * The content type for the protocol.
   */

  public static final String CONTENT_TYPE =
    "application/idstore_public+json";

  private final SimpleDeserializers serializers;
  private final JsonMapper mapper;

  /**
   * The Public API v1 message protocol.
   */

  public IdU1Messages()
  {
    this.serializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClass(BigInteger.class)
        .allowClass(IdU1CommandEmailAddBegin.class)
        .allowClass(IdU1CommandEmailAddDeny.class)
        .allowClass(IdU1CommandEmailAddPermit.class)
        .allowClass(IdU1CommandEmailRemoveBegin.class)
        .allowClass(IdU1CommandEmailRemoveDeny.class)
        .allowClass(IdU1CommandEmailRemovePermit.class)
        .allowClass(IdU1CommandLogin.class)
        .allowClass(IdU1CommandRealnameUpdate.class)
        .allowClass(IdU1CommandUserSelf.class)
        .allowClass(IdU1MessageType.class)
        .allowClass(IdU1Password.class)
        .allowClass(IdU1ResponseEmailAddBegin.class)
        .allowClass(IdU1ResponseEmailAddDeny.class)
        .allowClass(IdU1ResponseEmailAddPermit.class)
        .allowClass(IdU1ResponseEmailRemoveBegin.class)
        .allowClass(IdU1ResponseEmailRemoveDeny.class)
        .allowClass(IdU1ResponseEmailRemovePermit.class)
        .allowClass(IdU1ResponseError.class)
        .allowClass(IdU1ResponseLogin.class)
        .allowClass(IdU1ResponseRealnameUpdate.class)
        .allowClass(IdU1ResponseUserSelf.class)
        .allowClass(IdU1User.class)
        .allowClass(String.class)
        .allowClass(URI.class)
        .allowClass(UUID.class)
        .allowClass(boolean.class)
        .allowClassName(listOf(String.class))
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
  public IdU1MessageType parse(
    final byte[] data)
    throws IdProtocolException
  {
    try {
      return this.mapper.readValue(data, IdU1MessageType.class);
    } catch (final IOException e) {
      throw new IdProtocolException(e.getMessage(), e);
    }
  }

  @Override
  public byte[] serialize(
    final IdU1MessageType message)
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
    return "User API 1.0 messages.";
  }

  @Override
  public String toString()
  {
    return "[IdU1Messages 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
