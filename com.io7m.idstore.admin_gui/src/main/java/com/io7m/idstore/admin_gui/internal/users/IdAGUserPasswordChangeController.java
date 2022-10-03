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


package com.io7m.idstore.admin_gui.internal.users;

import com.io7m.idstore.admin_gui.IdAGConfiguration;
import com.io7m.idstore.admin_gui.internal.IdAGCSS;
import com.io7m.idstore.admin_gui.internal.IdAGStrings;
import com.io7m.idstore.admin_gui.internal.main.IdAGMainScreenController;
import com.io7m.idstore.admin_gui.internal.main.IdAGScreenControllerType;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * A password creation controller.
 */

public final class IdAGUserPasswordChangeController
  implements IdAGScreenControllerType
{
  private final IdAGConfiguration configuration;
  private final IdAGStrings strings;
  private final Stage stage;

  @FXML private Node passwordFieldBad;
  @FXML private Node passwordConfirmFieldBad;
  @FXML private TextField passwordField;
  @FXML private TextField passwordConfirmField;
  @FXML private Button buttonChange;
  @FXML private Button buttonCancel;

  private Optional<IdPassword> result;

  /**
   * A password creation controller.
   *
   * @param inConfiguration The configuration
   * @param inStrings       The string resources
   * @param inStage         The owning stage
   */

  public IdAGUserPasswordChangeController(
    final IdAGConfiguration inConfiguration,
    final IdAGStrings inStrings,
    final Stage inStage)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.strings =
      Objects.requireNonNull(inStrings, "inStrings");
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.result =
      Optional.empty();
  }

  /**
   * @return The result of the password creation, if creation wasn't cancelled
   */

  public Optional<IdPassword> result()
  {
    return this.result;
  }

  @FXML
  private void onCancelSelected()
  {
    this.stage.close();
  }

  @FXML
  private void onChangeSelected()
  {
    try {
      this.result = Optional.of(
        IdPasswordAlgorithmPBKDF2HmacSHA256.create()
          .createHashed(this.passwordField.getText())
      );
      this.stage.close();
    } catch (final IdPasswordException e) {
      throw new IllegalStateException(e);
    }
  }

  @FXML
  private void onFieldTyped()
  {
    this.passwordFieldBad.setVisible(false);
    this.passwordConfirmFieldBad.setVisible(false);

    final var passwordsSame =
      this.passwordField.getText().trim()
        .equals(this.passwordConfirmField.getText().trim());

    final var passwordNonEmpty =
      !this.passwordField.getText()
        .isEmpty();

    final var passwordOk =
      passwordsSame && passwordNonEmpty;

    this.buttonChange.setDisable(!passwordOk);
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.passwordConfirmFieldBad.setVisible(true);
    this.passwordFieldBad.setVisible(true);
    this.buttonChange.setDisable(true);
  }

  /**
   * Open a dialog box.
   *
   * @param configuration The configuration
   * @param strings       The string resources
   *
   * @return The controller
   *
   * @throws IOException On I/O errors
   */

  public static IdAGUserPasswordChangeController openDialog(
    final IdAGConfiguration configuration,
    final IdAGStrings strings)
    throws IOException
  {
    final var stage = new Stage();
    final var connectXML =
      IdAGMainScreenController.class.getResource(
        "/com/io7m/idstore/admin_gui/internal/passwordChange.fxml");

    final var resources =
      strings.resources();
    final var loader =
      new FXMLLoader(connectXML, resources);

    loader.setControllerFactory(
      clazz -> new IdAGUserPasswordChangeController(
        configuration,
        strings,
        stage)
    );

    final Pane pane = loader.load();
    IdAGCSS.setCSS(configuration, pane);

    final IdAGUserPasswordChangeController controller = loader.getController();
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setScene(new Scene(pane));
    stage.setTitle(strings.format("users.passwordChange.title"));
    stage.showAndWait();
    return controller;
  }
}
