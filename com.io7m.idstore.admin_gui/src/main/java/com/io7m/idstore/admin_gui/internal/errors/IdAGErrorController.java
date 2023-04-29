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

package com.io7m.idstore.admin_gui.internal.errors;

import com.io7m.idstore.admin_gui.IdAGConfiguration;
import com.io7m.idstore.admin_gui.internal.IdAGStrings;
import com.io7m.idstore.admin_gui.internal.main.IdAGScreenControllerType;
import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.seltzer.api.SStructuredErrorType;
import com.io7m.taskrecorder.core.TRStep;
import com.io7m.taskrecorder.core.TRTask;
import com.io7m.taskrecorder.core.TRTaskItemType;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A controller for the error screen.
 */

public final class IdAGErrorController
  implements IdAGScreenControllerType
{
  private final TRTask<?> task;
  private final SStructuredErrorType<IdErrorCode> error;
  private final Stage stage;
  private final IdAGConfiguration configuration;
  private final IdAGStrings strings;

  @FXML private TableColumn<Map.Entry<String, String>, String> errorNameColumn;
  @FXML private TableColumn<Map.Entry<String, String>, String> errorValueColumn;
  @FXML private VBox errorContainer;
  @FXML private ImageView errorIcon;
  @FXML private Label errorTaskTitle;
  @FXML private Label errorTaskMessage;
  @FXML private TreeView<TRTaskItemType> errorDetails;
  @FXML private TableView<Map.Entry<String, String>> errorAttributes;

  /**
   * A controller for the error screen.
   *
   * @param inStrings       The strings
   * @param inConfiguration The application configuration
   * @param inTask          The failed task
   * @param inError         The error
   * @param inStage         The containing window
   */

  public IdAGErrorController(
    final IdAGConfiguration inConfiguration,
    final IdAGStrings inStrings,
    final TRTask<?> inTask,
    final SStructuredErrorType<IdErrorCode> inError,
    final Stage inStage)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.task =
      Objects.requireNonNull(inTask, "task");
    this.error =
      Objects.requireNonNull(inError, "error");
    this.stage =
      Objects.requireNonNull(inStage, "stage");
  }

  private static TreeItem<TRTaskItemType> buildTree(
    final TRTaskItemType node)
  {
    if (node instanceof TRStep step) {
      return new TreeItem<>(step);
    }

    if (node instanceof TRTask<?> task) {
      final var taskNode = new TreeItem<TRTaskItemType>(task);
      for (final var step : task.items()) {
        taskNode.getChildren().add(buildTree(step));
      }
      return taskNode;
    }

    throw new IllegalStateException();
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    this.errorNameColumn.setCellValueFactory(
      param -> new ReadOnlyObjectWrapper<>(param.getValue().getKey()));
    this.errorValueColumn.setCellValueFactory(
      param -> new ReadOnlyObjectWrapper<>(param.getValue().getValue()));

    this.errorTaskTitle.setText(this.task.description());
    this.errorTaskMessage.setText(firstLineOf(this.task.resolution().message()));

    final var attributes = this.error.attributes();
    if (attributes.isEmpty()) {
      this.errorContainer.getChildren().remove(this.errorAttributes);
    } else {
      this.errorAttributes.setItems(
        FXCollections.observableList(
          attributes.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .toList()
        )
      );
    }

    this.errorDetails.setCellFactory(param -> {
      return new IdAGErrorTreeCell(this.configuration, this.strings);
    });
    this.errorDetails.setRoot(buildTree(this.task));
    this.errorDetails.setShowRoot(false);
  }

  private static String firstLineOf(final String message)
  {
    return message.split("\n")[0];
  }

  @FXML
  private void onDismissSelected()
  {
    this.stage.close();
  }

  @FXML
  private void onReportSelected()
  {

  }
}
