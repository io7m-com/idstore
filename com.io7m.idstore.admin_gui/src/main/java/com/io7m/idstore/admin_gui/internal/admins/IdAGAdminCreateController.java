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


package com.io7m.idstore.admin_gui.internal.admins;

import com.io7m.idstore.admin_gui.IdAGConfiguration;
import com.io7m.idstore.admin_gui.internal.IdAGCSS;
import com.io7m.idstore.admin_gui.internal.IdAGStrings;
import com.io7m.idstore.admin_gui.internal.main.IdAGMainScreenController;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminCreate;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.jaffirm.core.Invariants;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The admin creation controller.
 */

public final class IdAGAdminCreateController
  implements Initializable
{
  private final Stage stage;
  private final IdAdmin currentAdmin;
  private final ObservableList<IdAdminPermission> permissionsAssigned;
  private final ObservableList<IdAdminPermission> permissionsAvailable;
  private Optional<IdAdminCreate> result;

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
  @FXML private ListView<IdAdminPermission> permissionsAvailableView;
  @FXML private ListView<IdAdminPermission> permissionsAssignedView;
  @FXML private Button permissionAssignButton;
  @FXML private Button permissionUnassignButton;

  /**
   * The admin creation controller.
   *
   * @param inStage               The stage hosting the controller
   * @param adminPerformingCreate The admin performing the creation (not the
   *                              admin being created)
   */

  public IdAGAdminCreateController(
    final Stage inStage,
    final IdAdmin adminPerformingCreate)
  {
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.currentAdmin =
      Objects.requireNonNull(adminPerformingCreate, "adminPerformingCreate");

    this.result = Optional.empty();

    this.permissionsAssigned =
      FXCollections.observableArrayList();
    this.permissionsAvailable =
      FXCollections.observableArrayList();

    this.permissionsAvailable.setAll(
      this.currentAdmin.permissions()
        .impliedPermissions()
        .stream()
        .sorted(Comparator.comparing(Enum::toString))
        .toList()
    );
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

    this.permissionsAvailableView.setItems(
      this.permissionsAvailable);
    this.permissionsAssignedView.setItems(
      this.permissionsAssigned);

    this.permissionUnassignButton.setDisable(true);
    this.permissionAssignButton.setDisable(true);

    this.permissionsAvailableView.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);
    this.permissionsAssignedView.getSelectionModel()
      .setSelectionMode(SelectionMode.MULTIPLE);

    this.permissionsAvailableView.getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, permThen, permNow) -> {
        this.onPermissionAvailableSelectionChanged();
      });

    this.permissionsAssignedView.getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, permThen, permNow) -> {
        this.onPermissionAssignedSelectionChanged();
      });
  }

  private void onPermissionAssignedSelectionChanged()
  {
    this.permissionUnassignButton.setDisable(
      this.permissionsAssignedView.getSelectionModel()
        .getSelectedItems()
        .isEmpty()
    );
  }

  private void onPermissionAvailableSelectionChanged()
  {
    this.permissionAssignButton.setDisable(
      this.permissionsAvailableView.getSelectionModel()
        .getSelectedItems()
        .isEmpty()
    );
  }

  @FXML
  private void onPermissionAssignSelected()
  {
    final var toAssign =
      this.permissionsAvailableView.getSelectionModel()
        .getSelectedItems()
        .stream()
        .collect(Collectors.toUnmodifiableSet());

    final var newAvailable =
      this.permissionsAvailable.stream()
        .filter(p -> !toAssign.contains(p))
        .collect(Collectors.toUnmodifiableSet())
        .stream()
        .sorted(Comparator.comparing(Enum::toString))
        .toList();

    final var newAssigned =
      Stream.concat(toAssign.stream(), this.permissionsAssigned.stream())
        .collect(Collectors.toUnmodifiableSet())
        .stream()
        .sorted(Comparator.comparing(Enum::toString))
        .toList();

    this.permissionsAssigned.setAll(newAssigned);
    this.permissionsAvailable.setAll(newAvailable);

    this.checkPermissionInvariants();
  }

  @FXML
  private void onPermissionUnassignSelected()
  {
    final var toUnassign =
      this.permissionsAssignedView.getSelectionModel()
        .getSelectedItems()
        .stream()
        .collect(Collectors.toUnmodifiableSet());

    final var newAvailable =
      Stream.concat(toUnassign.stream(), this.permissionsAvailable.stream())
        .collect(Collectors.toUnmodifiableSet())
        .stream()
        .sorted(Comparator.comparing(Enum::toString))
        .toList();

    final var newAssigned =
      this.permissionsAssigned.stream()
        .filter(p -> !toUnassign.contains(p))
        .collect(Collectors.toUnmodifiableSet())
        .stream()
        .sorted(Comparator.comparing(Enum::toString))
        .toList();

    this.permissionsAssigned.setAll(newAssigned);
    this.permissionsAvailable.setAll(newAvailable);

    this.checkPermissionInvariants();
  }

  private void checkPermissionInvariants()
  {
    final var allPermissions =
      this.currentAdmin.permissions().impliedPermissions();
    final var viewPermissions =
      new HashSet<IdAdminPermission>(allPermissions.size());

    viewPermissions.addAll(this.permissionsAssigned);
    viewPermissions.addAll(this.permissionsAvailable);

    Invariants.checkInvariantV(
      allPermissions.equals(viewPermissions),
      "All permissions %s must equal view permissions %s",
      allPermissions,
      viewPermissions
    );
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
          new IdAdminCreate(
            Optional.empty(),
            new IdName(this.idNameField.getText()),
            new IdRealName(this.realNameField.getText()),
            new IdEmail(this.emailField.getText()),
            IdPasswordAlgorithmPBKDF2HmacSHA256.create()
              .createHashed(this.passwordField.getText()),
            IdAdminPermissionSet.of(
              new HashSet<>(this.permissionsAssigned))
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

  public Optional<IdAdminCreate> result()
  {
    return this.result;
  }

  /**
   * Open a admin creation dialog.
   *
   * @param configuration           The configuration
   * @param strings                 The strings
   * @param adminPerformingCreation The admin performing the creation operation
   *
   * @return A dialog
   *
   * @throws IOException On errors
   */

  public static IdAGAdminCreateController openDialog(
    final IdAGConfiguration configuration,
    final IdAGStrings strings,
    final IdAdmin adminPerformingCreation)
    throws IOException
  {
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(strings, "strings");
    Objects.requireNonNull(adminPerformingCreation, "currentAdmin");

    final var stage = new Stage();
    final var connectXML =
      IdAGMainScreenController.class.getResource(
        "/com/io7m/idstore/admin_gui/internal/adminCreate.fxml");

    final var resources =
      strings.resources();
    final var loader =
      new FXMLLoader(connectXML, resources);

    loader.setControllerFactory(clazz -> {
      return new IdAGAdminCreateController(stage, adminPerformingCreation);
    });

    final Pane pane = loader.load();
    IdAGCSS.setCSS(configuration, pane);

    final IdAGAdminCreateController controller = loader.getController();
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setScene(new Scene(pane));
    stage.setTitle(strings.format("adminCreate.title"));
    stage.showAndWait();
    return controller;
  }
}
