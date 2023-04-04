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

package com.io7m.idstore.admin_gui.internal.audit;

import com.io7m.idstore.admin_gui.IdAGConfiguration;
import com.io7m.idstore.admin_gui.internal.IdAGStrings;
import com.io7m.idstore.admin_gui.internal.client.IdAGClientService;
import com.io7m.idstore.admin_gui.internal.client.IdAGClientStatus;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;

import java.net.URL;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.io7m.idstore.admin_gui.internal.client.IdAGClientStatus.DISCONNECTED;
import static javafx.scene.control.SelectionMode.SINGLE;

/**
 * The audit tab controller.
 */

public final class IdAGAuditController implements Initializable
{
  private final IdAGConfiguration configuration;
  private final RPServiceDirectoryType mainServices;
  private final IdAGStrings strings;
  private final IdAGClientService client;
  private final ObservableList<IdAuditEvent> events;

  @FXML private DatePicker lowerDate;
  @FXML private Spinner<OffsetDateTime> lowerTime;
  @FXML private DatePicker upperDate;
  @FXML private Spinner<OffsetDateTime> upperTime;
  @FXML private TextField ownerField;
  @FXML private TextField typeField;
  @FXML private TextField messageField;
  @FXML private ListView<IdAuditEvent> eventList;
  @FXML private Button auditPageNext;
  @FXML private Button auditPagePrev;
  @FXML private Label auditPageLabel;

  /**
   * The audit tab controller.
   *
   * @param inMainServices  The service directory
   * @param inConfiguration The configuration
   */

  public IdAGAuditController(
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

    this.events =
      FXCollections.observableArrayList();
  }

  private static Optional<String> nonBlank(
    final String text)
  {
    if (text.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(text.trim());
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.eventTableControlsLock();

    final var lowerTimeFactory =
      new IdAGAuditTimeSpinnerValueFactory();
    final var upperTimeFactory =
      new IdAGAuditTimeSpinnerValueFactory();
    final var dateConverter =
      new IdAGAuditDateStringConverter();
    final var timeConverter =
      new IdAGAuditTimeOnlyStringConverter();

    lowerTimeFactory.setConverter(timeConverter);
    upperTimeFactory.setConverter(timeConverter);

    this.lowerTime.setValueFactory(lowerTimeFactory);
    this.lowerDate.setConverter(dateConverter);
    this.upperTime.setValueFactory(upperTimeFactory);
    this.upperDate.setConverter(dateConverter);

    this.lowerDate.setValue(LocalDate.now().minusDays(1L));
    this.upperDate.setValue(LocalDate.now().plusDays(1L));
    lowerTimeFactory.increment(1);
    upperTimeFactory.increment(1);

    this.eventList.setCellFactory(new IsAGAuditEventCellFactory(this.strings));
    this.eventList.setFixedCellSize(24.0);
    this.eventList.getSelectionModel().setSelectionMode(SINGLE);
    this.eventList.setItems(this.events);

    this.client.status()
      .addListener((obs, statusOld, statusNew) -> {
        this.onClientStatusChanged(statusNew);
      });
  }

  private void onClientStatusChanged(
    final IdAGClientStatus statusNew)
  {
    if (statusNew == DISCONNECTED) {
      this.events.clear();
      this.eventTableControlsLock();
    }
  }

  @FXML
  private void onAuditPageNext()
  {
    final var future =
      this.client.auditSearchNext();

    future.whenComplete((page, exception) -> {
      if (page != null) {
        this.onPageReceived(page);
      }
    });
  }

  @FXML
  private void onAuditPagePrevious()
  {
    final var future =
      this.client.auditSearchPrevious();

    future.whenComplete((page, exception) -> {
      if (page != null) {
        this.onPageReceived(page);
      }
    });
  }

  @FXML
  private void onAuditSearch()
  {
    final var future =
      this.client.auditSearchBegin(
        this.timeRange(),
        this.owner(),
        this.type(),
        this.message()
      );

    future.whenComplete((page, exception) -> {
      if (page != null) {
        this.onPageReceived(page);
      }
    });
  }

  private void eventTableControlsLock()
  {
    this.auditPageNext.setDisable(true);
    this.auditPagePrev.setDisable(true);
    this.auditPageLabel.setText("");
  }

  private void onPageReceived(
    final IdPage<IdAuditEvent> page)
  {
    Platform.runLater(() -> {
      final var pageIndex = page.pageIndex();
      final var pageCount = page.pageCount();

      this.auditPagePrev.setDisable(pageIndex == 1);
      this.auditPageNext.setDisable(pageIndex == pageCount);

      this.auditPageLabel.setText(
        this.strings.format(
          "users.page",
          Integer.valueOf(pageIndex),
          Integer.valueOf(pageCount))
      );

      this.events.setAll(page.items());
    });
  }

  private Optional<String> owner()
  {
    return nonBlank(this.ownerField.getText());
  }

  private Optional<String> type()
  {
    return nonBlank(this.typeField.getText());
  }

  private Optional<String> message()
  {
    return nonBlank(this.messageField.getText());
  }

  private IdTimeRange timeRange()
  {
    final var dateLow =
      this.lowerDate.getValue();
    final var dateHigh =
      this.upperDate.getValue();

    final var timeLow =
      this.lowerTime.getValue();
    final var timeHigh =
      this.upperTime.getValue();

    final var outTimeLow =
      OffsetDateTime.of(
        dateLow.getYear(),
        dateLow.getMonthValue(),
        dateLow.getDayOfMonth(),
        timeLow.getHour(),
        timeLow.getMinute(),
        timeLow.getSecond(),
        0,
        ZoneOffset.UTC
      );

    final var outTimeHigh =
      OffsetDateTime.of(
        dateHigh.getYear(),
        dateHigh.getMonthValue(),
        dateHigh.getDayOfMonth(),
        timeHigh.getHour(),
        timeHigh.getMinute(),
        timeHigh.getSecond(),
        0,
        ZoneOffset.UTC
      );

    return new IdTimeRange(outTimeLow, outTimeHigh);
  }
}
