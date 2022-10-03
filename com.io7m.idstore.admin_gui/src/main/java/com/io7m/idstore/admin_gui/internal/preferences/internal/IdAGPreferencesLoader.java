/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.idstore.admin_gui.internal.preferences.internal;

import com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferenceServerBookmark;
import com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferenceServerCredentialsType;
import com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferenceServerUsernamePassword;
import com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferences;
import com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferencesDebuggingEnabled;
import com.io7m.jproperties.JPropertyIncorrectType;
import com.io7m.jproperties.JPropertyNonexistent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import static com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferencesDebuggingEnabled.DEBUGGING_DISABLED;
import static com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferencesDebuggingEnabled.DEBUGGING_ENABLED;
import static com.io7m.jproperties.JProperties.getBoolean;
import static com.io7m.jproperties.JProperties.getBooleanWithDefault;
import static com.io7m.jproperties.JProperties.getInteger;
import static com.io7m.jproperties.JProperties.getString;
import static com.io7m.jproperties.JProperties.getUUIDWithDefault;

/**
 * A preferences loader.
 */

public final class IdAGPreferencesLoader
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAGPreferencesLoader.class);

  private final FileSystem fileSystem;
  private final Properties properties;

  /**
   * A preferences loader.
   *
   * @param inFileSystem The filesystem used for paths
   * @param inProperties Properties
   */

  public IdAGPreferencesLoader(
    final FileSystem inFileSystem,
    final Properties inProperties)
  {
    this.fileSystem =
      Objects.requireNonNull(inFileSystem, "fileSystem");
    this.properties =
      Objects.requireNonNull(inProperties, "properties");
  }

  /**
   * @return A loaded set of preferences
   */

  public IdAGPreferences load()
  {
    return new IdAGPreferences(
      this.loadInstallationId(),
      this.loadDebuggingEnabled(),
      this.loadServerBookmarks(),
      this.loadRecentFiles()
    );
  }

  private UUID loadInstallationId()
  {
    final var fallback = UUID.randomUUID();
    try {
      return getUUIDWithDefault(this.properties, "installationId", fallback);
    } catch (final JPropertyIncorrectType e) {
      return fallback;
    }
  }

  private List<Path> loadRecentFiles()
  {
    final var countText = this.properties.getProperty("recentFiles.count");
    if (countText == null) {
      return List.of();
    }

    int count = 0;
    try {
      count = Integer.parseUnsignedInt(countText);
    } catch (final NumberFormatException e) {
      LOG.error("unable to load recent files: ", e);
    }

    final var results = new ArrayList<Path>();
    for (int index = 0; index < count; ++index) {
      try {
        results.add(this.loadRecentFile(Integer.valueOf(index)));
      } catch (final Exception e) {
        LOG.error("failed to load recent file: ", e);
      }
    }

    LOG.debug("loaded {} recent files", Integer.valueOf(results.size()));
    return List.copyOf(results);
  }

  private Path loadRecentFile(
    final Integer index)
    throws JPropertyNonexistent
  {
    return this.fileSystem.getPath(
      getString(this.properties, String.format("recentFiles.%s", index))
    );
  }

  private IdAGPreferencesDebuggingEnabled loadDebuggingEnabled()
  {
    try {
      if (getBooleanWithDefault(this.properties, "debugging", false)) {
        return DEBUGGING_ENABLED;
      }
      return DEBUGGING_DISABLED;
    } catch (final JPropertyIncorrectType e) {
      return DEBUGGING_DISABLED;
    }
  }

  private List<IdAGPreferenceServerBookmark> loadServerBookmarks()
  {
    final var countText = this.properties.getProperty("server.bookmarks");
    if (countText == null) {
      return List.of();
    }

    int count = 0;
    try {
      count = Integer.parseUnsignedInt(countText);
    } catch (final NumberFormatException e) {
      LOG.error("unable to load server bookmarks: ", e);
    }

    final var results = new ArrayList<IdAGPreferenceServerBookmark>();
    for (int index = 0; index < count; ++index) {
      try {
        results.add(this.loadServerBookmark(Integer.valueOf(index)));
      } catch (final Exception e) {
        LOG.error("failed to load server bookmark: ", e);
      }
    }

    LOG.debug("loaded {} server bookmarks", Integer.valueOf(results.size()));
    return List.copyOf(results);
  }

  private IdAGPreferenceServerBookmark loadServerBookmark(
    final Integer i)
    throws JPropertyNonexistent, JPropertyIncorrectType
  {
    return new IdAGPreferenceServerBookmark(
      getString(this.properties, "server.bookmarks.%d.name".formatted(i)),
      getString(this.properties, "server.bookmarks.%d.host".formatted(i)),
      getInteger(this.properties, "server.bookmarks.%d.port".formatted(i)),
      getBoolean(this.properties, "server.bookmarks.%d.https".formatted(i)),
      this.loadServerBookmarkCredentials(i)
    );
  }

  private IdAGPreferenceServerCredentialsType loadServerBookmarkCredentials(
    final Integer i)
    throws JPropertyNonexistent
  {
    final var value =
      getString(
        this.properties,
        "server.bookmarks.%d.credentials.type".formatted(i));

    return switch (value) {
      case "usernamePassword" -> new IdAGPreferenceServerUsernamePassword(
        getString(
          this.properties,
          "server.bookmarks.%d.credentials.username".formatted(i)),
        getString(
          this.properties,
          "server.bookmarks.%d.credentials.password".formatted(i))
      );
      default -> throw new IllegalStateException("Unexpected value: " + value);
    };
  }
}
