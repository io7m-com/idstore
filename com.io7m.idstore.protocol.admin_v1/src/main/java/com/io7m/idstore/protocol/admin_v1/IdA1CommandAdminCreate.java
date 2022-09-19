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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Create an admin.
 *
 * @param id          The admin ID
 * @param idName      The id name
 * @param email       The primary email
 * @param realName    The real name
 * @param password    The password
 * @param permissions The set of permissions
 */

@JsonDeserialize
@JsonSerialize
public record IdA1CommandAdminCreate(
  @JsonProperty(value = "Id", required = true)
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  Optional<UUID> id,
  @JsonProperty(value = "IdName", required = true)
  String idName,
  @JsonProperty(value = "RealName", required = true)
  String realName,
  @JsonProperty(value = "Email", required = true)
  String email,
  @JsonProperty(value = "Password", required = true)
  IdA1Password password,
  @JsonProperty(value = "Permissions", required = true)
  Set<IdA1AdminPermission> permissions)
  implements IdA1CommandType<IdA1ResponseAdminCreate>
{
  /**
   * Create an admin.
   */

  public IdA1CommandAdminCreate
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(idName, "idName");
    Objects.requireNonNull(realName, "realName");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(permissions, "permissions");
  }
}
