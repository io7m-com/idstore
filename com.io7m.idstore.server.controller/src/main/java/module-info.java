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

/**
 * Identity server (Server controller)
 */

module com.io7m.idstore.server.controller
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.idstore.database.api;
  requires com.io7m.idstore.protocol.admin;
  requires com.io7m.idstore.protocol.api;
  requires com.io7m.idstore.protocol.user;
  requires com.io7m.idstore.server.api;
  requires com.io7m.idstore.server.security;
  requires com.io7m.idstore.server.service.branding;
  requires com.io7m.idstore.server.service.clock;
  requires com.io7m.idstore.server.service.configuration;
  requires com.io7m.idstore.server.service.mail;
  requires com.io7m.idstore.server.service.maintenance;
  requires com.io7m.idstore.server.service.ratelimit;
  requires com.io7m.idstore.server.service.sessions;
  requires com.io7m.idstore.server.service.telemetry.api;
  requires com.io7m.idstore.server.service.templating;
  requires com.io7m.idstore.strings;

  requires com.io7m.jaffirm.core;
  requires com.io7m.jdeferthrow.core;
  requires com.io7m.jxtrand.vanilla;
  requires com.io7m.repetoir.core;
  requires io.opentelemetry.api;
  requires io.opentelemetry.context;
  requires io.opentelemetry.semconv;
  requires org.slf4j;

  opens com.io7m.idstore.server.controller
    to com.io7m.jxtrand.vanilla;

  exports com.io7m.idstore.server.controller.admin;
  exports com.io7m.idstore.server.controller.command_exec;
  exports com.io7m.idstore.server.controller.user;
  exports com.io7m.idstore.server.controller.user_pwreset;
}
