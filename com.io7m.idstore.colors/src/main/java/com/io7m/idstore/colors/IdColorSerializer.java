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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * A serializer for color values.
 */

public final class IdColorSerializer
  extends StdSerializer<IdColor>
{
  /**
   * A serializer for color values.
   *
   * @inheritDoc
   */

  public IdColorSerializer()
  {
    this(null);
  }


  /**
   * A serializer for color values.
   *
   * @inheritDoc
   *
   * @param t The serialized class
   */

  public IdColorSerializer(
    final Class<IdColor> t)
  {
    super(t);
  }

  @Override
  public void serialize(
    final IdColor value,
    final JsonGenerator jgen,
    final SerializerProvider provider)
    throws IOException
  {
    jgen.writeString(value.toString());
  }
}
