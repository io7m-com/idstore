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

package com.io7m.idstore.user_client.internal;

import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.idstore.user_client.api.IdUClientUsersType;

import java.net.URI;
import java.util.Map;

/**
 * A versioned protocol handler.
 */

public interface IdUClientProtocolHandlerType
  extends IdUClientUsersType
{
  /**
   * Attempt to log in.
   *
   * @param user     The user
   * @param password The password
   * @param base     The base URI
   * @param metadata Optional metadata properties to include with the request
   *
   * @return A new protocol handler
   *
   * @throws IdUClientException   On errors
   * @throws InterruptedException On interruption
   */

  IdUNewHandler login(
    String user,
    String password,
    URI base,
    Map<String, String> metadata)
    throws IdUClientException, InterruptedException;
}
