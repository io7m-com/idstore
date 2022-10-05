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

import com.io7m.idstore.tests.arbitraries.IdArbAMessageProvider;
import com.io7m.idstore.tests.arbitraries.IdArbAdminOrderingProvider;
import com.io7m.idstore.tests.arbitraries.IdArbAdminPermissionSetProvider;
import com.io7m.idstore.tests.arbitraries.IdArbAdminProvider;
import com.io7m.idstore.tests.arbitraries.IdArbAdminSearchByEmailParametersProvider;
import com.io7m.idstore.tests.arbitraries.IdArbAdminSearchParametersProvider;
import com.io7m.idstore.tests.arbitraries.IdArbAdminSummaryProvider;
import com.io7m.idstore.tests.arbitraries.IdArbAuditEventProvider;
import com.io7m.idstore.tests.arbitraries.IdArbAuditSearchParametersProvider;
import com.io7m.idstore.tests.arbitraries.IdArbBanProvider;
import com.io7m.idstore.tests.arbitraries.IdArbEmailProvider;
import com.io7m.idstore.tests.arbitraries.IdArbHashProvider;
import com.io7m.idstore.tests.arbitraries.IdArbIdNameProvider;
import com.io7m.idstore.tests.arbitraries.IdArbInetAddressProvider;
import com.io7m.idstore.tests.arbitraries.IdArbLoginProvider;
import com.io7m.idstore.tests.arbitraries.IdArbOffsetDateTimeProvider;
import com.io7m.idstore.tests.arbitraries.IdArbPasswordProvider;
import com.io7m.idstore.tests.arbitraries.IdArbRealNameProvider;
import com.io7m.idstore.tests.arbitraries.IdArbSubsetMatchProvider;
import com.io7m.idstore.tests.arbitraries.IdArbTimeRangeProvider;
import com.io7m.idstore.tests.arbitraries.IdArbTokenProvider;
import com.io7m.idstore.tests.arbitraries.IdArbUMessageProvider;
import com.io7m.idstore.tests.arbitraries.IdArbURIProvider;
import com.io7m.idstore.tests.arbitraries.IdArbUUIDProvider;
import com.io7m.idstore.tests.arbitraries.IdArbUserOrderingProvider;
import com.io7m.idstore.tests.arbitraries.IdArbUserProvider;
import com.io7m.idstore.tests.arbitraries.IdArbUserSearchByEmailParametersProvider;
import com.io7m.idstore.tests.arbitraries.IdArbUserSearchParametersProvider;
import com.io7m.idstore.tests.arbitraries.IdArbUserSummaryProvider;
import net.jqwik.api.providers.ArbitraryProvider;

/**
 * Identity server (Arbitrary instances)
 */

module com.io7m.idstore.tests.arbitraries
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires transitive com.io7m.idstore.model;
  requires transitive com.io7m.idstore.protocol.admin;
  requires transitive com.io7m.idstore.protocol.api;
  requires transitive com.io7m.idstore.protocol.user;

  requires transitive net.jqwik.api;

  provides ArbitraryProvider
    with
      IdArbAMessageProvider,
      IdArbAdminOrderingProvider,
      IdArbAdminPermissionSetProvider,
      IdArbAdminProvider,
      IdArbAdminSearchByEmailParametersProvider,
      IdArbAdminSearchParametersProvider,
      IdArbAdminSummaryProvider,
      IdArbAuditEventProvider,
      IdArbAuditSearchParametersProvider,
      IdArbBanProvider,
      IdArbEmailProvider,
      IdArbHashProvider,
      IdArbIdNameProvider,
      IdArbInetAddressProvider,
      IdArbLoginProvider,
      IdArbOffsetDateTimeProvider,
      IdArbPasswordProvider,
      IdArbRealNameProvider,
      IdArbSubsetMatchProvider,
      IdArbTimeRangeProvider,
      IdArbTokenProvider,
      IdArbUMessageProvider,
      IdArbURIProvider,
      IdArbUUIDProvider,
      IdArbUserOrderingProvider,
      IdArbUserProvider,
      IdArbUserSearchByEmailParametersProvider,
      IdArbUserSearchParametersProvider,
      IdArbUserSummaryProvider
    ;

  exports com.io7m.idstore.tests.arbitraries;
}
