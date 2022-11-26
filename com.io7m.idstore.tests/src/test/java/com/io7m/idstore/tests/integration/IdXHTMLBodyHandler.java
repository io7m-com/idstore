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


package com.io7m.idstore.tests.integration;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public final class IdXHTMLBodyHandler implements HttpResponse.BodySubscriber<Document>,
  HttpResponse.BodyHandler<Document>
{
  private final HttpResponse.BodySubscriber<byte[]> bytes;

  public IdXHTMLBodyHandler()
  {
    this.bytes = HttpResponse.BodySubscribers.ofByteArray();
  }

  @Override
  public CompletionStage<Document> getBody()
  {
    return this.bytes.getBody().thenApply(
      byteData -> {
        try {
          final var source =
            new InputSource(new ByteArrayInputStream(byteData));
          final var documents =
            DocumentBuilderFactory.newDefaultInstance();
          final var documentBuilder =
            documents.newDocumentBuilder();

          return documentBuilder.parse(source);
        } catch (final Exception e) {
          throw new RuntimeException(e);
        }
      }
    );
  }

  @Override
  public void onSubscribe(
    final Flow.Subscription subscription)
  {
    this.bytes.onSubscribe(subscription);
  }

  @Override
  public void onNext(final List<ByteBuffer> item)
  {
    this.bytes.onNext(item);
  }

  @Override
  public void onError(final Throwable throwable)
  {
    this.bytes.onError(throwable);
  }

  @Override
  public void onComplete()
  {
    this.bytes.onComplete();
  }

  @Override
  public HttpResponse.BodySubscriber<Document> apply(
    final HttpResponse.ResponseInfo responseInfo)
  {
    return this;
  }
}
