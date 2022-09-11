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

import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUserSummary;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class IdTestUserSet
{
  private IdTestUserSet()
  {

  }

  public static List<IdUserSummary> users()
    throws IOException
  {
    try (InputStreamReader reader = open()) {
      final ArrayList<IdUserSummary> users = new ArrayList<>();
      try (var br = new BufferedReader(reader)) {
        while (true) {
          final var line = br.readLine();
          if (line == null) {
            break;
          }
          final var segments = List.of(line.split(":"));
          users.add(
            new IdUserSummary(
              UUID.fromString(segments.get(0)),
              new IdName(segments.get(1)),
              new IdRealName(segments.get(2)),
              OffsetDateTime.now(),
              OffsetDateTime.now()
            )
          );
        }
      }
      return List.copyOf(users);
    }
  }

  private static InputStreamReader open()
  {
    final var stream =
      IdTestUserSet.class.getResourceAsStream(
        "/com/io7m/idstore/tests/users.txt");
    return new InputStreamReader(stream, UTF_8);
  }
}
