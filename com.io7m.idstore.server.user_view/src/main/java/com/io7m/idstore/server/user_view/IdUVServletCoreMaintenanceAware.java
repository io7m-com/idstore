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


package com.io7m.idstore.server.user_view;

import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.maintenance.IdClosedForMaintenanceService;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A core that executes the given core if the server is not closed for maintenance.
 */

public final class IdUVServletCoreMaintenanceAware
  implements IdHTTPServletFunctionalCoreType
{
  private final IdHTTPServletFunctionalCoreType core;
  private final IdClosedForMaintenanceService maintenance;
  private final IdServerStrings strings;
  private final IdServerBrandingServiceType branding;
  private final IdFMTemplateType<IdFMMessageData> errorTemplate;

  private IdUVServletCoreMaintenanceAware(
    final RPServiceDirectoryType services,
    final IdHTTPServletFunctionalCoreType inCore)
  {
    Objects.requireNonNull(services, "services");

    this.maintenance =
      services.requireService(IdClosedForMaintenanceService.class);
    this.strings =
      services.requireService(IdServerStrings.class);
    this.branding =
      services.requireService(IdServerBrandingServiceType.class);
    this.errorTemplate =
      services.requireService(IdFMTemplateServiceType.class)
        .pageMessage();

    this.core =
      Objects.requireNonNull(inCore, "core");
  }

  /**
   * @param services The services
   * @param inCore   The executed core
   *
   * @return A core that executes the given core if the server is not closed for maintenance
   */

  public static IdHTTPServletFunctionalCoreType withMaintenanceAwareness(
    final RPServiceDirectoryType services,
    final IdHTTPServletFunctionalCoreType inCore)
  {
    return new IdUVServletCoreMaintenanceAware(services, inCore);
  }

  @Override
  public IdHTTPServletResponseType execute(
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var closed = this.maintenance.isClosed();
    if (closed.isPresent()) {
      try (var writer = new StringWriter()) {
        this.errorTemplate.process(
          new IdFMMessageData(
            this.branding.htmlTitle(this.strings.format("maintenanceModeTitle")),
            this.branding.title(),
            information.requestId(),
            false,
            false,
            this.strings.format("maintenanceModeTitle"),
            closed.get(),
            "/"
          ),
          writer
        );
        writer.flush();
        return new IdHTTPServletResponseFixedSize(
          503,
          IdUVContentTypes.xhtml(),
          writer.toString().getBytes(UTF_8)
        );
      } catch (final TemplateException e) {
        throw new IllegalStateException(e);
      } catch (final IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return this.core.execute(request, information);
  }
}
