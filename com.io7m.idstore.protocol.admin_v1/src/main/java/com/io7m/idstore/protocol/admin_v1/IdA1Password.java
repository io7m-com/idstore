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

package com.io7m.idstore.protocol.admin_v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithms;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.protocol.api.IdProtocolFromModel;
import com.io7m.idstore.protocol.api.IdProtocolToModel;

import java.util.Objects;

/**
 * Information for a password.
 *
 * @param algorithm The password algorithm
 * @param hash      The password hash
 * @param salt      The password salt
 */

@JsonDeserialize
@JsonSerialize
public record IdA1Password(
  @JsonProperty(value = "Algorithm", required = true)
  String algorithm,
  @JsonProperty(value = "Hash", required = true)
  String hash,
  @JsonProperty(value = "Salt", required = true)
  String salt)
{
  /**
   * Information for a single user.
   *
   * @param algorithm The password algorithm
   * @param hash      The password hash
   * @param salt      The password salt
   */

  public IdA1Password
  {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(hash, "hash");
    Objects.requireNonNull(salt, "salt");
  }

  /**
   * Convert this to a model password.
   *
   * @return The model password
   *
   * @throws IdPasswordException On password errors
   */

  @IdProtocolToModel
  public IdPassword toPassword()
    throws IdPasswordException
  {
    return new IdPassword(
      IdPasswordAlgorithms.parse(this.algorithm),
      this.hash,
      this.salt
    );
  }

  /**
   * Convert a model password to a V1 password.
   *
   * @param password The model password
   *
   * @return A password
   */

  @IdProtocolFromModel
  public static IdA1Password ofPassword(
    final IdPassword password)
  {
    return new IdA1Password(
      password.algorithm().identifier(),
      password.hash(),
      password.salt()
    );
  }
}
