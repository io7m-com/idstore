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
 * Identity server (Test suite)
 */

open module com.io7m.idstore.tests
{
  requires com.io7m.idstore.admin_client;
  requires com.io7m.idstore.database.api;
  requires com.io7m.idstore.database.postgres;
  requires com.io7m.idstore.main;
  requires com.io7m.idstore.model;
  requires com.io7m.idstore.protocol.admin.cb;
  requires com.io7m.idstore.protocol.admin;
  requires com.io7m.idstore.protocol.user.cb;
  requires com.io7m.idstore.protocol.user;
  requires com.io7m.idstore.server.api;
  requires com.io7m.idstore.server.controller;
  requires com.io7m.idstore.server.security;
  requires com.io7m.idstore.server.service.branding;
  requires com.io7m.idstore.server.service.clock;
  requires com.io7m.idstore.server.service.configuration;
  requires com.io7m.idstore.server.service.mail;
  requires com.io7m.idstore.server.service.maintenance;
  requires com.io7m.idstore.server.service.ratelimit;
  requires com.io7m.idstore.server.service.reqlimit;
  requires com.io7m.idstore.server.service.sessions;
  requires com.io7m.idstore.server.service.telemetry.api;
  requires com.io7m.idstore.server.service.templating;
  requires com.io7m.idstore.server.vanilla;
  requires com.io7m.idstore.shell.admin;
  requires com.io7m.idstore.strings;
  requires com.io7m.idstore.tests.extensions;
  requires com.io7m.idstore.user_client;

  requires com.helger.css;
  requires com.io7m.ervilla.api;
  requires com.io7m.ervilla.test_extension;
  requires com.io7m.junreachable.core;
  requires com.io7m.quarrel.ext.xstructural;
  requires com.io7m.quixote.core;
  requires com.io7m.repetoir.core;
  requires com.io7m.verdant.core.cb;
  requires com.io7m.verdant.core;
  requires com.io7m.zelador.test_extension;
  requires freemarker;
  requires io.opentelemetry.api;
  requires jakarta.mail;
  requires jakarta.servlet;
  requires java.net.http;
  requires java.sql;
  requires java.xml;
  requires net.bytebuddy.agent;
  requires net.bytebuddy;
  requires net.jqwik.api;
  requires org.mockito;
  requires org.postgresql.jdbc;
  requires org.slf4j;
  requires subethasmtp;

  requires org.junit.jupiter.api;
  requires org.junit.jupiter.engine;
  requires org.junit.platform.commons;
  requires org.junit.platform.engine;

  exports com.io7m.idstore.tests.database;
  exports com.io7m.idstore.tests.integration;
  exports com.io7m.idstore.tests.model;
  exports com.io7m.idstore.tests.protocol.admin.cb;
  exports com.io7m.idstore.tests.protocol.user.cb;
  exports com.io7m.idstore.tests.server.api;
  exports com.io7m.idstore.tests.server.controller.admin;
  exports com.io7m.idstore.tests.server.controller.user;
  exports com.io7m.idstore.tests.server.controller.user_pwreset;
  exports com.io7m.idstore.tests.server.events;
  exports com.io7m.idstore.tests.server.health;
  exports com.io7m.idstore.tests.server.main;
  exports com.io7m.idstore.tests.server.security;
  exports com.io7m.idstore.tests.server.service.branding;
  exports com.io7m.idstore.tests.server.service.configuration;
  exports com.io7m.idstore.tests.server.service.clock;
  exports com.io7m.idstore.tests.server.service.mail;
  exports com.io7m.idstore.tests.server.service.ratelimit;
  exports com.io7m.idstore.tests.server.service.reqlimit;
  exports com.io7m.idstore.tests.server.service.sessions;
  exports com.io7m.idstore.tests.server.service.templating;
  exports com.io7m.idstore.tests.server.service;
  exports com.io7m.idstore.tests.server;
}
