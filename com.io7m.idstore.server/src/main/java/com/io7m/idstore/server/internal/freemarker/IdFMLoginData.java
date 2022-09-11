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

package com.io7m.idstore.server.internal.freemarker;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Data for the login screen template.
 *
 * @param htmlTitle       The HTML title
 * @param pageHeaderTitle The page header title
 * @param logo            {@code true} if the logo should be displayed
 * @param loginTitle The login title, if any
 * @param errorMessage    The error message, if any
 */

public record IdFMLoginData(
  String htmlTitle,
  String pageHeaderTitle,
  boolean logo,
  Optional<String> loginTitle,
  Optional<String> errorMessage)
  implements IdFMDataModelType
{
  /**
   * Data for the login screen template.
   *
   * @param htmlTitle       The HTML title
   * @param pageHeaderTitle The page header title
   * @param logo            {@code true} if the logo should be displayed
   * @param loginTitle The login title, if any
   * @param errorMessage    The error message, if any
   */

  public IdFMLoginData
  {
    Objects.requireNonNull(htmlTitle, "htmlTitle");
    Objects.requireNonNull(pageHeaderTitle, "pageHeaderTitle");
    Objects.requireNonNull(loginTitle, "loginTitle");
    Objects.requireNonNull(errorMessage, "errorMessage");
  }

  @Override
  public Map<String, Object> toTemplateHash()
  {
    final var m = new HashMap<String, Object>();
    m.put("htmlTitle", this.htmlTitle);
    m.put("pageHeaderTitle", this.pageHeaderTitle);
    m.put("logo", Boolean.valueOf(this.logo));
    this.loginTitle.ifPresent(title -> m.put("loginTitle", title));
    this.errorMessage().ifPresent(error -> m.put("errorMessage", error));
    return m;
  }
}
