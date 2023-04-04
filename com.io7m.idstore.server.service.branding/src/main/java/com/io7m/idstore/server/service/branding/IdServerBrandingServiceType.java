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


package com.io7m.idstore.server.service.branding;

import com.io7m.repetoir.core.RPServiceType;

import java.util.Optional;

/**
 * The service that supplies branding information.
 */

public interface IdServerBrandingServiceType extends RPServiceType
{
  /**
   * @return The bytes of an SVG logo image
   */

  byte[] logoImage();

  /**
   * @return The product title
   */

  String title();

  /**
   * @return The xButton CSS
   */

  String xButtonCSS();

  /**
   * @return The CSS text
   */

  String css();

  /**
   * @param name The name/phrase
   *
   * @return An HTML title for the given name/phrase
   */

  String htmlTitle(
    String name);

  /**
   * @return The extra text inserted below the login form (XHTML)
   */

  Optional<String> loginExtraText();

  /**
   * The branded email subject.
   *
   * @param subject The subject txt
   *
   * @return The branded string
   */

  String emailSubject(
    String subject);
}
