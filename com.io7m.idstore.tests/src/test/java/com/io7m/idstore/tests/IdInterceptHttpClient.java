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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class IdInterceptHttpClient extends HttpClient
{
  private final Function<URI, URI> filterURI;
  private final HttpClient client;

  public IdInterceptHttpClient(
    final Function<URI, URI> inFilterURI,
    final HttpClient inClient)
  {
    this.filterURI =
      Objects.requireNonNull(inFilterURI, "filterURI");
    this.client =
      Objects.requireNonNull(inClient, "client");
  }

  @Override
  public Optional<CookieHandler> cookieHandler()
  {
    return this.client.cookieHandler();
  }

  @Override
  public Optional<Duration> connectTimeout()
  {
    return this.client.connectTimeout();
  }

  @Override
  public Redirect followRedirects()
  {
    return this.client.followRedirects();
  }

  @Override
  public Optional<ProxySelector> proxy()
  {
    return this.client.proxy();
  }

  @Override
  public SSLContext sslContext()
  {
    return this.client.sslContext();
  }

  @Override
  public SSLParameters sslParameters()
  {
    return this.client.sslParameters();
  }

  @Override
  public Optional<Authenticator> authenticator()
  {
    return this.client.authenticator();
  }

  @Override
  public Version version()
  {
    return this.client.version();
  }

  @Override
  public Optional<Executor> executor()
  {
    return this.client.executor();
  }

  @Override
  public <T> HttpResponse<T> send(
    final HttpRequest request,
    final HttpResponse.BodyHandler<T> responseBodyHandler)
    throws IOException, InterruptedException
  {
    return this.client.send(this.filterRequest(request), responseBodyHandler);
  }

  private HttpRequest filterRequest(
    final HttpRequest request)
  {
    return HttpRequest.newBuilder(request, (s0, s1) -> true)
      .uri(this.filterURI.apply(request.uri()))
      .build();
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(
    final HttpRequest request,
    final HttpResponse.BodyHandler<T> responseBodyHandler)
  {
    return this.client.sendAsync(
      this.filterRequest(request),
      responseBodyHandler
    );
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(
    final HttpRequest request,
    final HttpResponse.BodyHandler<T> responseBodyHandler,
    final HttpResponse.PushPromiseHandler<T> pushPromiseHandler)
  {
    return this.client.sendAsync(
      this.filterRequest(request),
      responseBodyHandler,
      pushPromiseHandler
    );
  }

  @Override
  public WebSocket.Builder newWebSocketBuilder()
  {
    return this.client.newWebSocketBuilder();
  }
}
