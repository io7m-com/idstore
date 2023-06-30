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

package com.io7m.idstore.tests.integration;

import com.io7m.ervilla.api.EContainerSupervisorType;
import com.io7m.ervilla.test_extension.ErvillaCloseAfterAll;
import com.io7m.ervilla.test_extension.ErvillaConfiguration;
import com.io7m.ervilla.test_extension.ErvillaExtension;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import com.io7m.idstore.tests.extensions.IdTestDatabases;
import com.io7m.idstore.tests.extensions.IdTestServers;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static java.net.http.HttpClient.Redirect.ALWAYS;
import static java.net.http.HttpClient.Redirect.NEVER;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.net.http.HttpResponse.BodyHandlers.discarding;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@Tag("user-view")
@ExtendWith({ErvillaExtension.class, ZeladorExtension.class})
@ErvillaConfiguration(disabledIfUnsupported = true)
public final class IdServerUserViewIT
{
  private static IdTestDatabases.IdDatabaseFixture DATABASE_FIXTURE;
  private HttpClient httpClient;
  private CookieManager cookies;
  private HttpClient httpClientWithoutRedirects;
  private IdTestServers.IdTestServerFixture serverFixture;

  @BeforeAll
  public static void setupOnce(
    final @ErvillaCloseAfterAll EContainerSupervisorType containers)
    throws Exception
  {
    DATABASE_FIXTURE = IdTestDatabases.create(containers, 15432);
  }

  @BeforeEach
  public void setup(
    final CloseableResourcesType closeables)
    throws Exception
  {
    DATABASE_FIXTURE.reset();

    final var rateLimitConfiguration =
      new IdServerRateLimitConfiguration(
        Duration.ofSeconds(0L),
        Duration.ofSeconds(0L),
        Duration.ofSeconds(0L),
        Duration.ofSeconds(0L),
        Duration.ofSeconds(0L),
        Duration.ofSeconds(0L)
      );

    this.serverFixture =
      closeables.addPerTestResource(
        IdTestServers.createWithRateLimitConfiguration(
          DATABASE_FIXTURE,
          rateLimitConfiguration,
          10025,
          50000,
          50001,
          51000
        ));

    this.cookies =
      new CookieManager();

    this.httpClient =
      HttpClient.newBuilder()
        .followRedirects(ALWAYS)
        .cookieHandler(this.cookies)
        .build();

    this.httpClientWithoutRedirects =
      HttpClient.newBuilder()
        .followRedirects(NEVER)
        .cookieHandler(this.cookies)
        .build();
  }

  /**
   * Fetching CSS works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCSS()
    throws Exception
  {
    {
      final var req =
        HttpRequest.newBuilder(this.viewURL("/css/reset.css"))
          .build();
      final var res =
        this.httpClient.send(req, HttpResponse.BodyHandlers.ofString());

      assertTrue(res.body().startsWith(
        "/*! normalize.css v8.0.1 | MIT License | github.com/necolas/normalize.css */"));
    }

    {
      final var req =
        HttpRequest.newBuilder(this.viewURL("/css/style.css"))
          .build();
      final var res =
        this.httpClient.send(req, HttpResponse.BodyHandlers.ofString());

      assertTrue(res.body().contains("font-family:"));
    }
  }

  /**
   * Fetching the logo works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLogo()
    throws Exception
  {
    {
      final var req =
        HttpRequest.newBuilder(this.viewURL("/logo"))
          .build();
      final var res =
        this.httpClient.send(req, HttpResponse.BodyHandlers.ofString());

      assertTrue(res.body().contains("<svg width=\"64\""));
    }
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginSelf()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");

    this.serverFixture.createUser(admin, "someone");

    this.login();
    this.logout();
  }

  /**
   * Logging in requires valid parameters.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginNoUsername()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/login"))
        .POST(ofString("password=12345678"))
        .header(
          "Content-Type",
          "application/x-www-form-urlencoded"),
      200,
      "idstore: Login");
  }

  /**
   * Logging in requires valid parameters.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginNoPassword()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/login"))
        .POST(ofString("username=someone"))
        .header(
          "Content-Type",
          "application/x-www-form-urlencoded"),
      200,
      "idstore: Login");
  }

  /**
   * Logging in requires valid parameters.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginNonexistent()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/login"))
        .POST(ofString("username=nonexistent&password=12345678"))
        .header(
          "Content-Type",
          "application/x-www-form-urlencoded"),
      401,
      "idstore: Login");
  }

  /**
   * Logging in requires valid parameters.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginWrongPassword()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/login"))
        .POST(ofString("username=someone&password=1"))
        .header(
          "Content-Type",
          "application/x-www-form-urlencoded"),
      401,
      "idstore: Login");
  }

  /**
   * Adding an email works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAddEmailPermit()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");
    this.openPage(
      "/email-add-run?email=extras@example.com",
      "idstore: Verification");

    this.completeEmailChallenge(
      "extras@example.com",
      "X-IDStore-Verification-Token-Permit",
      "/email-verification-permit/?token=%s"
    );
  }

  /**
   * Rejecting an email works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAddEmailDeny()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");
    this.openPage(
      "/email-add-run?email=extras@example.com",
      "idstore: Verification");

    this.completeEmailChallenge(
      "extras@example.com",
      "X-IDStore-Verification-Token-Deny",
      "/email-verification-deny/?token=%s"
    );
  }

  /**
   * Email verification requires a token.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAddEmailDenyNonexistentToken()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");

    this.expectError(
      HttpRequest.newBuilder(
        this.viewURL(
          "/email-verification-deny/?token=C0DE290A52CE988DAD77E16F60671830")),
      400,
      "idstore: Error");
  }

  /**
   * Email verification requires a token.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAddEmailPermitNonexistentToken()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");

    this.expectError(
      HttpRequest.newBuilder(
        this.viewURL(
          "/email-verification-permit/?token=C0DE290A52CE988DAD77E16F60671830")),
      400,
      "idstore: Error");
  }

  /**
   * Email verification requires a token.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAddEmailPermitInvalidToken()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL(
        "/email-verification-permit/?token=what")),
      400,
      "idstore: Error");
  }

  /**
   * Email verification requires a token.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAddEmailDenyInvalidToken()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/email-verification-deny/?token=what")),
      400,
      "idstore: Error");
  }


  /**
   * Email verification requires a token.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAddEmailPermitMissingToken()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/email-verification-permit/")),
      400,
      "idstore: Error");
  }

  /**
   * Email verification requires a token.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAddEmailDenyMissingToken()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");

    this.expectError(HttpRequest.newBuilder(
      this.viewURL("/email-verification-deny/")), 400, "idstore: Error");
  }

  /**
   * Starting email verification requires an email.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAddEmailAddRunNoAddress()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();

    this.expectError(HttpRequest.newBuilder(
      this.viewURL("/email-add-run")), 400, "idstore: Error");
  }

  /**
   * Starting email verification requires an email.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAddEmailAddRunBadAddress()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();

    this.expectError(HttpRequest.newBuilder(
      this.viewURL("/email-add-run/?email=*@*")), 400, "idstore: Error");
  }

  /**
   * Removing an email works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRemoveEmailPermit()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");
    this.openPage(
      "/email-add-run?email=extras@example.com",
      "idstore: Verification");
    this.completeEmailChallenge(
      "extras@example.com",
      "X-IDStore-Verification-Token-Permit",
      "/email-verification-permit/?token=%s");
    this.openPage(
      "/email-remove-run?email=extras@example.com",
      "idstore: Verification");
    this.completeEmailChallenge(
      "extras@example.com",
      "X-IDStore-Verification-Token-Permit",
      "/email-verification-permit/?token=%s");
  }

  /**
   * Removing an email works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRemoveEmailDeny()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");
    this.openPage(
      "/email-add-run?email=extras@example.com",
      "idstore: Verification");
    this.completeEmailChallenge(
      "extras@example.com",
      "X-IDStore-Verification-Token-Permit",
      "/email-verification-permit/?token=%s");

    this.openPage(
      "/email-remove-run?email=extras@example.com",
      "idstore: Verification");
    this.completeEmailChallenge(
      "extras@example.com",
      "X-IDStore-Verification-Token-Deny",
      "/email-verification-deny/?token=%s");
  }

  /**
   * Starting email verification requires an email.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRemoveEmailAddRunNoAddress()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/email-remove-run")),
      400,
      "idstore: Error");
  }

  /**
   * Starting email verification requires an email.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRemoveEmailAddRunBadAddress()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.login();

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/email-remove-run/?email=*@*")),
      400,
      "idstore: Error");
  }

  /**
   * Authentication is required.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUnauthAddEmailPermit()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/email-add")),
      401,
      "idstore: Login");
  }

  /**
   * Authentication is required.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUnauthRemoveEmailPermit()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/email-remove")),
      401,
      "idstore: Login");
  }

  /**
   * Authentication is required.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUnauthAddEmailRunPermit()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/email-add-run")),
      401,
      "idstore: Login");
  }

  /**
   * Authentication is required.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUnauthRemoveEmailRunPermit()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/email-remove-run")),
      401,
      "idstore: Login");
  }

  /**
   * Authentication is required.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUnauthUpdateRealnameRun()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/realname-update-run")),
      401,
      "idstore: Login");
  }

  /**
   * Authentication is required.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUnauthUpdateRealname()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/realname-update")),
      401,
      "idstore: Login");
  }

  /**
   * Updating a realname works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdateRealname()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    final var userId =
      this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/realname-update", "idstore: Update your real name.");
    this.openPage(
      "/realname-update-run?realname=Someone%20Else",
      "idstore: User Profile");

    final var user = this.serverFixture.getUser(userId);
    assertEquals("Someone Else", user.realName().value());
  }

  /**
   * Updating a realname fails for invalid names.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdateRealnameInvalid()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    final var userId =
      this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/realname-update", "idstore: Update your real name.");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/realname-update-run?realname=")),
      400,
      "idstore: Error");
  }

  /**
   * Updating a realname fails for missing names.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdateRealnameMissing()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    final var userId =
      this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/realname-update", "idstore: Update your real name.");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/realname-update-run")),
      400,
      "idstore: Error");
  }

  /**
   * Authentication is required.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUnauthUpdatePasswordRun()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/password-update-run")),
      401,
      "idstore: Login");
  }

  /**
   * Authentication is required.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUnauthUpdatePassword()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    this.serverFixture.createUser(admin, "someone");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/password-update")),
      401,
      "idstore: Login");
  }

  /**
   * Updating a password works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdatePassword()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    final var userId =
      this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/password-update", "idstore: Update your password.");
    this.openPage(
      "/password-update-run?password0=abc&password1=abc",
      "idstore: Password Updated");
  }

  /**
   * Updating a password fails if the confirmation does not match.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdatePasswordInvalid()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    final var userId =
      this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/password-update", "idstore: Update your password.");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL(
        "/password-update-run?password0=abc&password1=xyz")),
      400,
      "idstore: Error");
  }

  /**
   * Updating a password fails if the confirmation does not match.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdatePasswordInvalidMissing0()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    final var userId =
      this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/password-update", "idstore: Update your password.");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL(
        "/password-update-run?password0=abc")),
      400,
      "idstore: Error");
  }

  /**
   * Updating a password fails if the confirmation does not match.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdatePasswordInvalidMissing1()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    final var userId =
      this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/password-update", "idstore: Update your password.");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL(
        "/password-update-run?password1=xyz")),
      400,
      "idstore: Error");
  }

  /**
   * Resetting a password works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetPasswordWorks()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    final var userId =
      this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/password-reset", "idstore: Reset password.");

    this.openPage(
      "/password-reset-run?username=someone&email=someone@example.com",
      "idstore: Password Reset"
    );

    final var mail =
      this.serverFixture.mailReceived().poll();
    final var token =
      mail.getHeader("X-IDStore-PasswordReset-Token")[0];

    this.openPage(
      "/password-reset-confirm?token=%s".formatted(token),
      "idstore: Reset password."
    );

    this.openPage(
      "/password-reset-confirm-run?password0=abc&password1=abc&token=%s"
        .formatted(token),
      "idstore: Password Reset"
    );

    this.loginWith("someone", "abc");
  }

  /**
   * Resetting a password fails with a bad token.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetPasswordBadToken0()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    final var userId =
      this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/password-reset", "idstore: Reset password.");

    this.openPage(
      "/password-reset-run?username=someone&email=someone@example.com",
      "idstore: Password Reset"
    );

    final var mail =
      this.serverFixture.mailReceived().poll();
    final var token =
      mail.getHeader("X-IDStore-PasswordReset-Token")[0];

    this.openPage(
      "/password-reset-confirm?token=%s".formatted(token),
      "idstore: Reset password."
    );

    this.expectError(
      HttpRequest.newBuilder(this.viewURL(
        "/password-reset-confirm-run?password0=abc&password1=abc&token=%s"
          .formatted(new StringBuilder(token).reverse()))),
      400,
      "idstore: Error"
    );
  }

  /**
   * Resetting a password fails with a bad token.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetPasswordBadToken1()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    final var userId =
      this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/password-reset", "idstore: Reset password.");

    this.openPage(
      "/password-reset-run?username=someone&email=someone@example.com",
      "idstore: Password Reset"
    );

    this.expectError(
      HttpRequest.newBuilder(this.viewURL(
        "/password-reset-confirm-run?password0=abc&password1=abc&token=%s"
          .formatted("not%20token"))),
      400,
      "idstore: Error"
    );
  }

  /**
   * Resetting a password fails with a bad username.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetPasswordBadUsername0()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    final var userId =
      this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/password-reset", "idstore: Reset password.");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL(
        "/password-reset-run?username=not%20user")),
      400,
      "idstore: Error"
    );
  }

  /**
   * Resetting a password fails with a bad email address.
   *
   * @throws Exception On errors
   */

  @Test
  public void testResetPasswordBadEmail0()
    throws Exception
  {
    final var admin =
      this.serverFixture.createAdminInitial("admin", "12345678");
    final var userId =
      this.serverFixture.createUser(admin, "someone");

    this.login();
    this.openPage("/password-reset", "idstore: Reset password.");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL(
        "/password-reset-run?email=email@example")),
      400,
      "idstore: Error"
    );
  }

  private void expectError(
    final HttpRequest.Builder newBuilder,
    final int expected,
    final String Login)
    throws IOException, InterruptedException
  {
    final var req =
      newBuilder.build();
    final var res =
      this.httpClient.send(req, new IdXHTMLBodyHandler());

    final var titles =
      IdDocuments.elementsWithName(res.body(), "title");
    assertEquals(Login, titles.get(0).getTextContent());
    assertEquals(expected, res.statusCode());
  }

  private void completeEmailChallenge(
    final String emailTo,
    final String header,
    final String baseURL)
    throws Exception
  {
    final var received =
      List.copyOf(this.serverFixture.mailReceived());

    this.serverFixture.mailReceived().clear();

    IdToken token = null;
    for (final var email : received) {
      if (Objects.equals(email.getAllRecipients()[0].toString(), emailTo)) {
        token = new IdToken(
          email.getHeader(header)[0]
        );
        break;
      }
    }

    if (token == null) {
      throw new IllegalStateException("No email available with a token");
    }

    final var req =
      HttpRequest.newBuilder(
          this.viewURL(baseURL.formatted(token)))
        .build();
    final var res =
      this.httpClient.send(req, new IdXHTMLBodyHandler());
    assertEquals(200, res.statusCode());

    final var titles =
      IdDocuments.elementsWithName(res.body(), "title");
    assertEquals("idstore: Verified", titles.get(0).getTextContent());
  }

  private void openPage(
    final String endpoint,
    final String expectedTitle)
    throws IOException, InterruptedException
  {
    final var req =
      HttpRequest.newBuilder(this.viewURL(endpoint))
        .build();
    final var res =
      this.httpClient.send(req, new IdXHTMLBodyHandler());
    final var titles =
      IdDocuments.elementsWithName(res.body(), "title");
    assertEquals(expectedTitle, titles.get(0).getTextContent());
  }

  private URI viewURL(final String str)
  {
    return this.serverFixture.server().userView().resolve(str);
  }

  private void logout()
    throws IOException, InterruptedException
  {
    final var req =
      HttpRequest.newBuilder(this.viewURL("/logout"))
        .build();
    final var res =
      this.httpClientWithoutRedirects.send(req, discarding());
    assertEquals(302, res.statusCode());
  }

  private void login()
    throws IOException, InterruptedException
  {
    this.loginWith("someone", "12345678");
  }

  private void loginWith(
    final String username,
    final String password)
    throws IOException, InterruptedException
  {
    {
      final var req =
        HttpRequest.newBuilder(this.viewURL("/login"))
          .POST(ofString("username=%s&password=%s".formatted(
            username,
            password)))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .build();
      final var res =
        this.httpClient.send(req, new IdXHTMLBodyHandler());
      assertEquals(200, res.statusCode());
    }

    this.openPage("/", "idstore: User Profile");
  }
}
