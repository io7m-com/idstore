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


package com.io7m.idstore.admin_gui.internal;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A controller for the about screen.
 */

public final class IdAGAboutController
  implements Initializable
{
  private final IdAGStrings strings;

  @FXML private Label aboutVersion;
  @FXML private Label aboutCommit;
  @FXML private Hyperlink aboutURL;

  /**
   * A controller for the about screen.
   *
   * @param inStrings The string resources
   */

  public IdAGAboutController(
    final IdAGStrings inStrings)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.aboutVersion.setText(IdAGApplication.appVersionedTitle(this.strings));
    this.aboutCommit.setText(IdAGAbout.APP_BUILD);
    this.aboutURL.setText(IdAGAbout.APP_URL);
  }
}
