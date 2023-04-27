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

import com.io7m.idstore.server.http.IdCommonInstrumentedServlet;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.server.service.sessions.IdSessionUserService;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

/**
 * Log out.
 */

public final class IdUViewLogout extends IdCommonInstrumentedServlet
{
  private final IdSessionUserService userSessions;

  /**
   * Log out.
   *
   * @param inServices The service directory
   */

  public IdUViewLogout(
    final RPServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "services"));

    this.userSessions =
      inServices.requireService(IdSessionUserService.class);
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws ServletException, IOException
  {
    final var httpSession =
      request.getSession(true);
    final var userSessionId =
      (IdSessionSecretIdentifier) httpSession.getAttribute("ID");

    if (userSessionId != null) {
      this.userSessions.deleteSession(userSessionId);
      httpSession.invalidate();
    }

    servletResponse.sendRedirect("/");
  }
}
