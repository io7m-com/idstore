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

import com.io7m.blackthorne.core.BTElementHandlerConstructorType;
import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.blackthorne.core.BTQualifiedName;
import com.io7m.cxbutton.core.CxButtonStateColors;
import com.io7m.cxbutton.core.CxColor;

import java.util.Map;
import java.util.Objects;

import static com.io7m.idstore.server.service.configuration.v1.IdC1Names.qName;
import static java.util.Map.entry;

abstract class IdC1AbstractButtonStateColors
  implements BTElementHandlerType<IdC1Color, IdC1ButtonStateColors>
{
  private final String semantic;
  private IdC1Color bodyColor;
  private IdC1Color borderColor;
  private IdC1Color embossEColor;
  private IdC1Color embossWColor;
  private IdC1Color embossSColor;
  private IdC1Color embossNColor;
  private IdC1Color textColor;

  IdC1AbstractButtonStateColors(
    final String inSemantic,
    final BTElementParsingContextType context)
  {
    this.semantic =
      Objects.requireNonNull(inSemantic, "semantic");
  }

  @Override
  public final Map<BTQualifiedName, BTElementHandlerConstructorType<?, ? extends IdC1Color>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      entry(qName("BodyColor"), IdC1BodyColor::new),
      entry(qName("BorderColor"), IdC1BorderColor::new),
      entry(qName("EmbossEColor"), IdC1EmbossEColor::new),
      entry(qName("EmbossWColor"), IdC1EmbossWColor::new),
      entry(qName("EmbossSColor"), IdC1EmbossSColor::new),
      entry(qName("EmbossNColor"), IdC1EmbossNColor::new),
      entry(qName("TextColor"), IdC1TextColor::new)
    );
  }

  @Override
  public final void onChildValueProduced(
    final BTElementParsingContextType context,
    final IdC1Color color)
  {
    switch (color.semantic()) {
      case "BodyColor" -> {
        this.bodyColor = color;
      }
      case "BorderColor" -> {
        this.borderColor = color;
      }
      case "EmbossEColor" -> {
        this.embossEColor = color;
      }
      case "EmbossWColor" -> {
        this.embossWColor = color;
      }
      case "EmbossSColor" -> {
        this.embossSColor = color;
      }
      case "EmbossNColor" -> {
        this.embossNColor = color;
      }
      case "TextColor" -> {
        this.textColor = color;
      }
      default -> {
        throw new IllegalArgumentException(
          "Unrecognized color semantic: %s".formatted(color.semantic())
        );
      }
    }
  }

  @Override
  public final IdC1ButtonStateColors onElementFinished(
    final BTElementParsingContextType context)
  {
    return new IdC1ButtonStateColors(
      this.semantic,
      new CxButtonStateColors(
        cx(this.textColor),
        cx(this.bodyColor),
        cx(this.borderColor),
        cx(this.embossEColor),
        cx(this.embossNColor),
        cx(this.embossSColor),
        cx(this.embossWColor)
      )
    );
  }

  private static CxColor cx(
    final IdC1Color c)
  {
    final var cc = c.color();
    return new CxColor(
      cc.red(),
      cc.green(),
      cc.blue()
    );
  }
}
