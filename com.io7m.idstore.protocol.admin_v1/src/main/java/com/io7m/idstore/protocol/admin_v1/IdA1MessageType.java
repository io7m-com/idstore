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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.io7m.idstore.protocol.api.IdProtocolMessageType;

import java.util.Map;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;
import static java.util.Map.entry;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * The type of Public API v1 messages.
 */

@JsonTypeInfo(
  use = JsonTypeInfo.Id.CUSTOM,
  include = JsonTypeInfo.As.PROPERTY,
  property = "%Type"
)
@JsonTypeIdResolver(IdA1IdTypeResolver.class)
@JsonPropertyOrder({"%Schema", "%Type"})
public sealed interface IdA1MessageType
  extends IdProtocolMessageType permits IdA1CommandType, IdA1ResponseType
{
  /**
   * A mapping of classes to type IDs.
   */

  Map<Class<?>, String> TYPE_ID_FOR_CLASS =
    Stream.of(
      IdA1CommandAdminBanCreate.class,
      IdA1CommandAdminBanDelete.class,
      IdA1CommandAdminBanGet.class,
      IdA1CommandAdminCreate.class,
      IdA1CommandAdminDelete.class,
      IdA1CommandAdminEmailAdd.class,
      IdA1CommandAdminEmailRemove.class,
      IdA1CommandAdminGet.class,
      IdA1CommandAdminGetByEmail.class,
      IdA1CommandAdminPermissionGrant.class,
      IdA1CommandAdminPermissionRevoke.class,
      IdA1CommandAdminSearchBegin.class,
      IdA1CommandAdminSearchByEmailBegin.class,
      IdA1CommandAdminSearchByEmailNext.class,
      IdA1CommandAdminSearchByEmailPrevious.class,
      IdA1CommandAdminSearchNext.class,
      IdA1CommandAdminSearchPrevious.class,
      IdA1CommandAdminSelf.class,
      IdA1CommandAdminUpdate.class,
      IdA1CommandAuditSearchBegin.class,
      IdA1CommandAuditSearchNext.class,
      IdA1CommandAuditSearchPrevious.class,
      IdA1CommandLogin.class,
      IdA1CommandUserBanCreate.class,
      IdA1CommandUserBanDelete.class,
      IdA1CommandUserBanGet.class,
      IdA1CommandUserCreate.class,
      IdA1CommandUserDelete.class,
      IdA1CommandUserEmailAdd.class,
      IdA1CommandUserEmailRemove.class,
      IdA1CommandUserGet.class,
      IdA1CommandUserGetByEmail.class,
      IdA1CommandUserLoginHistory.class,
      IdA1CommandUserSearchBegin.class,
      IdA1CommandUserSearchByEmailBegin.class,
      IdA1CommandUserSearchByEmailNext.class,
      IdA1CommandUserSearchByEmailPrevious.class,
      IdA1CommandUserSearchNext.class,
      IdA1CommandUserSearchPrevious.class,
      IdA1CommandUserUpdate.class,
      IdA1ResponseAdminBanCreate.class,
      IdA1ResponseAdminBanDelete.class,
      IdA1ResponseAdminBanGet.class,
      IdA1ResponseAdminCreate.class,
      IdA1ResponseAdminDelete.class,
      IdA1ResponseAdminGet.class,
      IdA1ResponseAdminSearchBegin.class,
      IdA1ResponseAdminSearchByEmailBegin.class,
      IdA1ResponseAdminSearchByEmailNext.class,
      IdA1ResponseAdminSearchByEmailPrevious.class,
      IdA1ResponseAdminSearchNext.class,
      IdA1ResponseAdminSearchPrevious.class,
      IdA1ResponseAdminSelf.class,
      IdA1ResponseAdminUpdate.class,
      IdA1ResponseAuditSearchBegin.class,
      IdA1ResponseAuditSearchNext.class,
      IdA1ResponseAuditSearchPrevious.class,
      IdA1ResponseError.class,
      IdA1ResponseLogin.class,
      IdA1ResponseUserBanCreate.class,
      IdA1ResponseUserBanDelete.class,
      IdA1ResponseUserBanGet.class,
      IdA1ResponseUserCreate.class,
      IdA1ResponseUserDelete.class,
      IdA1ResponseUserGet.class,
      IdA1ResponseUserLoginHistory.class,
      IdA1ResponseUserSearchBegin.class,
      IdA1ResponseUserSearchByEmailBegin.class,
      IdA1ResponseUserSearchByEmailNext.class,
      IdA1ResponseUserSearchByEmailPrevious.class,
      IdA1ResponseUserSearchNext.class,
      IdA1ResponseUserSearchPrevious.class,
      IdA1ResponseUserUpdate.class
    ).collect(toUnmodifiableMap(identity(), IdA1MessageType::typeIdOf));

  /**
   * A mapping of type IDs to classes.
   */

  Map<String, Class<?>> CLASS_FOR_TYPE_ID =
    makeClassForTypeId();

  private static String typeIdOf(
    final Class<?> c)
  {
    return c.getSimpleName().replace("IdA1", "");
  }

  private static Map<String, Class<?>> makeClassForTypeId()
  {
    return TYPE_ID_FOR_CLASS.entrySet()
      .stream()
      .map(e -> entry(e.getValue(), e.getKey()))
      .collect(toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * @return The schema identifier
   */

  @JsonProperty(value = "%Schema", required = false, access = READ_ONLY)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  default String schemaId()
  {
    return IdA1Messages.SCHEMA_ID;
  }
}
