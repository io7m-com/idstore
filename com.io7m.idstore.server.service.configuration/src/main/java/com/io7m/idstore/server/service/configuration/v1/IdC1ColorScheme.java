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
import com.io7m.idstore.server.api.IdServerColorScheme;

import java.util.Map;

import static com.io7m.idstore.server.service.configuration.v1.IdC1Names.qName;
import static java.util.Map.entry;

final class IdC1ColorScheme
  implements BTElementHandlerType<Object, IdServerColorScheme>
{
  private IdC1Color errorBorderColor;
  private IdC1Color headerBackgroundColor;
  private IdC1Color headerLinkColor;
  private IdC1Color headerTextColor;
  private IdC1Color mainBackgroundColor;
  private IdC1Color mainLinkColor;
  private IdC1Color mainMessageBorderColor;
  private IdC1Color mainTableBorderColor;
  private IdC1Color mainTextColor;
  private CxButtonColors buttonColors;

  IdC1ColorScheme(
    final BTElementParsingContextType context)
  {

  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      entry(qName("ButtonColors"), IdC1ButtonColors::new),
      entry(qName("ErrorBorderColor"), IdC1ErrorBorderColor::new),
      entry(qName("HeaderBackgroundColor"), IdC1HeaderBackgroundColor::new),
      entry(qName("HeaderLinkColor"), IdC1HeaderLinkColor::new),
      entry(qName("HeaderTextColor"), IdC1HeaderTextColor::new),
      entry(qName("MainBackgroundColor"), IdC1MainBackgroundColor::new),
      entry(qName("MainLinkColor"), IdC1MainLinkColor::new),
      entry(qName("MainMessageBorderColor"), IdC1MainMessageBorderColor::new),
      entry(qName("MainTableBorderColor"), IdC1MainTableBorderColor::new),
      entry(qName("MainTextColor"), IdC1MainTextColor::new)
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
    throws Exception
  {
    switch (result) {
      case final CxButtonColors colors -> {
        this.buttonColors = colors;
      }

      case final IdC1Color color -> {
        switch (color.semantic()) {
          case "ErrorBorderColor" -> {
            this.errorBorderColor = color;
          }
          case "HeaderBackgroundColor" -> {
            this.headerBackgroundColor = color;
          }
          case "HeaderLinkColor" -> {
            this.headerLinkColor = color;
          }
          case "HeaderTextColor" -> {
            this.headerTextColor = color;
          }
          case "MainBackgroundColor" -> {
            this.mainBackgroundColor = color;
          }
          case "MainLinkColor" -> {
            this.mainLinkColor = color;
          }
          case "MainMessageBorderColor" -> {
            this.mainMessageBorderColor = color;
          }
          case "MainTableBorderColor" -> {
            this.mainTableBorderColor = color;
          }
          case "MainTextColor" -> {
            this.mainTextColor = color;
          }
          default -> {
            throw new IllegalArgumentException(
              "Unrecognized color semantic: %s".formatted(color.semantic())
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
  public IdServerColorScheme onElementFinished(
    final BTElementParsingContextType context)
  {
    return new IdServerColorScheme(
      this.buttonColors,
      this.errorBorderColor.color(),
      this.headerBackgroundColor.color(),
      this.headerLinkColor.color(),
      this.headerTextColor.color(),
      this.mainBackgroundColor.color(),
      this.mainLinkColor.color(),
      this.mainMessageBorderColor.color(),
      this.mainTableBorderColor.color(),
      this.mainTextColor.color()
    );
  }
}
