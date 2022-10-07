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

import com.io7m.idstore.database.api.IdDatabaseFactoryType;
import com.io7m.idstore.database.postgres.IdDatabases;

/**
 * Identity server (Server database Postgresql implementation)
 */

module com.io7m.idstore.database.postgres
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires transitive com.io7m.idstore.database.api;

  requires com.io7m.anethum.common;
  requires com.io7m.jaffirm.core;
  requires com.io7m.jdeferthrow.core;
  requires com.io7m.trasco.api;
  requires com.io7m.trasco.vanilla;
  requires com.zaxxer.hikari;
  requires org.jooq;
  requires org.postgresql.jdbc;
  requires org.slf4j;

  requires io.opentelemetry.api;
  requires io.opentelemetry.context;
  requires io.opentelemetry.semconv;

  exports com.io7m.idstore.database.postgres;
  exports com.io7m.idstore.database.postgres.internal.tables to org.jooq;
  exports com.io7m.idstore.database.postgres.internal.tables.records to org.jooq;
  exports com.io7m.idstore.database.postgres.internal to org.jooq;

  provides IdDatabaseFactoryType with IdDatabases;
}
