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

import com.io7m.idstore.server.http.IdHTTPHandlerFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPRequestInformation;
import com.io7m.idstore.server.http.IdHTTPResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.maintenance.IdClosedForMaintenanceService;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import io.helidon.webserver.http.ServerRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Set;

import static com.io7m.idstore.strings.IdStringConstants.MAINTENANCE_MODE_TITLE;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A core that executes the given core if the server is not closed for maintenance.
 */

public final class IdUVHandlerCoreMaintenanceAware
  implements IdHTTPHandlerFunctionalCoreType
{
  private final IdHTTPHandlerFunctionalCoreType core;
  private final IdClosedForMaintenanceService maintenance;
  private final IdStrings strings;
  private final IdServerBrandingServiceType branding;
  private final IdFMTemplateType<IdFMMessageData> errorTemplate;

  private IdUVHandlerCoreMaintenanceAware(
    final RPServiceDirectoryType services,
    final IdHTTPHandlerFunctionalCoreType inCore)
  {
    Objects.requireNonNull(services, "services");

    this.maintenance =
      services.requireService(IdClosedForMaintenanceService.class);
    this.strings =
      services.requireService(IdStrings.class);
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

  public static IdHTTPHandlerFunctionalCoreType withMaintenanceAwareness(
    final RPServiceDirectoryType services,
    final IdHTTPHandlerFunctionalCoreType inCore)
  {
    return new IdUVHandlerCoreMaintenanceAware(services, inCore);
  }

  @Override
  public IdHTTPResponseType execute(
    final ServerRequest request,
    final IdHTTPRequestInformation information)
  {
    final var closed = this.maintenance.isClosed();
    if (closed.isPresent()) {
      try (var writer = new StringWriter()) {
        this.errorTemplate.process(
          new IdFMMessageData(
            this.branding.htmlTitle(this.strings.format(MAINTENANCE_MODE_TITLE)),
            this.branding.title(),
            information.requestId(),
            false,
            false,
            this.strings.format(MAINTENANCE_MODE_TITLE),
            closed.get(),
            "/"
          ),
          writer
        );
        writer.flush();
        return new IdHTTPResponseFixedSize(
          503,
          Set.of(),
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
