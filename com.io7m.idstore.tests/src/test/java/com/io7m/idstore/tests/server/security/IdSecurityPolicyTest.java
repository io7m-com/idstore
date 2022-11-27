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


package com.io7m.idstore.tests.server.security;

import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.server.security.IdSecActionType;
import com.io7m.idstore.server.security.IdSecAdminActionAdminCreate;
import com.io7m.idstore.server.security.IdSecAdminActionAdminDelete;
import com.io7m.idstore.server.security.IdSecAdminActionAdminEmailAdd;
import com.io7m.idstore.server.security.IdSecAdminActionAdminEmailRemove;
import com.io7m.idstore.server.security.IdSecAdminActionAdminPermissionGrant;
import com.io7m.idstore.server.security.IdSecAdminActionAdminPermissionRevoke;
import com.io7m.idstore.server.security.IdSecAdminActionAdminRead;
import com.io7m.idstore.server.security.IdSecAdminActionAdminUpdate;
import com.io7m.idstore.server.security.IdSecAdminActionAuditRead;
import com.io7m.idstore.server.security.IdSecAdminActionUserCreate;
import com.io7m.idstore.server.security.IdSecAdminActionUserDelete;
import com.io7m.idstore.server.security.IdSecAdminActionUserRead;
import com.io7m.idstore.server.security.IdSecAdminActionUserUpdate;
import com.io7m.idstore.server.security.IdSecPolicyResultDenied;
import com.io7m.idstore.server.security.IdSecPolicyResultPermitted;
import com.io7m.idstore.server.security.IdSecurity;
import com.io7m.idstore.server.security.IdSecurityException;
import com.io7m.junreachable.UnreachableCodeException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;

import static com.io7m.idstore.model.IdAdminPermission.ADMIN_CREATE;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_DELETE;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_READ;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_SELF;
import static com.io7m.idstore.model.IdAdminPermission.AUDIT_READ;
import static com.io7m.idstore.model.IdAdminPermission.USER_CREATE;
import static com.io7m.idstore.model.IdAdminPermission.USER_DELETE;
import static com.io7m.idstore.model.IdAdminPermission.USER_READ;
import static com.io7m.idstore.model.IdAdminPermission.USER_WRITE;
import static java.time.OffsetDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class IdSecurityPolicyTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdSecurityPolicyTest.class);

  private static final IdPassword BAD_PASSWORD;

  static {
    try {
      BAD_PASSWORD = IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("12345678");
    } catch (final IdPasswordException e) {
      throw new UnreachableCodeException(e);
    }
  }

  /**
   * Admins cannot be created by an admin without ADMIN_CREATE.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminCreateNoCreate()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminCreate(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.empty()
        ),
        Set.of()
      );

    failsWith(action, "Creating admins requires the ADMIN_CREATE permission.");
  }

  /**
   * Admins cannot be created with more permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminCreateMorePermissions()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminCreate(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_CREATE)
        ),
        Set.of(ADMIN_CREATE, ADMIN_WRITE)
      );

    failsWith(
      action,
      "The current admin cannot grant the following permissions: [ADMIN_WRITE]");
  }

  /**
   * Admins can be created.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminCreateOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminCreate(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_CREATE)
        ),
        Set.of(ADMIN_CREATE)
      );

    succeeds(action);
  }

  /**
   * Admins cannot be updated by an admin without ADMIN_WRITE.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminUpdateNoUpdate()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminUpdate(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.empty()
        ),
        UUID.randomUUID()
      );

    failsWith(action, "Modifying admins requires the ADMIN_WRITE permission.");
  }

  /**
   * Admins cannot be updated by an admin without ADMIN_WRITE.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminUpdateNoUpdateSelf()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminUpdate(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.empty()
        ),
        id
      );

    failsWith(action, "Modifying admins requires the ADMIN_WRITE_SELF permission.");
  }

  /**
   * Admins can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminUpdateOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminUpdate(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        UUID.randomUUID()
      );

    succeeds(action);
  }

  /**
   * Admins can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminUpdateOKSelf0()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminUpdate(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        id
      );

    succeeds(action);
  }

  /**
   * Admins can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminUpdateOKSelf1()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminUpdate(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE_SELF)
        ),
        id
      );

    succeeds(action);
  }

  /**
   * Admins cannot be read without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminReadWithoutPermissions()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminRead(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        )
      );

    failsWith(
      action,
      "Reading admins requires the ADMIN_READ permission.");
  }

  /**
   * Admins can be read.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminReadOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminRead(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_READ)
        )
      );

    succeeds(action);
  }


  /**
   * Admins cannot be deleted without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminDeleteWithoutPermissions()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminDelete(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_READ)
        )
      );

    failsWith(
      action,
      "Deleting admins requires the ADMIN_DELETE permission.");
  }

  /**
   * Admins can be deleted.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminDeleteOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminDelete(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_DELETE)
        )
      );

    succeeds(action);
  }


  /**
   * Users cannot be created by an admin without USER_CREATE.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testUserCreateNoCreate()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionUserCreate(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of()
        )
      );

    failsWith(action, "Creating users requires the USER_CREATE permission.");
  }

  /**
   * Users can be created.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testUserCreateOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionUserCreate(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(USER_CREATE)
        )
      );

    succeeds(action);
  }

  /**
   * Users cannot be updated by an admin without USER_UPDATE.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testUserUpdateNoUpdate()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionUserUpdate(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of()
        )
      );

    failsWith(action, "Updating users requires the USER_WRITE permission.");
  }

  /**
   * Users can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testUserUpdateOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionUserUpdate(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(USER_WRITE)
        )
      );

    succeeds(action);
  }

  /**
   * Users cannot be read without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testUserReadWithoutPermissions()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionUserRead(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(USER_WRITE)
        )
      );

    failsWith(
      action,
      "Reading users requires the USER_READ permission.");
  }

  /**
   * Users can be read.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testUserReadOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionUserRead(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(USER_READ)
        )
      );

    succeeds(action);
  }


  /**
   * Users cannot be deleted without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testUserDeleteWithoutPermissions()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionUserDelete(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(USER_READ)
        )
      );

    failsWith(
      action,
      "Deleting users requires the USER_DELETE permission.");
  }

  /**
   * Users can be deleted.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testUserDeleteOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionUserDelete(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(USER_DELETE)
        )
      );

    succeeds(action);
  }

  /**
   * The audit log requires permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminAuditLogOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAuditRead(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(AUDIT_READ)
        )
      );

    succeeds(action);
  }

  /**
   * The audit log requires permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminAuditLogWithoutPermissions()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAuditRead(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(USER_READ)
        )
      );

    failsWith(
      action,
      "Reading audit records requires the AUDIT_READ permission.");
  }

  /**
   * Admin emails can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminEmailAddOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminEmailAdd(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        UUID.randomUUID()
      );

    succeeds(action);
  }

  /**
   * Admin emails can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminEmailAddSelf0OK()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminEmailAdd(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE_SELF)
        ),
        id
      );

    succeeds(action);
  }

  /**
   * Admin emails can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminEmailAddSelf1OK()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminEmailAdd(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        id
      );

    succeeds(action);
  }

  /**
   * Admin emails cannot be updated without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminEmailAddFailure0()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminEmailAdd(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of()
        ),
        id
      );

    failsWith(
      action,
      "Modifying admins requires the ADMIN_WRITE_SELF permission.");
  }

  /**
   * Admin emails cannot be updated without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminEmailAddFailure1()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminEmailAdd(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of()
        ),
        UUID.randomUUID()
      );

    failsWith(action, "Modifying admins requires the ADMIN_WRITE permission.");
  }

  /**
   * Admin emails can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminEmailRemoveOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminEmailRemove(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        UUID.randomUUID()
      );

    succeeds(action);
  }

  /**
   * Admin emails can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminEmailRemoveSelf0OK()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminEmailRemove(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE_SELF)
        ),
        id
      );

    succeeds(action);
  }

  /**
   * Admin emails can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminEmailRemoveSelf1OK()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminEmailRemove(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        id
      );

    succeeds(action);
  }

  /**
   * Admin emails cannot be updated without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminEmailRemoveFailure0()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminEmailRemove(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of()
        ),
        id
      );

    failsWith(
      action,
      "Modifying admins requires the ADMIN_WRITE_SELF permission.");
  }

  /**
   * Admin emails cannot be updated without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminEmailRemoveFailure1()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminEmailRemove(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of()
        ),
        UUID.randomUUID()
      );

    failsWith(action, "Modifying admins requires the ADMIN_WRITE permission.");
  }

  /**
   * Admin permissions can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionGrantOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminPermissionGrant(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        UUID.randomUUID(),
        ADMIN_WRITE
      );

    succeeds(action);
  }

  /**
   * Admin permissions can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionGrantSelf0OK()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminPermissionGrant(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE_SELF)
        ),
        id,
        ADMIN_WRITE_SELF
      );

    succeeds(action);
  }

  /**
   * Admin permissions can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionGrantSelf1OK()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminPermissionGrant(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        id,
        ADMIN_WRITE
      );

    succeeds(action);
  }

  /**
   * Admin permissions cannot be updated without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionGrantFailure0()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminPermissionGrant(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of()
        ),
        id,
        ADMIN_WRITE
      );

    failsWith(
      action,
      "Modifying admins requires the ADMIN_WRITE_SELF permission.");
  }

  /**
   * Admin permissions cannot be updated without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionGrantFailure1()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminPermissionGrant(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of()
        ),
        UUID.randomUUID(),
        ADMIN_WRITE
      );

    failsWith(action, "Modifying admins requires the ADMIN_WRITE permission.");
  }

  /**
   * Admin permissions cannot be updated without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionGrantFailure2()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminPermissionGrant(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        UUID.randomUUID(),
        ADMIN_READ
      );

    failsWith(
      action,
      "The ADMIN_READ permission cannot be granted by an admin that does not have it.");
  }

  /**
   * Admin permissions cannot be updated without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionGrantFailure3()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminPermissionGrant(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        id,
        ADMIN_READ
      );

    failsWith(
      action,
      "The ADMIN_READ permission cannot be granted by an admin that does not have it.");
  }


  /**
   * Admin permissions can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionRevokeOK()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminPermissionRevoke(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        UUID.randomUUID(),
        ADMIN_WRITE
      );

    succeeds(action);
  }

  /**
   * Admin permissions can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionRevokeSelf0OK()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminPermissionRevoke(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE_SELF)
        ),
        id,
        ADMIN_WRITE_SELF
      );

    succeeds(action);
  }

  /**
   * Admin permissions can be updated.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionRevokeSelf1OK()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminPermissionRevoke(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        id,
        ADMIN_WRITE
      );

    succeeds(action);
  }

  /**
   * Admin permissions cannot be updated without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionRevokeFailure0()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminPermissionRevoke(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of()
        ),
        id,
        ADMIN_WRITE
      );

    failsWith(
      action,
      "Modifying admins requires the ADMIN_WRITE_SELF permission.");
  }

  /**
   * Admin permissions cannot be updated without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionRevokeFailure1()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminPermissionRevoke(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of()
        ),
        UUID.randomUUID(),
        ADMIN_WRITE
      );

    failsWith(action, "Modifying admins requires the ADMIN_WRITE permission.");
  }

  /**
   * Admin permissions cannot be updated without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionRevokeFailure2()
    throws IdSecurityException
  {
    final var action =
      new IdSecAdminActionAdminPermissionRevoke(
        new IdAdmin(
          UUID.randomUUID(),
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        UUID.randomUUID(),
        ADMIN_READ
      );

    failsWith(
      action,
      "The ADMIN_READ permission cannot be revoked by an admin that does not have it.");
  }

  /**
   * Admin permissions cannot be updated without permissions.
   *
   * @throws IdSecurityException On errors
   */

  @Test
  public void testAdminPermissionRevokeFailure3()
    throws IdSecurityException
  {
    final var id = UUID.randomUUID();
    final var action =
      new IdSecAdminActionAdminPermissionRevoke(
        new IdAdmin(
          id,
          new IdName("admin-0"),
          new IdRealName("Someone R. Incognito"),
          IdNonEmptyList.single(new IdEmail("someone@example.com")),
          now(),
          now(),
          BAD_PASSWORD,
          IdAdminPermissionSet.of(ADMIN_WRITE)
        ),
        id,
        ADMIN_READ
      );

    failsWith(
      action,
      "The ADMIN_READ permission cannot be revoked by an admin that does not have it.");
  }

  private static void succeeds(
    final IdSecActionType action)
    throws IdSecurityException
  {
    final var check = IdSecurity.check(action);
    LOG.debug("check: {}", check);
    assertEquals(IdSecPolicyResultPermitted.class, check.getClass());
  }

  private static void failsWith(
    final IdSecActionType action,
    final String message)
    throws IdSecurityException
  {
    final var check = IdSecurity.check(action);
    LOG.debug("check: {}", check);
    final var result = (IdSecPolicyResultDenied) check;
    assertEquals(message, result.message());
  }
}
