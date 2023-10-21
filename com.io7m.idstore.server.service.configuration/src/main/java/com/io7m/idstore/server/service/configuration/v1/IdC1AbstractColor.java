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


package com.io7m.idstore.server.service.configuration.v1;

import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.idstore.server.api.IdColor;
import org.xml.sax.Attributes;

import java.util.Objects;

abstract class IdC1AbstractColor
  implements BTElementHandlerType<Object, IdC1Color>
{
  private final String semantic;
  private IdC1Color result;

  IdC1AbstractColor(
    final String inSemantic,
    final BTElementParsingContextType context)
  {
    this.semantic =
      Objects.requireNonNull(inSemantic, "semantic");
  }

  @Override
  public final void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.result =
      new IdC1Color(
        this.semantic,
        new IdColor(
          Double.parseDouble(attributes.getValue("Red")),
          Double.parseDouble(attributes.getValue("Green")),
          Double.parseDouble(attributes.getValue("Blue"))
        )
      );
  }

  @Override
  public final IdC1Color onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.result;
  }
}
