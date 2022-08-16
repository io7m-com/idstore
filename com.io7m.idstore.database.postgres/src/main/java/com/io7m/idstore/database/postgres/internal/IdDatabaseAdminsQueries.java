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

package com.io7m.idstore.database.postgres.internal;

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.postgres.internal.tables.records.AdminsRecord;
import com.io7m.idstore.database.postgres.internal.tables.records.EmailsRecord;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithms;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.io7m.idstore.database.postgres.internal.IdDatabaseExceptions.handleDatabaseException;
import static com.io7m.idstore.database.postgres.internal.Tables.ADMINS;
import static com.io7m.idstore.database.postgres.internal.Tables.AUDIT;
import static com.io7m.idstore.database.postgres.internal.Tables.EMAILS;
import static com.io7m.idstore.database.postgres.internal.Tables.LOGIN_HISTORY;
import static com.io7m.idstore.database.postgres.internal.Tables.USER_IDS;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_EMAIL;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_ID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_ID_NAME;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NOT_INITIAL;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_ERROR;

final class IdDatabaseAdminsQueries
  extends IdBaseQueries
  implements IdDatabaseAdminsQueriesType
{
  static final Supplier<IdDatabaseException> ADMIN_DOES_NOT_EXIST = () -> {
    return new IdDatabaseException(
      "Admin does not exist",
      ADMIN_NONEXISTENT
    );
  };

  IdDatabaseAdminsQueries(
    final IdDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  private static IdAdmin adminMap(
    final AdminsRecord adminRecord,
    final Result<EmailsRecord> emails)
    throws IdPasswordException
  {
    return new IdAdmin(
      adminRecord.getId(),
      new IdName(adminRecord.getIdName()),
      new IdRealName(adminRecord.getRealName()),
      IdNonEmptyList.ofList(
        emails.stream()
          .map(e -> new IdEmail(e.getEmailAddress()))
          .toList()
      ),
      adminRecord.getTimeCreated(),
      adminRecord.getTimeUpdated(),
      new IdPassword(
        IdPasswordAlgorithms.parse(adminRecord.getPasswordAlgo()),
        adminRecord.getPasswordHash().toUpperCase(Locale.ROOT),
        adminRecord.getPasswordSalt().toUpperCase(Locale.ROOT)
      ),
      permissionsDeserializeRecord(adminRecord)
    );
  }

  private static Set<IdAdminPermission> permissionsDeserializeRecord(
    final AdminsRecord adminRecord)
  {
    return permissionsDeserialize(adminRecord.getPermissions());
  }

  private static Set<IdAdminPermission> permissionsDeserialize(
    final String str)
  {
    return Arrays.stream(str.split(","))
      .filter(s -> !s.isBlank())
      .map(IdAdminPermission::valueOf)
      .collect(Collectors.toUnmodifiableSet());
  }

  private static String permissionsSerialize(
    final Set<IdAdminPermission> permissions)
  {
    return permissions.stream()
      .map(Enum::toString)
      .sorted()
      .collect(Collectors.joining(","));
  }

  private static IdDatabaseException handlePasswordException(
    final IdPasswordException exception)
  {
    return new IdDatabaseException(
      exception.getMessage(),
      exception,
      PASSWORD_ERROR
    );
  }

  @Override
  public IdAdmin adminCreateInitial(
    final UUID id,
    final IdName idName,
    final IdRealName realName,
    final IdEmail email,
    final OffsetDateTime created,
    final IdPassword password)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(idName, "idName");
    Objects.requireNonNull(realName, "realName");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(password, "password");

    final var context =
      this.transaction().createContext();

    try {
      final var existing =
        context.selectFrom(ADMINS)
          .limit(Integer.valueOf(1))
          .fetch();

      if (existing.isNotEmpty()) {
        throw new IdDatabaseException(
          "Admin already exists",
          ADMIN_NOT_INITIAL
        );
      }

      final var idCreate =
        context.insertInto(USER_IDS)
          .set(USER_IDS.ID, id);

      idCreate.execute();

      final var permissionString =
        permissionsSerialize(EnumSet.allOf(IdAdminPermission.class));

      final var adminCreate =
        context.insertInto(ADMINS)
          .set(ADMINS.ID, id)
          .set(ADMINS.ID_NAME, idName.value())
          .set(ADMINS.REAL_NAME, realName.value())
          .set(ADMINS.TIME_CREATED, created)
          .set(ADMINS.TIME_UPDATED, created)
          .set(ADMINS.PASSWORD_ALGO, password.algorithm().identifier())
          .set(ADMINS.PASSWORD_HASH, password.hash())
          .set(ADMINS.PASSWORD_SALT, password.salt())
          .set(ADMINS.PERMISSIONS, permissionString);

      adminCreate.execute();

      context.insertInto(EMAILS)
        .set(EMAILS.EMAIL_ADDRESS, email.value())
        .set(EMAILS.ADMIN_ID, id)
        .execute();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "ADMIN_CREATED")
          .set(AUDIT.USER_ID, id)
          .set(AUDIT.MESSAGE, id.toString());

      audit.execute();
      return this.adminGet(id).orElseThrow();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public IdAdmin adminCreate(
    final UUID id,
    final IdName idName,
    final IdRealName realName,
    final IdEmail email,
    final OffsetDateTime created,
    final IdPassword password,
    final Set<IdAdminPermission> permissions)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(idName, "idName");
    Objects.requireNonNull(realName, "realName");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(permissions, "permissions");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var adminId = transaction.adminId();

    try {
      {
        final var existing =
          context.fetchOptional(USER_IDS, USER_IDS.ID.eq(id));
        if (existing.isPresent()) {
          throw new IdDatabaseException(
            "Admin ID already exists",
            ADMIN_DUPLICATE_ID
          );
        }
      }

      {
        final var existing =
          context.fetchOptional(ADMINS, ADMINS.ID_NAME.eq(idName.value()));
        if (existing.isPresent()) {
          throw new IdDatabaseException(
            "Admin ID name already exists",
            ADMIN_DUPLICATE_ID_NAME
          );
        }
      }

      {
        final var emails =
          this.transaction().queries(IdDatabaseEmailsQueriesType.class);
        final var existingOpt =
          emails.emailExists(email);

        if (existingOpt.isPresent()) {
          final var existing = existingOpt.get();
          if (existing.isAdmin()) {
            throw new IdDatabaseException(
              "Email already exists",
              ADMIN_DUPLICATE_EMAIL
            );
          }
        }
      }

      final var permissionString =
        permissionsSerialize(permissions);

      context.insertInto(USER_IDS)
        .set(USER_IDS.ID, id).execute();

      context.insertInto(ADMINS)
        .set(ADMINS.ID, id)
        .set(ADMINS.ID_NAME, idName.value())
        .set(ADMINS.REAL_NAME, realName.value())
        .set(ADMINS.TIME_CREATED, created)
        .set(ADMINS.TIME_UPDATED, created)
        .set(ADMINS.PASSWORD_ALGO, password.algorithm().identifier())
        .set(ADMINS.PASSWORD_HASH, password.hash())
        .set(ADMINS.PASSWORD_SALT, password.salt())
        .set(ADMINS.PERMISSIONS, permissionString).execute();

      context.insertInto(EMAILS)
        .set(EMAILS.EMAIL_ADDRESS, email.value())
        .set(EMAILS.ADMIN_ID, id)
        .execute();

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "ADMIN_CREATED")
        .set(AUDIT.USER_ID, adminId)
        .set(AUDIT.MESSAGE, id.toString()).execute();

      return this.adminGet(id).orElseThrow();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public Optional<IdAdmin> adminGet(
    final UUID id)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var context = this.transaction().createContext();
    try {
      final var adminRecordOpt =
        context.selectFrom(ADMINS)
          .where(ADMINS.ID.eq(id))
          .fetchOptional();

      if (adminRecordOpt.isEmpty()) {
        return Optional.empty();
      }

      final var adminRecord =
        adminRecordOpt.get();

      final var emails =
        context.selectFrom(EMAILS)
          .where(EMAILS.ADMIN_ID.eq(adminRecord.getId()))
          .fetch();

      return Optional.of(adminMap(adminRecord, emails));
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    } catch (final IdPasswordException e) {
      throw handlePasswordException(e);
    }
  }

  @Override
  public Optional<IdAdmin> adminGetForName(
    final IdName name)
    throws IdDatabaseException
  {
    Objects.requireNonNull(name, "name");

    final var context = this.transaction().createContext();
    try {
      final var adminRecordOpt =
        context.selectFrom(ADMINS)
          .where(ADMINS.ID_NAME.eq(name.value()))
          .fetchOptional();

      if (adminRecordOpt.isEmpty()) {
        return Optional.empty();
      }

      final var adminRecord =
        adminRecordOpt.get();

      final var emails =
        context.selectFrom(EMAILS)
          .where(EMAILS.ADMIN_ID.eq(adminRecord.getId()))
          .fetch();

      return Optional.of(adminMap(adminRecord, emails));
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    } catch (final IdPasswordException e) {
      throw handlePasswordException(e);
    }
  }

  @Override
  public Optional<IdAdmin> adminGetForEmail(
    final IdEmail email)
    throws IdDatabaseException
  {
    Objects.requireNonNull(email, "email");

    final var context = this.transaction().createContext();
    try {
      final var emailOpt =
        context.selectFrom(EMAILS)
          .where(EMAILS.EMAIL_ADDRESS.eq(email.value()))
          .fetchOptional();

      if (emailOpt.isEmpty()) {
        return Optional.empty();
      }

      final var emailRecord = emailOpt.get();
      if (emailRecord.getAdminId() == null) {
        return Optional.empty();
      }

      return this.adminGet(emailRecord.getAdminId());
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public void adminLogin(
    final UUID id,
    final String userAgent,
    final String host)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userAgent, "userAgent");
    Objects.requireNonNull(host, "host");

    final var context =
      this.transaction().createContext();

    try {
      final var time = this.currentTime();

      context.fetchOptional(ADMINS, ADMINS.ID.eq(id))
        .orElseThrow(ADMIN_DOES_NOT_EXIST);

      /*
       * Record the login.
       */

      context.insertInto(LOGIN_HISTORY)
        .set(LOGIN_HISTORY.USER_ID, id)
        .set(LOGIN_HISTORY.TIME, this.currentTime())
        .set(LOGIN_HISTORY.AGENT, userAgent)
        .set(LOGIN_HISTORY.HOST, host)
        .execute();

      /*
       * The audit event is considered confidential because IP addresses
       * are tentatively considered confidential.
       */

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, "ADMIN_LOGGED_IN")
          .set(AUDIT.USER_ID, id)
          .set(AUDIT.MESSAGE, host);

      audit.execute();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public List<IdAdminSummary> adminSearch(
    final String query)
    throws IdDatabaseException
  {
    Objects.requireNonNull(query, "query");

    final var context =
      this.transaction().createContext();

    try {
      final var wildcardQuery =
        "%%%s%%".formatted(query);

      final var records =
        context.selectFrom(ADMINS)
          .where(ADMINS.ID_NAME.likeIgnoreCase(wildcardQuery))
          .or(ADMINS.REAL_NAME.likeIgnoreCase(wildcardQuery))
          .or(ADMINS.ID.likeIgnoreCase(wildcardQuery))
          .orderBy(ADMINS.REAL_NAME)
          .fetch();

      final var summaries = new ArrayList<IdAdminSummary>(records.size());
      for (final var record : records) {
        summaries.add(
          new IdAdminSummary(
            record.get(ADMINS.ID),
            new IdName(record.get(ADMINS.ID_NAME)),
            new IdRealName(record.get(ADMINS.REAL_NAME))
          )
        );
      }
      return List.copyOf(summaries);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public IdAdmin adminGetRequire(
    final UUID id)
    throws IdDatabaseException
  {
    return this.adminGet(id).orElseThrow(ADMIN_DOES_NOT_EXIST);
  }
}
