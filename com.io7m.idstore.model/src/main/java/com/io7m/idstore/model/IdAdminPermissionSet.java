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

package com.io7m.idstore.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A set of permissions.
 */

public final class IdAdminPermissionSet
{
  private static final IdAdminPermissionSet EMPTY =
    new IdAdminPermissionSet(
      EnumSet.noneOf(IdAdminPermission.class)
    );
  private static final IdAdminPermissionSet ALL =
    new IdAdminPermissionSet(
      EnumSet.allOf(IdAdminPermission.class)
    );

  @Override
  public boolean equals(
    final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || !this.getClass().equals(o.getClass())) {
      return false;
    }
    final IdAdminPermissionSet that = (IdAdminPermissionSet) o;
    return this.impliedPermissions().equals(that.impliedPermissions());
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(this.impliedPermissions());
  }

  private final EnumSet<IdAdminPermission> permissions;

  private IdAdminPermissionSet(
    final EnumSet<IdAdminPermission> inPermissions)
  {
    this.permissions =
      Objects.requireNonNull(inPermissions, "permissions");
  }

  /**
   * @return The empty set of permissions
   */

  public static IdAdminPermissionSet empty()
  {
    return EMPTY;
  }

  /**
   * @return The set of permissions containing every permission
   */

  public static IdAdminPermissionSet all()
  {
    return ALL;
  }

  /**
   * Construct a set of permissions.
   *
   * @param permissions The permissions
   *
   * @return A set of permissions
   */

  public static IdAdminPermissionSet of(
    final Set<IdAdminPermission> permissions)
  {
    Objects.requireNonNull(permissions, "permissions");
    if (permissions.isEmpty()) {
      return EMPTY;
    }
    return new IdAdminPermissionSet(EnumSet.copyOf(permissions));
  }

  /**
   * Construct a set of permissions.
   *
   * @param permissions The permissions
   *
   * @return A set of permissions
   */

  public static IdAdminPermissionSet of(
    final IdAdminPermission... permissions)
  {
    Objects.requireNonNull(permissions, "permissions");

    if (permissions.length == 0) {
      return EMPTY;
    }

    final var s =
      EnumSet.noneOf(IdAdminPermission.class);
    s.addAll(Arrays.asList(permissions));
    return new IdAdminPermissionSet(s);
  }

  /**
   * Construct a set of permissions based on this set.
   *
   * @param permission The permission to add
   *
   * @return A set of permissions
   */

  public IdAdminPermissionSet plus(
    final IdAdminPermission permission)
  {
    Objects.requireNonNull(permission, "permission");

    final var s = EnumSet.copyOf(this.permissions);
    s.add(permission);
    return new IdAdminPermissionSet(s);
  }

  /**
   * Construct a set of permissions based on this set.
   *
   * @param permission The permission to remove
   *
   * @return A set of permissions
   */

  public IdAdminPermissionSet minus(
    final IdAdminPermission permission)
  {
    Objects.requireNonNull(permission, "permission");

    final var s = EnumSet.copyOf(this.permissions);
    s.remove(permission);
    return new IdAdminPermissionSet(s);
  }

  /**
   * @param permission The permission
   *
   * @return {@code true} if this set implies the given permission
   */

  public boolean implies(
    final IdAdminPermission permission)
  {
    Objects.requireNonNull(permission, "permission");

    if (this.permissions.contains(permission)) {
      return true;
    }

    for (final var held : this.permissions) {
      if (impliesPermission(held, permission)) {
        return true;
      }
    }

    return false;
  }

  private static boolean impliesPermission(
    final IdAdminPermission held,
    final IdAdminPermission permission)
  {
    final var implied = held.implies();
    if (implied.contains(permission)) {
      return true;
    }

    for (final var heldI : implied) {
      if (impliesPermission(heldI, permission)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Construct a set of permissions based on the given string. Unrecognized
   * permissions will be ignored.
   *
   * @param text The permission string
   *
   * @return A set of permissions
   */

  public static IdAdminPermissionSet parse(
    final String text)
  {
    Objects.requireNonNull(text, "text");

    final var set =
      EnumSet.noneOf(IdAdminPermission.class);
    final var segments =
      List.of(text.trim().split(","));

    for (final var segment : segments) {
      try {
        set.add(IdAdminPermission.valueOf(segment));
      } catch (final IllegalArgumentException e) {
        // Ignore
      }
    }

    return new IdAdminPermissionSet(set);
  }

  @Override
  public String toString()
  {
    return this.permissions.stream()
      .sorted()
      .map(Enum::toString)
      .collect(Collectors.joining(","));
  }

  /**
   * @return The set of implied permissions
   */

  public Set<IdAdminPermission> impliedPermissions()
  {
    return Set.copyOf(this.permissions);
  }

  /**
   * @param permissions The permissions to check
   *
   * @return {@code true} if this set implies all the given permissions
   */

  public boolean impliesAll(
    final Collection<IdAdminPermission> permissions)
  {
    return permissions.stream()
      .allMatch(this::implies);
  }
}
