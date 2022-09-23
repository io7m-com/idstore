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

package com.io7m.idstore.tests;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.net.http.HttpClient.Redirect.ALWAYS;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdServerUserViewTest extends IdWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdServerUserViewTest.class);

  private HttpClient httpClient;
  private CookieManager cookies;

  @BeforeEach
  public void setup()
  {
    this.cookies =
      new CookieManager();

    this.httpClient =
      HttpClient.newBuilder()
        .followRedirects(ALWAYS)
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
    this.serverStartIfNecessary();

    {
      final var req =
        HttpRequest.newBuilder(this.viewURL("/css/reset.css"))
          .build();
      final var res =
        this.httpClient.send(req, HttpResponse.BodyHandlers.ofString());

      assertTrue(res.body().startsWith("/*! normalize.css v8.0.1 | MIT License | github.com/necolas/normalize.css */"));
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
    this.serverStartIfNecessary();

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.login();
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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");
    this.openPage("/email-add-run?email=extras@example.com", "idstore: Verification");

    this.permitEmailChallenge("/email-verification-permit/?token=%s");
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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");
    this.openPage("/email-add-run?email=extras@example.com", "idstore: Verification");

    this.permitEmailChallenge("/email-verification-deny/?token=%s");
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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/email-verification-permit/?token=what")),
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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");
    this.openPage("/email-add-run?email=extras@example.com", "idstore: Verification");
    this.permitEmailChallenge("/email-verification-permit/?token=%s");
    this.openPage("/email-remove-run?email=extras@example.com", "idstore: Verification");
    this.permitEmailChallenge("/email-verification-permit/?token=%s");
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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.login();
    this.openPage("/email-add", "idstore: Add an email address.");
    this.openPage("/email-add-run?email=extras@example.com", "idstore: Verification");
    this.permitEmailChallenge("/email-verification-permit/?token=%s");
    this.openPage("/email-remove-run?email=extras@example.com", "idstore: Verification");
    this.permitEmailChallenge("/email-verification-deny/?token=%s");
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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    final var userId =
      this.serverCreateUser(admin, "someone");

    this.login();
    this.openPage("/realname-update", "idstore: Update your real name.");
    this.openPage("/realname-update-run?realname=Someone%20Else", "idstore: User Profile");

    final var user = this.userGet(userId);
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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    final var userId =
      this.serverCreateUser(admin, "someone");

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
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    final var userId =
      this.serverCreateUser(admin, "someone");

    this.login();
    this.openPage("/realname-update", "idstore: Update your real name.");

    this.expectError(
      HttpRequest.newBuilder(this.viewURL("/realname-update-run")),
      400,
      "idstore: Error");
  }

  private void expectError(
    final HttpRequest.Builder newBuilder,
    final int expected,
    final String Login)
    throws IOException, InterruptedException
  {
    final var req =
      newBuilder
        .build();
    final var res =
      this.httpClient.send(req, new IdXHTMLBodyHandler());
    assertEquals(expected, res.statusCode());

    final var titles =
      IdDocuments.elementsWithName(res.body(), "title");
    assertEquals(Login, titles.get(0).getTextContent());
  }

  private void permitEmailChallenge(final String x)
    throws IOException, InterruptedException, MessagingException
  {
    final var email = this.emailsReceived().poll();
    final var token =
      email.getHeader("X-IDStore-Verification-Token")[0];

    final var req =
      HttpRequest.newBuilder(
          this.viewURL(x.formatted(token)))
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
    return this.serverUserViewURL().resolve(str);
  }

  private void login()
    throws IOException, InterruptedException
  {
    {
      final var req =
        HttpRequest.newBuilder(this.viewURL("/login"))
          .POST(ofString("username=someone&password=12345678"))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .build();
      final var res =
        this.httpClient.send(req, new IdXHTMLBodyHandler());
      assertEquals(200, res.statusCode());
    }

    this.openPage("/", "idstore: User Profile");
  }
}
