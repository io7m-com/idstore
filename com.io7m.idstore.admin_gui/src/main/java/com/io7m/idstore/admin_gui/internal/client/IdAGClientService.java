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

package com.io7m.idstore.admin_gui.internal.client;

import com.io7m.hibiscus.api.HBState;
import com.io7m.idstore.admin_client.api.IdAClientAsynchronousType;
import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.admin_client.api.IdAClientCredentials;
import com.io7m.idstore.admin_client.api.IdAClientEventCommandFailed;
import com.io7m.idstore.admin_client.api.IdAClientEventCommandSucceeded;
import com.io7m.idstore.admin_client.api.IdAClientEventLoginFailed;
import com.io7m.idstore.admin_client.api.IdAClientEventLoginSucceeded;
import com.io7m.idstore.admin_client.api.IdAClientEventType;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.admin_client.api.IdAClientFactoryType;
import com.io7m.idstore.admin_gui.internal.IdAGPerpetualSubscriber;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventBus;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventStatusCompleted;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventStatusFailed;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventType;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminColumn;
import com.io7m.idstore.model.IdAdminColumnOrdering;
import com.io7m.idstore.model.IdAdminCreate;
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
import com.io7m.idstore.model.IdUserColumnOrdering;
import com.io7m.idstore.model.IdUserCreate;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.idstore.protocol.admin.IdACommandAdminCreate;
import com.io7m.idstore.protocol.admin.IdACommandAdminDelete;
import com.io7m.idstore.protocol.admin.IdACommandAdminEmailAdd;
import com.io7m.idstore.protocol.admin.IdACommandAdminEmailRemove;
import com.io7m.idstore.protocol.admin.IdACommandAdminGet;
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
import com.io7m.idstore.protocol.admin.IdAResponseLogin;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanDelete;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserCreate;
import com.io7m.idstore.protocol.admin.IdAResponseUserGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserLoginHistory;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseUserSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseUserUpdate;
import com.io7m.repetoir.core.RPServiceType;
import com.io7m.taskrecorder.core.TRTaskRecorder;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.io7m.idstore.model.IdUserColumn.BY_IDNAME;

/**
 * A client service.
 */

public final class IdAGClientService implements RPServiceType, AutoCloseable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAGClientService.class);

  private static final IdUserColumnOrdering DEFAULT_USER_ORDERING =
    new IdUserColumnOrdering(BY_IDNAME, true);

  private static final IdAdminColumnOrdering DEFAULT_ADMIN_ORDERING =
    new IdAdminColumnOrdering(IdAdminColumn.BY_IDNAME, true);

  private final IdAGEventBus eventBus;
  private final SimpleObjectProperty<HBState> status;
  private final IdAClientAsynchronousType client;
  private URI serverLatest;
  private IdAdmin self;

  private IdAGClientService(
    final IdAGEventBus inEventBus,
    final IdAClientAsynchronousType inClient)
  {
    this.eventBus =
      Objects.requireNonNull(inEventBus, "eventBus");
    this.client =
      Objects.requireNonNull(inClient, "client");

    this.serverLatest =
      URI.create("urn:unspecified");
    this.status =
      new SimpleObjectProperty<>(HBState.CLIENT_DISCONNECTED);
  }

  /**
   * Create a new client service.
   *
   * @param eventBus The event bus
   * @param clients  The client factory
   * @param locale   The locale
   *
   * @return A new service
   *
   * @throws IdAClientException   On errors
   * @throws InterruptedException On interruption
   */

  public static IdAGClientService create(
    final IdAGEventBus eventBus,
    final IdAClientFactoryType clients,
    final Locale locale)
    throws IdAClientException, InterruptedException
  {
    final var client =
      clients.openAsynchronousClient(new IdAClientConfiguration(locale));
    final var service =
      new IdAGClientService(eventBus, client);

    client.state()
      .subscribe(new IdAGPerpetualSubscriber<>(service.status::set));

    client.events()
      .subscribe(new IdAGPerpetualSubscriber<>(e -> {
        transformEvent(e).ifPresent(eventBus::submit);
      }));

    return service;
  }

  private static Optional<IdAGEventType> transformEvent(
    final IdAClientEventType e)
  {
    if (e instanceof final IdAClientEventCommandFailed cmd) {
      return transformEventCommandFailed(cmd);
    }
    if (e instanceof final IdAClientEventLoginFailed login) {
      return transformEventLoginFailed(login);
    }
    if (e instanceof final IdAClientEventCommandSucceeded cmd) {
      return transformEventCommandSucceeded(cmd);
    }
    if (e instanceof final IdAClientEventLoginSucceeded login) {
      return transformEventLoginSucceeded(login);
    }
    return Optional.empty();
  }

  private static Optional<IdAGEventType> transformEventLoginSucceeded(
    final IdAClientEventLoginSucceeded login)
  {
    return Optional.of(
      new IdAGClientEvent(
        "Logged in successfully.",
        new IdAGEventStatusCompleted()
      )
    );
  }

  private static Optional<IdAGEventType> transformEventCommandSucceeded(
    final IdAClientEventCommandSucceeded cmd)
  {
    return Optional.of(
      new IdAGClientEvent(
        "Executed %s successfully.".formatted(cmd.command()),
        new IdAGEventStatusCompleted()
      )
    );
  }

  private static Optional<IdAGEventType> transformEventLoginFailed(
    final IdAClientEventLoginFailed login)
  {
    final var recorder =
      TRTaskRecorder.create(LOG, "Logging in...");

    final var error = login.error();
    recorder.setStepFailed(error.message());
    recorder.setTaskFailed(error.message());
    final var task = recorder.toTask();

    return Optional.of(
      new IdAGClientEvent(
        error.message(),
        new IdAGEventStatusFailed(
          task,
          error.errorCode(),
          error.message(),
          error.attributes(),
          error.remediatingAction(),
          error.exception()
        )
      )
    );
  }

  private static Optional<IdAGEventType> transformEventCommandFailed(
    final IdAClientEventCommandFailed cmd)
  {
    final var recorder =
      TRTaskRecorder.create(LOG, "Executing " + cmd.command());

    final var error = cmd.error();
    recorder.setStepFailed(error.message());
    recorder.setTaskFailed(error.message());
    final var task = recorder.toTask();

    return Optional.of(
      new IdAGClientEvent(
        error.message(),
        new IdAGEventStatusFailed(
          task,
          error.errorCode(),
          error.message(),
          error.attributes(),
          error.remediatingAction(),
          error.exception()
        )
      )
    );
  }

  private static URI uriOf(
    final boolean https,
    final String host,
    final int port)
  {
    if (https) {
      return URI.create(
        "https://%s:%d/".formatted(host, Integer.valueOf(port))
      );
    } else {
      return URI.create(
        "http://%s:%d/".formatted(host, Integer.valueOf(port))
      );
    }
  }

  /**
   * @return The current client status
   */

  public ReadOnlyObjectProperty<HBState> status()
  {
    return this.status;
  }

  @Override
  public String description()
  {
    return String.format(
      "[IdAGClientService 0x%s]",
      Integer.toUnsignedString(this.hashCode(), 16)
    );
  }

  @Override
  public String toString()
  {
    return this.description();
  }

  @Override
  public void close()
    throws Exception
  {
    this.client.close();
  }

  /**
   * Connect to the server and log in.
   *
   * @param host     The hostname
   * @param port     The port
   * @param https    {@code true} if https is required
   * @param username The username
   * @param password The password
   *
   * @return The future representing the login in process
   */

  public CompletableFuture<IdAdmin> login(
    final String host,
    final int port,
    final boolean https,
    final String username,
    final String password)
  {
    this.serverLatest =
      uriOf(https, host, port);

    final var credentials =
      new IdAClientCredentials(username, password, this.serverLatest, Map.of());

    return this.client.loginAsyncOrElseThrow(credentials, IdAClientException::ofError)
      .thenApply(IdAResponseLogin.class::cast)
      .thenCompose(x -> this.client.executeAsyncOrElseThrow(new IdACommandAdminSelf(), IdAClientException::ofError))
      .thenApply(IdAResponseAdminSelf.class::cast)
      .thenApply(IdAResponseAdminSelf::admin);
  }

  /**
   * Disconnect from the server.
   */

  public void disconnect()
  {
    this.client.disconnectAsync();
  }

  /**
   * Start searching for users.
   *
   * @param search           The search query
   * @param timeCreatedRange The created time range
   * @param timeUpdatedRange The updated time range
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdUserSummary>> userSearchBegin(
    final IdTimeRange timeCreatedRange,
    final IdTimeRange timeUpdatedRange,
    final Optional<String> search)
  {
    final var command =
      new IdACommandUserSearchBegin(
        new IdUserSearchParameters(
          timeCreatedRange,
          timeUpdatedRange,
          search,
          DEFAULT_USER_ORDERING,
          100
        ));

    return this.client.executeAsyncOrElseThrow(
        command,
        IdAClientException::ofError)
      .thenApply(IdAResponseUserSearchBegin.class::cast)
      .thenApply(IdAResponseUserSearchBegin::page);
  }

  /**
   * Get the next page of users.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdUserSummary>> userSearchNext()
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserSearchNext(),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserSearchNext.class::cast)
      .thenApply(IdAResponseUserSearchNext::page);
  }

  /**
   * Get the previous page of users.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdUserSummary>> userSearchPrevious()
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserSearchPrevious(),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserSearchPrevious.class::cast)
      .thenApply(IdAResponseUserSearchPrevious::page);
  }

  /**
   * Retrieve a user.
   *
   * @param id The user ID
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<Optional<IdUser>> userGet(
    final UUID id)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserGet(id),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserGet.class::cast)
      .thenApply(IdAResponseUserGet::user);
  }

  /**
   * Update the given user.
   *
   * @param id       The ID
   * @param idName   The ID name
   * @param realName The real name
   * @param password The password
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdUser> userUpdate(
    final UUID id,
    final Optional<IdName> idName,
    final Optional<IdRealName> realName,
    final Optional<IdPassword> password)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserUpdate(id, idName, realName, password),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserUpdate.class::cast)
      .thenApply(IdAResponseUserUpdate::user);
  }

  /**
   * Retrieve a user.
   *
   * @param email The user email
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<Optional<IdUser>> userGetForEmail(
    final IdEmail email)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserGetByEmail(email),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserGet.class::cast)
      .thenApply(IdAResponseUserGet::user);
  }

  /**
   * Start searching for audit events.
   *
   * @param timeRange The time range
   * @param owner     The owner
   * @param type      The type
   * @param message   The message
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdAuditEvent>> auditSearchBegin(
    final IdTimeRange timeRange,
    final Optional<String> owner,
    final Optional<String> type,
    final Optional<String> message)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAuditSearchBegin(new IdAuditSearchParameters(
          timeRange,
          owner,
          type,
          message,
          100
        )),
        IdAClientException::ofError)
      .thenApply(IdAResponseAuditSearchBegin.class::cast)
      .thenApply(IdAResponseAuditSearchBegin::page);
  }

  /**
   * Get the previous page of events.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdAuditEvent>> auditSearchPrevious()
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAuditSearchPrevious(),
        IdAClientException::ofError)
      .thenApply(IdAResponseAuditSearchPrevious.class::cast)
      .thenApply(IdAResponseAuditSearchPrevious::page);
  }

  /**
   * Get the next page of events.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdAuditEvent>> auditSearchNext()
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAuditSearchNext(),
        IdAClientException::ofError)
      .thenApply(IdAResponseAuditSearchNext.class::cast)
      .thenApply(IdAResponseAuditSearchNext::page);
  }

  /**
   * Start searching for users.
   *
   * @param search           The search query
   * @param timeCreatedRange The created time range
   * @param timeUpdatedRange The updated time range
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdUserSummary>> userSearchByEmailBegin(
    final IdTimeRange timeCreatedRange,
    final IdTimeRange timeUpdatedRange,
    final String search)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserSearchByEmailBegin(
          new IdUserSearchByEmailParameters(
            timeCreatedRange,
            timeUpdatedRange,
            search,
            DEFAULT_USER_ORDERING,
            100
          )
        ),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserSearchByEmailBegin.class::cast)
      .thenApply(IdAResponseUserSearchByEmailBegin::page);
  }

  /**
   * Get the next page of users.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdUserSummary>> userSearchByEmailNext()
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserSearchByEmailNext(),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserSearchByEmailNext.class::cast)
      .thenApply(IdAResponseUserSearchByEmailNext::page);
  }

  /**
   * Get the previous page of users.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdUserSummary>> userSearchByEmailPrevious()
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserSearchByEmailPrevious(),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserSearchByEmailPrevious.class::cast)
      .thenApply(IdAResponseUserSearchByEmailPrevious::page);
  }

  /**
   * Delete a user.
   *
   * @param id The user ID
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<Void> userDelete(
    final UUID id)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserDelete(id),
        IdAClientException::ofError)
      .thenRun(() -> {
      });
  }

  /**
   * Fetch the admin's own profile.
   *
   * @return The logged-in admin
   */

  public IdAdmin self()
  {
    if (this.self == null) {
      throw new IllegalStateException("Not logged in.");
    }

    return this.self;
  }

  /**
   * Get the admin's own profile.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdAdmin> adminSelf()
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminSelf(),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminSelf.class::cast)
      .thenApply(IdAResponseAdminSelf::admin);
  }

  /**
   * Add an email to the given admin.
   *
   * @param email The email
   * @param id    The ID
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdAdmin> adminEmailAdd(
    final UUID id,
    final IdEmail email)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminEmailAdd(id, email),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminUpdate.class::cast)
      .thenApply(IdAResponseAdminUpdate::admin);
  }

  /**
   * Remove an email from the given admin.
   *
   * @param email The email
   * @param id    The ID
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdAdmin> adminEmailRemove(
    final UUID id,
    final IdEmail email)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminEmailRemove(id, email),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminUpdate.class::cast)
      .thenApply(IdAResponseAdminUpdate::admin);
  }

  /**
   * Add an email to the given user.
   *
   * @param email The email
   * @param id    The ID
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdUser> userEmailAdd(
    final UUID id,
    final IdEmail email)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserEmailAdd(id, email),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserUpdate.class::cast)
      .thenApply(IdAResponseUserUpdate::user);
  }

  /**
   * Remove an email from the given user.
   *
   * @param email The email
   * @param id    The ID
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdUser> userEmailRemove(
    final UUID id,
    final IdEmail email)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserEmailRemove(id, email),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserUpdate.class::cast)
      .thenApply(IdAResponseUserUpdate::user);
  }

  /**
   * Get the ban for the given user.
   *
   * @param id The ID
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<Optional<IdBan>> userBanGet(
    final UUID id)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserBanGet(id),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserBanGet.class::cast)
      .thenApply(IdAResponseUserBanGet::ban);
  }

  /**
   * Create a ban for the given user.
   *
   * @param ban The ban
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdBan> userBanCreate(
    final IdBan ban)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserBanCreate(ban),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserBanCreate.class::cast)
      .thenApply(IdAResponseUserBanCreate::ban);
  }

  /**
   * Delete a ban for the given user.
   *
   * @param id The user ID
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<Optional<IdBan>> userBanDelete(
    final UUID id)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserBanDelete(id),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserBanDelete.class::cast)
      .thenApply(x -> Optional.empty());
  }

  /**
   * Get the login history for the given user.
   *
   * @param id The user ID
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<List<IdLogin>> userLoginHistory(
    final UUID id)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserLoginHistory(id),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserLoginHistory.class::cast)
      .thenApply(IdAResponseUserLoginHistory::history);
  }

  /**
   * Create a user.
   *
   * @param create The user creation info
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdUser> userCreate(
    final IdUserCreate create)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandUserCreate(
          create.id(),
          create.idName(),
          create.realName(),
          create.email(),
          create.password()),
        IdAClientException::ofError)
      .thenApply(IdAResponseUserCreate.class::cast)
      .thenApply(IdAResponseUserCreate::user);
  }

  /**
   * Create an admin.
   *
   * @param create The admin creation info
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdAdmin> adminCreate(
    final IdAdminCreate create)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminCreate(
          create.id(),
          create.idName(),
          create.realName(),
          create.email(),
          create.password(),
          create.permissions().impliedPermissions()),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminCreate.class::cast)
      .thenApply(IdAResponseAdminCreate::admin);
  }

  /**
   * Retrieve an admin.
   *
   * @param id The admin ID
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<Optional<IdAdmin>> adminGet(
    final UUID id)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminGet(id),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminGet.class::cast)
      .thenApply(IdAResponseAdminGet::admin);
  }

  /**
   * Start searching for admins.
   *
   * @param search           The search query
   * @param timeCreatedRange The created time range
   * @param timeUpdatedRange The updated time range
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdAdminSummary>> adminSearchByEmailBegin(
    final IdTimeRange timeCreatedRange,
    final IdTimeRange timeUpdatedRange,
    final String search)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminSearchByEmailBegin(
          new IdAdminSearchByEmailParameters(
            timeCreatedRange,
            timeUpdatedRange,
            search,
            DEFAULT_ADMIN_ORDERING,
            100
          )),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminSearchByEmailBegin.class::cast)
      .thenApply(IdAResponseAdminSearchByEmailBegin::page);
  }

  /**
   * Get the next page of admins.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdAdminSummary>> adminSearchByEmailNext()
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminSearchByEmailNext(),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminSearchByEmailNext.class::cast)
      .thenApply(IdAResponseAdminSearchByEmailNext::page);
  }

  /**
   * Get the previous page of admins.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdAdminSummary>> adminSearchByEmailPrevious()
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminSearchByEmailPrevious(),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminSearchByEmailPrevious.class::cast)
      .thenApply(IdAResponseAdminSearchByEmailPrevious::page);
  }

  /**
   * Start searching for admins.
   *
   * @param search           The search query
   * @param timeCreatedRange The created time range
   * @param timeUpdatedRange The updated time range
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdAdminSummary>> adminSearchBegin(
    final IdTimeRange timeCreatedRange,
    final IdTimeRange timeUpdatedRange,
    final Optional<String> search)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminSearchBegin(
          new IdAdminSearchParameters(
            timeCreatedRange,
            timeUpdatedRange,
            search,
            DEFAULT_ADMIN_ORDERING,
            100
          )),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminSearchBegin.class::cast)
      .thenApply(IdAResponseAdminSearchBegin::page);
  }

  /**
   * Get the next page of admins.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdAdminSummary>> adminSearchNext()
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminSearchNext(),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminSearchNext.class::cast)
      .thenApply(IdAResponseAdminSearchNext::page);
  }

  /**
   * Get the previous page of admins.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdAdminSummary>> adminSearchPrevious()
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminSearchPrevious(),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminSearchPrevious.class::cast)
      .thenApply(IdAResponseAdminSearchPrevious::page);
  }

  /**
   * Update the given admin.
   *
   * @param id       The ID
   * @param idName   The ID name
   * @param realName The real name
   * @param password The password
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdAdmin> adminUpdate(
    final UUID id,
    final Optional<IdName> idName,
    final Optional<IdRealName> realName,
    final Optional<IdPassword> password)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminUpdate(id, idName, realName, password),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminUpdate.class::cast)
      .thenApply(IdAResponseAdminUpdate::admin);
  }

  /**
   * Delete an admin.
   *
   * @param id The admin ID
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<Void> adminDelete(
    final UUID id)
  {
    return this.client.executeAsyncOrElseThrow(
        new IdACommandAdminDelete(id),
        IdAClientException::ofError)
      .thenApply(IdAResponseAdminDelete.class::cast)
      .thenRun(() -> {
      });
  }
}
