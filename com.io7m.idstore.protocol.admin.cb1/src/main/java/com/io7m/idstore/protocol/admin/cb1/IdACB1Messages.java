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


package com.io7m.idstore.protocol.admin.cb1;

import com.io7m.cedarbridge.runtime.api.CBCoreSerializers;
import com.io7m.cedarbridge.runtime.api.CBProtocolSerializerCollectionType;
import com.io7m.cedarbridge.runtime.api.CBProtocolSerializerType;
import com.io7m.cedarbridge.runtime.api.CBSerializerDirectoryMutable;
import com.io7m.cedarbridge.runtime.bssio.CBSerializationContextBSSIO;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolMessagesType;
import com.io7m.idstore.services.api.IdServiceType;
import com.io7m.jbssio.api.BSSReaderProviderType;
import com.io7m.jbssio.api.BSSWriterProviderType;
import com.io7m.jbssio.vanilla.BSSReaders;
import com.io7m.jbssio.vanilla.BSSWriters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;

/**
 * The protocol messages for Admin v1 Cedarbridge.
 */

public final class IdACB1Messages
  implements IdProtocolMessagesType<IdAMessageType>, IdServiceType
{
  /**
   * The schema identifier for the protocol.
   */

  public static final String SCHEMA_ID =
    "https://www.io7m.com/idstore/Admin1.cbs";

  /**
   * The content type for the protocol.
   */

  public static final String CONTENT_TYPE =
    "application/idstore_admin+cedarbridge";

  private final BSSReaderProviderType readers;
  private final BSSWriterProviderType writers;
  private final CBProtocolSerializerCollectionType<ProtocolIdA1Type> protocols;
  private final CBSerializerDirectoryMutable serializers;
  private final IdACB1Validation validator;
  private final CBProtocolSerializerType<ProtocolIdA1Type> serializer;

  /**
   * The protocol messages for Admin v1 Cedarbridge.
   *
   * @param inReaders The readers
   * @param inWriters The writers
   */

  public IdACB1Messages(
    final BSSReaderProviderType inReaders,
    final BSSWriterProviderType inWriters)
  {
    this.readers =
      Objects.requireNonNull(inReaders, "readers");
    this.writers =
      Objects.requireNonNull(inWriters, "writers");

    this.validator = new IdACB1Validation();
    this.protocols = ProtocolIdA1.factories();
    this.serializers = new CBSerializerDirectoryMutable();
    this.serializers.addCollection(CBCoreSerializers.get());
    this.serializers.addCollection(Serializers.get());
    this.serializer =
      this.protocols.findOrThrow(1L)
        .create(this.serializers);
  }

  /**
   * The protocol messages for Admin v1 Cedarbridge.
   */

  public IdACB1Messages()
  {
    this(new BSSReaders(), new BSSWriters());
  }

  /**
   * @return The content type
   */

  public static String contentType()
  {
    return CONTENT_TYPE;
  }

  /**
   * @return The schema identifier
   */

  public static String schemaId()
  {
    return SCHEMA_ID;
  }

  @Override
  public IdAMessageType parse(
    final byte[] data)
    throws IdProtocolException
  {
    final var context =
      CBSerializationContextBSSIO.createFromByteArray(this.readers, data);

    try {
      return this.validator.convertFromWire(
        (ProtocolIdA1v1Type) this.serializer.deserialize(context)
      );
    } catch (final IOException e) {
      throw new IdProtocolException(IO_ERROR, e.getMessage(), e);
    }
  }

  @Override
  public byte[] serialize(
    final IdAMessageType message)
    throws IdProtocolException
  {
    try (var output = new ByteArrayOutputStream()) {
      final var context =
        CBSerializationContextBSSIO.createFromOutputStream(
          this.writers,
          output);
      this.serializer.serialize(context, this.validator.convertToWire(message));
      return output.toByteArray();
    } catch (final IOException e) {
      throw new IdProtocolException(IO_ERROR, e.getMessage(), e);
    }
  }

  @Override
  public String description()
  {
    return "Admin v1 Cedarbridge message service.";
  }

  @Override
  public String toString()
  {
    return "[IdACB1Messages 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
