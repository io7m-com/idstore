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


package com.io7m.idstore.tests.model;

import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminPermissionSet;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_CREDENTIALS_SELF;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_EMAIL_SELF;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_PERMISSIONS_SELF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdAdminPermissionSetTest
{
  /**
   * Adding a permission to a set results in that set implying that permission.
   */

  @Property
  public void testPlusImplies(
    @ForAll final IdAdminPermissionSet permissions,
    @ForAll final IdAdminPermission permission)
  {
    final var with = permissions.plus(permission);
    assertTrue(with.implies(permission));
  }

  /**
   * Removing a permission from a set results in that set not implying that
   * permission.
   */

  @Property
  public void testMinusImplies(
    @ForAll final IdAdminPermissionSet permissions,
    @ForAll final IdAdminPermission permission)
  {
    Assumptions.assumeFalse(permission == ADMIN_WRITE_PERMISSIONS_SELF);
    Assumptions.assumeFalse(permission == ADMIN_WRITE_CREDENTIALS_SELF);
    Assumptions.assumeFalse(permission == ADMIN_WRITE_EMAIL_SELF);

    final var without = permissions.minus(permission);
    assertFalse(without.implies(permission));
  }

  /**
   * Parsing and serializing is an identity operation.
   */

  @Property
  public void testParseIdentity(
    @ForAll final IdAdminPermissionSet permissions)
  {
    assertEquals(
      permissions,
      IdAdminPermissionSet.parse(permissions.toString())
    );
  }

  /**
   * ADMIN_WRITE_PERMISSIONS implies ADMIN_WRITE_PERMISSIONS_SELF.
   */

  @Test
  public void testImplies0()
  {
    final var s = IdAdminPermissionSet.of(IdAdminPermission.ADMIN_WRITE_PERMISSIONS);
    assertTrue(s.implies(ADMIN_WRITE_PERMISSIONS_SELF));
  }

  /**
   * ADMIN_WRITE_EMAIL implies ADMIN_WRITE_EMAIL_SELF.
   */

  @Test
  public void testImplies1()
  {
    final var s = IdAdminPermissionSet.of(IdAdminPermission.ADMIN_WRITE_EMAIL);
    assertTrue(s.implies(ADMIN_WRITE_EMAIL_SELF));
  }

  /**
   * ADMIN_WRITE_CREDENTIALS implies ADMIN_WRITE_CREDENTIALS_SELF.
   */

  @Test
  public void testImplies2()
  {
    final var s = IdAdminPermissionSet.of(IdAdminPermission.ADMIN_WRITE_CREDENTIALS);
    assertTrue(s.implies(ADMIN_WRITE_CREDENTIALS_SELF));
  }
}
