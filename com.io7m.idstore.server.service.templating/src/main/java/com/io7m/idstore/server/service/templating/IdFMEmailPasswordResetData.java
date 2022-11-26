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

package com.io7m.idstore.server.service.templating;

import com.io7m.idstore.model.IdUserPasswordReset;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data for email password reset templates.
 *
 * @param productTitle The product title
 * @param reset        The reset
 * @param host         The host
 * @param userAgent    The user agent
 * @param linkConfirm  The link to confirm and select a new password
 */

public record IdFMEmailPasswordResetData(
  String productTitle,
  IdUserPasswordReset reset,
  String host,
  String userAgent,
  URI linkConfirm)
  implements IdFMDataModelType
{
  /**
   * Data for email password reset templates.
   *
   * @param productTitle The product title
   * @param reset        The reset
   * @param host         The host
   * @param userAgent    The user agent
   * @param linkConfirm  The link to confirm and select a new password
   */

  public IdFMEmailPasswordResetData
  {
    Objects.requireNonNull(productTitle, "productTitle");
    Objects.requireNonNull(reset, "reset");
    Objects.requireNonNull(host, "host");
    Objects.requireNonNull(userAgent, "userAgent");
    Objects.requireNonNull(linkConfirm, "linkAllow");
  }

  @Override
  public Map<String, Object> toTemplateHash()
  {
    final var m = new HashMap<String, Object>();
    m.put("productTitle", this.productTitle());
    m.put("reset", this.reset());
    m.put("host", this.host());
    m.put("userAgent", this.userAgent());
    m.put("linkConfirm", this.linkConfirm());
    return m;
  }
}
