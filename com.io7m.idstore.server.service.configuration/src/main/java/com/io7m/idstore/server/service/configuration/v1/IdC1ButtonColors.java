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
import com.io7m.cxbutton.core.CxButtonColors;
import com.io7m.cxbutton.core.CxButtonStateColors;

import java.util.Map;

import static com.io7m.idstore.server.service.configuration.v1.IdC1Names.qName;
import static java.util.Map.entry;

final class IdC1ButtonColors
  implements BTElementHandlerType<Object, CxButtonColors>
{
  private CxButtonStateColors disabled;
  private CxButtonStateColors enabled;
  private CxButtonStateColors hover;
  private CxButtonStateColors pressed;

  IdC1ButtonColors(
    final BTElementParsingContextType context)
  {

  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      entry(qName("Disabled"), IdC1ButtonColorsDisabled::new),
      entry(qName("Enabled"), IdC1ButtonColorsEnabled::new),
      entry(qName("Hover"), IdC1ButtonColorsHover::new),
      entry(qName("Pressed"), IdC1ButtonColorsPressed::new)
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    switch (result) {
      case final IdC1ButtonStateColors c -> {
        switch (c.semantic()) {
          case "Disabled" -> {
            this.disabled = c.colors();
          }
          case "Enabled" -> {
            this.enabled = c.colors();
          }
          case "Pressed" -> {
            this.pressed = c.colors();
          }
          case "Hover" -> {
            this.hover = c.colors();
          }
          default -> {
            throw new IllegalArgumentException(
              "Unrecognized semantic: %s".formatted(c.semantic())
            );
          }
        }
      }
      default -> {
        throw new IllegalArgumentException(
          "Unrecognized element: %s".formatted(result)
        );
      }
    }
  }

  @Override
  public CxButtonColors onElementFinished(
    final BTElementParsingContextType context)
  {
    return new CxButtonColors(
      this.enabled,
      this.disabled,
      this.pressed,
      this.hover
    );
  }
}
