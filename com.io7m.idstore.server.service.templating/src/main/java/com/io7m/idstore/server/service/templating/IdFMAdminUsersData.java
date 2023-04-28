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

package com.io7m.idstore.server.service.templating;

import com.io7m.idstore.model.IdUserSummary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Data for the "users" admin screen template.
 *
 * @param htmlTitle             The page title
 * @param pageHeaderTitle       The page header title
 * @param users                 The users
 * @param pagePreviousAvailable There is a previous page
 * @param pageNextAvailable     There is a next page
 * @param pageNumber            The page number
 * @param pageCount             The page count
 * @param search                The search query
 */

public record IdFMAdminUsersData(
  String htmlTitle,
  String pageHeaderTitle,
  List<IdUserSummary> users,
  boolean pagePreviousAvailable,
  boolean pageNextAvailable,
  int pageNumber,
  long pageCount,
  Optional<String> search)
  implements IdFMDataModelType
{
  /**
   * Data for the "users" admin screen template.
   *
   * @param htmlTitle             The page title
   * @param pageHeaderTitle       The page header title
   * @param users                 The users
   * @param pagePreviousAvailable There is a previous page
   * @param pageNextAvailable     There is a next page
   * @param pageNumber            The page number
   * @param pageCount             The page count
   * @param search                The search query
   */

  public IdFMAdminUsersData
  {
    Objects.requireNonNull(htmlTitle, "htmlTitle");
    Objects.requireNonNull(pageHeaderTitle, "pageHeaderTitle");
    Objects.requireNonNull(users, "users");
  }

  @Override
  public Map<String, Object> toTemplateHash()
  {
    final var m = new HashMap<String, Object>();
    m.put("htmlTitle", this.htmlTitle());
    m.put("pageHeaderTitle", this.pageHeaderTitle());
    m.put("users", this.users());
    m.put("pagePrevious", this.pagePreviousAvailable);
    m.put("pageNext", this.pageNextAvailable);
    m.put("pageNumberCurrent", this.pageNumber);
    m.put("pageNumberMaximum", this.pageCount);
    m.put("searchQuery", this.search.orElse(""));
    return m;
  }
}
