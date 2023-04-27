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

import java.util.Set;

/**
 * The type of admin permissions.
 */

public enum IdAdminPermission
{
  /**
   * A permission that allows for deleting admins.
   */

  ADMIN_DELETE {
    @Override
    public Set<IdAdminPermission> implies()
    {
      return Set.of();
    }
  },

  /**
   * A permission that allows for creating admins.
   */

  ADMIN_CREATE {
    @Override
    public Set<IdAdminPermission> implies()
    {
      return Set.of();
    }
  },

  /**
   * A permission that allows for banning users.
   */

  USER_BAN {
    @Override
    public Set<IdAdminPermission> implies()
    {
      return Set.of();
    }
  },

  /**
   * A permission that allows for banning admins.
   */

  ADMIN_BAN {
    @Override
    public Set<IdAdminPermission> implies()
    {
      return Set.of();
    }
  },

  /**
   * A permission that allows for modifying admins.
   */

  ADMIN_WRITE {
    @Override
    public Set<IdAdminPermission> implies()
    {
      return ADMIN_WRITE_SELF_IMPLIES;
    }
  },

  /**
   * A permission that allows an admin to modify itself.
   */

  ADMIN_WRITE_SELF {
    @Override
    public Set<IdAdminPermission> implies()
    {
      return Set.of();
    }
  },

  /**
   * A permission that allows reading admins.
   */

  ADMIN_READ {
    @Override
    public Set<IdAdminPermission> implies()
    {
      return Set.of();
    }
  },

  /**
   * A permission that allows reading the audit log.
   */

  AUDIT_READ {
    @Override
    public Set<IdAdminPermission> implies()
    {
      return Set.of();
    }
  },

  /**
   * A permission that allows deleting users.
   */

  USER_DELETE {
    @Override
    public Set<IdAdminPermission> implies()
    {
      return Set.of();
    }
  },

  /**
   * A permission that allows creating users.
   */

  USER_CREATE {
    @Override
    public Set<IdAdminPermission> implies()
    {
      return Set.of();
    }
  },

  /**
   * A permission that allows modifying users.
   */

  USER_WRITE {
    @Override
    public Set<IdAdminPermission> implies()
    {
      return Set.of();
    }
  },

  /**
   * A permission that allows reading users.
   */

  USER_READ {
    @Override
    public Set<IdAdminPermission> implies()
    {
      return Set.of();
    }
  };

  private static final Set<IdAdminPermission> ADMIN_WRITE_SELF_IMPLIES =
    Set.of(ADMIN_WRITE_SELF);

  /**
   * @return The set of permissions implied by this permission
   */

  public abstract Set<IdAdminPermission> implies();
}
