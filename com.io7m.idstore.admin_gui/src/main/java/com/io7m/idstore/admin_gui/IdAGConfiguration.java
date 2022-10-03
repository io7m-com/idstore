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

package com.io7m.idstore.admin_gui;

import com.io7m.jade.api.ApplicationDirectoriesType;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration information for the UI.
 *
 * @param locale      The locale
 * @param directories The directories for the application
 * @param customCSS   The custom CSS, if any
 */

public record IdAGConfiguration(
  Locale locale,
  ApplicationDirectoriesType directories,
  Optional<URI> customCSS)
{
  /**
   * Configuration information for the UI.
   */

  public IdAGConfiguration
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(directories, "directories");
    Objects.requireNonNull(customCSS, "customCSS");
  }
}
