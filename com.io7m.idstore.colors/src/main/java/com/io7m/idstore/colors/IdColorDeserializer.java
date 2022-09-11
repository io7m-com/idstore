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


package com.io7m.idstore.colors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * A deserializer for color values.
 */

public final class IdColorDeserializer
  extends StdDeserializer<IdColor>
{
  private static final Pattern COLOR_PATTERN =
    Pattern.compile("#([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})");

  /**
   * A deserializer for color values.
   *
   * @inheritDoc
   */

  public IdColorDeserializer()
  {
    this(null);
  }

  /**
   * A deserializer for color values.
   *
   * @inheritDoc
   *
   * @param t The deserialized class
   */

  public IdColorDeserializer(
    final Class<IdColor> t)
  {
    super(t);
  }

  @Override
  public IdColor deserialize(
    final JsonParser p,
    final DeserializationContext ctxt)
    throws IOException
  {
    final var text = p.getValueAsString();

    final var matcher = COLOR_PATTERN.matcher(text);
    if (matcher.matches()) {
      final var r = matcher.group(1);
      final var g = matcher.group(2);
      final var b = matcher.group(3);
      return new IdColor(
        (double) Integer.parseUnsignedInt(r, 16) / 255.0,
        (double) Integer.parseUnsignedInt(g, 16) / 255.0,
        (double) Integer.parseUnsignedInt(b, 16) / 255.0
      );
    }

    throw new JsonParseException(
      p,
      "Color values must match the pattern %s".formatted(COLOR_PATTERN),
      p.getCurrentLocation()
    );
  }
}
