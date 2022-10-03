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
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUserCreate;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
 * The user creation controller.
 */

public final class IdAGUserCreateController
  implements Initializable
{
  private final Stage stage;
  private Optional<IdUserCreate> result;

  @FXML private Button cancelButton;
  @FXML private Button createButton;
  @FXML private Node idNameFieldBad;
  @FXML private TextField idNameField;
  @FXML private Node realNameFieldBad;
  @FXML private TextField realNameField;
  @FXML private Node passwordFieldBad;
  @FXML private TextField passwordField;
  @FXML private Node passwordConfirmFieldBad;
  @FXML private TextField passwordConfirmField;
  @FXML private Node emailFieldBad;
  @FXML private TextField emailField;

  /**
   * The user creation controller.
   *
   * @param inStage The stage hosting the controller
   */

  public IdAGUserCreateController(
    final Stage inStage)
  {
    this.stage = Objects.requireNonNull(inStage, "stage");
    this.result = Optional.empty();
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.createButton.setDisable(true);

    this.idNameFieldBad.setVisible(true);
    this.realNameFieldBad.setVisible(true);
    this.passwordFieldBad.setVisible(false);
    this.passwordConfirmFieldBad.setVisible(false);
    this.emailFieldBad.setVisible(true);
  }

  /**
   * The cancel button was selected.
   */

  @FXML
  private void onCancelSelected()
  {
    this.stage.close();
  }

  /**
   * The create button was selected.
   */

  @FXML
  private void onCreateSelected()
  {
    try {
      this.result =
        Optional.of(
          new IdUserCreate(
            Optional.empty(),
            new IdName(this.idNameField.getText()),
            new IdRealName(this.realNameField.getText()),
            new IdEmail(this.emailField.getText()),
            IdPasswordAlgorithmPBKDF2HmacSHA256.create()
              .createHashed(this.passwordField.getText())
          )
        );
      this.stage.close();
    } catch (final IdPasswordException e) {
      throw new IllegalStateException(e);
    }
  }

  @FXML
  private void onFieldChanged()
  {
    var ok = true;

    try {
      new IdName(this.idNameField.getText().trim());
      this.idNameFieldBad.setVisible(false);
    } catch (final Exception e) {
      this.idNameFieldBad.setVisible(true);
      ok = false;
    }

    try {
      new IdRealName(this.realNameField.getText().trim());
      this.realNameFieldBad.setVisible(false);
    } catch (final Exception e) {
      this.realNameFieldBad.setVisible(true);
      ok = false;
    }

    try {
      new IdEmail(this.emailField.getText().trim());
      this.emailFieldBad.setVisible(false);
    } catch (final Exception e) {
      this.emailFieldBad.setVisible(true);
      ok = false;
    }

    final var passwordOk =
      this.passwordField.getText()
        .trim()
        .equals(this.passwordConfirmField.getText().trim());

    this.passwordConfirmFieldBad.setVisible(!passwordOk);
    this.passwordFieldBad.setVisible(!passwordOk);
    ok &= passwordOk;

    this.createButton.setDisable(!ok);
  }

  /**
   * @return The result, if any
   */

  public Optional<IdUserCreate> result()
  {
    return this.result;
  }

  /**
   * Open a user creation dialog.
   *
   * @param configuration The configuration
   * @param strings       The strings
   *
   * @return A dialog
   *
   * @throws IOException On errors
   */

  public static IdAGUserCreateController openDialog(
    final IdAGConfiguration configuration,
    final IdAGStrings strings)
    throws IOException
  {
    final var stage = new Stage();
    final var connectXML =
      IdAGMainScreenController.class.getResource(
        "/com/io7m/idstore/admin_gui/internal/userCreate.fxml");

    final var resources =
      strings.resources();
    final var loader =
      new FXMLLoader(connectXML, resources);

    loader.setControllerFactory(clazz -> new IdAGUserCreateController(stage));

    final Pane pane = loader.load();
    IdAGCSS.setCSS(configuration, pane);

    final IdAGUserCreateController controller = loader.getController();
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setScene(new Scene(pane));
    stage.setTitle(strings.format("userCreate.title"));
    stage.showAndWait();
    return controller;
  }
}
