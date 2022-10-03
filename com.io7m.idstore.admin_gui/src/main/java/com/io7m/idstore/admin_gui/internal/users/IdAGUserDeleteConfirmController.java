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

import com.io7m.idstore.model.IdUser;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A user deletion controller.
 */

public final class IdAGUserDeleteConfirmController
  implements Initializable
{
  private final IdUser user;
  private final Stage stage;
  private boolean result;

  @FXML private Button cancel;
  @FXML private Button delete;
  @FXML private TextField userNameField;

  /**
   * A user deletion controller.
   *
   * @param inUser  The user
   * @param inStage The stage hosting the dialog
   */

  public IdAGUserDeleteConfirmController(
    final IdUser inUser,
    final Stage inStage)
  {
    this.user =
      Objects.requireNonNull(inUser, "user");
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.result =
      false;
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.delete.setDisable(true);
  }

  @FXML
  private void onCancelSelected()
  {
    this.result = false;
    this.stage.close();
  }

  @FXML
  private void onDeleteSelected()
  {
    this.result = true;
    this.stage.close();
  }

  @FXML
  private void onUserNameFieldChanged()
  {
    this.delete.setDisable(
      !Objects.equals(this.userNameField.getText(), this.user.idName().value())
    );
  }

  /**
   * @return {@code true} if deletion is confirmed
   */

  public boolean isDeleteRequested()
  {
    return this.result;
  }
}
