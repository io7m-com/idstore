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

package com.io7m.idstore.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

/**
 * Configuration for the database.
 *
 * @param user         The database user
 * @param password     The database password
 * @param address      The database address
 * @param create       {@code} true if the database should be created
 * @param databaseName The database name
 * @param kind         The underlying SQL database kind
 * @param port         The database port
 * @param upgrade      {@code true} if the database schema should be upgraded
 */

@JsonDeserialize
@JsonSerialize
public record IdServerDatabaseConfiguration(
  @JsonProperty(value = "Kind", required = true)
  IdServerDatabaseKind kind,
  @JsonProperty(value = "User", required = true)
  String user,
  @JsonProperty(value = "Password", required = true)
  String password,
  @JsonProperty(value = "Host", required = true)
  String address,
  @JsonProperty(value = "Port", required = true)
  int port,
  @JsonProperty(value = "Name", required = true)
  String databaseName,
  @JsonProperty(value = "Create", required = true)
  boolean create,
  @JsonProperty(value = "Upgrade", required = true)
  boolean upgrade)
  implements IdServerJSONConfigurationElementType
{
  /**
   * Configuration for the database.
   *
   * @param user         The database user
   * @param password     The database password
   * @param address      The database address
   * @param create       {@code} true if the database should be created
   * @param databaseName The database name
   * @param kind         The underlying SQL database kind
   * @param port         The database port
   * @param upgrade      {@code true} if the database schema should be upgraded
   */

  public IdServerDatabaseConfiguration
  {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(address, "address");
    Objects.requireNonNull(databaseName, "databaseName");
  }
}
