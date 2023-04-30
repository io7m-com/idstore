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

package com.io7m.idstore.admin_gui.internal.admins;

import com.io7m.hibiscus.api.HBStateType;
import com.io7m.hibiscus.api.HBStateType.HBStateDisconnected;
import com.io7m.idstore.admin_gui.IdAGConfiguration;
import com.io7m.idstore.admin_gui.internal.IdAGCSS;
import com.io7m.idstore.admin_gui.internal.IdAGStrings;
import com.io7m.idstore.admin_gui.internal.client.IdAGClientService;
import com.io7m.idstore.admin_gui.internal.main.IdAGMainScreenController;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminCreate;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static javafx.scene.control.SelectionMode.SINGLE;

/**
 * The admin tab controller.
 */

public final class IdAGAdminsController implements Initializable
{
  private final IdAGConfiguration configuration;
  private final RPServiceDirectoryType mainServices;
  private final IdAGStrings strings;
  private final IdAGClientService client;
  private final ObservableList<IdAGAdmin> admins;
  private final ObservableList<IdEmail> adminEmails;
  private final ObservableList<IdLogin> adminLoginHistory;
  private final ObservableList<IdAdminPermission> adminPermissions;
  private IdAdmin admin;
  private IdAGAdminSearchKind searchKindAtStart;

  @FXML private Button emailAdd;
  @FXML private Button emailDelete;
  @FXML private Button adminCreate;
  @FXML private Button adminDelete;
  @FXML private Button adminPageNext;
  @FXML private Button adminPagePrev;
  @FXML private ChoiceBox<IdAGAdminSearchKind> searchKind;
  @FXML private Label adminPageLabel;
  @FXML private ListView<IdAGAdmin> adminList;
  @FXML private ListView<IdEmail> adminEmailList;
  @FXML private Parent adminDetailContainer;
  @FXML private Parent adminTableContainer;
  @FXML private TextField adminIdField;
  @FXML private TextField adminIdNameField;
  @FXML private TextField adminRealNameField;
  @FXML private TextField adminSearchField;
  @FXML private TextField adminPasswordField;
  @FXML private ListView<IdAdminPermission> permissionListView;

  /**
   * The admin tab controller.
   *
   * @param inMainServices  The service directory
   * @param inConfiguration The configuration
   */

  public IdAGAdminsController(
    final RPServiceDirectoryType inMainServices,
    final IdAGConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.mainServices =
      Objects.requireNonNull(inMainServices, "mainServices");
    this.strings =
      this.mainServices.requireService(IdAGStrings.class);
    this.client =
      this.mainServices.requireService(IdAGClientService.class);
    this.admins =
      FXCollections.observableArrayList();
    this.adminEmails =
      FXCollections.observableArrayList();
    this.adminLoginHistory =
      FXCollections.observableArrayList();
    this.adminPermissions =
      FXCollections.observableArrayList();
  }

  private void onClientStatusChanged(
    final HBStateType<?, ?, ?, ?> statusNew)
  {
    if (statusNew instanceof HBStateDisconnected) {
      Platform.runLater(() -> {
        this.admins.clear();
        this.admin = null;
        this.adminDetailsLock();
        this.adminTableControlsLock();
      });
    }
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.searchKind.setConverter(
      new IdAGAdminSearchKindStringConverter(this.strings));
    this.searchKind.setItems(
      FXCollections.observableArrayList(IdAGAdminSearchKind.values())
    );
    this.searchKind.getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, kindOld, kindNew) -> {
        this.onSearchKindSelected(kindNew);
      });
    this.searchKind.getSelectionModel()
      .select(IdAGAdminSearchKind.BY_DETAILS);

    this.adminDetailsLock();
    this.adminTableControlsLock();

    this.permissionListView.setItems(this.adminPermissions);

    this.adminList.setCellFactory(new IsAGAdminCellFactory(this.strings));
    this.adminList.setFixedCellSize(24.0);
    this.adminList.getSelectionModel().setSelectionMode(SINGLE);
    this.adminList.setItems(this.admins);
    this.adminList.getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, adminOld, adminNew) -> {
        this.onAdminSelected(adminNew);
      });

    this.adminEmailList.setItems(this.adminEmails);
    this.adminEmailList.getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, emailOld, emailNew) -> {
        this.onEmailSelected(emailNew);
      });

    this.client.status()
      .addListener((obs, statusOld, statusNew) -> {
        this.onClientStatusChanged(statusNew);
      });
  }

  private void onSearchKindSelected(
    final IdAGAdminSearchKind kind)
  {
    switch (kind) {
      case BY_DETAILS -> {
        this.adminSearchField.setPromptText(
          this.strings.format("admins.searchPrompt.searchDetails")
        );
      }
      case BY_EMAIL -> {
        this.adminSearchField.setPromptText(
          this.strings.format("admins.searchPrompt.searchEmail")
        );
      }
    }
  }

  private void onEmailSelected(
    final IdEmail email)
  {
    this.emailDelete.setDisable(true);

    if (email == null) {
      return;
    }

    this.emailDelete.setDisable(this.adminEmails.size() <= 1);
  }

  private void adminTableControlsLock()
  {
    this.adminPageNext.setDisable(true);
    this.adminPagePrev.setDisable(true);
    this.adminPageLabel.setText("");
  }

  private void adminDetailsLock()
  {
    this.adminDelete.setDisable(true);
    this.adminDetailContainer.setDisable(true);
    this.adminIdField.setText("");
    this.adminIdNameField.setText("");
    this.adminRealNameField.setText("");
    this.adminEmails.clear();
    this.emailAdd.setDisable(true);
    this.emailDelete.setDisable(true);
    this.adminLoginHistory.clear();
  }

  private void adminDetailsUnlock()
  {
    this.adminDelete.setDisable(false);
    this.adminDetailContainer.setDisable(false);
  }

  private void onAdminSelected(
    final IdAGAdmin adminNew)
  {
    this.adminDetailsLock();
    if (adminNew == null) {
      return;
    }

    {
      final var future = this.client.adminGet(adminNew.id());
      future.whenComplete((received, exception) -> {
        if (received != null) {
          this.onAdminReceived(received);
        }
      });
    }

    this.adminDetailsUnlock();
  }

  private void onAdminReceived(
    final Optional<IdAdmin> adminOpt)
  {
    Platform.runLater(() -> {
      this.adminDetailsLock();

      if (adminOpt.isEmpty()) {
        this.admin = null;
        return;
      }

      this.admin = adminOpt.get();
      this.adminIdField.setText(this.admin.id().toString());
      this.adminIdNameField.setText(this.admin.idName().toString());
      this.adminRealNameField.setText(this.admin.realName().toString());
      this.adminEmails.setAll(this.admin.emails().toList());
      this.adminPermissions.setAll(
        this.admin.permissions()
          .impliedPermissions()
          .stream()
          .sorted(Comparator.comparing(Enum::toString))
          .toList()
      );
      this.adminPasswordField.setText("%s".formatted(this.admin.password()));
      this.adminDetailsUnlock();
      this.emailAdd.setDisable(false);
      this.emailDelete.setDisable(true);
    });
  }

  @FXML
  private void onAdminCreateSelected()
    throws IOException
  {
    final var controller =
      IdAGAdminCreateController.openDialog(
        this.configuration,
        this.strings,
        this.client.self()
      );

    final Optional<IdAdminCreate> create = controller.result();
    create.ifPresent(this.client::adminCreate);
  }

  @FXML
  private void onPasswordChangeSelected()
    throws IOException
  {
    final var controller =
      IdAGAdminPasswordChangeController.openDialog(
        this.configuration,
        this.strings);

    final Optional<IdPassword> password = controller.result();
    if (password.isPresent()) {
      final var future =
        this.client.adminUpdate(
          this.admin.id(),
          Optional.empty(),
          Optional.empty(),
          password
        );

      future.whenComplete((received, exception) -> {
        if (received != null) {
          this.onAdminReceived(Optional.of(received));
        }
      });
    }
  }

  @FXML
  private void onAdminDeleteSelected()
    throws IOException
  {
    final var controller =
      this.openAdminDeleteConfirmDialog();

    if (controller.isDeleteRequested()) {
      final var future = this.client.adminDelete(this.admin.id());
      future.whenComplete((page, exception) -> {
        // OK
      });
    }
  }

  @FXML
  private void onAdminPageNext()
  {
    switch (this.searchKindAtStart) {
      case BY_DETAILS -> {
        final var future = this.client.adminSearchNext();
        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
      case BY_EMAIL -> {
        final var future = this.client.adminSearchByEmailNext();
        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
    }
  }

  @FXML
  private void onAdminPagePrevious()
  {
    switch (this.searchKindAtStart) {
      case BY_DETAILS -> {
        final var future = this.client.adminSearchPrevious();
        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
      case BY_EMAIL -> {
        final var future = this.client.adminSearchByEmailPrevious();
        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
    }
  }

  @FXML
  private void onAdminSearchSelected()
  {
    this.searchKindAtStart = this.searchKind.getValue();

    switch (this.searchKind.getValue()) {
      case BY_DETAILS -> {
        final var future =
          this.client.adminSearchBegin(
            IdTimeRange.largest(),
            IdTimeRange.largest(),
            Optional.of(this.adminSearchField.getCharacters().toString())
          );

        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }

      case BY_EMAIL -> {
        final var future =
          this.client.adminSearchByEmailBegin(
            IdTimeRange.largest(),
            IdTimeRange.largest(),
            this.adminSearchField.getCharacters().toString()
          );

        future.whenComplete((page, exception) -> {
          if (page != null) {
            this.onPageReceived(page);
          }
        });
      }
    }
  }

  private void onPageReceived(
    final IdPage<IdAdminSummary> page)
  {
    Platform.runLater(() -> {
      final var pageIndex = page.pageIndex();
      final var pageCount = page.pageCount();

      this.adminPagePrev.setDisable(pageIndex == 1);
      this.adminPageNext.setDisable(pageIndex == pageCount);

      this.adminPageLabel.setText(
        this.strings.format(
          "admins.page",
          Integer.valueOf(pageIndex),
          Integer.valueOf(pageCount))
      );

      this.admins.setAll(
        page.items()
          .stream()
          .map(IdAGAdmin::of)
          .toList()
      );
    });
  }

  @FXML
  private void onEmailAddSelected()
    throws IOException
  {
    final var controller =
      IdAGAdminEmailAddController.openDialog(this.configuration, this.strings);
    final var emailOpt = controller.result();
    if (emailOpt.isPresent()) {
      final var future =
        this.client.adminEmailAdd(this.admin.id(), emailOpt.get());
      future.whenComplete((received, exception) -> {
        if (received != null) {
          this.onAdminReceived(Optional.of(received));
        }
      });
    }
  }

  private IdAGAdminDeleteConfirmController openAdminDeleteConfirmDialog()
    throws IOException
  {
    final var stage = new Stage();
    final var connectXML =
      IdAGMainScreenController.class.getResource(
        "/com/io7m/idstore/admin_gui/internal/adminDeleteConfirm.fxml");

    final var resources =
      this.strings.resources();
    final var loader =
      new FXMLLoader(connectXML, resources);

    loader.setControllerFactory(
      clazz -> new IdAGAdminDeleteConfirmController(this.admin, stage)
    );

    final Pane pane = loader.load();
    IdAGCSS.setCSS(this.configuration, pane);

    final IdAGAdminDeleteConfirmController controller = loader.getController();
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setScene(new Scene(pane));
    stage.setTitle(this.strings.format("adminDelete.confirmTitle"));
    stage.showAndWait();
    return controller;
  }

  @FXML
  private void onEmailDeleteSelected()
  {
    final var future =
      this.client.adminEmailRemove(
        this.admin.id(),
        this.adminEmailList.getSelectionModel().getSelectedItem()
      );
    future.whenComplete((received, exception) -> {
      if (received != null) {
        this.onAdminReceived(Optional.of(received));
      }
    });
  }

  @FXML
  private void onAdminUpdateSelected()
  {
    final var future =
      this.client.adminUpdate(
        this.admin.id(),
        Optional.of(new IdName(this.adminIdNameField.getText())),
        Optional.of(new IdRealName(this.adminRealNameField.getText())),
        Optional.empty()
      );

    future.whenComplete((received, exception) -> {
      if (received != null) {
        this.onAdminReceived(Optional.of(received));
      }
    });
  }
}
