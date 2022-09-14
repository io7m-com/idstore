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
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolFromModel;
import com.io7m.idstore.protocol.api.IdProtocolToModelType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_ERROR;

/**
 * Information for a single admin.
 *
 * @param timeCreated The date the admin was created
 * @param emails      The admin's emails
 * @param id          The admin's ID
 * @param idName      The admin's ID name
 * @param timeUpdated The date the admin was last updated
 * @param password    The admin's password
 * @param realName    The admin's real name
 * @param permissions The admin's permissions
 */

public record IdA1Admin(
  @JsonProperty(value = "ID", required = true)
  UUID id,
  @JsonProperty(value = "IDName", required = true)
  String idName,
  @JsonProperty(value = "RealName", required = true)
  String realName,
  @JsonProperty(value = "Emails", required = true)
  List<String> emails,
  @JsonProperty(value = "Created", required = true)
  OffsetDateTime timeCreated,
  @JsonProperty(value = "LastLogin", required = true)
  OffsetDateTime timeUpdated,
  @JsonProperty(value = "Password", required = true)
  IdA1Password password,
  @JsonProperty(value = "Permissions", required = true)
  Set<IdA1AdminPermission> permissions)
  implements IdProtocolToModelType<IdAdmin>
{
  /**
   * Information for a single admin.
   *
   * @param timeCreated The date the admin was created
   * @param emails      The admin's emails
   * @param id          The admin's ID
   * @param idName      The admin's ID name
   * @param timeUpdated The date the admin was last updated
   * @param password    The admin's password
   * @param realName    The admin's real name
   * @param permissions The admin's permissions
   */

  public IdA1Admin
  {
    Objects.requireNonNull(timeCreated, "created");
    Objects.requireNonNull(emails, "emails");
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(idName, "idName");
    Objects.requireNonNull(timeUpdated, "lastLoginTime");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(realName, "realName");
  }

  /**
   * Create a v1 admin from the given model admin.
   *
   * @param admin The model admin
   *
   * @return A v1 admin
   *
   * @see #toModel()
   */

  @IdProtocolFromModel
  public static IdA1Admin ofAdmin(
    final IdAdmin admin)
  {
    Objects.requireNonNull(admin, "admin");
    return new IdA1Admin(
      admin.id(),
      admin.idName().value(),
      admin.realName().value(),
      admin.emails()
        .toList()
        .stream()
        .map(IdEmail::value)
        .toList(),
      admin.timeCreated(),
      admin.timeUpdated(),
      IdA1Password.ofPassword(admin.password()),
      admin.permissions()
        .impliedPermissions()
        .stream()
        .map(IdA1AdminPermission::ofPermission)
        .collect(Collectors.toUnmodifiableSet())
    );
  }

  @Override
  public IdAdmin toModel()
    throws IdProtocolException
  {
    try {
      return new IdAdmin(
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
        this.password.toPassword(),
        IdAdminPermissionSet.of(
          this.permissions.stream()
            .map(IdA1AdminPermission::toPermission)
            .collect(Collectors.toUnmodifiableSet()))
      );
    } catch (final IdPasswordException e) {
      throw new IdProtocolException(PASSWORD_ERROR, e.getMessage(), e);
    }
  }
}
