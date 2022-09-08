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

import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Data for the "user" admin screen template.
 *
 * @param htmlTitle       The page title
 * @param pageHeaderTitle The page header title
 * @param user            The user
 * @param loginHistory    The login history
 */

public record IdFMAdminUserData(
  String htmlTitle,
  String pageHeaderTitle,
  IdUser user,
  List<IdLogin> loginHistory)
  implements IdFMDataModelType
{
  /**
   * Data for the "user" admin screen template.
   *
   * @param htmlTitle       The page title
   * @param pageHeaderTitle The page header title
   * @param user            The user
   * @param loginHistory    The login history
   */

  public IdFMAdminUserData
  {
    Objects.requireNonNull(htmlTitle, "htmlTitle");
    Objects.requireNonNull(pageHeaderTitle, "pageHeaderTitle");
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(loginHistory, "loginHistory");
  }

  @Override
  public Map<String, Object> toTemplateHash()
  {
    final var m = new HashMap<String, Object>();
    m.put("htmlTitle", this.htmlTitle());
    m.put("pageHeaderTitle", this.pageHeaderTitle());
    m.put("user", this.user());
    m.put("loginHistory", this.loginHistory());
    return m;
  }
}
