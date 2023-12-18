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


package com.io7m.idstore.tests.database;

import com.io7m.ervilla.api.EContainerSupervisorType;
import com.io7m.ervilla.test_extension.ErvillaCloseAfterSuite;
import com.io7m.ervilla.test_extension.ErvillaConfiguration;
import com.io7m.ervilla.test_extension.ErvillaExtension;
import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseConnectionType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.model.IdAdminColumnOrdering;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.tests.containers.IdTestContainerInstances;
import com.io7m.idstore.tests.extensions.IdTestDatabases;
import com.io7m.idstore.tests.extensions.IdTestDatabases.ExpectedEvent;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_ID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_ID_NAME;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NOT_INITIAL;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_UNSET;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_DUPLICATE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_ONE_REQUIRED;
import static com.io7m.idstore.model.IdAdminColumn.BY_IDNAME;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHost;
import static com.io7m.idstore.model.IdLoginMetadataStandard.userAgent;
import static com.io7m.idstore.tests.extensions.IdTestDatabases.ExpectedEvent.eventOf;
import static java.time.OffsetDateTime.now;
import static java.util.Map.entry;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({ErvillaExtension.class, ZeladorExtension.class})
@ErvillaConfiguration(disabledIfUnsupported = true, projectName = "com.io7m.idstore")
public final class IdDatabaseAdminsTest
{
  private static IdTestDatabases.IdDatabaseFixture DATABASE_FIXTURE;
  private IdDatabaseConnectionType connection;
  private IdDatabaseTransactionType transaction;
  private IdDatabaseType database;

  @BeforeAll
  public static void setupOnce(
    final @ErvillaCloseAfterSuite EContainerSupervisorType containers)
    throws Exception
  {
    DATABASE_FIXTURE =
      IdTestContainerInstances.database(containers);
  }

  @BeforeEach
  public void setup(
    final CloseableResourcesType closeables)
    throws Exception
  {
    DATABASE_FIXTURE.reset();

    this.database =
      closeables.addPerTestResource(DATABASE_FIXTURE.createDatabase());
    this.connection =
      closeables.addPerTestResource(this.database.openConnection(IDSTORE));
    this.transaction =
      closeables.addPerTestResource(this.connection.openTransaction());
  }

  /**
   * Setting the admin to a nonexistent admin fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminSetNonexistent()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(
        this.transaction, "admin", "12345678");

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        this.transaction.adminIdSet(randomUUID());
      });
    assertEquals(ADMIN_NONEXISTENT, ex.errorCode());
  }

  /**
   * Creating an admin works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminCreate0()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(
        this.transaction, "admin", "12345678");
    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      IdTestDatabases.generateDifferentBadPassword();

    this.transaction.adminIdSet(adminId);

    var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    assertEquals("someone", admin.idName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());

    admin = admins.adminGet(reqId).orElseThrow();
    assertEquals("someone", admin.idName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());

    admin = admins.adminGetForEmail(new IdEmail("someone@example.com")).orElseThrow();
    assertEquals("someone", admin.idName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());

    admin = admins.adminGetForName(new IdName("someone")).orElseThrow();
    assertEquals("someone", admin.idName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());

    IdTestDatabases.checkAuditLog(
      this.transaction,
      eventOf("ADMIN_CREATED", entry("AdminID", adminId)),
      eventOf("ADMIN_CREATED", entry("AdminID", reqId))
    );
  }

  /**
   * Creating an admin works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminCreate1()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");
    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var password =
      IdTestDatabases.generateDifferentBadPassword();

    this.transaction.adminIdSet(adminId);

    var admin =
      admins.adminCreate(
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        password,
        Set.of()
      );

    assertEquals("someone", admin.idName().value());
    assertEquals("someone@example.com", admin.emails().first().value());

    admin = admins.adminGet(admin.id()).orElseThrow();
    assertEquals("someone", admin.idName().value());
    assertEquals("someone@example.com", admin.emails().first().value());

    admin = admins.adminGetForEmail(new IdEmail("someone@example.com")).orElseThrow();
    assertEquals("someone", admin.idName().value());
    assertEquals("someone@example.com", admin.emails().first().value());

    admin = admins.adminGetForName(new IdName("someone")).orElseThrow();
    assertEquals("someone", admin.idName().value());
    assertEquals("someone@example.com", admin.emails().first().value());

    IdTestDatabases.checkAuditLog(
      this.transaction,
      eventOf("ADMIN_CREATED", entry("AdminID", adminId)),
      eventOf("ADMIN_CREATED", entry("AdminID", admin.id()))
    );
  }

  /**
   * Trying to create an initial admin when one already exists, fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminNotInitial()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");
    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      IdTestDatabases.generateDifferentBadPassword();

    this.transaction.adminIdSet(adminId);

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreateInitial(
          reqId,
          new IdName("someone"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone@example.com"),
          now,
          password
        );
      });

    assertEquals(ADMIN_NOT_INITIAL, ex.errorCode());
  }

  /**
   * Creating an admin requires an admin.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminRequiresAdmin()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");
    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      IdTestDatabases.generateDifferentBadPassword();

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreate(
          reqId,
          new IdName("someone2"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone2@example.com"),
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_UNSET, ex.errorCode());
  }

  /**
   * Creating an admin with a duplicate ID fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDuplicateId()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");
    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      IdTestDatabases.generateDifferentBadPassword();

    this.transaction.adminIdSet(adminId);

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreate(
          reqId,
          new IdName("someone2"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone2@example.com"),
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_DUPLICATE_ID, ex.errorCode());
  }

  /**
   * Creating an admin with a duplicate email fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDuplicateEmail()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");
    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      IdTestDatabases.generateDifferentBadPassword();

    this.transaction.adminIdSet(adminId);

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreate(
          randomUUID(),
          new IdName("someone2"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone@example.com"),
          now,
          password,
          Set.of()
        );
      });

    assertEquals(EMAIL_DUPLICATE, ex.errorCode());
  }

  /**
   * Creating an admin with a duplicate name fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDuplicateName()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");
    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      IdTestDatabases.generateDifferentBadPassword();

    this.transaction.adminIdSet(adminId);

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreate(
          randomUUID(),
          new IdName("someone"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone2@example.com"),
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_DUPLICATE_ID_NAME, ex.errorCode());
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminLogin()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");
    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var id =
      randomUUID();
    final var now =
      now();

    final var password =
      IdTestDatabases.generateDifferentBadPassword();

    this.transaction.adminIdSet(adminId);

    final var admin =
      admins.adminCreate(
        id,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    admins.adminLogin(
      admin.id(),
      Map.ofEntries(
        entry(remoteHost(), "127.0.0.1"),
        entry(userAgent(), "Mozilla/5.0 (X11; Linux x86_64)")
      )
    );

    IdTestDatabases.checkAuditLog(
      this.transaction,
      eventOf("ADMIN_CREATED", entry("AdminID", adminId)),
      eventOf("ADMIN_CREATED", entry("AdminID", id)),
      eventOf("ADMIN_LOGGED_IN", entry("Host", "127.0.0.1"))
    );
  }

  /**
   * Logging in fails for nonexistent admins.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminLoginNonexistent()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");
    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminLogin(
          randomUUID(),
          Map.ofEntries(
            entry(remoteHost(), "127.0.0.1"),
            entry(userAgent(), "Mozilla/5.0 (X11; Linux x86_64)")
          )
        );
      });
    assertEquals(ADMIN_NONEXISTENT, ex.errorCode());
  }

  private static void checkPage(
    final int indexLow,
    final int indexHigh,
    final List<IdAdminSummary> items)
  {
    var index = indexLow;
    final var iter = items.iterator();
    while (index < indexHigh) {
      final var item = iter.next();
      final var name =
        new IdName("someone_%03d".formatted(index));
      final var realName =
        new IdRealName("someone %03d".formatted(index));
      final var email =
        new IdEmail("someone_%03d@example.com".formatted(index));

      assertEquals(name, item.idName());
      assertEquals(realName, item.realName());
      ++index;
    }
  }

  /**
   * Updating an admin works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminUpdate()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");

    this.transaction.adminIdSet(adminId);

    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      IdTestDatabases.generateBadPassword();

    var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password,
        EnumSet.allOf(IdAdminPermission.class)
      );

    assertEquals("someone", admin.realName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());
    assertEquals(password, admin.password());

    this.transaction.adminIdSet(admin.id());

    final var otherPassword = IdTestDatabases.generateDifferentBadPassword();
    admins.adminUpdate(
      admin.id(),
      Optional.of(new IdName("newIdName")),
      Optional.of(new IdRealName("newRealName")),
      Optional.of(otherPassword),
      Optional.of(EnumSet.noneOf(IdAdminPermission.class))
    );

    admin = admins.adminGetRequire(admin.id());

    assertEquals("newRealName", admin.realName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals("newIdName", admin.idName().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());
    assertEquals(otherPassword, admin.password());
    assertEquals(IdAdminPermissionSet.empty(), admin.permissions());

    IdTestDatabases.checkAuditLog(
      this.transaction,
      eventOf("ADMIN_CREATED", entry("AdminID", adminId)),
      eventOf("ADMIN_CREATED", entry("AdminID", admin.id()))
    );
  }

  /**
   * Updating an admin fails for nonexistent admins.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminUpdateNonexistent()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");

    this.transaction.adminIdSet(adminId);

    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      IdTestDatabases.generateBadPassword();

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password,
        EnumSet.allOf(IdAdminPermission.class)
      );

    assertEquals("someone", admin.realName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());
    assertEquals(password, admin.password());

    this.transaction.adminIdSet(admin.id());

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        final var otherPassword = IdTestDatabases.generateDifferentBadPassword();
        admins.adminUpdate(
          randomUUID(),
          Optional.of(new IdName("newIdName")),
          Optional.of(new IdRealName("newRealName")),
          Optional.of(otherPassword),
          Optional.of(EnumSet.noneOf(IdAdminPermission.class))
        );
      });

    assertEquals(ADMIN_NONEXISTENT, ex.errorCode());
  }

  /**
   * Adding and removing email addresses works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminEmailAddresses()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");

    this.transaction.adminIdSet(adminId);

    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      IdTestDatabases.generateBadPassword();

    var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password,
        EnumSet.allOf(IdAdminPermission.class)
      );

    admins.adminEmailAdd(reqId, new IdEmail("someone2@example.com"));
    admin = admins.adminGetRequire(reqId);

    assertTrue(
      admin.emails().contains(new IdEmail("someone@example.com")));
    assertTrue(
      admin.emails().contains(new IdEmail("someone2@example.com")));
    assertEquals(2, admin.emails().toList().size());

    admins.adminEmailRemove(reqId, new IdEmail("someone@example.com"));
    admin = admins.adminGetRequire(reqId);
    assertEquals(1, admin.emails().toList().size());

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminEmailRemove(reqId, new IdEmail("someone2@example.com"));
      });
    assertEquals(EMAIL_ONE_REQUIRED, ex.errorCode());
  }

  /**
   * Deleting an admin works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDelete()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");

    this.transaction.adminIdSet(adminId);

    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      IdTestDatabases.generateBadPassword();

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password,
        EnumSet.allOf(IdAdminPermission.class)
      );

    admins.adminDelete(reqId);

    {
      final var ex =
        assertThrows(IdDatabaseException.class, () -> {
          admins.adminGetRequire(reqId);
        });

      assertEquals(ADMIN_NONEXISTENT, ex.errorCode());
    }

    {
      final var ex =
        assertThrows(IdDatabaseException.class, () -> {
          admins.adminCreate(
            reqId,
            new IdName("someone"),
            new IdRealName("someone"),
            new IdEmail("someone@example.com"),
            now,
            password,
            EnumSet.allOf(IdAdminPermission.class)
          );
        });

      assertEquals(ADMIN_DUPLICATE_ID, ex.errorCode());
    }

    IdTestDatabases.checkAuditLog(
      this.transaction,
      eventOf("ADMIN_CREATED", entry("AdminID", adminId)),
      eventOf("ADMIN_CREATED", entry("AdminID", admin.id())),
      eventOf(
        "ADMIN_EMAIL_REMOVED",
        entry("AdminID", admin.id()),
        entry("Email", "someone@example.com")),
      eventOf("ADMIN_DELETED", entry("AdminID", admin.id()))
    );
  }

  /**
   * Bans work.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminBan()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");

    this.transaction.adminIdSet(adminId);

    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var now =
      IdTestDatabases.timeNow();

    this.transaction.adminIdSet(adminId);

    final var ban = new IdBan(adminId, "No reason.", Optional.of(now));
    admins.adminBanCreate(ban);
    assertEquals(Optional.of(ban), admins.adminBanGet(adminId));
    admins.adminBanDelete(ban);
    assertEquals(Optional.empty(), admins.adminBanGet(adminId));

    IdTestDatabases.checkAuditLog(
      this.transaction,
      eventOf(
        "ADMIN_CREATED",
        entry("AdminID", adminId)),
      eventOf(
        "ADMIN_BANNED",
        entry("AdminID", adminId),
        entry("BanReason", "No reason.")),
      eventOf(
        "ADMIN_BAN_REMOVED",
        entry("AdminID", adminId))
    );
  }

  /**
   * Admins can be listed and paging works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminSearchByEmailPaged()
    throws Exception
  {
    final var adminId =
      IdTestDatabases.createAdminInitial(this.transaction, "admin", "12345678");

    this.transaction.adminIdSet(adminId);

    final var admins =
      this.transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var now =
      now();
    final var password =
      IdTestDatabases.generateBadPassword();

    final var adminList = new ArrayList<>();
    for (int index = 0; index < 500; ++index) {
      adminList.add(
        admins.adminCreate(
          randomUUID(),
          new IdName("someone_%03d".formatted(index)),
          new IdRealName("someone %03d".formatted(index)),
          new IdEmail("someone_%03d@example.com".formatted(index)),
          now,
          password,
          EnumSet.allOf(IdAdminPermission.class)
        )
      );
    }

    final var parameters =
      new IdAdminSearchByEmailParameters(
        new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
        new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
        "0@example.com",
        new IdAdminColumnOrdering(BY_IDNAME, true),
        150
      );

    final var paging =
      admins.adminSearchByEmail(parameters);

    {
      final IdPage<IdAdminSummary> page = paging.pageCurrent(admins);
      assertEquals(1, page.pageIndex());
      assertEquals(1, page.pageCount());
      assertTrue(page.items().size() >= 50);
      assertTrue(page.items().size() <= 52);

      for (final var item : page.items()) {
        final var name = item.idName().value();
        if (Objects.equals(name, "admin")) {
          continue;
        }
        assertTrue(
          name.endsWith("0"),
          "Name %s must end with 0".formatted(name)
        );
      }
    }
  }
}
