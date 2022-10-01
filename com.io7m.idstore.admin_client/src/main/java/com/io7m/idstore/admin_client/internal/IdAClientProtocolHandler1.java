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
import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdAuditSearchParameters;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanCreate;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanDelete;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanGet;
import com.io7m.idstore.protocol.admin.IdACommandAdminCreate;
import com.io7m.idstore.protocol.admin.IdACommandAdminDelete;
import com.io7m.idstore.protocol.admin.IdACommandAdminEmailAdd;
import com.io7m.idstore.protocol.admin.IdACommandAdminEmailRemove;
import com.io7m.idstore.protocol.admin.IdACommandAdminGet;
import com.io7m.idstore.protocol.admin.IdACommandAdminGetByEmail;
import com.io7m.idstore.protocol.admin.IdACommandAdminPermissionGrant;
import com.io7m.idstore.protocol.admin.IdACommandAdminPermissionRevoke;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandAdminSelf;
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdate;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandLogin;
import com.io7m.idstore.protocol.admin.IdACommandType;
import com.io7m.idstore.protocol.admin.IdACommandUserBanCreate;
import com.io7m.idstore.protocol.admin.IdACommandUserBanDelete;
import com.io7m.idstore.protocol.admin.IdACommandUserBanGet;
import com.io7m.idstore.protocol.admin.IdACommandUserCreate;
import com.io7m.idstore.protocol.admin.IdACommandUserDelete;
import com.io7m.idstore.protocol.admin.IdACommandUserEmailAdd;
import com.io7m.idstore.protocol.admin.IdACommandUserEmailRemove;
import com.io7m.idstore.protocol.admin.IdACommandUserGet;
import com.io7m.idstore.protocol.admin.IdACommandUserGetByEmail;
import com.io7m.idstore.protocol.admin.IdACommandUserLoginHistory;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandUserSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandUserUpdate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanDelete;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseAdminCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminDelete;
import com.io7m.idstore.protocol.admin.IdAResponseAdminGet;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSelf;
import com.io7m.idstore.protocol.admin.IdAResponseAdminUpdate;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseLogin;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanDelete;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserDelete;
import com.io7m.idstore.protocol.admin.IdAResponseUserGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserLoginHistory;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserUpdate;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.protocol.api.IdProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
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
  private final IdACB1Messages messages;
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
      new IdACB1Messages();

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
    this.sendLogin(new IdACommandLogin(new IdName(admin), password));
    return this;
  }

  private IdAResponseLogin sendLogin(
    final IdACommandLogin message)
    throws InterruptedException, IdAClientException
  {
    return this.send(this.loginURI, IdAResponseLogin.class, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends IdAResponseType> T sendCommand(
    final Class<T> responseClass,
    final IdACommandType<T> message)
    throws InterruptedException, IdAClientException
  {
    return this.send(this.commandURI, responseClass, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends IdAResponseType> Optional<T> send(
    final URI uri,
    final Class<T> responseClass,
    final IdACommandType<T> message,
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

      if (!contentType.equals(IdACB1Messages.contentType())) {
        throw new IdAClientException(
          PROTOCOL_ERROR,
          this.strings()
            .format(
              "errorContentType",
              commandType,
              IdACB1Messages.contentType(),
              contentType)
        );
      }

      final var responseMessage =
        this.messages.parse(response.body());

      if (!(responseMessage instanceof IdAResponseType)) {
        throw new IdAClientException(
          PROTOCOL_ERROR,
          this.strings()
            .format(
              "errorResponseType",
              "(unavailable)",
              commandType,
              IdAResponseType.class,
              responseMessage.getClass())
        );
      }

      final var responseActual = (IdAResponseType) responseMessage;
      if (responseActual instanceof IdAResponseError error) {
        throw new IdAClientException(
          new IdErrorCode(error.errorCode()),
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
          PROTOCOL_ERROR,
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
    } catch (final IdProtocolException e) {
      throw new IdAClientException(PROTOCOL_ERROR, e);
    } catch (final IOException e) {
      throw new IdAClientException(IO_ERROR, e);
    }
  }

  @Override
  public IdAdmin adminSelf()
    throws IdAClientException, InterruptedException
  {
    final var response =
      this.sendCommand(IdAResponseAdminSelf.class, new IdACommandAdminSelf());

    return response.admin();
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
    return this.sendCommand(
      IdAResponseUserSearchBegin.class,
      new IdACommandUserSearchBegin(parameters)
    ).page();
  }

  @Override
  public IdPage<IdUserSummary> userSearchNext()
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseUserSearchNext.class,
      new IdACommandUserSearchNext()
    ).page();
  }

  @Override
  public IdPage<IdUserSummary> userSearchPrevious()
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseUserSearchPrevious.class,
      new IdACommandUserSearchPrevious()
    ).page();
  }

  @Override
  public IdPage<IdUserSummary> userSearchByEmailBegin(
    final IdUserSearchByEmailParameters parameters)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseUserSearchByEmailBegin.class,
      new IdACommandUserSearchByEmailBegin(parameters)
    ).page();
  }

  @Override
  public IdPage<IdUserSummary> userSearchByEmailNext()
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseUserSearchByEmailNext.class,
      new IdACommandUserSearchByEmailNext()
    ).page();
  }

  @Override
  public IdPage<IdUserSummary> userSearchByEmailPrevious()
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseUserSearchByEmailPrevious.class,
      new IdACommandUserSearchByEmailPrevious()
    ).page();
  }

  @Override
  public Optional<IdUser> userGet(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseUserGet.class,
      new IdACommandUserGet(id)
    ).user();
  }

  @Override
  public Optional<IdUser> userGetByEmail(
    final IdEmail email)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseUserGet.class,
      new IdACommandUserGetByEmail(email)
    ).user();
  }

  @Override
  public IdUser userUpdate(
    final UUID user,
    final Optional<IdName> idName,
    final Optional<IdRealName> realName,
    final Optional<IdPassword> password)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseUserUpdate.class,
      new IdACommandUserUpdate(user, idName, realName, password)
    ).user();
  }

  @Override
  public IdUser userEmailAdd(
    final UUID id,
    final IdEmail email)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseUserUpdate.class,
      new IdACommandUserEmailAdd(id, email)
    ).user();
  }

  @Override
  public IdUser userEmailRemove(
    final UUID id,
    final IdEmail email)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseUserUpdate.class,
      new IdACommandUserEmailRemove(id, email)
    ).user();
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
    return this.sendCommand(
      IdAResponseUserCreate.class,
      new IdACommandUserCreate(id, idName, realName, email, password)
    ).user();
  }

  @Override
  public void userDelete(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    this.sendCommand(
      IdAResponseUserDelete.class,
      new IdACommandUserDelete(id)
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
    return this.sendCommand(
      IdAResponseAuditSearchBegin.class,
      new IdACommandAuditSearchBegin(
        new IdAuditSearchParameters(timeRange, owner, type, message, pageSize)
      )
    ).page();
  }

  @Override
  public IdPage<IdAuditEvent> auditSearchNext()
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAuditSearchNext.class,
      new IdACommandAuditSearchNext()
    ).page();
  }

  @Override
  public IdPage<IdAuditEvent> auditSearchPrevious()
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAuditSearchPrevious.class,
      new IdACommandAuditSearchPrevious()
    ).page();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchBegin(
    final IdAdminSearchParameters parameters)
    throws IdAClientException, InterruptedException
  {
    final var response =
      this.sendCommand(
        IdAResponseAdminSearchBegin.class,
        new IdACommandAdminSearchBegin(parameters)
      );
    return response.page();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchNext()
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminSearchNext.class,
      new IdACommandAdminSearchNext()
    ).page();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchPrevious()
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminSearchPrevious.class,
      new IdACommandAdminSearchPrevious()
    ).page();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchByEmailBegin(
    final IdAdminSearchByEmailParameters parameters)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminSearchByEmailBegin.class,
      new IdACommandAdminSearchByEmailBegin(parameters)
    ).page();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchByEmailNext()
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminSearchByEmailNext.class,
      new IdACommandAdminSearchByEmailNext()
    ).page();
  }

  @Override
  public IdPage<IdAdminSummary> adminSearchByEmailPrevious()
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminSearchByEmailPrevious.class,
      new IdACommandAdminSearchByEmailPrevious()
    ).page();
  }

  @Override
  public Optional<IdAdmin> adminGet(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminGet.class,
      new IdACommandAdminGet(id)
    ).admin();
  }

  @Override
  public Optional<IdAdmin> adminGetByEmail(
    final IdEmail email)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminGet.class,
      new IdACommandAdminGetByEmail(email)
    ).admin();
  }

  @Override
  public IdAdmin adminUpdate(
    final UUID admin,
    final Optional<IdName> idName,
    final Optional<IdRealName> realName,
    final Optional<IdPassword> password)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminUpdate.class,
      new IdACommandAdminUpdate(admin, idName, realName, password)
    ).admin();
  }

  @Override
  public IdAdmin adminCreate(
    final Optional<UUID> id,
    final IdName idName,
    final IdRealName realName,
    final IdEmail email,
    final IdPassword password,
    final IdAdminPermissionSet permissions)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminCreate.class,
      new IdACommandAdminCreate(
        id,
        idName,
        realName,
        email,
        password,
        permissions.impliedPermissions()
      )
    ).admin();
  }

  @Override
  public void adminDelete(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    this.sendCommand(
      IdAResponseAdminDelete.class,
      new IdACommandAdminDelete(id)
    );
  }

  @Override
  public IdAdmin adminEmailAdd(
    final UUID id,
    final IdEmail email)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminUpdate.class,
      new IdACommandAdminEmailAdd(id, email)
    ).admin();
  }

  @Override
  public IdAdmin adminEmailRemove(
    final UUID id,
    final IdEmail email)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminUpdate.class,
      new IdACommandAdminEmailRemove(id, email)
    ).admin();
  }

  @Override
  public IdAdmin adminPermissionGrant(
    final UUID id,
    final IdAdminPermission permission)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminUpdate.class,
      new IdACommandAdminPermissionGrant(id, permission)
    ).admin();
  }

  @Override
  public IdAdmin adminPermissionRevoke(
    final UUID id,
    final IdAdminPermission permission)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminUpdate.class,
      new IdACommandAdminPermissionRevoke(id, permission)
    ).admin();
  }

  @Override
  public void adminBanCreate(
    final IdBan ban)
    throws IdAClientException, InterruptedException
  {
    this.sendCommand(
      IdAResponseAdminBanCreate.class,
      new IdACommandAdminBanCreate(ban)
    );
  }

  @Override
  public Optional<IdBan> adminBanGet(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseAdminBanGet.class,
      new IdACommandAdminBanGet(id)
    ).ban();
  }

  @Override
  public void adminBanDelete(
    final IdBan ban)
    throws IdAClientException, InterruptedException
  {
    this.sendCommand(
      IdAResponseAdminBanDelete.class,
      new IdACommandAdminBanDelete(ban.user())
    );
  }

  @Override
  public void userBanCreate(
    final IdBan ban)
    throws IdAClientException, InterruptedException
  {
    this.sendCommand(
      IdAResponseUserBanCreate.class,
      new IdACommandUserBanCreate(ban)
    );
  }

  @Override
  public Optional<IdBan> userBanGet(
    final UUID id)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseUserBanGet.class,
      new IdACommandUserBanGet(id)
    ).ban();
  }

  @Override
  public void userBanDelete(final IdBan ban)
    throws IdAClientException, InterruptedException
  {
    this.sendCommand(
      IdAResponseUserBanDelete.class,
      new IdACommandUserBanDelete(ban.user())
    );
  }

  @Override
  public List<IdLogin> userLoginHistory(final UUID id)
    throws IdAClientException, InterruptedException
  {
    return this.sendCommand(
      IdAResponseUserLoginHistory.class,
      new IdACommandUserLoginHistory(id)
    ).history();
  }
}
