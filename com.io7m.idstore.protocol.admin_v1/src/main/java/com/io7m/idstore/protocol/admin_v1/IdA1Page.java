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

package com.io7m.idstore.protocol.admin_v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolToModelType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A page of items.
 *
 * @param items           The items
 * @param pageIndex       The page index (starting at 0)
 * @param pageCount       The total page count
 * @param pageFirstOffset The offset of the first item in the list
 * @param <T>             The type of data
 * @param <U>             The type of model data
 */

public record IdA1Page<U, T extends IdProtocolToModelType<U>>(
  @JsonProperty(value = "PageItems", required = true)
  List<T> items,
  @JsonProperty(value = "PageIndex", required = true)
  int pageIndex,
  @JsonProperty(value = "PageCount", required = true)
  int pageCount,
  @JsonProperty(value = "PageFirstOffset", required = true)
  long pageFirstOffset)
  implements IdProtocolToModelType<IdPage<U>>
{
  /**
   * A page of items.
   */

  public IdA1Page
  {
    Objects.requireNonNull(items, "items");
  }

  @Override
  public IdPage<U> toModel()
    throws IdProtocolException
  {
    final var data = new ArrayList<U>();
    for (final var i : this.items) {
      data.add(i.toModel());
    }

    return new IdPage<>(
      List.copyOf(data),
      this.pageIndex,
      this.pageCount,
      this.pageFirstOffset
    );
  }
}
