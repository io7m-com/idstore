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
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerColorScheme;
import org.xml.sax.Attributes;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static com.io7m.idstore.server.service.configuration.v1.IdC1Names.qName;
import static java.util.Map.entry;

final class IdC1Branding
  implements BTElementHandlerType<Object, IdServerBrandingConfiguration>
{
  private Optional<IdServerColorScheme> scheme;
  private String title;
  private Optional<Path> logo;
  private Optional<Path> loginExtra;

  IdC1Branding(
    final BTElementParsingContextType context)
  {
    this.scheme = Optional.empty();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      entry(qName("ColorScheme"), IdC1ColorScheme::new)
    );
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.title =
      attributes.getValue("ProductTitle");
    this.logo =
      Optional.ofNullable(attributes.getValue("Logo"))
        .map(Path::of);
    this.loginExtra =
      Optional.ofNullable(attributes.getValue("LoginExtra"))
        .map(Path::of);
  }

  @Override
  public IdServerBrandingConfiguration onElementFinished(
    final BTElementParsingContextType context)
  {
    return new IdServerBrandingConfiguration(
      this.title,
      this.logo,
      this.loginExtra,
      this.scheme
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    switch (result) {
      case final IdServerColorScheme s -> {
        this.scheme = Optional.of(s);
      }
      default -> {
        throw new IllegalArgumentException(
          "Unrecognized element: %s".formatted(result)
        );
      }
    }
  }
}
