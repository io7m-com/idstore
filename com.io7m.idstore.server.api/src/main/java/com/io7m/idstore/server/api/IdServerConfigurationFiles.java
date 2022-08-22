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

package com.io7m.idstore.server.api;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.io7m.idstore.services.api.IdServiceType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

/**
 * The configuration file parser.
 */

public final class IdServerConfigurationFiles
  implements IdServiceType
{
  /**
   * The JSON schema identifier for the protocol.
   */

  public static final String SCHEMA_ID =
    "https://www.io7m.com/idstore/server-configuration-1.json";

  /**
   * The content type for the protocol.
   */

  public static final String CONTENT_TYPE =
    "application/idstore_configuration+json";

  private final JsonMapper mapper;

  /**
   * The Public API v1 message protocol.
   */

  public IdServerConfigurationFiles()
  {
    this.mapper =
      JsonMapper.builder()
        .enable(USE_BIG_INTEGER_FOR_INTS)
        .enable(ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SORT_PROPERTIES_ALPHABETICALLY)
        .disable(WRITE_DATES_AS_TIMESTAMPS)
        .build();

    final var simpleModule = new SimpleModule();
    simpleModule.addDeserializer(
      IdServerColor.class,
      new IdServerColorDeserializer()
    );
    simpleModule.addSerializer(
      IdServerColor.class,
      new IdServerColorSerializer()
    );

    this.mapper.registerModule(simpleModule);
    this.mapper.registerModule(new JavaTimeModule());
    this.mapper.registerModule(new Jdk8Module());
  }

  /**
   * Parse a configuration file.
   *
   * @param stream The input stream
   *
   * @return The file
   *
   * @throws IOException On errors
   */

  public IdServerConfigurationFile parse(
    final InputStream stream)
    throws IOException
  {
    return this.mapper.readValue(stream, IdServerConfigurationFile.class);
  }

  /**
   * Parse a configuration file.
   *
   * @param file The input file
   *
   * @return The file
   *
   * @throws IOException On errors
   */

  public IdServerConfigurationFile parse(
    final Path file)
    throws IOException
  {
    try (var stream = Files.newInputStream(file)) {
      return this.parse(stream);
    }
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
  public String description()
  {
    return "Server configuration elements.";
  }

  @Override
  public String toString()
  {
    return "[IdServerConfigurationFiles 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
