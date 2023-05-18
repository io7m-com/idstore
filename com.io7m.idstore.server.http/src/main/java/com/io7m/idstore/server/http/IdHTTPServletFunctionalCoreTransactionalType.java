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


package com.io7m.idstore.server.http;

import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A transactional functional servlet core. Consumes a request (and request
 * information) and returns a response.
 */

public interface IdHTTPServletFunctionalCoreTransactionalType
{
  /**
   * Execute the core in a transaction. Automatically roll back the transaction
   * if nothing explicitly commits it.
   *
   * @param request     The request
   * @param information The request information
   * @param transaction The transaction
   *
   * @return The response
   */

  IdHTTPServletResponseType executeTransactional(
    HttpServletRequest request,
    IdHTTPServletRequestInformation information,
    IdDatabaseTransactionType transaction
  );
}
