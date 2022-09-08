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

import com.io7m.idstore.tests.arbitraries.IdArbA1MessageProvider;
import com.io7m.idstore.tests.arbitraries.IdArbA1UserColumnOrderingProvider;
import com.io7m.idstore.tests.arbitraries.IdArbA1UserOrderingProvider;
import com.io7m.idstore.tests.arbitraries.IdArbAdminProvider;
import com.io7m.idstore.tests.arbitraries.IdArbAuditEventProvider;
import com.io7m.idstore.tests.arbitraries.IdArbHashProvider;
import com.io7m.idstore.tests.arbitraries.IdArbIdA1AdminProvider;
import com.io7m.idstore.tests.arbitraries.IdArbIdA1TimeRangeProvider;
import com.io7m.idstore.tests.arbitraries.IdArbIdA1UserListParametersProvider;
import com.io7m.idstore.tests.arbitraries.IdArbIdU1PasswordProvider;
import com.io7m.idstore.tests.arbitraries.IdArbIdU1UserProvider;
import com.io7m.idstore.tests.arbitraries.IdArbIdA1PasswordProvider;
import com.io7m.idstore.tests.arbitraries.IdArbIdA1UserProvider;
import com.io7m.idstore.tests.arbitraries.IdArbIdNameProvider;
import com.io7m.idstore.tests.arbitraries.IdArbIdVMessagesProvider;
import com.io7m.idstore.tests.arbitraries.IdArbIdVProtocolSupportedProvider;
import com.io7m.idstore.tests.arbitraries.IdArbOffsetDateTimeProvider;
import com.io7m.idstore.tests.arbitraries.IdArbPasswordProvider;
import com.io7m.idstore.tests.arbitraries.IdArbSubsetMatchProvider;
import com.io7m.idstore.tests.arbitraries.IdArbTokenProvider;
import com.io7m.idstore.tests.arbitraries.IdArbU1MessageProvider;
import com.io7m.idstore.tests.arbitraries.IdArbURIProvider;
import com.io7m.idstore.tests.arbitraries.IdArbUUIDProvider;
import com.io7m.idstore.tests.arbitraries.IdArbRealNameProvider;
import com.io7m.idstore.tests.arbitraries.IdArbEmailProvider;
import com.io7m.idstore.tests.arbitraries.IdArbUserProvider;
import com.io7m.idstore.tests.arbitraries.IdArbUserSummaryProvider;
import net.jqwik.api.providers.ArbitraryProvider;

/**
 * Identity server (Arbitrary instances)
 */

module com.io7m.idstore.tests.arbitraries
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires transitive com.io7m.idstore.protocol.api;
  requires transitive com.io7m.idstore.protocol.user_v1;
  requires transitive com.io7m.idstore.protocol.admin_v1;
  requires transitive com.io7m.idstore.protocol.versions;
  requires transitive com.io7m.idstore.model;
  requires transitive net.jqwik.api;

  provides ArbitraryProvider
    with
      IdArbA1MessageProvider,
      IdArbAdminProvider,
      IdArbAuditEventProvider,
      IdArbEmailProvider,
      IdArbHashProvider,
      IdArbIdA1AdminProvider,
      IdArbIdA1PasswordProvider,
      IdArbIdA1TimeRangeProvider,
      IdArbIdA1UserListParametersProvider,
      IdArbIdA1UserProvider,
      IdArbIdNameProvider,
      IdArbIdU1PasswordProvider,
      IdArbIdU1UserProvider,
      IdArbIdVMessagesProvider,
      IdArbIdVProtocolSupportedProvider,
      IdArbOffsetDateTimeProvider,
      IdArbPasswordProvider,
      IdArbRealNameProvider,
      IdArbSubsetMatchProvider,
      IdArbTokenProvider,
      IdArbU1MessageProvider,
      IdArbURIProvider,
      IdArbUUIDProvider,
      IdArbUserProvider,
      IdArbUserSummaryProvider,
      IdArbA1UserOrderingProvider,
      IdArbA1UserColumnOrderingProvider
    ;

  exports com.io7m.idstore.tests.arbitraries;
}
