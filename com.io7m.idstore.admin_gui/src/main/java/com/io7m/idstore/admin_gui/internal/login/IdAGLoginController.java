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

import com.io7m.idstore.admin_gui.IdAGConfiguration;
import com.io7m.idstore.admin_gui.internal.IdAGCSS;
import com.io7m.idstore.admin_gui.internal.IdAGStrings;
import com.io7m.idstore.admin_gui.internal.client.IdAGClientService;
import com.io7m.idstore.admin_gui.internal.preferences.IdAGBookmarkStringConverter;
import com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferenceServerBookmark;
import com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferenceServerUsernamePassword;
import com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferences;
import com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferencesServiceType;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import com.io7m.junreachable.UnreachableCodeException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * The login dialog controller.
 */

public final class IdAGLoginController implements Initializable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAGLoginController.class);

  private final IdAGConfiguration configuration;
  private final Stage stage;
  private final IdServiceDirectoryType mainServices;
  private final IdAGPreferencesServiceType preferences;
  private final IdAGStrings strings;
  private final IdAGClientService client;

  @FXML private Button bookmarkCreate;
  @FXML private Button bookmarkDelete;
  @FXML private Button cancelButton;
  @FXML private Button connectButton;
  @FXML private TextField hostField;
  @FXML private Label hostFieldBad;
  @FXML private TextField portField;
  @FXML private Label portFieldBad;
  @FXML private TextField userField;
  @FXML private Label userFieldBad;
  @FXML private PasswordField passField;
  @FXML private Label passFieldBad;
  @FXML private CheckBox httpsBox;
  @FXML private GridPane grid;
  @FXML private ComboBox<IdAGPreferenceServerBookmark> bookmarks;
  @FXML private HBox bookmarksContainer;

  /**
   * The login dialog controller.
   *
   * @param inMainServices  The service directory
   * @param inConfiguration The configuration
   * @param inStage         The host stage
   */

  public IdAGLoginController(
    final IdServiceDirectoryType inMainServices,
    final IdAGConfiguration inConfiguration,
    final Stage inStage)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.mainServices =
      Objects.requireNonNull(inMainServices, "mainServices");
    this.preferences =
      this.mainServices.requireService(IdAGPreferencesServiceType.class);
    this.strings =
      this.mainServices.requireService(IdAGStrings.class);
    this.client =
      this.mainServices.requireService(IdAGClientService.class);
  }

  @FXML
  private void onHTTPSBoxChanged()
  {
    this.validate();
  }

  private String uriStringNow()
  {
    final var builder = new StringBuilder(128);
    if (this.httpsBox.isSelected()) {
      builder.append("https://");
    } else {
      builder.append("http://");
    }
    builder.append(this.hostField.getCharacters());

    final var portText = this.portField.getCharacters();
    if (!portText.isEmpty()) {
      builder.append(":");
      builder.append(portText);
    }

    builder.append("/");
    return builder.toString();
  }

  @FXML
  private void onConnect()
  {
    final var connect =
      this.validate().orElseThrow();

    if (connect.credentials() instanceof IdAGPreferenceServerUsernamePassword creds) {
      this.client.login(
        connect.host(),
        connect.port(),
        connect.isHTTPs(),
        creds.username(),
        creds.password()
      );
    } else {
      throw new UnreachableCodeException();
    }

    this.stage.close();
  }

  private void bookmarkDeleteNow(
    final String name)
  {
    try {
      LOG.debug("delete bookmark {}", name);

      this.preferences.update(oldPreferences -> {
        final var newBookmarks =
          new ArrayList<>(oldPreferences.serverBookmarks());

        newBookmarks.removeIf(mark -> Objects.equals(mark.name(), name));
        return new IdAGPreferences(
          oldPreferences.installationId(),
          oldPreferences.debuggingEnabled(),
          List.copyOf(newBookmarks),
          oldPreferences.recentFiles()
        );
      });
    } catch (final IOException e) {
      LOG.error("unable to save bookmarks: ", e);
    }
  }

  private void bookmarkSaveNow(
    final String name,
    final IdAGPreferenceServerBookmark newBookmark)
  {
    try {
      LOG.debug("save bookmark {}", name);

      this.preferences.update(oldPreferences -> {
        final var newBookmarks =
          new ArrayList<>(oldPreferences.serverBookmarks());

        newBookmarks.removeIf(mark -> Objects.equals(mark.name(), name));
        newBookmarks.add(newBookmark);
        return new IdAGPreferences(
          oldPreferences.installationId(),
          oldPreferences.debuggingEnabled(),
          List.copyOf(newBookmarks),
          oldPreferences.recentFiles()
        );
      });
    } catch (final IOException e) {
      LOG.error("unable to save bookmarks: ", e);
    }
  }

  @FXML
  private Optional<IdAGPreferenceServerBookmark> validate()
  {
    boolean ok = true;

    int port = 0;
    try {
      final var portText = this.portField.getCharacters().toString();
      if (!portText.isEmpty()) {
        port = Integer.parseUnsignedInt(portText);
      }
      this.portFieldBad.setVisible(false);
    } catch (final NumberFormatException e) {
      this.portFieldBad.setVisible(true);
      ok = false;
    }

    final var hostBad = this.hostField.getCharacters().isEmpty();
    if (hostBad) {
      this.hostFieldBad.setVisible(true);
      ok = false;
    } else {
      this.hostFieldBad.setVisible(false);
    }

    final var userBad = this.userField.getCharacters().isEmpty();
    if (userBad) {
      this.userFieldBad.setVisible(true);
      ok = false;
    } else {
      this.userFieldBad.setVisible(false);
    }

    final var passBad = this.passField.getCharacters().isEmpty();
    if (passBad) {
      this.passFieldBad.setVisible(true);
      ok = false;
    } else {
      this.passFieldBad.setVisible(false);
    }

    try {
      new URI(this.uriStringNow());
    } catch (final URISyntaxException e) {
      ok = false;
    }

    if (ok) {
      this.bookmarkCreate.setDisable(false);
      this.connectButton.setDisable(false);

      return Optional.of(
        new IdAGPreferenceServerBookmark(
          "",
          this.hostField.getCharacters().toString(),
          port,
          this.httpsBox.isSelected(),
          new IdAGPreferenceServerUsernamePassword(
            this.userField.getCharacters().toString(),
            this.passField.getCharacters().toString()
          )
        ));
    }

    this.bookmarkCreate.setDisable(true);
    this.connectButton.setDisable(true);
    return Optional.empty();
  }

  @FXML
  private void onRequestBookmarkDelete()
  {
    final var selected =
      this.bookmarks.getSelectionModel()
        .getSelectedItem();

    if (selected == null) {
      return;
    }

    this.bookmarkDeleteNow(selected.name());
    this.reloadBookmarks();
  }

  @FXML
  private void onRequestBookmarkCreate()
  {
    final var dialog = new TextInputDialog();
    dialog.setTitle(this.strings.format("connect.bookmark.createTitle"));
    dialog.setHeaderText(null);
    dialog.setContentText(this.strings.format("connect.bookmark.createMain"));
    IdAGCSS.setCSS(this.configuration, dialog.getDialogPane());

    final var nameOpt = dialog.showAndWait();
    nameOpt.ifPresent(name -> {
      this.validate().ifPresent(newBookmark -> {
        this.bookmarkSaveNow(name, newBookmark);
        this.reloadBookmarks();
      });
    });
  }

  @FXML
  private void onCancel()
  {
    this.stage.close();
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.hostFieldBad.setVisible(false);
    this.userFieldBad.setVisible(false);
    this.portFieldBad.setVisible(false);
    this.passFieldBad.setVisible(false);
    this.connectButton.setDisable(true);

    this.portField.setText("51000");

    this.bookmarks.setConverter(new IdAGBookmarkStringConverter());
    this.bookmarks.getSelectionModel()
      .selectedItemProperty()
      .addListener(
        (observable, oldValue, newValue) ->
          this.onSelectedBookmark(newValue)
      );

    this.reloadBookmarks();
    this.bookmarks.getSelectionModel()
      .selectFirst();

    Platform.runLater(() -> {
      this.hostField.requestFocus();
    });
  }

  private void reloadBookmarks()
  {
    final var items = this.bookmarks.getItems();
    items.clear();
    items.addAll(this.preferences.preferences().serverBookmarks());
  }

  private void onSelectedBookmark(
    final IdAGPreferenceServerBookmark bookmark)
  {
    if (bookmark == null) {
      return;
    }

    this.hostField.setText(bookmark.host());
    this.portField.setText(Integer.toUnsignedString(bookmark.port()));
    this.httpsBox.setSelected(bookmark.isHTTPs());

    if (bookmark.credentials()
      instanceof IdAGPreferenceServerUsernamePassword usernamePassword) {
      this.userField.setText(usernamePassword.username());
      this.passField.setText(usernamePassword.password());
    }

    this.validate();
  }
}
