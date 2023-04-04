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


package com.io7m.idstore.server.service.mail;

import com.io7m.idstore.model.IdEmail;
import com.io7m.repetoir.core.RPServiceType;
import io.opentelemetry.api.trace.Span;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A mail service.
 */

public interface IdServerMailServiceType extends RPServiceType, AutoCloseable
{
  /**
   * Send a message to the given target address.
   *
   * @param parentSpan The parent span for metrics
   * @param requestId  The request ID
   * @param to         The target address
   * @param subject    The message subject
   * @param text       The message text
   * @param headers    Extra message headers
   *
   * @return The send in progress
   */

  CompletableFuture<Void> sendMail(
    Span parentSpan,
    UUID requestId,
    IdEmail to,
    Map<String, String> headers,
    String subject,
    String text);
}
