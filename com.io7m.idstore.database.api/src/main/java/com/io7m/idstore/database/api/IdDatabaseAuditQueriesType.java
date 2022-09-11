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

package com.io7m.idstore.database.api;

import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdAuditListParameters;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * The database queries involving the audit log.
 */

public non-sealed interface IdDatabaseAuditQueriesType
  extends IdDatabaseQueriesType
{
  /**
   * Retrieve all audit events from the database matching the given parameters.
   *
   * @param parameters The search parameters
   * @param seek       The record to which to seek, if any
   *
   * @return A series of audit events, sorted by time
   *
   * @throws IdDatabaseException On errors
   */

  List<IdAuditEvent> auditEvents(
    IdAuditListParameters parameters,
    OptionalLong seek)
    throws IdDatabaseException;

  /**
   * Create an audit event.
   *
   * @param userId  The user ID of the event
   * @param time    The event time
   * @param type    The event type
   * @param message The event message
   *
   * @throws IdDatabaseException On errors
   */

  void auditPut(
    UUID userId,
    OffsetDateTime time,
    String type,
    String message)
    throws IdDatabaseException;

  /**
   * @param parameters The parameters
   *
   * @return The number of events matching the given parameter
   */

  long auditCount(IdAuditListParameters parameters)
    throws IdDatabaseException;

}
