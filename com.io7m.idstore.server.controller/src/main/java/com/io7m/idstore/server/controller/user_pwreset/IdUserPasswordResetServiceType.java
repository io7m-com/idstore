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


package com.io7m.idstore.server.controller.user_pwreset;

import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.repetoir.core.RPServiceType;

import java.util.Optional;
import java.util.UUID;

/**
 * The user password reset service.
 */

public interface IdUserPasswordResetServiceType extends RPServiceType
{
  /**
   * Start a password reset.
   *
   * @param sourceHost The source remoteHost of the request
   * @param userAgent  The user agent
   * @param requestId  The request ID
   * @param email      The email
   * @param userName   The username
   *
   * @throws IdCommandExecutionFailure On errors
   */

  void resetBegin(
    String sourceHost,
    String userAgent,
    UUID requestId,
    Optional<String> email,
    Optional<String> userName)
    throws IdCommandExecutionFailure;

  /**
   * Check a password reset token.
   *
   * @param sourceHost The source remoteHost of the request
   * @param userAgent  The user agent
   * @param requestId  The request ID
   * @param token      The token
   *
   * @return The checked token
   *
   * @throws IdCommandExecutionFailure On errors
   */

  IdToken resetCheck(
    String sourceHost,
    String userAgent,
    UUID requestId,
    Optional<String> token)
    throws IdCommandExecutionFailure;

  /**
   * Confirm a password reset.
   *
   * @param sourceHost   The source remoteHost of the request
   * @param userAgent    The user agent
   * @param requestId    The request ID
   * @param password0Opt The password
   * @param password1Opt The confirmed password
   * @param tokenOpt     The reset token
   *
   * @throws IdCommandExecutionFailure On errors
   */

  void resetConfirm(
    String sourceHost,
    String userAgent,
    UUID requestId,
    Optional<String> password0Opt,
    Optional<String> password1Opt,
    Optional<String> tokenOpt)
    throws IdCommandExecutionFailure;
}
