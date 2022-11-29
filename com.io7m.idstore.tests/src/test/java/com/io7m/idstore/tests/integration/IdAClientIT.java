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


package com.io7m.idstore.tests.integration;

import com.io7m.idstore.admin_client.IdAClients;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.admin_client.api.IdAClientType;
import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPasswordAlgorithmRedacted;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.protocol.admin.IdACommandAdminSelf;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSelf;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseLogin;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanDelete;
import com.io7m.idstore.protocol.admin.cb.IdACB1Messages;
import com.io7m.idstore.tests.server.IdWithServerContract;
import com.io7m.verdant.core.VProtocolSupported;
import com.io7m.verdant.core.VProtocols;
import com.io7m.verdant.core.cb.VProtocolMessages;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.providers.TypeUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.NOT_LOGGED_IN;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("integration")
@Tag("admin-client")
public final class IdAClientIT extends IdWithServerContract
{
  private ClientAndServer mockServer;
  private IdACB1Messages messages;
  private IdAdmin admin;
  private IdAClients clients;
  private IdAClientType client;
  private VProtocols versions;
  private VProtocolMessages versionMessages;
  private byte[] versionHeader;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.admin =
      new IdAdmin(
        UUID.randomUUID(),
        new IdName("someone"),
        new IdRealName("Someone"),
        IdNonEmptyList.single(new IdEmail("someone@example.com")),
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        IdPasswordAlgorithmRedacted.create().createHashed("x"),
        IdAdminPermissionSet.empty()
      );

    this.clients =
      new IdAClients();
    this.client =
      this.clients.create(Locale.ROOT);

    this.messages =
      new IdACB1Messages();

    final var v1 =
      new VProtocolSupported(
        IdACB1Messages.protocolId(),
        1L,
        0L,
        "/v1/"
      );

    this.versions =
      new VProtocols(List.of(v1));
    this.versionMessages =
      VProtocolMessages.create();
    this.versionHeader =
      this.versionMessages.serialize(this.versions, 1);

    this.mockServer =
      ClientAndServer.startClientAndServer(Integer.valueOf(60000));
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.mockServer.close();
    this.client.close();
  }

  /**
   * Command retries work when the server indicates a session has expired.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandRetry()
    throws Exception
  {
    this.mockServer.when(
      HttpRequest.request()
        .withPath("/")
    ).respond(
      HttpResponse.response()
        .withBody(this.versionHeader)
        .withHeader("Content-Type", "application/verdant+cedarbridge")
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/login")
    ).respond(
      HttpResponse.response()
        .withBody(this.messages.serialize(new IdAResponseLogin(
          UUID.randomUUID(),
          this.admin)))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/command"),
      Times.exactly(1)
    ).respond(
      HttpResponse.response()
        .withStatusCode(401)
        .withBody(this.messages.serialize(
          new IdAResponseError(
            UUID.randomUUID(),
            AUTHENTICATION_ERROR.id(),
            "error")))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/command"),
      Times.exactly(1)
    ).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withBody(this.messages.serialize(
          new IdAResponseAdminSelf(UUID.randomUUID(), this.admin)))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.client.login(
      "someone",
      "whatever",
      URI.create("http://localhost:60000/")
    );

    final var result = this.client.adminSelf();
    assertEquals(this.admin.id(), result.id());
  }

  /**
   * The client fails if the server returns a non-response.
   *
   * @throws Exception On errors
   */

  @Test
  public void testServerReturnsNonResponse()
    throws Exception
  {
    this.mockServer.when(
      HttpRequest.request()
        .withPath("/")
    ).respond(
      HttpResponse.response()
        .withBody(this.versionHeader)
        .withHeader("Content-Type", "application/verdant+cedarbridge")
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/login")
    ).respond(
      HttpResponse.response()
        .withBody(this.messages.serialize(new IdAResponseLogin(
          UUID.randomUUID(),
          this.admin)))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/command"),
      Times.exactly(1)
    ).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withBody(this.messages.serialize(new IdACommandAdminSelf()))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.client.login(
      "someone",
      "whatever",
      URI.create("http://localhost:60000/")
    );

    final var ex =
      assertThrows(IdAClientException.class, () -> this.client.adminSelf());

    assertEquals(PROTOCOL_ERROR, ex.errorCode());
  }

  /**
   * The client fails if the server returns the wrong response.
   *
   * @throws Exception On errors
   */

  @Test
  public void testServerReturnsWrongResponse()
    throws Exception
  {
    this.mockServer.when(
      HttpRequest.request()
        .withPath("/")
    ).respond(
      HttpResponse.response()
        .withBody(this.versionHeader)
        .withHeader("Content-Type", "application/verdant+cedarbridge")
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/login")
    ).respond(
      HttpResponse.response()
        .withBody(this.messages.serialize(new IdAResponseLogin(
          UUID.randomUUID(),
          this.admin)))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/command"),
      Times.exactly(1)
    ).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withBody(this.messages.serialize(
          new IdAResponseUserBanDelete(UUID.randomUUID())))
        .withHeader("Content-Type", IdACB1Messages.contentType())
    );

    this.client.login(
      "someone",
      "whatever",
      URI.create("http://localhost:60000/")
    );

    final var ex =
      assertThrows(IdAClientException.class, () -> this.client.adminSelf());

    assertEquals(PROTOCOL_ERROR, ex.errorCode());
  }

  /**
   * Logging in and seeing oneself works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginSelf()
    throws Exception
  {
    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    this.client.login("admin", "12345678", this.serverAdminAPIURL());

    final var self = this.client.adminSelf();
    assertEquals(admin, self.id());

    this.client.close();
    this.client.login("admin", "12345678", this.serverAdminAPIURL());
  }

  /**
   * Logging in with the wrong password fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginWrongPassword()
    throws Exception
  {
    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    final var ex =
      assertThrows(IdAClientException.class, () -> {
        this.client.login("admin", "1234", this.serverAdminAPIURL());
      });

    assertEquals(AUTHENTICATION_ERROR, ex.errorCode());
  }

  /**
   * Every method that requires a login throws an exception if the user is not
   * logged in.
   *
   * @return The tests
   */

  @TestFactory
  public Stream<DynamicTest> testDisconnected()
  {
    return disconnectionRelevantMethodsOf(IdAClientType.class)
      .map(this::disconnectedOf);
  }

  private DynamicTest disconnectedOf(
    final Method method)
  {
    return DynamicTest.dynamicTest(
      "testDisconnected_%s".formatted(method.getName()),
      () -> {
        final var parameterTypes =
          method.getGenericParameterTypes();
        final var parameters =
          new Object[parameterTypes.length];

        for (var index = 0; index < parameterTypes.length; ++index) {
          final var pType = parameterTypes[index];
          if (pType instanceof ParameterizedType param) {
            final List<TypeUsage> typeArgs =
              Arrays.stream(param.getActualTypeArguments())
                .map(TypeUsage::forType)
                .toList();

            final var typeArgsArray = new TypeUsage[typeArgs.size()];
            typeArgs.toArray(typeArgsArray);

            final var mainType =
              TypeUsage.of((Class<?>) param.getRawType(), typeArgsArray);

            parameters[index] = Arbitraries.defaultFor(mainType).sample();
          } else if (pType instanceof Class<?> clazz) {
            parameters[index] = Arbitraries.defaultFor(clazz).sample();
          }
        }

        try {
          method.invoke(this.client, parameters);
        } catch (final IllegalAccessException | IllegalArgumentException e) {
          throw new RuntimeException(e);
        } catch (final InvocationTargetException e) {
          if (e.getCause() instanceof IdAClientException ex) {
            if (Objects.equals(ex.errorCode(), NOT_LOGGED_IN)) {
              return;
            }
          }
          throw e;
        }
      });
  }

  private static Stream<Method> disconnectionRelevantMethodsOf(
    final Class<? extends IdAClientType> clazz)
  {
    return Stream.of(clazz.getMethods())
      .filter(IdAClientIT::isDisconnectionRelevantMethod);
  }

  private static boolean isDisconnectionRelevantMethod(
    final Method m)
  {
    return switch (m.getName()) {
      case "toString",
        "equals",
        "hashCode",
        "getClass",
        "close",
        "login",
        "notify",
        "wait",
        "notifyAll" -> false;
      default -> true;
    };
  }

  /**
   * A smoke test that simply calls every method with random arguments.
   *
   * @return The tests
   */

  @TestFactory
  public Stream<DynamicTest> testSmoke()
    throws Exception
  {
    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    return disconnectionRelevantMethodsOf(IdAClientType.class)
      .map(this::smokeOf);
  }

  private static final Set<IdErrorCode> ALLOWED_SMOKE_CODES =
    Set.of(
      ADMIN_NONEXISTENT,
      USER_NONEXISTENT,
      PROTOCOL_ERROR
    );

  private DynamicTest smokeOf(
    final Method method)
  {
    return DynamicTest.dynamicTest(
      "testSmoke_%s".formatted(method.getName()),
      () -> {
        final var parameterTypes =
          method.getGenericParameterTypes();
        final var parameters =
          new Object[parameterTypes.length];

        for (var index = 0; index < parameterTypes.length; ++index) {
          final var pType = parameterTypes[index];
          if (pType instanceof ParameterizedType param) {
            final List<TypeUsage> typeArgs =
              Arrays.stream(param.getActualTypeArguments())
                .map(TypeUsage::forType)
                .toList();

            final var typeArgsArray = new TypeUsage[typeArgs.size()];
            typeArgs.toArray(typeArgsArray);

            final var mainType =
              TypeUsage.of((Class<?>) param.getRawType(), typeArgsArray);

            parameters[index] = Arbitraries.defaultFor(mainType).sample();
          } else if (pType instanceof Class<?> clazz) {
            parameters[index] = Arbitraries.defaultFor(clazz).sample();
          }
        }

        this.client.login("admin", "12345678", this.serverAdminAPIURL());

        try {
          method.invoke(this.client, parameters);
        } catch (final IllegalAccessException | IllegalArgumentException e) {
          throw e;
        } catch (final InvocationTargetException e) {
          if (e.getCause() instanceof IdAClientException ex) {
            if (ALLOWED_SMOKE_CODES.contains(ex.errorCode())) {
              return;
            }
          }
          throw e;
        }
      });
  }
}
