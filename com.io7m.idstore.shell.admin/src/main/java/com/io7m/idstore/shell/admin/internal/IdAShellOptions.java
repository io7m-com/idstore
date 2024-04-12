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


package com.io7m.idstore.shell.admin.internal;

import com.io7m.idstore.shell.admin.internal.formatting.IdAFormatterPretty;
import com.io7m.idstore.shell.admin.internal.formatting.IdAFormatterType;
import com.io7m.repetoir.core.RPServiceType;
import org.jline.terminal.Terminal;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shell options.
 */

public final class IdAShellOptions
  implements RPServiceType
{
  private final AtomicBoolean terminateOnErrors;
  private Duration loginTimeout;
  private Duration commandTimeout;
  private IdAFormatterType formatter;

  /**
   * Shell options.
   *
   * @param inTerminal The terminal
   */

  public IdAShellOptions(
    final Terminal inTerminal)
  {
    this.terminateOnErrors =
      new AtomicBoolean(false);
    this.formatter =
      new IdAFormatterPretty(inTerminal);
    this.loginTimeout =
      Duration.ofSeconds(30L);
    this.commandTimeout =
      Duration.ofSeconds(30L);
  }

  /**
   * @return The login timeout
   */

  public Duration loginTimeout()
  {
    return this.loginTimeout;
  }

  /**
   * @return The command timeout
   */

  public Duration commandTimeout()
  {
    return this.commandTimeout;
  }

  /**
   * @return A flag indicating if the shell should exit on errors
   */

  public AtomicBoolean terminateOnErrors()
  {
    return this.terminateOnErrors;
  }

  /**
   * Set the formatter.
   *
   * @param inFormatter The formatter
   */

  public void setFormatter(
    final IdAFormatterType inFormatter)
  {
    this.formatter =
      Objects.requireNonNull(inFormatter, "formatter");
  }

  /**
   * @return The shell formatter
   */

  public IdAFormatterType formatter()
  {
    return this.formatter;
  }

  @Override
  public String toString()
  {
    return "[%s]".formatted(this.getClass().getSimpleName());
  }

  @Override
  public String description()
  {
    return "Shell options service.";
  }

  /**
   * Set the login timeout.
   *
   * @param t The timeout
   */

  public void setLoginTimeout(
    final Duration t)
  {
    this.loginTimeout = Objects.requireNonNull(t, "timeout");
  }

  /**
   * Set the command timeout.
   *
   * @param t The timeout
   */

  public void setCommandTimeout(
    final Duration t)
  {
    this.commandTimeout = Objects.requireNonNull(t, "timeout");
  }
}
