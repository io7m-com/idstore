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


package com.io7m.idstore.server.service.events;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * An admin provided incorrect credentials when trying to log in.
 *
 * @param remoteHost The remote remoteHost
 * @param adminId    The admin
 */

public record IdEventAdminLoginAuthenticationFailed(
  String remoteHost,
  UUID adminId)
  implements IdEventAdminType
{
  /**
   * An admin provided incorrect credentials when trying to log in.
   *
   * @param remoteHost The remote remoteHost
   * @param adminId    The admin
   */

  public IdEventAdminLoginAuthenticationFailed
  {
    Objects.requireNonNull(remoteHost, "remoteHost");
    Objects.requireNonNull(adminId, "adminId");
  }

  @Override
  public IdEventSeverity severity()
  {
    return IdEventSeverity.WARNING;
  }

  @Override
  public String name()
  {
    return "security.admin.login.authentication_failed";
  }

  @Override
  public String message()
  {
    return "%s %s %s".formatted(this.name(), this.adminId, this.remoteHost);
  }

  @Override
  public Map<String, String> asAttributes()
  {
    return Map.ofEntries(
      Map.entry("event.domain", this.domain()),
      Map.entry("event.name", this.name()),
      Map.entry("idstore.remote_host", this.remoteHost()),
      Map.entry("idstore.admin", this.adminId().toString())
    );
  }
}
