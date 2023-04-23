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

import com.io7m.idstore.error_codes.IdErrorCode;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPasswordAlgorithmRedacted;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.IdUCommandUserSelf;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemoveDeny;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.protocol.user.IdUResponseUserSelf;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.tests.server.IdWithServerContract;
import com.io7m.idstore.user_client.IdUClients;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.idstore.user_client.api.IdUClientType;
import com.io7m.quixote.core.QWebServerType;
import com.io7m.quixote.core.QWebServers;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_VERIFICATION_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.NOT_LOGGED_IN;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_RESET_MISMATCH;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("integration")
@Tag("user-client")
public final class IdUClientIT extends IdWithServerContract
{
  private IdUCB1Messages messages;
  private IdUser user;
  private IdUClients clients;
  private IdUClientType client;
  private VProtocols versions;
  private VProtocolMessages versionMessages;
  private byte[] versionHeader;
  private QWebServerType webServer;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.user =
      new IdUser(
        UUID.randomUUID(),
        new IdName("someone"),
        new IdRealName("Someone"),
        IdNonEmptyList.single(new IdEmail("someone@example.com")),
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        IdPasswordAlgorithmRedacted.create().createHashed("x")
      );

    this.clients =
      new IdUClients();
    this.client =
      this.clients.create(Locale.ROOT);

    this.messages =
      new IdUCB1Messages();

    final var v1 =
      new VProtocolSupported(
        IdUCB1Messages.protocolId(),
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
    this.webServer =
      QWebServers.createServer(60001);
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.webServer.close();
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
    this.webServer.addResponse()
      .forPath("/")
      .withStatus(200)
      .withContentType("application/verdant+cedarbridge")
      .withFixedData(this.versionHeader);

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdUResponseLogin(UUID.randomUUID(), this.user))
      );

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(401)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdUResponseError(
            UUID.randomUUID(),
            "error",
            AUTHENTICATION_ERROR.id(),
            Map.of(),
            Optional.empty()))
      );

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdUResponseLogin(UUID.randomUUID(), this.user))
      );

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdUResponseUserSelf(UUID.randomUUID(), this.user))
      );

    this.client.login(
      "someone",
      "whatever",
      URI.create("http://localhost:60001/"),
      Map.of()
    );

    final var result = this.client.userSelf();
    assertEquals(this.user.id(), result.id());
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
    this.webServer.addResponse()
      .forPath("/")
      .withStatus(200)
      .withContentType("application/verdant+cedarbridge")
      .withFixedData(this.versionHeader);

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdUResponseLogin(UUID.randomUUID(), this.user))
      );

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(this.messages.serialize(new IdUCommandUserSelf()));

    this.client.login(
      "someone",
      "whatever",
      URI.create("http://localhost:60001/"),
      Map.of()
    );

    final var ex =
      assertThrows(IdUClientException.class, () -> this.client.userSelf());

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
    this.webServer.addResponse()
      .forPath("/")
      .withStatus(200)
      .withContentType("application/verdant+cedarbridge")
      .withFixedData(this.versionHeader);

    this.webServer.addResponse()
      .forPath("/v1/login")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(
        this.messages.serialize(
          new IdUResponseLogin(UUID.randomUUID(), this.user))
      );

    this.webServer.addResponse()
      .forPath("/v1/command")
      .withStatus(200)
      .withContentType(IdUCB1Messages.contentType())
      .withFixedData(this.messages.serialize(
        new IdUResponseEmailRemoveDeny(UUID.randomUUID()))
      );

    this.client.login(
      "someone",
      "whatever",
      URI.create("http://localhost:60001/"),
      Map.of()
    );

    final var ex =
      assertThrows(IdUClientException.class, () -> this.client.userSelf());

    assertEquals(PROTOCOL_ERROR, ex.errorCode());
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
    return disconnectionRelevantMethodsOf(IdUClientType.class)
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
          if (e.getCause() instanceof IdUClientException ex) {
            if (Objects.equals(ex.errorCode(), NOT_LOGGED_IN)) {
              return;
            }
          }
          throw e;
        }
      });
  }

  private static Stream<Method> disconnectionRelevantMethodsOf(
    final Class<? extends IdUClientType> clazz)
  {
    return Stream.of(clazz.getMethods())
      .filter(IdUClientIT::isDisconnectionRelevantMethod);
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

    this.serverCreateUser(admin, "someone");

    return disconnectionRelevantMethodsOf(IdUClientType.class)
      .map(this::smokeOf);
  }

  private static final Set<IdErrorCode> ALLOWED_SMOKE_CODES =
    Set.of(
      ADMIN_NONEXISTENT,
      EMAIL_NONEXISTENT,
      EMAIL_VERIFICATION_NONEXISTENT,
      PASSWORD_RESET_MISMATCH,
      PROTOCOL_ERROR,
      USER_NONEXISTENT
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

        this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

        try {
          method.invoke(this.client, parameters);
        } catch (final IllegalAccessException | IllegalArgumentException e) {
          throw e;
        } catch (final InvocationTargetException e) {
          if (e.getCause() instanceof IdUClientException ex) {
            if (ALLOWED_SMOKE_CODES.contains(ex.errorCode())) {
              return;
            }
          }
          throw e;
        }
      });
  }
}
