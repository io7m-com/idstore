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

import com.io7m.idstore.protocol.api.IdProtocolFromModel;
import com.io7m.idstore.protocol.api.IdProtocolToModel;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailAddBegin;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailAddDeny;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailRemovePermit;
import com.io7m.idstore.protocol.user_v1.IdU1CommandLogin;
import com.io7m.idstore.protocol.user_v1.IdU1CommandType;
import com.io7m.idstore.protocol.user_v1.IdU1CommandUserSelf;
import com.io7m.idstore.protocol.user_v1.IdU1IdTypeResolver;
import com.io7m.idstore.protocol.user_v1.IdU1MessageType;
import com.io7m.idstore.protocol.user_v1.IdU1Messages;
import com.io7m.idstore.protocol.user_v1.IdU1Password;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseEmailAddBegin;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseEmailRemoveDeny;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseEmailRemovePermit;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseError;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseLogin;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseType;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseUserSelf;
import com.io7m.idstore.protocol.user_v1.IdU1User;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.CannotFindArbitraryException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public final class IdIdentitiesReflective
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdIdentitiesReflective.class);

  private static final List<Class<?>> CLASSES = List.of(
    IdU1CommandEmailAddBegin.class,
    IdU1CommandEmailAddDeny.class,
    IdU1CommandEmailRemovePermit.class,
    IdU1CommandLogin.class,
    IdU1CommandType.class,
    IdU1CommandUserSelf.class,
    IdU1IdTypeResolver.class,
    IdU1Messages.class,
    IdU1MessageType.class,
    IdU1Password.class,
    IdU1ResponseEmailAddBegin.class,
    IdU1ResponseEmailRemoveDeny.class,
    IdU1ResponseEmailRemovePermit.class,
    IdU1ResponseError.class,
    IdU1ResponseLogin.class,
    IdU1ResponseType.class,
    IdU1ResponseUserSelf.class,
    IdU1User.class
  );

  @TestFactory
  public Stream<DynamicTest> identities()
  {
    return CLASSES.stream()
      .map(IdIdentitiesReflective::identityTestFor);
  }

  private static DynamicTest identityTestFor(
    final Class<?> c)
  {
    return DynamicTest.dynamicTest("test_" + c.getSimpleName(), () -> {
      try {

        /*
         * Find the "to model" and "from model" methods.
         */

        final var toModelMethod =
          findToModelMethod(c);
        final var toVersionMethod =
          findFromModelMethod(c);

        /*
         * If only one method is defined, this is likely a mistake!
         */

        if ((toModelMethod == null) == (toVersionMethod != null)) {
          throw new IllegalStateException(
            "Protocol issue: toModelMethod %s, toVersionMethod %s"
              .formatted(toModelMethod, toVersionMethod)
          );
        }

        /*
         * If there are type parameters, instantiate them all to Object.
         */

        final Arbitrary<?> arb;
        final var typeParameters = c.getTypeParameters();
        if (typeParameters.length > 0) {
          final var ps = new Class[typeParameters.length];
          for (int index = 0; index < typeParameters.length; ++index) {
            ps[index] = Object.class;
          }
          arb = Arbitraries.defaultFor(c, ps);
        } else {
          arb = Arbitraries.defaultFor(c);
        }

        /*
         * Try 1000 values.
         */

        LOG.debug("checking 1000 cases for {}", c.getSimpleName());
        for (int index = 0; index < 1000; ++index) {
          final var v0 =
            arb.sample();
          final var vm =
            toModelMethod.invoke(v0);
          final var v1 =
            toVersionMethod.invoke(null, vm);

          assertNotNull(v0);
          assertNotNull(vm);
          assertNotNull(v1);
          assertNotSame(v0, vm);
          assertNotSame(vm, v1);
          assertEquals(v0, v1);
        }

      } catch (final CannotFindArbitraryException e) {
        LOG.debug("No arbitrary for {}: ", c, e);
      } catch (final NotApplicable e) {
        LOG.debug("Not applicable {}: {}", c, e.getMessage());
      }
    });
  }

  private static Method findFromModelMethod(
    final Class<?> c)
  {
    return Arrays.stream(c.getMethods())
      .filter(m -> m.isAnnotationPresent(IdProtocolFromModel.class))
      .findFirst()
      .orElseThrow(() -> new NotApplicable("No IdProtocolFromModel method"));
  }

  private static Method findToModelMethod(
    final Class<?> c)
  {
    return Arrays.stream(c.getMethods())
      .filter(m -> m.isAnnotationPresent(IdProtocolToModel.class))
      .findFirst()
      .orElseThrow(() -> new NotApplicable("No IdProtocolToModel method"));
  }

  private static final class NotApplicable extends RuntimeException
  {
    private NotApplicable(
      final String message)
    {
      super(Objects.requireNonNull(message, "message"));
    }
  }
}
