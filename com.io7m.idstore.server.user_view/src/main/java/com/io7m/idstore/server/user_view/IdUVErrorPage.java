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

import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.strings.IdStrings;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

import static com.io7m.idstore.strings.IdStringConstants.ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Functions to display error pages.
 */

public final class IdUVErrorPage
{
  private IdUVErrorPage()
  {

  }

  /**
   * Display an error page.
   *
   * @param message       The message
   * @param statusCode    The status code
   * @param branding      The branding
   * @param destination   The redirect destination
   * @param errorTemplate The error message template
   * @param information   The request information
   * @param strings       The string resources
   */

  static IdHTTPServletResponseType showError(
    final IdStrings strings,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> errorTemplate,
    final IdHTTPServletRequestInformation information,
    final int statusCode,
    final String message,
    final String destination)
  {
    try (var writer = new StringWriter()) {
      errorTemplate.process(
        new IdFMMessageData(
          branding.htmlTitle(strings.format(ERROR)),
          branding.title(),
          information.requestId(),
          true,
          statusCode >= 500,
          strings.format(ERROR),
          message,
          destination
        ),
        writer
      );
      writer.flush();
      return new IdHTTPServletResponseFixedSize(
        statusCode,
        IdUVContentTypes.xhtml(),
        writer.toString().getBytes(UTF_8)
      );
    } catch (final TemplateException e) {
      throw new IllegalStateException(e);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
