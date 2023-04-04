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


package com.io7m.idstore.admin_gui.internal.main;

import com.io7m.idstore.admin_gui.IdAGConfiguration;
import com.io7m.idstore.admin_gui.internal.IdAGAboutController;
import com.io7m.idstore.admin_gui.internal.IdAGApplication;
import com.io7m.idstore.admin_gui.internal.IdAGCSS;
import com.io7m.idstore.admin_gui.internal.IdAGPerpetualSubscriber;
import com.io7m.idstore.admin_gui.internal.IdAGStrings;
import com.io7m.idstore.admin_gui.internal.client.IdAGClientService;
import com.io7m.idstore.admin_gui.internal.client.IdAGClientStatus;
import com.io7m.idstore.admin_gui.internal.errors.IdAGErrorDialogs;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventBus;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventStatusCancelled;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventStatusCompleted;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventStatusFailed;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventStatusInProgress;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventType;
import com.io7m.idstore.admin_gui.internal.login.IdAGLoginController;
import com.io7m.idstore.admin_gui.internal.services.IdAGBootEvent;
import com.io7m.idstore.admin_gui.internal.services.IdAGBootServices;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.taskrecorder.core.TRTask;
import com.io7m.taskrecorder.core.TRTaskFailed;
import com.io7m.taskrecorder.core.TRTaskRecorder;
import com.io7m.taskrecorder.core.TRTaskSucceeded;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS;

/**
 * The main screen controller.
 */

public final class IdAGMainScreenController implements Initializable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAGMainScreenController.class);

  private final IdAGConfiguration configuration;
  private final IdAGStrings strings;

  @FXML private ImageView mainStatusIcon;
  @FXML private ProgressBar mainProgress;
  @FXML private TextField mainStatusText;
  @FXML private MenuBar mainMenuBar;
  @FXML private MenuItem mainConnectMenuItem;
  @FXML private AnchorPane mainContent;

  private volatile RPServiceDirectoryType services;
  private volatile TRTask<RPServiceDirectoryType> task;
  private volatile IdAGClientService client;
  private volatile IdAGEventBus events;
  private Image iconError;
  private Image iconApp;
  private IdAGErrorDialogs errorDialogs;

  /**
   * The main screen controller.
   *
   * @param inConfiguration The application configuration
   * @param inStrings       The application strings
   */

  public IdAGMainScreenController(
    final IdAGConfiguration inConfiguration,
    final IdAGStrings inStrings)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.contentHide();

    this.iconApp =
      loadIcon("idstore.png");
    this.iconError =
      loadIcon("error16.png");

    this.mainMenuBar.setDisable(true);
    this.mainStatusIcon.setImage(this.iconApp);
    this.mainStatusText.setText("");
    this.mainProgress.setProgress(INDETERMINATE_PROGRESS);

    final var bootFuture =
      IdAGBootServices.create(
        this.configuration,
        this.strings,
        this::onBootEvent
      );

    bootFuture.whenComplete((bootTask, exception) -> {
      Platform.runLater(() -> {
        this.mainMenuBar.setDisable(false);
        this.mainProgress.setVisible(false);
      });

      this.task = bootTask;
      if (exception != null) {
        LOG.debug("services failed: ", exception);
        try (var recorder =
               TRTaskRecorder.<RPServiceDirectoryType>create(
                 LOG, "Booting application...")) {
          recorder.setTaskFailed(exception.getMessage(), Optional.of(exception));
          this.task = recorder.toTask();
        }
        this.onBootFailed();
        return;
      }

      final var resolution = bootTask.resolution();
      if (resolution instanceof TRTaskFailed<?> failed) {
        LOG.debug("services failed: {}", failed);
        this.onBootFailed();
        return;
      }

      if (resolution instanceof TRTaskSucceeded<RPServiceDirectoryType> succeeded) {
        this.services = succeeded.result();
      }

      Platform.runLater(() -> {
        try {
          this.onBootCompleted();
        } catch (final IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    });
  }

  private static Image loadIcon(final String x)
  {
    return new Image(IdAGMainScreenController.class.getResource(
        "/com/io7m/idstore/admin_gui/internal/" + x)
                       .toString());
  }

  private void onBootCompleted()
    throws IOException
  {
    this.client =
      this.services.requireService(IdAGClientService.class);
    this.events =
      this.services.requireService(IdAGEventBus.class);
    this.errorDialogs =
      this.services.requireService(IdAGErrorDialogs.class);

    this.events.subscribe(new IdAGPerpetualSubscriber<>(this::onEvent));

    final var tabs = this.createTabs();
    this.mainContent.getChildren().add(tabs);
    AnchorPane.setBottomAnchor(tabs, Double.valueOf(0.0));
    AnchorPane.setTopAnchor(tabs, Double.valueOf(0.0));
    AnchorPane.setLeftAnchor(tabs, Double.valueOf(0.0));
    AnchorPane.setRightAnchor(tabs, Double.valueOf(0.0));

    this.client.status()
      .addListener((obs, statusOld, statusNew) -> {
        this.configureMainContentViewForClientStatus(statusNew);
      });
  }

  private void configureMainContentViewForClientStatus(
    final IdAGClientStatus status)
  {
    switch (status) {
      case CONNECTION_FAILED, DISCONNECTED -> {
        this.contentHide();
        this.mainConnectMenuItem.setDisable(false);
        this.mainConnectMenuItem.setText(
          this.strings.format("menu.connect"));
      }

      case CONNECTING -> {
        this.contentHide();
        this.mainConnectMenuItem.setDisable(true);
        this.mainConnectMenuItem.setText(
          this.strings.format("menu.disconnect"));
      }

      case CONNECTED, REQUESTING, REQUEST_FAILED -> {
        this.contentShow();
        this.mainConnectMenuItem.setDisable(false);
        this.mainConnectMenuItem.setText(
          this.strings.format("menu.disconnect"));
      }
    }
  }

  private Node createTabs()
    throws IOException
  {
    final var mainXML =
      IdAGApplication.class.getResource(
        "/com/io7m/idstore/admin_gui/internal/mainContent.fxml");
    Objects.requireNonNull(mainXML, "mainXML");

    final var mainLoader =
      new FXMLLoader(mainXML, this.strings.resources());
    final var factory =
      new IdAGMainControllerFactory(this.services, this.configuration);

    mainLoader.setControllerFactory(factory);

    final Pane pane = mainLoader.load();
    IdAGCSS.setCSS(this.configuration, pane);
    return pane;
  }

  private void contentShow()
  {
    this.mainContent.setVisible(true);
    this.mainContent.setDisable(false);
  }

  private void contentHide()
  {
    this.mainContent.setVisible(false);
    this.mainContent.setDisable(true);
  }

  private void onEvent(
    final IdAGEventType event)
  {
    Platform.runLater(() -> {
      this.configureStatusBarForEvent(event);
      this.openErrorDialogForEventIfNecessary(event);
    });
  }

  private void openErrorDialogForEventIfNecessary(
    final IdAGEventType event)
  {
    final var status = event.status();
    if (status instanceof IdAGEventStatusFailed failed) {
      this.errorDialogs.open(failed.task());
    }
  }

  private void configureStatusBarForEvent(
    final IdAGEventType event)
  {
    final var status = event.status();
    this.mainStatusText.setText(firstLineOf(event));
    this.mainStatusIcon.setImage(this.iconApp);

    if (status instanceof IdAGEventStatusInProgress inProgress) {
      this.mainProgress.setVisible(true);
      final var progressOpt = inProgress.progress();
      if (progressOpt.isPresent()) {
        this.mainProgress.setProgress(progressOpt.getAsDouble());
      } else {
        this.mainProgress.setProgress(INDETERMINATE_PROGRESS);
      }
    } else if (status instanceof IdAGEventStatusCompleted) {
      this.mainProgress.setVisible(false);
    } else if (status instanceof IdAGEventStatusCancelled) {
      this.mainProgress.setVisible(false);
    } else if (status instanceof IdAGEventStatusFailed) {
      this.mainStatusIcon.setImage(this.iconError);
      this.mainProgress.setVisible(false);
    }
  }

  private static String firstLineOf(
    final IdAGEventType event)
  {
    return event.message().split("\n")[0];
  }

  private void onBootFailed()
  {
    Platform.runLater(() -> {

    });
  }

  private void onBootEvent(
    final IdAGBootEvent event)
  {
    Platform.runLater(() -> {
      this.mainStatusText.setText(event.message());
      this.mainProgress.setVisible(true);
      this.mainProgress.setProgress(event.progress());
    });
  }

  @FXML
  private void onConnectSelected()
    throws IOException
  {
    switch (this.client.status().get()) {
      case DISCONNECTED, CONNECTION_FAILED -> {
        final var stage = new Stage();
        final var connectXML =
          IdAGMainScreenController.class.getResource(
            "/com/io7m/idstore/admin_gui/internal/connect.fxml");

        final var resources =
          this.strings.resources();
        final var loader =
          new FXMLLoader(connectXML, resources);

        loader.setControllerFactory(
          clazz -> new IdAGLoginController(
            this.services,
            this.configuration,
            stage)
        );

        final AnchorPane pane = loader.load();
        IdAGCSS.setCSS(this.configuration, pane);

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(pane));
        stage.setTitle(this.strings.format("connect.connect"));
        stage.showAndWait();
      }
      case CONNECTING -> {
        // Nothing
      }
      case CONNECTED, REQUESTING -> {
        this.client.disconnect();
      }
    }
  }

  @FXML
  private void onExitSelected()
  {
    Platform.exit();
  }

  @FXML
  private void onAboutSelected()
    throws IOException
  {
    final var stage = new Stage();
    final var connectXML =
      IdAGMainScreenController.class.getResource(
        "/com/io7m/idstore/admin_gui/internal/about.fxml");

    final var resources =
      this.strings.resources();
    final var loader =
      new FXMLLoader(connectXML, resources);

    loader.setControllerFactory(clazz -> new IdAGAboutController(this.strings));

    final Pane pane = loader.load();
    IdAGCSS.setCSS(this.configuration, pane);

    final var width = 400.0;
    stage.setWidth(width);
    stage.setMaxWidth(width);
    stage.setMinWidth(width);

    final var height = 196.0;
    stage.setHeight(height);
    stage.setMinHeight(height);
    stage.setMaxHeight(height);

    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setScene(new Scene(pane));
    stage.setTitle(IdAGApplication.appVersionedTitle(this.strings));
    stage.showAndWait();
  }
}
