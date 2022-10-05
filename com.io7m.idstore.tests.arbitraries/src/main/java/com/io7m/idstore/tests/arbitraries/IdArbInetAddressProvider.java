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


package com.io7m.idstore.tests.arbitraries;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.TypeUsage;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * A provider of InetAddress values.
 */

public final class IdArbInetAddressProvider extends IdArbAbstractProvider
{
  /**
   * A provider of InetAddress values.
   */

  public IdArbInetAddressProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(InetAddress.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Set.of(ipv4(), ipv6());
  }

  private static Arbitrary<InetAddress> ipv4()
  {
    return Arbitraries.bytes()
      .list()
      .ofSize(4)
      .map(bytes -> {
        try {
          return Inet4Address.getByAddress(
            new byte[]{
              bytes.get(0).byteValue(),
              bytes.get(1).byteValue(),
              bytes.get(2).byteValue(),
              bytes.get(3).byteValue(),
            }
          );
        } catch (final UnknownHostException e) {
          throw new IllegalStateException(e);
        }
      });
  }

  private static Arbitrary<InetAddress> ipv6()
  {
    return Arbitraries.bytes()
      .list()
      .ofSize(16)
      .map(bytes -> {
        try {
          return Inet6Address.getByAddress(
            new byte[]{
              bytes.get(0).byteValue(),
              bytes.get(1).byteValue(),
              bytes.get(2).byteValue(),
              bytes.get(3).byteValue(),

              bytes.get(4).byteValue(),
              bytes.get(5).byteValue(),
              bytes.get(6).byteValue(),
              bytes.get(7).byteValue(),

              bytes.get(8).byteValue(),
              bytes.get(9).byteValue(),
              bytes.get(10).byteValue(),
              bytes.get(11).byteValue(),

              bytes.get(12).byteValue(),
              bytes.get(13).byteValue(),
              bytes.get(14).byteValue(),
              bytes.get(15).byteValue(),
            }
          );
        } catch (final UnknownHostException e) {
          throw new IllegalStateException(e);
        }
      });
  }
}
