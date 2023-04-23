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

package com.io7m.idstore.user_client.internal;

import com.io7m.hibiscus.api.HBResultFailure;
import com.io7m.hibiscus.api.HBResultSuccess;
import com.io7m.hibiscus.api.HBResultType;
import com.io7m.hibiscus.api.HBState;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientCredentials;
import com.io7m.idstore.user_client.api.IdUClientEventCommandFailed;
import com.io7m.idstore.user_client.api.IdUClientEventType;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.idstore.user_client.api.IdUClientType;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.io7m.hibiscus.api.HBState.CLIENT_AUTHENTICATING;
import static com.io7m.hibiscus.api.HBState.CLIENT_AUTHENTICATION_FAILED;
import static com.io7m.hibiscus.api.HBState.CLIENT_CONNECTED;
import static com.io7m.hibiscus.api.HBState.CLIENT_IDLE;
import static com.io7m.hibiscus.api.HBState.CLIENT_SENDING_COMMAND;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.user_client.internal.IdUUUIDs.nullUUID;

/**
 * The basic client.
 */

public final class IdUClient
  implements IdUClientType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUClient.class);

  private final AtomicReference<HBState> stateNow;
  private final SubmissionPublisher<HBState> state;
  private final SubmissionPublisher<IdUClientEventType> events;
  private final IdUClientConfiguration configuration;
  private final ExecutorService commandExecutor;
  private final CloseableCollectionType<IdUClientException> resources;
  private final IdUStrings strings;
  private final HttpClient httpClient;
  private final AtomicBoolean closed;
  private volatile IdUHandlerType handler;

  private IdUClient(
    final IdUClientConfiguration inConfiguration,
    final ExecutorService inCommandExecutor,
    final CloseableCollectionType<IdUClientException> inResources,
    final IdUStrings inStrings,
    final HttpClient inHttpClient)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.commandExecutor =
      Objects.requireNonNull(inCommandExecutor, "commandExecutor");
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");

    this.events =
      new SubmissionPublisher<>();
    this.state =
      new SubmissionPublisher<>();
    this.stateNow =
      new AtomicReference<>(HBState.CLIENT_DISCONNECTED);
    this.handler =
      new IdUHandlerDisconnected(inConfiguration, inStrings, inHttpClient);
    this.closed =
      new AtomicBoolean(false);
  }

  /**
   * Open the basic client.
   *
   * @param configuration The configuration
   * @param strings       String resources
   * @param httpClient    The HTTP client used for requests
   *
   * @return The basic client
   */

  public static IdUClientType open(
    final IdUClientConfiguration configuration,
    final IdUStrings strings,
    final HttpClient httpClient)
  {
    final var resources =
      CloseableCollection.create(() -> {
        return new IdUClientException(
          strings.format("resourceCloseFailed"),
          IO_ERROR,
          Map.of(),
          Optional.empty(),
          Optional.empty()
        );
      });

    final var commandExecutor =
      Executors.newSingleThreadExecutor(r -> {
        final var th = new Thread(r);
        th.setName(
          "com.io7m.cardant.client.basic.command[%s]"
            .formatted(Long.toUnsignedString(th.getId()))
        );
        return th;
      });

    resources.add(commandExecutor::shutdown);

    return new IdUClient(
      configuration,
      commandExecutor,
      resources,
      strings,
      httpClient
    );
  }

  private static void pause()
  {
    try {
      Thread.sleep(5_000L);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void publishState(
    final HBState newState)
  {
    this.stateNow.set(newState);
    this.state.submit(newState);
  }

  private void publishEvent(
    final IdUClientEventType event)
  {
    this.events.submit(event);
  }

  @Override
  public boolean isConnected()
  {
    return this.handler.isConnected();
  }

  @Override
  public Flow.Publisher<IdUClientEventType> events()
  {
    return this.events;
  }

  @Override
  public Flow.Publisher<HBState> state()
  {
    return this.state;
  }

  @Override
  public HBState stateNow()
  {
    return this.stateNow.get();
  }

  @Override
  public <RS1 extends IdUResponseType> HBResultType<RS1, IdUResponseError> login(
    final IdUClientCredentials credentials)
  {
    Objects.requireNonNull(credentials, "credentials");

    this.checkNotClosed();
    this.publishState(CLIENT_AUTHENTICATING);

    HBResultType<IdUNewHandler, IdUResponseError> result;
    try {
      result = this.handler.login(credentials);
    } catch (final InterruptedException e) {
      result = new HBResultFailure<>(new IdUResponseError(
        nullUUID(),
        e.getMessage(),
        IO_ERROR,
        Map.of(),
        Optional.empty()
      ));
    }

    if (result instanceof final HBResultSuccess<IdUNewHandler, IdUResponseError> success) {
      this.handler = success.result().handler();
      this.publishState(CLIENT_CONNECTED);
      return success.map(h -> (RS1) h.response());
    }
    if (result instanceof final HBResultFailure<IdUNewHandler, IdUResponseError> failure) {
      this.publishState(CLIENT_AUTHENTICATION_FAILED);
      return failure.cast();
    }
    throw new UnreachableCodeException();
  }

  @Override
  public <C1 extends IdUCommandType<?>, RS1 extends IdUResponseType>
  HBResultType<RS1, IdUResponseError>
  execute(
    final C1 command)
  {
    this.checkNotClosed();

    try {
      final HBResultType<?, IdUResponseError> response;
      if (command instanceof IdUCommandLogin) {
        response = new HBResultFailure<>(
          new IdUResponseError(
            nullUUID(),
            this.strings.format("errorLoginNotHere"),
            PROTOCOL_ERROR,
            Map.of(),
            Optional.empty())
        );
      } else {
        this.publishState(CLIENT_SENDING_COMMAND);
        response = this.handler.executeCommand(
          (IdUCommandType<? extends IdUResponseType>) command
        );
        this.publishState(CLIENT_IDLE);

        if (response instanceof final HBResultFailure<?, IdUResponseError> error) {
          this.publishEvent(
            new IdUClientEventCommandFailed(
              command.getClass().getSimpleName(),
              error.result()
            )
          );
        }
      }

      return (HBResultType<RS1, IdUResponseError>) response;
    } catch (final Throwable e) {
      return new HBResultFailure<>(
        new IdUResponseError(
          nullUUID(),
          e.getMessage(),
          IO_ERROR,
          Map.of(),
          Optional.empty()
        )
      );
    } finally {
      this.publishState(CLIENT_IDLE);
    }
  }

  @Override
  public <T> CompletableFuture<T> runAsync(
    final Supplier<T> f)
  {
    this.checkNotClosed();

    final var future = new CompletableFuture<T>();
    this.commandExecutor.execute(() -> {
      try {
        future.complete(f.get());
      } catch (final Throwable e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  private void checkNotClosed()
  {
    if (this.closed.get()) {
      throw new IllegalStateException("Client is closed!");
    }
  }

  @Override
  public void close()
    throws IdUClientException
  {
    if (this.closed.compareAndSet(false, true)) {
      this.resources.close();
    }
  }
}
