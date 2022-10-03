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


package com.io7m.idstore.admin_client.api;

import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdTimeRange;

import java.util.Optional;

/**
 * Commands related to audit events.
 */

public interface IdAClientAuditType
{
  /**
   * Start searching/listing audit events. Calling this method will set the
   * search parameters and effectively reset searching back to page 1 of any
   * results.
   *
   * @param timeRange The time range within which events should have been
   *                  created
   * @param owner     The owner search query
   * @param type      The type search query
   * @param message   The message search query
   * @param pageSize  The page size
   *
   * @return The first page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdAuditEvent> auditSearchBegin(
    IdTimeRange timeRange,
    Optional<String> owner,
    Optional<String> type,
    Optional<String> message,
    int pageSize)
    throws IdAClientException, InterruptedException;

  /**
   * Continue searching/listing events. This will return the next page of
   * results.
   *
   * @return The next page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdAuditEvent> auditSearchNext()
    throws IdAClientException, InterruptedException;

  /**
   * Continue searching/listing events. This will return the previous page of
   * results.
   *
   * @return The previous page of results
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdPage<IdAuditEvent> auditSearchPrevious()
    throws IdAClientException, InterruptedException;

}
