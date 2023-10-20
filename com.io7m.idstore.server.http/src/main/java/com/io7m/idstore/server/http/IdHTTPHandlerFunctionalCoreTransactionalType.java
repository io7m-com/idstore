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
import io.helidon.webserver.http.ServerRequest;

/**
 * A transactional functional handler core. Consumes a request (and request
 * information) and returns a response.
 */

public interface IdHTTPHandlerFunctionalCoreTransactionalType
{
  /**
   * Execute the core in a transaction. Automatically roll back the transaction
   * if nothing explicitly commits it.
   *
   * @param request     The request
   * @param transaction The transaction
   * @param information The request information
   *
   * @return The response
   */

  IdHTTPResponseType executeTransactional(
    ServerRequest request,
    IdHTTPRequestInformation information,
    IdDatabaseTransactionType transaction
  );
}
