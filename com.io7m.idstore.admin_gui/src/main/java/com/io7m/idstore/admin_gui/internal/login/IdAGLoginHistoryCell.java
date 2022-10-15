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
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A login cell.
 */

public final class IdAGLoginHistoryCell extends ListCell<IdLogin>
{
  private final Pane root;
  private final IdAGLoginHistoryCellController controller;

  /**
   * A login cell.
   *
   * @param strings The string resources
   */

  public IdAGLoginHistoryCell(
    final IdAGStrings strings)
  {
    try {
      final FXMLLoader loader =
        new FXMLLoader(
          IdAGLoginHistoryCell.class.getResource(
            "/com/io7m/idstore/admin_gui/internal/loginHistoryCell.fxml"));
      loader.setResources(strings.resources());

      this.root = loader.load();
      this.controller = loader.getController();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected void updateItem(
    final IdLogin item,
    final boolean empty)
  {
    super.updateItem(item, empty);
    if (empty || item == null) {
      this.setGraphic(null);
    } else {
      this.controller.setLogin(item);
      this.setGraphic(this.root);
    }
  }
}
