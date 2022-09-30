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

package com.io7m.idstore.protocol.versions.cb1;

import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned64;
import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolMessageValidatorType;
import com.io7m.idstore.protocol.versions.IdVMessageType;
import com.io7m.idstore.protocol.versions.IdVProtocolSupported;
import com.io7m.idstore.protocol.versions.IdVProtocolsSupported;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import static com.io7m.cedarbridge.runtime.api.CBCore.unsigned64;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;

/**
 * Functions to translate between the core command set and the Versions v1
 * Cedarbridge encoding command set.
 */

public final class IdVCB1Validation
  implements IdProtocolMessageValidatorType<IdVMessageType, ProtocolIdVersionsv1Type>
{
  /**
   * Functions to translate between the core command set and the Versions v1
   * Cedarbridge encoding command set.
   */

  public IdVCB1Validation()
  {

  }

  private static CBList<IdV1ProtocolSupported> toWireProtocols(
    final List<IdVProtocolSupported> protocols)
  {
    return new CBList<>(
      protocols.stream()
        .map(IdVCB1Validation::toWireProtocol)
        .toList()
    );
  }

  private static IdV1ProtocolSupported toWireProtocol(
    final IdVProtocolSupported p)
  {
    return new IdV1ProtocolSupported(
      toWireUUID(p.id()),
      unsigned64(Long.parseUnsignedLong(p.versionMajor().toString())),
      unsigned64(Long.parseUnsignedLong(p.versionMinor().toString())),
      new CBString(p.endpointPath())
    );
  }

  private static IdV1UUID toWireUUID(
    final UUID uuid)
  {
    return new IdV1UUID(
      new CBIntegerUnsigned64(uuid.getMostSignificantBits()),
      new CBIntegerUnsigned64(uuid.getLeastSignificantBits())
    );
  }

  private static UUID fromWireUUID(
    final IdV1UUID uuid)
  {
    return new UUID(
      uuid.fieldMsb().value(),
      uuid.fieldLsb().value()
    );
  }

  private static List<IdVProtocolSupported> fromWireProtocols(
    final CBList<IdV1ProtocolSupported> supported)
  {
    return supported.values()
      .stream()
      .map(IdVCB1Validation::fromWireProtocol)
      .toList();
  }

  private static IdVProtocolSupported fromWireProtocol(
    final IdV1ProtocolSupported p)
  {
    return new IdVProtocolSupported(
      fromWireUUID(p.fieldId()),
      new BigInteger(Long.toUnsignedString(p.fieldVersionMajor().value())),
      new BigInteger(Long.toUnsignedString(p.fieldVersionMinor().value())),
      p.fieldEndpointPath().value()
    );
  }

  @Override
  public ProtocolIdVersionsv1Type convertToWire(
    final IdVMessageType message)
    throws IdProtocolException
  {
    if (message instanceof IdVProtocolsSupported m) {
      return new IdV1ProtocolsSupported(toWireProtocols(m.protocols()));
    }

    throw new IdProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(message)
    );
  }

  @Override
  public IdVMessageType convertFromWire(
    final ProtocolIdVersionsv1Type message)
    throws IdProtocolException
  {
    if (message instanceof IdV1ProtocolsSupported m) {
      return new IdVProtocolsSupported(fromWireProtocols(m.fieldSupported()));
    }

    throw new IdProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(message)
    );
  }
}
