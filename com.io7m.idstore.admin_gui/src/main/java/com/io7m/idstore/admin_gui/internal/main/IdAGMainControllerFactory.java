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
import com.io7m.idstore.admin_gui.internal.admins.IdAGAdminsController;
import com.io7m.idstore.admin_gui.internal.audit.IdAGAuditController;
import com.io7m.idstore.admin_gui.internal.profile.IdAGProfileController;
import com.io7m.idstore.admin_gui.internal.users.IdAGUsersController;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.util.Callback;

import java.util.Objects;

/**
 * The main factory of controllers for the UI.
 */

public final class IdAGMainControllerFactory
  implements Callback<Class<?>, Object>
{
  private final RPServiceDirectoryType services;
  private final IdAGConfiguration configuration;

  /**
   * The main factory of controllers for the UI.
   *
   * @param inServices             The service directory
   * @param inConfiguration        The UI configuration
   */

  public IdAGMainControllerFactory(
    final RPServiceDirectoryType inServices,
    final IdAGConfiguration inConfiguration)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
  }

  @Override
  public Object call(
    final Class<?> param)
  {
    return switch (param.getCanonicalName()) {
      case "com.io7m.idstore.admin_gui.internal.users.IdAGUsersController" -> {
        yield new IdAGUsersController(this.services, this.configuration);
      }
      case "com.io7m.idstore.admin_gui.internal.admins.IdAGAdminsController" -> {
        yield new IdAGAdminsController(this.services, this.configuration);
      }
      case "com.io7m.idstore.admin_gui.internal.audit.IdAGAuditController" -> {
        yield new IdAGAuditController(this.services, this.configuration);
      }
      case "com.io7m.idstore.admin_gui.internal.profile.IdAGProfileController" -> {
        yield new IdAGProfileController(this.services, this.configuration);
      }

      default -> {
        throw new IllegalStateException(
          "Unrecognized controller: %s".formatted(param.getCanonicalName())
        );
      }
    };
  }
}
