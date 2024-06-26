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

package com.io7m.idstore.admin_client.api;

import com.io7m.hibiscus.api.HBConnectionParametersType;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * The client credentials.
 *
 * @param userName       The username
 * @param password       The password
 * @param baseURI        The base URI
 * @param attributes     The attributes
 * @param commandTimeout The command timeout
 * @param loginTimeout   The login timeout
 */

public record IdAClientConnectionParameters(
  String userName,
  String password,
  URI baseURI,
  Map<String, String> attributes,
  Duration loginTimeout,
  Duration commandTimeout)
  implements HBConnectionParametersType
{
  /**
   * The client credentials.
   *
   * @param userName       The username
   * @param password       The password
   * @param baseURI        The base URI
   * @param attributes     The attributes
   * @param commandTimeout The command timeout
   * @param loginTimeout   The login timeout
   */

  public IdAClientConnectionParameters
  {
    Objects.requireNonNull(userName, "userName");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(baseURI, "baseURI");
    Objects.requireNonNull(loginTimeout, "loginTimeout");
    Objects.requireNonNull(commandTimeout, "commandTimeout");

    attributes = Map.copyOf(attributes);
  }
}
