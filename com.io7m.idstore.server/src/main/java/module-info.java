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

/**
 * The server implementation.
 */

module com.io7m.idstore.server
{
  requires static org.osgi.annotation.versioning;
  requires static org.osgi.annotation.bundle;

  requires transitive com.io7m.idstore.server.api;

  requires com.io7m.idstore.protocol.admin_v1;
  requires com.io7m.idstore.protocol.api;
  requires com.io7m.idstore.protocol.user_v1;
  requires com.io7m.idstore.protocol.versions;
  requires com.io7m.idstore.server.security;
  requires com.io7m.idstore.services.api;

  requires ch.qos.logback.classic;
  requires ch.qos.logback.core;
  requires com.fasterxml.jackson.databind;
  requires com.io7m.jmulticlose.core;
  requires com.io7m.junreachable.core;
  requires com.io7m.jxtrand.vanilla;
  requires freemarker;
  requires java.desktop;
  requires java.management;
  requires org.apache.commons.io;
  requires org.eclipse.jetty.jmx;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.servlet;
  requires org.simplejavamail.core;
  requires org.simplejavamail;

  opens com.io7m.idstore.server.internal
    to com.io7m.jxtrand.vanilla;

  exports com.io7m.idstore.server;
  exports com.io7m.idstore.server.logging;

  opens com.io7m.idstore.server.logging
    to com.io7m.jxtrand.vanilla;
}
