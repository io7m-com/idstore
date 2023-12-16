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


package com.io7m.idstore.server.service.configuration;

import com.io7m.jxe.core.JXESchemaDefinition;
import com.io7m.jxe.core.JXESchemaResolutionMappings;

import java.net.URI;

/**
 * Configuration XML schemas.
 */

public final class IdServerConfigurationSchemas
{
  private static final JXESchemaDefinition SCHEMA_1 =
    JXESchemaDefinition.builder()
      .setFileIdentifier("configuration-1.xsd")
      .setLocation(IdServerConfigurationSchemas.class.getResource(
        "/com/io7m/idstore/server/service/configuration/configuration-1.xsd"))
      .setNamespace(URI.create("urn:com.io7m.idstore:configuration:1"))
      .build();

  private static final JXESchemaDefinition TLS_1 =
    JXESchemaDefinition.builder()
      .setFileIdentifier("tls-1.xsd")
      .setLocation(IdServerConfigurationSchemas.class.getResource(
        "/com/io7m/idstore/server/service/configuration/tls-1.xsd"))
      .setNamespace(URI.create("urn:com.io7m.idstore.tls:1"))
      .build();

  private static final JXESchemaResolutionMappings SCHEMA_MAPPINGS =
    JXESchemaResolutionMappings.builder()
      .putMappings(SCHEMA_1.namespace(), SCHEMA_1)
      .putMappings(TLS_1.namespace(), TLS_1)
      .build();

  /**
   * @return The v1 schema
   */

  public static JXESchemaDefinition schema1()
  {
    return SCHEMA_1;
  }

  /**
   * @return The TLS v1 schema
   */

  public static JXESchemaDefinition tls1()
  {
    return TLS_1;
  }

  /**
   * @return The set of supported schemas.
   */

  public static JXESchemaResolutionMappings schemas()
  {
    return SCHEMA_MAPPINGS;
  }

  private IdServerConfigurationSchemas()
  {

  }
}
