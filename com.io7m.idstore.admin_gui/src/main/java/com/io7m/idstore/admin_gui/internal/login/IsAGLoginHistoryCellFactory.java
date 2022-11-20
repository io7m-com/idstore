/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.idstore.admin_gui.internal.login;

import com.io7m.idstore.admin_gui.internal.IdAGStrings;
import com.io7m.idstore.model.IdLogin;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.Objects;

/**
 * A factory of user login history cells.
 */

public final class IsAGLoginHistoryCellFactory
  implements Callback<
  ListView<IdLogin>,
  ListCell<IdLogin>>
{
  private final IdAGStrings strings;

  /**
   * A factory of user login history cells.
   *
   * @param inStrings The string resources
   */

  public IsAGLoginHistoryCellFactory(
    final IdAGStrings inStrings)
  {
    this.strings = Objects.requireNonNull(inStrings, "inStrings");
  }

  @Override
  public ListCell<IdLogin> call(
    final ListView<IdLogin> param)
  {
    return new IdAGLoginHistoryCell(this.strings);
  }
}
