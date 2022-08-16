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
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.api.IdProtocolFromModel;
import com.io7m.idstore.protocol.api.IdProtocolToModel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Information for a single user.
 *
 * @param timeCreated The date the user was created
 * @param emails      The user's email
 * @param id          The user's ID
 * @param idName      The user's ID name
 * @param timeUpdated The date the user was updated
 * @param password    The user's password
 * @param realName    The user's real name
 */

public record IdA1User(
  @JsonProperty(value = "ID", required = true)
  UUID id,
  @JsonProperty(value = "IDName", required = true)
  String idName,
  @JsonProperty(value = "RealName", required = true)
  String realName,
  @JsonProperty(value = "Emails", required = true)
  List<String> emails,
  @JsonProperty(value = "TimeCreated", required = true)
  OffsetDateTime timeCreated,
  @JsonProperty(value = "TimeUpdated", required = true)
  OffsetDateTime timeUpdated,
  @JsonProperty(value = "Password", required = true)
  IdA1Password password)
{
  /**
   * Information for a single user.
   *
   * @param timeCreated The date the user was created
   * @param emails      The user's emails
   * @param id          The user's ID
   * @param idName      The user's ID name
   * @param timeUpdated The date the user was updated
   * @param password    The user's password
   * @param realName    The user's real name
   */

  public IdA1User
  {
    Objects.requireNonNull(timeCreated, "created");
    Objects.requireNonNull(emails, "emails");
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(idName, "idName");
    Objects.requireNonNull(timeUpdated, "timeUpdated");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(realName, "realName");
  }

  /**
   * Create a v1 user from the given model user.
   *
   * @param user The model user
   *
   * @return A v1 user
   *
   * @see #toUser()
   */

  @IdProtocolFromModel
  public static IdA1User ofUser(
    final IdUser user)
  {
    Objects.requireNonNull(user, "user");
    return new IdA1User(
      user.id(),
      user.idName().value(),
      user.realName().value(),
      user.emails()
        .toList()
        .stream()
        .map(IdEmail::value)
        .toList(),
      user.timeCreated(),
      user.timeUpdated(),
      IdA1Password.ofPassword(user.password())
    );
  }

  /**
   * Convert this to a model user.
   *
   * @return This as a model user
   *
   * @throws IdPasswordException On password errors
   * @see #ofUser(IdUser)
   */

  @IdProtocolToModel
  public IdUser toUser()
    throws IdPasswordException
  {
    return new IdUser(
      this.id,
      new IdName(this.idName),
      new IdRealName(this.realName),
      IdNonEmptyList.ofList(
        this.emails.stream()
          .map(IdEmail::new)
          .toList()
      ),
      this.timeCreated,
      this.timeUpdated,
      this.password.toPassword()
    );
  }
}
