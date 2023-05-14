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

open module com.io7m.idstore.tests
{
  requires transitive com.io7m.idstore.admin_client.api;
  requires transitive com.io7m.idstore.admin_client;
  requires transitive com.io7m.idstore.database.api;
  requires transitive com.io7m.idstore.database.postgres;
  requires transitive com.io7m.idstore.error_codes;
  requires transitive com.io7m.idstore.main;
  requires transitive com.io7m.idstore.model;
  requires transitive com.io7m.idstore.protocol.admin.cb;
  requires transitive com.io7m.idstore.protocol.admin;
  requires transitive com.io7m.idstore.protocol.api;
  requires transitive com.io7m.idstore.protocol.user.cb;
  requires transitive com.io7m.idstore.protocol.user;
  requires transitive com.io7m.idstore.server.admin_v1;
  requires transitive com.io7m.idstore.server.api;
  requires transitive com.io7m.idstore.server.controller;
  requires transitive com.io7m.idstore.server.http;
  requires transitive com.io7m.idstore.server.security;
  requires transitive com.io7m.idstore.server.service.branding;
  requires transitive com.io7m.idstore.server.service.clock;
  requires transitive com.io7m.idstore.server.service.configuration;
  requires transitive com.io7m.idstore.server.service.mail;
  requires transitive com.io7m.idstore.server.service.maintenance;
  requires transitive com.io7m.idstore.server.service.ratelimit;
  requires transitive com.io7m.idstore.server.service.reqlimit;
  requires transitive com.io7m.idstore.server.service.sessions;
  requires transitive com.io7m.idstore.server.service.telemetry.api;
  requires transitive com.io7m.idstore.server.service.telemetry.otp;
  requires transitive com.io7m.idstore.server.service.templating;
  requires transitive com.io7m.idstore.server.service.verdant;
  requires transitive com.io7m.idstore.server.user_v1;
  requires transitive com.io7m.idstore.server.user_view;
  requires transitive com.io7m.idstore.server.vanilla;
  requires transitive com.io7m.idstore.shell.admin;
  requires transitive com.io7m.idstore.tests.arbitraries;
  requires transitive com.io7m.idstore.tests.extensions;
  requires transitive com.io7m.idstore.user_client.api;
  requires transitive com.io7m.idstore.user_client;

  requires com.io7m.cxbutton.core;
  requires com.io7m.jmulticlose.core;
  requires com.io7m.repetoir.core;
  requires com.io7m.verdant.core.cb;
  requires com.io7m.verdant.core;
  requires jakarta.mail;
  requires java.net.http;
  requires java.sql;
  requires jul.to.slf4j;
  requires org.apache.commons.io;
  requires org.eclipse.jetty.jmx;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.servlet;
  requires org.slf4j;

  requires transitive org.junit.jupiter.api;
  requires transitive org.junit.jupiter.engine;
  requires transitive org.junit.platform.commons;
  requires transitive org.junit.platform.engine;
}
