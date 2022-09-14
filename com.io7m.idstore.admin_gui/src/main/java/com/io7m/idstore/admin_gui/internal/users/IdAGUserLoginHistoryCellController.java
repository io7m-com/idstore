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

package com.io7m.idstore.admin_gui.internal.users;

import com.io7m.idstore.model.IdLogin;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * A user cell controller.
 */

public final class IdAGUserLoginHistoryCellController
{
  @FXML private Label loginTime;
  @FXML private Label loginHost;
  @FXML private Label loginAgent;

  /**
   * A user cell controller.
   */

  public IdAGUserLoginHistoryCellController()
  {

  }

  /**
   * Set the current login.
   *
   * @param login The login
   */

  public void setLogin(
    final IdLogin login)
  {
    this.loginTime.setText(login.time().toString());
    this.loginHost.setText(login.host());
    this.loginAgent.setText(login.userAgent());
  }
}
