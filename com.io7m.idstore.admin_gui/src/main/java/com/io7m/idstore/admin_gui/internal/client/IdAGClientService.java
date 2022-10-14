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

package com.io7m.idstore.admin_gui.internal.client;

import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.admin_client.api.IdAClientFactoryType;
import com.io7m.idstore.admin_client.api.IdAClientType;
import com.io7m.idstore.admin_gui.internal.IdAGStrings;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventBus;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAuditEvent;
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
import com.io7m.idstore.services.api.IdServiceType;
import com.io7m.taskrecorder.core.TRTask;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.io7m.idstore.admin_gui.internal.client.IdAGClientStatus.DISCONNECTED;
import static com.io7m.idstore.model.IdUserColumn.BY_IDNAME;

/**
 * A client service.
 */

public final class IdAGClientService implements IdServiceType, Closeable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAGClientService.class);

  private static final IdUserColumnOrdering DEFAULT_USER_ORDERING =
    new IdUserColumnOrdering(BY_IDNAME, true);

  private final IdAGEventBus eventBus;
  private final ExecutorService executor;
  private final IdAClientType client;
  private final SimpleObjectProperty<IdAGClientStatus> status;
  private final IdAGStrings strings;
  private URI serverLatest;

  private IdAGClientService(
    final IdAGEventBus inEventBus,
    final ExecutorService inExecutor,
    final IdAClientType inClient,
    final IdAGStrings inStrings)
  {
    this.eventBus =
      Objects.requireNonNull(inEventBus, "eventBus");
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.client =
      Objects.requireNonNull(inClient, "client");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");

    this.serverLatest =
      URI.create("urn:unspecified");
    this.status =
      new SimpleObjectProperty<>(DISCONNECTED);
  }

  /**
   * Create a new client service.
   *
   * @param eventBus The event bus
   * @param clients  The client factory
   * @param strings  The string resources
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
    final IdAGStrings strings,
    final Locale locale)
    throws IdAClientException, InterruptedException
  {
    final var executor =
      Executors.newSingleThreadExecutor(r -> {
        final var thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName(
          "com.io7m.idstore.admin_gui.internal.client.IdAGClientService[%d]"
            .formatted(Long.valueOf(thread.getId())));
        return thread;
      });

    return new IdAGClientService(
      eventBus,
      executor,
      clients.create(locale),
      strings
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

  public ReadOnlyObjectProperty<IdAGClientStatus> status()
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
  public void close()
    throws IOException
  {
    this.client.close();
    this.executor.shutdown();
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

    final var eventConnecting =
      new IdAGClientEventConnecting(
        this.strings.format("client.connecting", this.serverLatest));

    final var eventConnectionOK =
      new IdAGClientEventConnectionSucceeded(
        this.strings.format("client.connected", this.serverLatest));

    final var eventConnected =
      new IdAGClientEventConnected(
        this.strings.format("client.connected", this.serverLatest));

    this.publishEvent(eventConnecting);

    final var future = new CompletableFuture<IdAdmin>();
    this.executor.submit(() -> {
      final var task =
        TRTask.create(LOG, eventConnecting.message());

      try {
        future.complete(this.client.login(
          username,
          password,
          this.serverLatest));
        task.setSucceeded();
        this.publishEvent(eventConnectionOK);
        this.publishEvent(eventConnected);
      } catch (final Exception e) {
        future.completeExceptionally(e);
        final var text =
          this.strings.format("client.connectionFailed", e.getMessage());
        task.setFailed(text, e);
        this.publishEvent(new IdAGClientEventConnectionFailed(task, text));
      }
    });
    return future;
  }

  private void publishEvent(
    final IdAGClientEventType event)
  {
    Platform.runLater(() -> {
      this.status.set(event.clientStatus());
      this.eventBus.submit(event);
    });
  }

  private TRTask<?> requestStart()
  {
    final var message = this.strings.format("client.requesting");
    this.publishEvent(new IdAGClientEventRequesting(message));
    return TRTask.create(LOG, message);
  }

  /**
   * Disconnect from the server.
   */

  public void disconnect()
  {
    this.executor.submit(() -> {
      try {
        this.client.close();
      } catch (final IOException e) {
        LOG.error("close: ", e);
      }
      this.publishEvent(
        new IdAGClientEventDisconnected(
          this.strings.format("client.disconnected"))
      );
    });
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
    final var future = new CompletableFuture<IdPage<IdUserSummary>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(
          this.client.userSearchBegin(
            new IdUserSearchParameters(
              timeCreatedRange,
              timeUpdatedRange,
              search,
              DEFAULT_USER_ORDERING,
              100
            )
          )
        );
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
  }

  private void requestFinish()
  {
    this.publishEvent(
      new IdAGClientEventConnected(
        this.strings.format("client.connected", this.serverLatest))
    );
  }

  /**
   * Get the next page of users.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdUserSummary>> userSearchNext()
  {
    final var future = new CompletableFuture<IdPage<IdUserSummary>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.userSearchNext());
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
  }

  /**
   * Get the previous page of users.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdUserSummary>> userSearchPrevious()
  {
    final var future = new CompletableFuture<IdPage<IdUserSummary>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.userSearchPrevious());
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
  }

  private Exception requestFailed(
    final TRTask<?> task,
    final Exception e)
  {
    task.setFailed(e.getMessage(), e);
    this.publishEvent(new IdAGClientEventRequestFailed(task, e.getMessage()));
    return e;
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
    final var future = new CompletableFuture<Optional<IdUser>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.userGet(id));
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<IdUser>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.userUpdate(id, idName, realName, password));
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
  }

  /**
   * Retrieve a user.
   *
   * @param email The user email
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<Optional<IdUser>> userGetForEmail(
    final String email)
  {
    final var future = new CompletableFuture<Optional<IdUser>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.userGetByEmail(new IdEmail(email)));
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<IdPage<IdAuditEvent>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.auditSearchBegin(
          timeRange,
          owner,
          type,
          message,
          100
        ));
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
  }

  /**
   * Get the previous page of events.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdAuditEvent>> auditSearchPrevious()
  {
    final var future = new CompletableFuture<IdPage<IdAuditEvent>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.auditSearchPrevious());
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
  }

  /**
   * Get the next page of events.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdAuditEvent>> auditSearchNext()
  {
    final var future = new CompletableFuture<IdPage<IdAuditEvent>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.auditSearchNext());
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<IdPage<IdUserSummary>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(
          this.client.userSearchByEmailBegin(
            new IdUserSearchByEmailParameters(
              timeCreatedRange,
              timeUpdatedRange,
              search,
              DEFAULT_USER_ORDERING,
              100
            )
          )
        );
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
  }

  /**
   * Get the next page of users.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdUserSummary>> userSearchByEmailNext()
  {
    final var future = new CompletableFuture<IdPage<IdUserSummary>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.userSearchByEmailNext());
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
  }

  /**
   * Get the previous page of users.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdPage<IdUserSummary>> userSearchByEmailPrevious()
  {
    final var future = new CompletableFuture<IdPage<IdUserSummary>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.userSearchByEmailPrevious());
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<Void>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        this.client.userDelete(id);
        future.complete(null);
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
  }

  /**
   * Fetch the admin's own profile.
   *
   * @return A future representing the operation in progress
   */

  public CompletableFuture<IdAdmin> self()
  {
    final var future = new CompletableFuture<IdAdmin>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.adminSelf());
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<IdAdmin>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.adminEmailAdd(id, email));
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<IdAdmin>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.adminEmailRemove(id, email));
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<IdUser>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.userEmailAdd(id, email));
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<IdUser>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.userEmailRemove(id, email));
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<Optional<IdBan>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.userBanGet(id));
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<IdBan>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        this.client.userBanCreate(ban);
        future.complete(ban);
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<Optional<IdBan>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        this.client.userBanDelete(new IdBan(id, "", Optional.empty()));
        future.complete(Optional.empty());
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<List<IdLogin>>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.userLoginHistory(id));
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
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
    final var future = new CompletableFuture<IdUser>();
    this.executor.submit(() -> {
      final var task = this.requestStart();

      try {
        future.complete(this.client.userCreate(create));
        this.requestFinish();
      } catch (final Exception e) {
        future.completeExceptionally(this.requestFailed(task, e));
      }
    });
    return future;
  }
}
