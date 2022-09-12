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

package com.io7m.idstore.admin_client.internal;

import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.idstore.protocol.admin_v1.IdA1Admin;
import com.io7m.idstore.protocol.admin_v1.IdA1AdminPermission;
import com.io7m.idstore.protocol.admin_v1.IdA1AdminSearchByEmailParameters;
import com.io7m.idstore.protocol.admin_v1.IdA1AdminSearchParameters;
import com.io7m.idstore.protocol.admin_v1.IdA1AuditListParameters;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminGetByEmail;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandType;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserGet;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserGetByEmail;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandUserUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1Messages;
import com.io7m.idstore.protocol.admin_v1.IdA1Password;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminGet;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminSelf;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAuditSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseError;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserDelete;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserGet;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchNext;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserSearchPrevious;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseUserUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1TimeRange;
import com.io7m.idstore.protocol.admin_v1.IdA1User;
import com.io7m.idstore.protocol.admin_v1.IdA1UserSearchByEmailParameters;
import com.io7m.idstore.protocol.admin_v1.IdA1UserSearchParameters;
import com.io7m.idstore.protocol.api.IdProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.net.http.HttpResponse.BodyHandlers;

/**
 * The version 1 protocol handler.
 */

public final class IdAClientProtocolHandler1
  extends IdAClientProtocolHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAClientProtocolHandler1.class);

  private final URI commandURI;
  private final URI transactionURI;
  private final IdA1Messages messages;
  private final URI loginURI;

  /**
   * The version 1 protocol handler.
   *
   * @param inHttpClient The HTTP client
   * @param inStrings    The string resources
   * @param inBase       The base URI
   */

  public IdAClientProtocolHandler1(
    final HttpClient inHttpClient,
    final IdAStrings inStrings,
    final URI inBase)
  {
    super(inHttpClient, inStrings, inBase);

    this.messages =
      new IdA1Messages();

    this.loginURI =
      inBase.resolve("login")
        .normalize();
    this.commandURI =
      inBase.resolve("command")
        .normalize();
    this.transactionURI =
      inBase.resolve("transaction")
        .normalize();
  }

  @Override
  public IdAClientProtocolHandlerType login(
    final String admin,
    final String password,
    final URI base)
    throws IdAClientException, InterruptedException
  {
    this.sendLogin(new IdA1CommandLogin(admin, password));
    return this;
  }

  private IdA1ResponseLogin sendLogin(
    final IdA1CommandLogin message)
    throws InterruptedException, IdAClientException
  {
    return this.send(this.loginURI, IdA1ResponseLogin.class, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends IdA1ResponseType> T sendCommand(
    final Class<T> responseClass,
    final IdA1CommandType<T> message)
    throws InterruptedException, IdAClientException
  {
    return this.send(this.commandURI, responseClass, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends IdA1ResponseType> Optional<T> sendCommandOptional(
    final Class<T> responseClass,
    final IdA1CommandType<T> message)
    throws InterruptedException, IdAClientException
  {
    return this.send(this.commandURI, responseClass, message, true);
  }

  private <T extends IdA1ResponseType> Optional<T> send(
    final URI uri,
    final Class<T> responseClass,
    final IdA1CommandType<T> message,
    final boolean allowNotFound)
    throws InterruptedException, IdAClientException
  {
    try {
      final var commandType = message.getClass().getSimpleName();
      LOG.debug("sending {} to {}", commandType, uri);

      final var sendBytes =
        this.messages.serialize(message);

      final var request =
        HttpRequest.newBuilder(uri)
          .header("User-Agent", userAgent())
          .POST(HttpRequest.BodyPublishers.ofByteArray(sendBytes))
          .build();

      final var response =
        this.httpClient()
          .send(request, BodyHandlers.ofByteArray());

      LOG.debug("server: status {}", response.statusCode());

      if (response.statusCode() == 404 && allowNotFound) {
        return Optional.empty();
      }

      final var responseHeaders =
        response.headers();

      final var contentType =
        responseHeaders.firstValue("content-type")
          .orElse("application/octet-stream");

      if (!contentType.equals(IdA1Messages.contentType())) {
        throw new IdAClientException(
          this.strings()
            .format(
              "errorContentType",
              commandType,
              IdA1Messages.contentType(),
              contentType)
        );
      }

      final var responseMessage =
        this.messages.parse(response.body());

      if (!(responseMessage instanceof IdA1ResponseType)) {
        throw new IdAClientException(
          this.strings()
            .format(
              "errorResponseType",
              "(unavailable)",
              commandType,
              IdA1ResponseType.class,
              responseMessage.getClass())
        );
      }

      final var responseActual = (IdA1ResponseType) responseMessage;
      if (responseActual instanceof IdA1ResponseError error) {
        throw new IdAClientException(
          this.strings()
            .format(
              "errorResponse",
              error.requestId(),
              commandType,
              Integer.valueOf(response.statusCode()),
              error.errorCode(),
              error.message())
        );
      }

      if (!Objects.equals(responseActual.getClass(), responseClass)) {
        throw new IdAClientException(
          this.strings()
            .format(
              "errorResponseType",
              responseActual.requestId(),
              commandType,
              responseClass,
              responseMessage.getClass())
        );
      }

      return Optional.of(responseClass.cast(responseMessage));
    } catch (final IdProtocolException | IOException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdAdmin adminSelf()
    throws IdAClientException, InterruptedException
  {
    final var response =
      this.sendCommand(IdA1ResponseAdminSelf.class, new IdA1CommandAdminSelf());

    try {
      return response.admin().toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  private static String userAgent()
  {
    final String version;
    final var pack = IdAClientProtocolHandler1.class.getPackage();
    if (pack != null) {
      version = pack.getImplementationVersion();
    } else {
      version = "0.0.0";
    }
    return "com.io7m.idstore.admin_client/%s".formatted(version);
  }

  @Override
  public IdPage<IdUserSummary> userSearchBegin(
    final IdUserSearchParameters parameters)
    throws IdAClientException, InterruptedException
  {
    try {
      final var a1parameters =
        IdA1UserSearchParameters.of(parameters);

      final var response =
        this.sendCommand(
          IdA1ResponseUserSearchBegin.class,
          new IdA1CommandUserSearchBegin(a1parameters)
        );

      return response.page().toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdUserSummary> userSearchNext()
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseUserSearchNext.class,
          new IdA1CommandUserSearchNext()
        ).page()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdUserSummary> userSearchPrevious()
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseUserSearchPrevious.class,
          new IdA1CommandUserSearchPrevious()
        ).page()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdUserSummary> userSearchByEmailBegin(
    final IdUserSearchByEmailParameters parameters)
    throws IdAClientException, InterruptedException
  {
    try {
      final var a1parameters =
        IdA1UserSearchByEmailParameters.of(parameters);

      final var response =
        this.sendCommand(
          IdA1ResponseUserSearchByEmailBegin.class,
          new IdA1CommandUserSearchByEmailBegin(a1parameters)
        );

      return response.page().toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdUserSummary> userSearchByEmailNext()
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseUserSearchByEmailNext.class,
          new IdA1CommandUserSearchByEmailNext()
        ).page()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdUserSummary> userSearchByEmailPrevious()
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseUserSearchByEmailPrevious.class,
          new IdA1CommandUserSearchByEmailPrevious()
        ).page()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public Optional<IdUser> userGet(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    try {
      final var user =
        this.sendCommand(IdA1ResponseUserGet.class, new IdA1CommandUserGet(id))
          .user();

      if (user.isPresent()) {
        return Optional.of(user.get().toUser());
      }

      return Optional.empty();
    } catch (final IdPasswordException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public Optional<IdUser> userGetByEmail(
    final IdEmail email)
    throws IdAClientException, InterruptedException
  {
    try {
      final var user =
        this.sendCommand(
            IdA1ResponseUserGet.class,
            new IdA1CommandUserGetByEmail(email.value()))
          .user();

      if (user.isPresent()) {
        return Optional.of(user.get().toUser());
      }

      return Optional.empty();
    } catch (final IdPasswordException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdUser userUpdate(
    final IdUser user)
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseUserUpdate.class,
          new IdA1CommandUserUpdate(IdA1User.ofUser(user))
        ).user()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdUser userCreate(
    final Optional<UUID> id,
    final IdName idName,
    final IdRealName realName,
    final IdEmail email,
    final IdPassword password)
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseUserCreate.class,
          new IdA1CommandUserCreate(
            id,
            idName.value(),
            realName.value(),
            email.value(),
            IdA1Password.ofPassword(password)
          )
        ).user()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public void userDelete(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    this.sendCommand(
      IdA1ResponseUserDelete.class,
      new IdA1CommandUserDelete(id)
    );
  }

  @Override
  public IdPage<IdAuditEvent> auditSearchBegin(
    final IdTimeRange timeRange,
    final Optional<String> owner,
    final Optional<String> type,
    final Optional<String> message,
    final int pageSize)
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseAuditSearchBegin.class,
          new IdA1CommandAuditSearchBegin(
            new IdA1AuditListParameters(
              IdA1TimeRange.of(timeRange),
              owner,
              type,
              message,
              pageSize
            )
          )
        ).page()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdAuditEvent> auditSearchNext()
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseAuditSearchNext.class,
          new IdA1CommandAuditSearchNext()
        ).page()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdAuditEvent> auditSearchPrevious()
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseAuditSearchPrevious.class,
          new IdA1CommandAuditSearchPrevious()
        ).page()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchBegin(
    final IdAdminSearchParameters parameters)
    throws IdAClientException, InterruptedException
  {
    try {
      final var a1parameters =
        IdA1AdminSearchParameters.of(parameters);

      final var response =
        this.sendCommand(
          IdA1ResponseAdminSearchBegin.class,
          new IdA1CommandAdminSearchBegin(a1parameters)
        );

      return response.page().toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchNext()
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseAdminSearchNext.class,
          new IdA1CommandAdminSearchNext()
        ).page()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchPrevious()
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseAdminSearchPrevious.class,
          new IdA1CommandAdminSearchPrevious()
        ).page()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchByEmailBegin(
    final IdAdminSearchByEmailParameters parameters)
    throws IdAClientException, InterruptedException
  {
    try {
      final var a1parameters =
        IdA1AdminSearchByEmailParameters.of(parameters);

      final var response =
        this.sendCommand(
          IdA1ResponseAdminSearchByEmailBegin.class,
          new IdA1CommandAdminSearchByEmailBegin(a1parameters)
        );

      return response.page().toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchByEmailNext()
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseAdminSearchByEmailNext.class,
          new IdA1CommandAdminSearchByEmailNext()
        ).page()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchByEmailPrevious()
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseAdminSearchByEmailPrevious.class,
          new IdA1CommandAdminSearchByEmailPrevious()
        ).page()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public Optional<IdAdmin> adminGet(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    try {
      final var admin =
        this.sendCommand(
            IdA1ResponseAdminGet.class,
            new IdA1CommandAdminGet(id))
          .admin();

      if (admin.isPresent()) {
        return Optional.of(admin.get().toModel());
      }

      return Optional.empty();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public Optional<IdAdmin> adminGetByEmail(
    final IdEmail email)
    throws IdAClientException, InterruptedException
  {
    try {
      final var admin =
        this.sendCommand(
            IdA1ResponseAdminGet.class,
            new IdA1CommandAdminGetByEmail(email.value()))
          .admin();

      if (admin.isPresent()) {
        return Optional.of(admin.get().toModel());
      }

      return Optional.empty();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdAdmin adminUpdate(
    final IdAdmin admin)
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseAdminUpdate.class,
          new IdA1CommandAdminUpdate(IdA1Admin.ofAdmin(admin))
        ).admin()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public IdAdmin adminCreate(
    final Optional<UUID> id,
    final IdName idName,
    final IdRealName realName,
    final IdEmail email,
    final IdPassword password,
    final Set<IdAdminPermission> permissions)
    throws IdAClientException, InterruptedException
  {
    try {
      return this.sendCommand(
          IdA1ResponseAdminCreate.class,
          new IdA1CommandAdminCreate(
            id,
            idName.value(),
            realName.value(),
            email.value(),
            IdA1Password.ofPassword(password),
            permissions.stream()
              .map(IdA1AdminPermission::ofPermission)
              .collect(Collectors.toUnmodifiableSet())
          )
        ).admin()
        .toModel();
    } catch (final IdProtocolException e) {
      throw new IdAClientException(e);
    }
  }

  @Override
  public void adminDelete(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    this.sendCommand(
      IdA1ResponseAdminDelete.class,
      new IdA1CommandAdminDelete(id)
    );
  }
}
