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

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseAuditListPaging;
import com.io7m.idstore.database.api.IdDatabaseAuditQueriesType;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseRole;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.postgres.IdDatabases;
import com.io7m.idstore.model.IdAuditEvent;
import com.io7m.idstore.model.IdAuditSearchParameters;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseCreate.CREATE_DATABASE;
import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.database.api.IdDatabaseUpgrade.UPGRADE_DATABASE;
import static java.time.OffsetDateTime.now;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class IdDatabaseAuditTest
{
  private static final UUID ADMIN_UUID =
    UUID.randomUUID();
  private static final IdDatabases DATABASES =
    new IdDatabases();

  @Container
  private final PostgreSQLContainer<?> container =
    new PostgreSQLContainer<>(DockerImageName.parse("postgres").withTag("14.4"))
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("12345678");

  private CloseableCollectionType<ClosingResourceFailedException> resources;

  private IdDatabaseType databaseOf(
    final PostgreSQLContainer<?> container)
    throws IdDatabaseException
  {
    final var database =
      DATABASES.open(
        new IdDatabaseConfiguration(
          "postgres",
          "12345678",
          container.getContainerIpAddress(),
          container.getFirstMappedPort().intValue(),
          "postgres",
          CREATE_DATABASE,
          UPGRADE_DATABASE,
          Clock.systemUTC()
        ),
        OpenTelemetry.noop(),
        message -> {

        }
      );

    try (var c = database.openConnection(IDSTORE)) {
      try (var t = c.openTransaction()) {
        final var a =
          t.queries(IdDatabaseAdminsQueriesType.class);

        try {
          final var password =
            IdPasswordAlgorithmPBKDF2HmacSHA256.create()
              .createHashed("12345678");

          a.adminCreateInitial(
            ADMIN_UUID,
            new IdName("someone"),
            new IdRealName("someone"),
            new IdEmail("someone@example.com"),
            now(),
            password
          );
        } catch (final IdPasswordException e) {
          throw new IllegalStateException(e);
        }

        t.commit();
      }
    }

    return this.resources.add(database);
  }

  private IdDatabaseTransactionType transactionOf(
    final IdDatabaseRole role)
    throws IdDatabaseException
  {
    final var database =
      this.databaseOf(this.container);
    final var connection =
      this.resources.add(database.openConnection(role));
    return this.resources.add(connection.openTransaction());
  }

  @BeforeEach
  public void setup()
  {
    this.resources = CloseableCollection.create();
  }

  @AfterEach
  public void tearDown()
    throws ClosingResourceFailedException
  {
    this.resources.close();
  }

  @Test
  public void testAuditQuery0()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var audit =
      transaction.queries(IdDatabaseAuditQueriesType.class);

    final var then = now();
    audit.auditPut(ADMIN_UUID, then.plusSeconds(1), "ET_0", "E0");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(2), "ET_0", "E1");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(3), "ET_0", "E2");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(4), "ET_1", "F3");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(5), "ET_1", "F4");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(6), "ET_1", "F5");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(7), "ET_2", "G6");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(8), "ET_2", "G7");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(9), "ET_2", "G8");

    transaction.commit();

    final var parameters =
      new IdAuditSearchParameters(
        new IdTimeRange(then, then.plusDays(1L)),
        empty(),
        empty(),
        empty(),
        4
      );

    assertEquals(9, audit.auditCount(parameters));

    OptionalLong seek = OptionalLong.empty();

    {
      final var events = audit.auditEvents(parameters, seek);
      assertEquals(4, events.size());
      assertEquals("E0", events.get(0).message());
      assertEquals("E1", events.get(1).message());
      assertEquals("E2", events.get(2).message());
      assertEquals("F3", events.get(3).message());
      seek = OptionalLong.of(events.get(3).id());
    }

    {
      final var events = audit.auditEvents(parameters, seek);
      assertEquals(4, events.size());
      assertEquals("F4", events.get(0).message());
      assertEquals("F5", events.get(1).message());
      assertEquals("G6", events.get(2).message());
      assertEquals("G7", events.get(3).message());
      seek = OptionalLong.of(events.get(3).id());
    }

    {
      final var events = audit.auditEvents(parameters, seek);
      assertEquals(1, events.size());
      assertEquals("G8", events.get(0).message());
      seek = OptionalLong.of(events.get(0).id());
    }
  }

  @Test
  public void testAuditQuery1()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var audit =
      transaction.queries(IdDatabaseAuditQueriesType.class);

    final var then = now();
    audit.auditPut(ADMIN_UUID, then.plusSeconds(1), "ET_0", "E0");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(2), "ET_0", "E1");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(3), "ET_0", "E2");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(4), "ET_1", "F3");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(5), "ET_1", "F4");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(6), "ET_1", "F5");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(7), "ET_2", "G6");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(8), "ET_2", "G7");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(9), "ET_2", "G8");

    transaction.commit();

    final var parameters =
      new IdAuditSearchParameters(
        new IdTimeRange(then, then.plusDays(1L)),
        empty(),
        Optional.of("ET_0"),
        empty(),
        1
      );

    OptionalLong seek = OptionalLong.empty();

    {
      final var events = audit.auditEvents(parameters, seek);
      assertEquals(1, events.size());
      assertEquals("E0", events.get(0).message());
      seek = OptionalLong.of(events.get(0).id());
    }

    {
      final var events = audit.auditEvents(parameters, seek);
      assertEquals(1, events.size());
      assertEquals("E1", events.get(0).message());
      seek = OptionalLong.of(events.get(0).id());
    }

    {
      final var events = audit.auditEvents(parameters, seek);
      assertEquals(1, events.size());
      assertEquals("E2", events.get(0).message());
      seek = OptionalLong.of(events.get(0).id());
    }
  }

  @Test
  public void testAuditQuery2()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var audit =
      transaction.queries(IdDatabaseAuditQueriesType.class);

    final var then = now();
    audit.auditPut(ADMIN_UUID, then.plusSeconds(1), "ET_0", "E0");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(2), "ET_0", "E1");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(3), "ET_0", "E2");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(4), "ET_1", "F3");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(5), "ET_1", "F4");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(6), "ET_1", "F5");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(7), "ET_2", "G6");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(8), "ET_2", "G7");
    audit.auditPut(ADMIN_UUID, then.plusSeconds(9), "ET_2", "G8");

    transaction.commit();

    final var parameters =
      new IdAuditSearchParameters(
        new IdTimeRange(then, then.plusDays(1L)),
        empty(),
        empty(),
        Optional.of("F"),
        1
      );

    OptionalLong seek = OptionalLong.empty();

    {
      final var events = audit.auditEvents(parameters, seek);
      assertEquals(1, events.size());
      assertEquals("F3", events.get(0).message());
      seek = OptionalLong.of(events.get(0).id());
    }

    {
      final var events = audit.auditEvents(parameters, seek);
      assertEquals(1, events.size());
      assertEquals("F4", events.get(0).message());
      seek = OptionalLong.of(events.get(0).id());
    }

    {
      final var events = audit.auditEvents(parameters, seek);
      assertEquals(1, events.size());
      assertEquals("F5", events.get(0).message());
      seek = OptionalLong.of(events.get(0).id());
    }
  }

  @Test
  public void testAuditPaging()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var audit =
      transaction.queries(IdDatabaseAuditQueriesType.class);

    final var then = now();
    for (int index = 0; index < 533; ++index) {
      audit.auditPut(
        ADMIN_UUID,
        then.plusSeconds(index),
        String.format("ET_%04d", Integer.valueOf(index)),
        String.format("E_%04d", Integer.valueOf(index))
      );
    }

    transaction.commit();

    final var parameters =
      new IdAuditSearchParameters(
        new IdTimeRange(then, then.plusDays(1L)),
        empty(),
        empty(),
        empty(),
        100
      );

    final var paging =
      IdDatabaseAuditListPaging.create(parameters);

    {
      final var items = paging.pageCurrent(audit);
      assertEquals(0, paging.pageNumber());
      assertEquals(5, paging.pageCount());
      assertEquals(100, items.size());
      checkPage(then, 0, 100, items);
      assertTrue(paging.pageNextAvailable());
      assertFalse(paging.pagePreviousAvailable());
    }

    {
      final var items = paging.pageNext(audit);
      assertEquals(1, paging.pageNumber());
      assertEquals(5, paging.pageCount());
      assertEquals(100, items.size());
      checkPage(then, 100, 200, items);
      assertTrue(paging.pageNextAvailable());
      assertTrue(paging.pagePreviousAvailable());
    }

    {
      final var items = paging.pageNext(audit);
      assertEquals(2, paging.pageNumber());
      assertEquals(5, paging.pageCount());
      assertEquals(100, items.size());
      checkPage(then, 200, 300, items);
      assertTrue(paging.pageNextAvailable());
      assertTrue(paging.pagePreviousAvailable());
    }

    {
      final var items = paging.pageNext(audit);
      assertEquals(3, paging.pageNumber());
      assertEquals(5, paging.pageCount());
      assertEquals(100, items.size());
      checkPage(then, 300, 400, items);
      assertTrue(paging.pageNextAvailable());
      assertTrue(paging.pagePreviousAvailable());
    }

    {
      final var items = paging.pageNext(audit);
      assertEquals(4, paging.pageNumber());
      assertEquals(5, paging.pageCount());
      assertEquals(100, items.size());
      checkPage(then, 400, 500, items);
      assertTrue(paging.pageNextAvailable());
      assertTrue(paging.pagePreviousAvailable());
    }

    {
      final var items = paging.pageNext(audit);
      assertEquals(5, paging.pageNumber());
      assertEquals(5, paging.pageCount());
      assertEquals(33, items.size());
      checkPage(then, 500, 533, items);
      assertFalse(paging.pageNextAvailable());
      assertTrue(paging.pagePreviousAvailable());
    }

    {
      final var items = paging.pagePrevious(audit);
      assertEquals(4, paging.pageNumber());
      assertEquals(5, paging.pageCount());
      assertEquals(100, items.size());
      checkPage(then, 400, 500, items);
      assertTrue(paging.pageNextAvailable());
      assertTrue(paging.pagePreviousAvailable());
    }

    {
      final var items = paging.pagePrevious(audit);
      assertEquals(3, paging.pageNumber());
      assertEquals(5, paging.pageCount());
      assertEquals(100, items.size());
      checkPage(then, 300, 400, items);
      assertTrue(paging.pageNextAvailable());
      assertTrue(paging.pagePreviousAvailable());
    }

    {
      final var items = paging.pagePrevious(audit);
      assertEquals(2, paging.pageNumber());
      assertEquals(5, paging.pageCount());
      assertEquals(100, items.size());
      checkPage(then, 200, 300, items);
      assertTrue(paging.pageNextAvailable());
      assertTrue(paging.pagePreviousAvailable());
    }

    {
      final var items = paging.pagePrevious(audit);
      assertEquals(1, paging.pageNumber());
      assertEquals(5, paging.pageCount());
      assertEquals(100, items.size());
      checkPage(then, 100, 200, items);
      assertTrue(paging.pageNextAvailable());
      assertTrue(paging.pagePreviousAvailable());
    }

    {
      final var items = paging.pagePrevious(audit);
      assertEquals(0, paging.pageNumber());
      assertEquals(5, paging.pageCount());
      assertEquals(100, items.size());
      checkPage(then, 0, 100, items);
      assertTrue(paging.pageNextAvailable());
      assertFalse(paging.pagePreviousAvailable());
    }
  }

  private static void checkPage(
    final OffsetDateTime time,
    final int indexLow,
    final int indexHigh,
    final List<IdAuditEvent> items)
  {
    final var timeCoarse = time.withNano(0);

    var index = indexLow;
    final var iter = items.iterator();
    while (index < indexHigh) {
      final var item =
        iter.next();
      final var itemTimeCoarse =
        item.time().withNano(0);

      assertEquals(timeCoarse.plusSeconds(index), itemTimeCoarse);
      assertEquals(String.format("ET_%04d", Integer.valueOf(index)), item.type());
      assertEquals(String.format("E_%04d", Integer.valueOf(index)), item.message());
      ++index;
    }
  }
}
