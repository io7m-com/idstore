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

import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;

import java.util.Optional;
import java.util.UUID;

/**
 * A server instance.
 */

public interface IdServerType extends AutoCloseable
{
  /**
   * Start the server instance. Can be called multiple times redundantly,
   * and can be called before or after #close() has been called.
   *
   * @throws IdServerException On errors
   */

  void start()
    throws IdServerException;

  /**
   * @return The server's database instance
   */

  IdDatabaseType database();

  /**
   * @return {@code true} if the server is closed
   *
   * @see #start()
   * @see #close()
   */

  boolean isClosed();

  @Override
  void close()
    throws IdServerException;

  /**
   * Do the work necessary to set up a server instance (such as initializing
   * and/or upgrading the database) but do not actually start the instance.
   *
   * @param adminId       The admin ID, if a specific one is required
   * @param adminName     The initial administrator to create
   * @param adminEmail    The admin email
   * @param adminRealName The admin's real name
   * @param adminPassword The password for the initial administrator
   *
   * @throws IdServerException On errors
   */

  void setup(
    Optional<UUID> adminId,
    IdName adminName,
    IdEmail adminEmail,
    IdRealName adminRealName,
    String adminPassword)
    throws IdServerException;
}
