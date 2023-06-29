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


package com.io7m.idstore.tests.server.events;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.server.service.telemetry.api.IdEventAdminLoggedIn;
import com.io7m.idstore.server.service.telemetry.api.IdEventAdminLoginAuthenticationFailed;
import com.io7m.idstore.server.service.telemetry.api.IdEventAdminLoginRateLimitExceeded;
import com.io7m.idstore.server.service.telemetry.api.IdEventType;
import com.io7m.idstore.server.service.telemetry.api.IdEventUserEmailVerificationRateLimitExceeded;
import com.io7m.idstore.server.service.telemetry.api.IdEventUserLoggedIn;
import com.io7m.idstore.server.service.telemetry.api.IdEventUserLoginAuthenticationFailed;
import com.io7m.idstore.server.service.telemetry.api.IdEventUserLoginRateLimitExceeded;
import com.io7m.idstore.server.service.telemetry.api.IdEventUserPasswordResetRateLimitExceeded;

import java.util.List;
import java.util.TreeMap;

import static java.util.UUID.randomUUID;

public final class IdEventDump
{
  private IdEventDump()
  {

  }

  public static void main(
    final String[] args)
  {
    for (final var e : events()) {
      System.out.printf("<!-- %s -->%n", e.getClass().getSimpleName());
      System.out.println("""
                           <Table type="genericTable">
                             <Columns>
                               <Column>Attribute</Column>
                               <Column>Description</Column>
                               <Column>Typical Value</Column>
                             </Columns>""");

      {
        for (final var entry : new TreeMap<>(e.asAttributes()).entrySet()) {
          System.out.printf("  <Row>%n");
          System.out.printf("    <Cell><Term type=\"expression\">%s</Term></Cell>%n", entry.getKey());
          System.out.printf("    <Cell>...</Cell>%n");
          System.out.printf("    <Cell><Term type=\"expression\">%s</Term></Cell>%n", entry.getValue());
          System.out.printf("  </Row>%n");
        }
      }

      System.out.println("</Table>\n");
    }
  }


  private static List<IdEventType> events()
  {
    return List.of(
      new IdEventAdminLoggedIn(randomUUID()),
      new IdEventAdminLoginAuthenticationFailed("x", randomUUID()),
      new IdEventAdminLoginRateLimitExceeded("x", "y"),
      new IdEventUserEmailVerificationRateLimitExceeded(randomUUID(), new IdEmail("e@ex.com")),
      new IdEventUserLoggedIn(randomUUID()),
      new IdEventUserLoginAuthenticationFailed("x", randomUUID()),
      new IdEventUserLoginRateLimitExceeded("x", "y"),
      new IdEventUserPasswordResetRateLimitExceeded("x", "y")
    );
  }
}
