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

import com.io7m.idstore.database.api.IdDatabaseAdminSearchByEmailType;
import com.io7m.idstore.database.api.IdDatabaseAdminSearchType;
import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.postgres.internal.tables.records.AdminsRecord;
import com.io7m.idstore.database.postgres.internal.tables.records.EmailsRecord;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminColumnOrdering;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithms;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.jqpage.core.JQField;
import com.io7m.jqpage.core.JQKeysetRandomAccessPageDefinition;
import com.io7m.jqpage.core.JQKeysetRandomAccessPagination;
import com.io7m.jqpage.core.JQOrder;
import org.jooq.Condition;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.io7m.idstore.database.postgres.internal.IdDatabaseExceptions.handleDatabaseException;
import static com.io7m.idstore.database.postgres.internal.IdDatabaseUsersQueries.formatHosts;
import static com.io7m.idstore.database.postgres.internal.Tables.ADMINS;
import static com.io7m.idstore.database.postgres.internal.Tables.AUDIT;
import static com.io7m.idstore.database.postgres.internal.Tables.BANS;
import static com.io7m.idstore.database.postgres.internal.Tables.EMAILS;
import static com.io7m.idstore.database.postgres.internal.Tables.LOGIN_HISTORY;
import static com.io7m.idstore.database.postgres.internal.Tables.USER_IDS;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_EMAIL;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_ID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_ID_NAME;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NOT_INITIAL;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_ERROR;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHost;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHostProxied;
import static com.io7m.idstore.model.IdLoginMetadataStandard.userAgent;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_STATEMENT;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

final class IdDatabaseAdminsQueries
  extends IdBaseQueries
  implements IdDatabaseAdminsQueriesType
{
  static final Supplier<IdDatabaseException> ADMIN_DOES_NOT_EXIST = () -> {
    return new IdDatabaseException(
      "Admin does not exist",
      ADMIN_NONEXISTENT,
      Map.of(),
      Optional.empty()
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

  private static IdAdminPermissionSet permissionsDeserializeRecord(
    final AdminsRecord adminRecord)
  {
    return IdAdminPermissionSet.parse(adminRecord.getPermissions());
  }

  static String permissionsSerialize(
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
      PASSWORD_ERROR,
      exception.attributes(),
      exception.remediatingAction()
    );
  }

  private static JQField orderingToJQField(
    final IdAdminColumnOrdering ordering)
  {
    final var field =
      switch (ordering.column()) {
        case BY_ID -> ADMINS.ID;
        case BY_IDNAME -> ADMINS.ID_NAME;
        case BY_REALNAME -> ADMINS.REAL_NAME;
        case BY_TIME_CREATED -> ADMINS.TIME_CREATED;
        case BY_TIME_UPDATED -> ADMINS.TIME_UPDATED;
      };

    return new JQField(
      field,
      ordering.ascending() ? JQOrder.ASCENDING : JQOrder.DESCENDING
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

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminCreateInitial");

    try {
      final var existing =
        context.selectFrom(ADMINS)
          .limit(Integer.valueOf(1))
          .fetch();

      if (existing.isNotEmpty()) {
        throw new IdDatabaseException(
          "Admin already exists",
          ADMIN_NOT_INITIAL,
          Map.of(),
          Optional.empty()
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
          .set(ADMINS.PERMISSIONS, permissionString)
          .set(ADMINS.DELETING, FALSE)
          .set(ADMINS.INITIAL, TRUE);

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
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
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
    final var executor = transaction.adminId();

    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminCreate");

    try {
      {
        final var existing =
          context.fetchOptional(USER_IDS, USER_IDS.ID.eq(id));
        if (existing.isPresent()) {
          throw new IdDatabaseException(
            "Admin ID already exists",
            ADMIN_DUPLICATE_ID,
            Map.of("Admin ID", id.toString()),
            Optional.empty()
          );
        }
      }

      {
        final var existing =
          context.fetchOptional(ADMINS, ADMINS.ID_NAME.eq(idName.value()));
        if (existing.isPresent()) {
          throw new IdDatabaseException(
            "Admin ID name already exists",
            ADMIN_DUPLICATE_ID_NAME,
            Map.of("Admin Name", idName.value()),
            Optional.empty()
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
              ADMIN_DUPLICATE_EMAIL,
              Map.of("Email", email.value()),
              Optional.empty()
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
        .set(ADMINS.PERMISSIONS, permissionString)
        .set(ADMINS.DELETING, FALSE)
        .set(ADMINS.INITIAL, FALSE)
        .execute();

      context.insertInto(EMAILS)
        .set(EMAILS.EMAIL_ADDRESS, email.value())
        .set(EMAILS.ADMIN_ID, id)
        .execute();

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "ADMIN_CREATED")
        .set(AUDIT.USER_ID, executor)
        .set(AUDIT.MESSAGE, id.toString()).execute();

      return this.adminGet(id).orElseThrow();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public Optional<IdAdmin> adminGet(
    final UUID id)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminGet");

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
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } catch (final IdPasswordException e) {
      querySpan.recordException(e);
      throw handlePasswordException(e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public Optional<IdAdmin> adminGetForName(
    final IdName name)
    throws IdDatabaseException
  {
    Objects.requireNonNull(name, "name");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminGetForName");

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
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } catch (final IdPasswordException e) {
      querySpan.recordException(e);
      throw handlePasswordException(e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public IdAdmin adminGetForNameRequire(
    final IdName name)
    throws IdDatabaseException
  {
    return this.adminGetForName(name)
      .orElseThrow(ADMIN_DOES_NOT_EXIST);
  }

  @Override
  public Optional<IdAdmin> adminGetForEmail(
    final IdEmail email)
    throws IdDatabaseException
  {
    Objects.requireNonNull(email, "email");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminGetForEmail");

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
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void adminLogin(
    final UUID id,
    final Map<String, String> metadata)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(metadata, "metadata");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminLogin");

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
        .set(LOGIN_HISTORY.AGENT, metadata.getOrDefault(userAgent(), ""))
        .set(LOGIN_HISTORY.HOST, metadata.getOrDefault(remoteHost(), ""))
        .set(
          LOGIN_HISTORY.PROXIED_HOST,
          metadata.getOrDefault(remoteHostProxied(), ""))
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
          .set(AUDIT.MESSAGE, formatHosts(metadata));

      audit.execute();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public IdDatabaseAdminSearchType adminSearch(
    final IdAdminSearchParameters parameters)
    throws IdDatabaseException
  {
    Objects.requireNonNull(parameters, "parameters");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseAdminsQueries.adminSearch.create");

    try {

      /*
       * The admins must lie within the given time ranges.
       */

      final var timeCreatedRange = parameters.timeCreatedRange();
      final var timeCreatedCondition =
        DSL.condition(
          ADMINS.TIME_CREATED.ge(timeCreatedRange.timeLower())
            .and(ADMINS.TIME_CREATED.le(timeCreatedRange.timeUpper()))
        );

      final var timeUpdatedRange = parameters.timeUpdatedRange();
      final var timeUpdatedCondition =
        DSL.condition(
          ADMINS.TIME_UPDATED.ge(timeUpdatedRange.timeLower())
            .and(ADMINS.TIME_UPDATED.le(timeUpdatedRange.timeUpper()))
        );

      /*
       * A search query might be present.
       */

      final Condition searchCondition;
      final var search = parameters.search();
      if (search.isPresent()) {
        final var searchText = "%%%s%%".formatted(search.get());
        searchCondition =
          DSL.condition(ADMINS.ID_NAME.likeIgnoreCase(searchText))
            .or(DSL.condition(ADMINS.REAL_NAME.likeIgnoreCase(searchText)))
            .or(DSL.condition(ADMINS.ID.likeIgnoreCase(searchText)));
      } else {
        searchCondition = DSL.trueCondition();
      }

      final var allConditions =
        timeCreatedCondition
          .and(timeUpdatedCondition)
          .and(searchCondition);

      final var orderField =
        orderingToJQField(parameters.ordering());

      final var pages =
        JQKeysetRandomAccessPagination.createPageDefinitions(
          context,
          ADMINS,
          List.of(orderField),
          List.of(allConditions),
          List.of(),
          Integer.toUnsignedLong(parameters.limit()),
          statement -> {
            querySpan.setAttribute(DB_STATEMENT, statement.toString());
          }
        );

      return new AdminsSearch(pages);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public IdAdmin adminGetRequire(
    final UUID id)
    throws IdDatabaseException
  {
    return this.adminGet(id).orElseThrow(ADMIN_DOES_NOT_EXIST);
  }

  @Override
  public void adminUpdate(
    final UUID id,
    final Optional<IdName> withIdName,
    final Optional<IdRealName> withRealName,
    final Optional<IdPassword> withPassword,
    final Optional<Set<IdAdminPermission>> withPermissions)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(withIdName, "withIdName");
    Objects.requireNonNull(withRealName, "withRealName");
    Objects.requireNonNull(withPassword, "withPassword");
    Objects.requireNonNull(withPermissions, "withPermissions");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var executor = transaction.adminId();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminUpdate");

    try {
      final var record = context.fetchOne(ADMINS, ADMINS.ID.eq(id));
      if (record == null) {
        throw ADMIN_DOES_NOT_EXIST.get();
      }

      if (withIdName.isPresent()) {
        final var name = withIdName.get();
        record.setIdName(name.value());

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "ADMIN_CHANGED_ID_NAME")
          .set(AUDIT.USER_ID, executor)
          .set(AUDIT.MESSAGE, "%s|%s".formatted(id.toString(), name.value()))
          .execute();
      }

      if (withRealName.isPresent()) {
        final var name = withRealName.get();
        record.setRealName(name.value());

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "ADMIN_CHANGED_REAL_NAME")
          .set(AUDIT.USER_ID, executor)
          .set(AUDIT.MESSAGE, "%s|%s".formatted(id.toString(), name.value()))
          .execute();
      }

      if (withPassword.isPresent()) {
        final var pass = withPassword.get();
        record.setPasswordAlgo(pass.algorithm().identifier());
        record.setPasswordHash(pass.hash());
        record.setPasswordSalt(pass.salt());

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "ADMIN_CHANGED_PASSWORD")
          .set(AUDIT.USER_ID, executor)
          .set(AUDIT.MESSAGE, id.toString())
          .execute();
      }

      if (withPermissions.isPresent()) {
        final var permissions =
          withPermissions.get();
        final var permissionString =
          permissionsSerialize(permissions);

        record.setPermissions(permissionString);

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "ADMIN_CHANGED_PERMISSIONS")
          .set(AUDIT.USER_ID, executor)
          .set(AUDIT.MESSAGE, "%s|%s".formatted(id, permissionString))
          .execute();
      }

      record.store();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void adminEmailAdd(
    final UUID id,
    final IdEmail email)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(email, "email");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var executor = transaction.adminId();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminEmailAdd");

    try {
      context.fetchOptional(ADMINS, ADMINS.ID.eq(id))
        .orElseThrow(ADMIN_DOES_NOT_EXIST);

      context.insertInto(EMAILS)
        .set(EMAILS.ADMIN_ID, id)
        .set(EMAILS.EMAIL_ADDRESS, email.value())
        .execute();

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "ADMIN_EMAIL_ADDED")
        .set(AUDIT.USER_ID, executor)
        .set(AUDIT.MESSAGE, "%s|%s".formatted(id, email.value()))
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void adminEmailRemove(
    final UUID id,
    final IdEmail email)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(email, "email");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var executor = transaction.adminId();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminEmailRemove");

    try {
      context.fetchOptional(ADMINS, ADMINS.ID.eq(id))
        .orElseThrow(ADMIN_DOES_NOT_EXIST);

      final var existing =
        context.fetchOptional(
          EMAILS,
          EMAILS.ADMIN_ID.eq(id)
            .and(EMAILS.EMAIL_ADDRESS.equalIgnoreCase(email.value()))
        );

      if (existing.isEmpty()) {
        return;
      }

      /*
       * There is a database trigger that prevents the last email address
       * being removed from the account, so we don't perform any check here.
       */

      context.deleteFrom(EMAILS)
        .where(EMAILS.ADMIN_ID.eq(id)
                 .and(EMAILS.EMAIL_ADDRESS.equalIgnoreCase(email.value())))
        .execute();

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "ADMIN_EMAIL_REMOVED")
        .set(AUDIT.USER_ID, executor)
        .set(AUDIT.MESSAGE, "%s|%s".formatted(id, email.value()))
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void adminDelete(
    final UUID id)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var executor = transaction.adminId();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminDelete");

    try {
      final var admin = this.adminGetRequire(id);

      context.update(ADMINS)
        .set(ADMINS.DELETING, TRUE)
        .where(ADMINS.ID.eq(id))
        .execute();

      for (final var email : admin.emails()) {
        this.adminEmailRemove(id, email);
      }

      context.deleteFrom(ADMINS)
        .where(ADMINS.ID.eq(id))
        .execute();

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "ADMIN_DELETED")
        .set(AUDIT.USER_ID, executor)
        .set(AUDIT.MESSAGE, id.toString())
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void adminBanCreate(
    final IdBan ban)
    throws IdDatabaseException
  {
    Objects.requireNonNull(ban, "ban");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var executor = transaction.adminId();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminBanCreate");

    try {
      final var user =
        this.adminGetRequire(ban.user());
      var banRecord =
        context.fetchOne(BANS, BANS.USER_ID.eq(user.id()));

      if (banRecord == null) {
        banRecord = context.newRecord(BANS);
      }

      banRecord.set(BANS.USER_ID, user.id());
      banRecord.set(BANS.EXPIRES, ban.expires().orElse(null));
      banRecord.set(BANS.REASON, ban.reason());
      banRecord.store();

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "ADMIN_BANNED")
        .set(AUDIT.USER_ID, executor)
        .set(AUDIT.MESSAGE, user.id().toString())
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public Optional<IdBan> adminBanGet(
    final UUID id)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminBanGet");

    try {
      final var user =
        this.adminGetRequire(id);
      final var banRecord =
        context.fetchOne(BANS, BANS.USER_ID.eq(user.id()));

      if (banRecord == null) {
        return Optional.empty();
      }

      return Optional.of(
        new IdBan(
          banRecord.getUserId(),
          banRecord.getReason(),
          Optional.ofNullable(banRecord.getExpires())
        )
      );
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void adminBanDelete(
    final IdBan ban)
    throws IdDatabaseException
  {
    Objects.requireNonNull(ban, "ban");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var executor = transaction.adminId();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseAdminsQueries.adminBanDelete");

    try {
      final var user =
        this.adminGetRequire(ban.user());
      final var banRecord =
        context.fetchOne(BANS, BANS.USER_ID.eq(user.id()));

      if (banRecord == null) {
        return;
      }

      banRecord.delete();

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "ADMIN_BAN_REMOVED")
        .set(AUDIT.USER_ID, executor)
        .set(AUDIT.MESSAGE, user.id().toString())
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public IdDatabaseAdminSearchByEmailType adminSearchByEmail(
    final IdAdminSearchByEmailParameters parameters)
    throws IdDatabaseException
  {
    Objects.requireNonNull(parameters, "parameters");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseAdminsQueries.adminSearchByEmail.create");

    try {
      final var baseTable =
        ADMINS.join(EMAILS)
          .on(ADMINS.ID.eq(EMAILS.ADMIN_ID));

      /*
       * The admins must lie within the given time ranges.
       */

      final var timeCreatedRange = parameters.timeCreatedRange();
      final var timeCreatedCondition =
        DSL.condition(
          ADMINS.TIME_CREATED.ge(timeCreatedRange.timeLower())
            .and(ADMINS.TIME_CREATED.le(timeCreatedRange.timeUpper()))
        );

      final var timeUpdatedRange = parameters.timeUpdatedRange();
      final var timeUpdatedCondition =
        DSL.condition(
          ADMINS.TIME_UPDATED.ge(timeUpdatedRange.timeLower())
            .and(ADMINS.TIME_UPDATED.le(timeUpdatedRange.timeUpper()))
        );

      /*
       * Only admins with matching email addresses will be returned.
       */

      final var searchLike =
        "%%%s%%".formatted(parameters.search());
      final var searchCondition =
        DSL.condition(EMAILS.EMAIL_ADDRESS.likeIgnoreCase(searchLike));

      final var allConditions =
        timeCreatedCondition
          .and(timeUpdatedCondition)
          .and(searchCondition);

      final var orderField =
        orderingToJQField(parameters.ordering());

      final var pages =
        JQKeysetRandomAccessPagination.createPageDefinitions(
          context,
          baseTable,
          List.of(orderField),
          List.of(allConditions),
          List.of(ADMINS.ID),
          Integer.toUnsignedLong(parameters.limit()),
          statement -> {
            querySpan.setAttribute(DB_STATEMENT, statement.toString());
          }
        );

      return new AdminsByEmailSearch(pages);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e);
    } finally {
      querySpan.end();
    }
  }

  private static final class AdminsByEmailSearch
    extends IdAbstractSearch<
    IdDatabaseAdminsQueries,
    IdDatabaseAdminsQueriesType,
    IdAdminSummary>
    implements IdDatabaseAdminSearchByEmailType
  {
    AdminsByEmailSearch(
      final List<JQKeysetRandomAccessPageDefinition> inPages)
    {
      super(inPages);
    }

    @Override
    protected IdPage<IdAdminSummary> page(
      final IdDatabaseAdminsQueries queries,
      final JQKeysetRandomAccessPageDefinition page)
      throws IdDatabaseException
    {
      final var transaction =
        queries.transaction();
      final var context =
        transaction.createContext();

      final var querySpan =
        transaction.createQuerySpan(
          "IdDatabaseAdminsQueries.adminSearchByEmail.page");

      try {
        final var query =
          page.queryFields(context, List.of(
            ADMINS.ID,
            ADMINS.ID_NAME,
            ADMINS.REAL_NAME,
            ADMINS.TIME_CREATED,
            ADMINS.TIME_UPDATED
          ));

        querySpan.setAttribute(DB_STATEMENT, query.toString());

        final var items =
          query.fetch().map(record -> {
            return new IdAdminSummary(
              record.get(ADMINS.ID),
              new IdName(record.get(ADMINS.ID_NAME)),
              new IdRealName(record.get(ADMINS.REAL_NAME)),
              record.get(ADMINS.TIME_CREATED),
              record.get(ADMINS.TIME_UPDATED)
            );
          });

        return new IdPage<>(
          items,
          (int) page.index(),
          this.pageCount(),
          page.firstOffset()
        );
      } catch (final DataAccessException e) {
        querySpan.recordException(e);
        throw handleDatabaseException(transaction, e);
      } finally {
        querySpan.end();
      }
    }
  }

  private static final class AdminsSearch
    extends IdAbstractSearch<
    IdDatabaseAdminsQueries,
    IdDatabaseAdminsQueriesType,
    IdAdminSummary>
    implements IdDatabaseAdminSearchType
  {
    AdminsSearch(
      final List<JQKeysetRandomAccessPageDefinition> inPages)
    {
      super(inPages);
    }

    @Override
    protected IdPage<IdAdminSummary> page(
      final IdDatabaseAdminsQueries queries,
      final JQKeysetRandomAccessPageDefinition page)
      throws IdDatabaseException
    {
      final var transaction =
        queries.transaction();
      final var context =
        transaction.createContext();

      final var querySpan =
        transaction.createQuerySpan(
          "IdDatabaseAdminsQueries.adminSearch.page");

      try {
        final var query =
          page.queryFields(context, List.of(
            ADMINS.ID,
            ADMINS.ID_NAME,
            ADMINS.REAL_NAME,
            ADMINS.TIME_CREATED,
            ADMINS.TIME_UPDATED
          ));

        querySpan.setAttribute(DB_STATEMENT, query.toString());

        final var items =
          query.fetch().map(record -> {
            return new IdAdminSummary(
              record.get(ADMINS.ID),
              new IdName(record.get(ADMINS.ID_NAME)),
              new IdRealName(record.get(ADMINS.REAL_NAME)),
              record.get(ADMINS.TIME_CREATED),
              record.get(ADMINS.TIME_UPDATED)
            );
          });

        return new IdPage<>(
          items,
          (int) page.index(),
          this.pageCount(),
          page.firstOffset()
        );
      } catch (final DataAccessException e) {
        querySpan.recordException(e);
        throw handleDatabaseException(transaction, e);
      } finally {
        querySpan.end();
      }
    }
  }
}
