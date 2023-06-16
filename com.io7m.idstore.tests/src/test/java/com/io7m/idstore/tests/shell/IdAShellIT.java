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

package com.io7m.idstore.tests.shell;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.server.api.IdServerException;
import com.io7m.idstore.server.api.IdServerType;
import com.io7m.idstore.shell.admin.IdAShellConfiguration;
import com.io7m.idstore.shell.admin.IdAShellType;
import com.io7m.idstore.shell.admin.IdAShells;
import com.io7m.idstore.tests.extensions.IdTestExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(IdTestExtension.class)
@Timeout(value = 10L)
public final class IdAShellIT
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAShellIT.class);

  private IdAShells shells;
  private IdAShellConfiguration configuration;
  private IdAFakeTerminal terminal;
  private ExecutorService executor;
  private volatile int exitCode;
  private CountDownLatch latch;

  private static void configureAdmin(
    final IdServerType server)
    throws IdServerException
  {
    server.close();
    server.setup(
      Optional.empty(),
      new IdName("admin"),
      new IdEmail("someone@example.com"),
      new IdRealName("AM"),
      "1234"
    );
    server.start();
  }

  private void waitForShell()
    throws InterruptedException
  {
    this.latch.await(3L, TimeUnit.SECONDS);
  }

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.terminal =
      new IdAFakeTerminal();
    this.shells =
      new IdAShells();
    this.configuration =
      new IdAShellConfiguration(Locale.ROOT, Optional.of(this.terminal));
    this.executor =
      Executors.newFixedThreadPool(1);

    this.latch = new CountDownLatch(1);
    this.exitCode = 0;
  }

  @AfterEach
  public void tearDown()
    throws InterruptedException
  {
    this.executor.shutdown();
    this.executor.awaitTermination(3L, TimeUnit.SECONDS);

    final var out =
      this.terminal.terminalProducedOutput();

    System.out.println(out.toString(StandardCharsets.UTF_8));
  }

  @Test
  public void testShellUnrecognized(
    final IdServerType server)
    throws Exception
  {
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.println("nonexistent");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(1, this.exitCode);
  }

  @Test
  public void testShellHelp(
    final IdServerType server)
    throws Exception
  {
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.println("help");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);
  }

  @Test
  public void testShellVersion(
    final IdServerType server)
    throws Exception
  {
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.println("version");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);
  }

  @Test
  public void testShellLogin(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);
  }

  @Test
  public void testShellAuditGet(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println(
      "user-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone "
    );
    w.println("audit-search-begin");
    w.println("audit-search-next");
    w.println("audit-search-previous");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("ADMIN_CREATED"));
    assertTrue(output.toString().contains("ADMIN_LOGGED_IN"));
    assertTrue(output.toString().contains("USER_CREATED"));
    assertTrue(output.toString().contains("Page 1 of 1, offset 0"));
  }

  @Test
  public void testShellUserCreateGet(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "user-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone "
    );
    w.println("user-get --user 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("user-get-by-email --email fresh0@example.com");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("Email: fresh0@example.com"));
  }

  @Test
  public void testShellUserGetNonexistent(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("user-get --user 3a193a61-9427-4c24-8bd4-667d19914970");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(1, this.exitCode);
  }

  @Test
  public void testShellUserGetByEmailNonexistent(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("user-get-by-email --email nonexistent@example.com");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(1, this.exitCode);
  }

  @Test
  public void testShellUserEmailsGet(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "user-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone "
    );
    w.println(
      "user-email-add --user 3a193a61-9427-4c24-8bd4-667d19914970 --email fresh1@example.com");
    w.println(
      "user-email-add --user 3a193a61-9427-4c24-8bd4-667d19914970 --email fresh2@example.com");
    w.println(
      "user-email-add --user 3a193a61-9427-4c24-8bd4-667d19914970 --email fresh3@example.com");
    w.println(
      "user-email-remove --user 3a193a61-9427-4c24-8bd4-667d19914970 --email fresh3@example.com");
    w.println("user-get --user 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("Email: fresh1@example.com"));
    assertTrue(output.toString().contains("Email: fresh2@example.com"));
    assertFalse(output.toString().contains("Email: fresh3@example.com"));
  }

  @Test
  public void testShellUserSearch(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "user-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone "
    );
    w.println("user-search-begin");
    w.println("user-search-next");
    w.println("user-search-previous");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("# Page 1 of 1, offset 0"));
    assertTrue(output.toString().contains(
      "3a193a61-9427-4c24-8bd4-667d19914970     someone"));
  }

  @Test
  public void testShellUserSearchByEmail(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "user-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone "
    );
    w.println("user-search-by-email-begin");
    w.println("user-search-by-email-next");
    w.println("user-search-by-email-previous");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("# Page 1 of 1, offset 0"));
    assertTrue(output.toString().contains(
      "3a193a61-9427-4c24-8bd4-667d19914970     someone"));
  }

  @Test
  public void testShellUserLoginHistory(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "user-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone "
    );
    w.println("user-login-history --user 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("# Time"));
  }

  @Test
  public void testShellUserBanCreateGetDelete(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "user-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone "
    );
    w.println("user-ban-create " +
              "--user 3a193a61-9427-4c24-8bd4-667d19914970 " +
              "--expires-on 2100-01-01T00:00:00+00:00 " +
              "--reason REASON!");
    w.println("user-ban-get --user 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("user-ban-delete --user 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("user-ban-get --user 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("user-ban-create " +
              "--user 3a193a61-9427-4c24-8bd4-667d19914970 " +
              "--reason REASON!");
    w.println("user-ban-get --user 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("User is banned: REASON!"));
    assertTrue(output.toString().contains("The ban expires on 2100-01-01T00:00Z"));
    assertTrue(output.toString().contains("The user is not banned."));
    assertTrue(output.toString().contains("The ban does not expire."));
  }


  @Test
  public void testShellAdminCreateGet(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "admin-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone " +
      "--permission ADMIN_READ " +
      "--permission AUDIT_READ "
    );
    w.println("admin-get --admin 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("admin-get-by-email --email fresh0@example.com");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("Email: fresh0@example.com"));
  }

  @Test
  public void testShellAdminGetNonexistent(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("admin-get --admin 3a193a61-9427-4c24-8bd4-667d19914970");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(1, this.exitCode);
  }

  @Test
  public void testShellAdminGetByEmailNonexistent(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("admin-get-by-email --email nonexistent@example.com");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(1, this.exitCode);
  }

  @Test
  public void testShellAdminEmailsGet(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "admin-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone " +
      "--permission ADMIN_READ " +
      "--permission AUDIT_READ "
    );
    w.println(
      "admin-email-add --admin 3a193a61-9427-4c24-8bd4-667d19914970 --email fresh1@example.com");
    w.println(
      "admin-email-add --admin 3a193a61-9427-4c24-8bd4-667d19914970 --email fresh2@example.com");
    w.println(
      "admin-email-add --admin 3a193a61-9427-4c24-8bd4-667d19914970 --email fresh3@example.com");
    w.println(
      "admin-email-remove --admin 3a193a61-9427-4c24-8bd4-667d19914970 --email fresh3@example.com");
    w.println("admin-get --admin 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("Email: fresh1@example.com"));
    assertTrue(output.toString().contains("Email: fresh2@example.com"));
    assertFalse(output.toString().contains("Email: fresh3@example.com"));
  }

  @Test
  public void testShellAdminSearch(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "admin-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone " +
      "--permission ADMIN_READ " +
      "--permission AUDIT_READ "
    );
    w.println("admin-search-begin");
    w.println("admin-search-next");
    w.println("admin-search-previous");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("# Page 1 of 1, offset 0"));
    assertTrue(output.toString().contains(
      "3a193a61-9427-4c24-8bd4-667d19914970     someone"));
  }

  @Test
  public void testShellAdminSearchByEmail(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "admin-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone " +
      "--permission ADMIN_READ " +
      "--permission AUDIT_READ "
    );
    w.println("admin-search-by-email-begin");
    w.println("admin-search-by-email-next");
    w.println("admin-search-by-email-previous");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("# Page 1 of 1, offset 0"));
    assertTrue(output.toString().contains(
      "3a193a61-9427-4c24-8bd4-667d19914970     someone"));
  }

  @Test
  public void testShellAdminBanCreateGetDelete(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "admin-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone "
    );
    w.println("admin-ban-create " +
              "--admin 3a193a61-9427-4c24-8bd4-667d19914970 " +
              "--expires-on 2100-01-01T00:00:00+00:00 " +
              "--reason REASON!");
    w.println("admin-ban-get --admin 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("admin-ban-delete --admin 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("admin-ban-get --admin 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("admin-ban-create " +
              "--admin 3a193a61-9427-4c24-8bd4-667d19914970 " +
              "--reason REASON!");
    w.println("admin-ban-get --admin 3a193a61-9427-4c24-8bd4-667d19914970");
    w.println("logout");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("Admin is banned: REASON!"));
    assertTrue(output.toString().contains("The ban expires on 2100-01-01T00:00Z"));
    assertTrue(output.toString().contains("The admin is not banned."));
    assertTrue(output.toString().contains("The ban does not expire."));
  }

  @Test
  public void testShellSetFailure(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.println("set --terminate-on-errors true");
    w.println("mysterious");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(1, this.exitCode);
  }

  @Test
  public void testShellAdminSetExpiration0(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "admin-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone " +
      "--permission ADMIN_READ " +
      "--permission AUDIT_READ "
    );
    w.println(
      "admin-update-password-expiration " +
      "--admin 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--expires never");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("The password will not expire."));
  }

  @Test
  public void testShellAdminSetExpiration1(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "admin-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone " +
      "--permission ADMIN_READ " +
      "--permission AUDIT_READ "
    );
    w.println(
      "admin-update-password-expiration " +
      "--admin 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--expires default");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("The password will not expire."));
  }

  @Test
  public void testShellAdminSetExpiration2(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "admin-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone " +
      "--permission ADMIN_READ " +
      "--permission AUDIT_READ "
    );
    w.println(
      "admin-update-password-expiration " +
      "--admin 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--expires 2100-01-01T00:00:00+00:00");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains(
      "The password will expire at 2100-01-01"));
  }

  @Test
  public void testShellUserSetExpiration0(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "user-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone "
    );
    w.println(
      "user-update-password-expiration " +
      "--user 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--expires never");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("The password will not expire."));
  }

  @Test
  public void testShellUserSetExpiration1(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "user-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone "
    );
    w.println(
      "user-update-password-expiration " +
      "--user 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--expires default");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains("The password will not expire."));
  }

  @Test
  public void testShellUserSetExpiration2(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "user-create --id 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--email fresh0@example.com " +
      "--password 12345678 " +
      "--real-name Real " +
      "--name someone "
    );
    w.println(
      "user-update-password-expiration " +
      "--user 3a193a61-9427-4c24-8bd4-667d19914970 " +
      "--expires 2100-01-01T00:00:00+00:00");
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains(
      "The password will expire at 2100-01-01"));
  }


  @Test
  public void testShellMailTest(
    final IdServerType server)
    throws Exception
  {
    configureAdmin(server);
    this.startShell();

    final var w = this.terminal.sendInputToTerminalWriter();
    w.printf("login %s admin 1234%n", server.adminAPI());
    w.println("self");
    w.println(
      "mail-test --email someone@example.com --token 123456"
    );
    w.flush();
    w.close();

    this.waitForShell();
    assertEquals(0, this.exitCode);

    final var output = this.terminal.terminalProducedOutput();
    assertTrue(output.toString().contains(
      "Mail sent successfully.\n" +
      "Token: 123456"));
  }


  private void startShell()
  {
    this.executor.execute(() -> {
      LOG.debug("starting shell");
      IdAShellType shellLeaked = null;
      try (var shell = this.shells.create(this.configuration)) {
        shellLeaked = shell;
        shell.run();
      } catch (final Throwable e) {
        LOG.debug("shell failed: ", e);
        throw new RuntimeException(e);
      } finally {
        LOG.debug("finished shell");
        this.exitCode = shellLeaked.exitCode();
        this.latch.countDown();
      }
    });
  }
}
