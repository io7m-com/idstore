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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Objects;

/**
 * A supported protocol.
 *
 * @param id           The protocol ID
 * @param endpointPath The base endpoint path
 * @param versionMajor The major version
 * @param versionMinor The minor version
 */

@JsonDeserialize
@JsonSerialize
public record IdVProtocolSupported(
  @JsonProperty(value = "ID", required = true)
  String id,
  @JsonProperty(value = "VersionMajor", required = true)
  BigInteger versionMajor,
  @JsonProperty(value = "VersionMinor", required = true)
  BigInteger versionMinor,
  @JsonProperty(value = "EndpointPath", required = true)
  String endpointPath)
  implements Comparable<IdVProtocolSupported>
{
  /**
   * A supported protocol.
   *
   * @param id           The protocol ID
   * @param endpointPath The base endpoint path
   * @param versionMajor The major version
   * @param versionMinor The minor version
   */

  public IdVProtocolSupported
  {
    Objects.requireNonNull(versionMajor, "versionMajor");
    Objects.requireNonNull(versionMinor, "versionMinor");
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(endpointPath, "endpointPath");
  }

  @Override
  public int compareTo(
    final IdVProtocolSupported other)
  {
    return Comparator.comparing(IdVProtocolSupported::versionMajor)
      .thenComparing(IdVProtocolSupported::versionMinor)
      .compare(this, other);
  }
}
