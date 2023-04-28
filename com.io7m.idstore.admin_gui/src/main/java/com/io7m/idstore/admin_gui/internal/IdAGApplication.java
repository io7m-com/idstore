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


package com.io7m.idstore.admin_gui.internal;

import com.io7m.idstore.admin_gui.IdAGConfiguration;
import com.io7m.idstore.admin_gui.internal.main.IdAGMainScreenController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * The main application class responsible for starting up the "main" view.
 */

public final class IdAGApplication extends Application
{
  private final IdAGConfiguration configuration;

  /**
   * The main application class responsible for starting up the "main" view.
   *
   * @param inConfiguration The configuration
   */

  public IdAGApplication(
    final IdAGConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
  }

  @Override
  public void start(
    final Stage stage)
    throws Exception
  {
    final var mainXML =
      IdAGApplication.class.getResource(
        "/com/io7m/idstore/admin_gui/internal/main.fxml");
    Objects.requireNonNull(mainXML, "mainXML");

    final var strings =
      new IdAGStrings(this.configuration.locale());
    final var mainLoader =
      new FXMLLoader(mainXML, strings.resources());

    mainLoader.setControllerFactory(ignored -> {
      return new IdAGMainScreenController(this.configuration, strings);
    });

    final Pane pane = mainLoader.load();
    IdAGCSS.setCSS(this.configuration, pane);

    final IdAGMainScreenController mainController =
      mainLoader.getController();

    stage.setTitle(appVersionedTitle(strings));
    stage.setWidth(800.0);
    stage.setHeight(600.0);
    stage.setScene(new Scene(pane));
    stage.show();
  }

  /**
   * The current application title, including version number.
   *
   * @param strings The string resources
   *
   * @return The title
   */

  public static String appVersionedTitle(
    final IdAGStrings strings)
  {
    return strings.format("appTitle", IdAGAbout.APP_VERSION);
  }
}
