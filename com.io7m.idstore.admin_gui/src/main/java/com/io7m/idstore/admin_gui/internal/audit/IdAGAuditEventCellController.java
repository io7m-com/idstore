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

import com.io7m.idstore.model.IdAuditEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * A user cell controller.
 */

public final class IdAGAuditEventCellController
{
  @FXML private TextField eventId;
  @FXML private TextField eventTime;
  @FXML private TextField eventOwner;
  @FXML private TextField eventType;
  @FXML private TextField eventMessage;

  /**
   * A user cell controller.
   */

  public IdAGAuditEventCellController()
  {

  }

  /**
   * Set the current audit event.
   *
   * @param event The event
   */

  public void setAuditEvent(
    final IdAuditEvent event)
  {
    this.eventId.setText(Long.toUnsignedString(event.id()));
    this.eventTime.setText(event.time().toString());
    this.eventOwner.setText(event.owner().toString());
    this.eventType.setText(event.type());
    this.eventMessage.setText(event.message());
  }
}
