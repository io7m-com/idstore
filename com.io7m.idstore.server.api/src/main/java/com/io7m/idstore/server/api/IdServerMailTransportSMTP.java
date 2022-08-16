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


package com.io7m.idstore.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

/**
 * Vanilla SMTP with an insecure STARTTLS upgrade (if supported).
 *
 * This falls back to plaintext when a mail server does not indicate support for
 * STARTTLS. Additionally, even if a TLS session is negotiated, server
 * certificates are not validated in any way. This TransportStrategy only offers
 * protection against passive network eavesdroppers when the mail server
 * indicates support for STARTTLS. Active network attackers can trivially bypass
 * the encryption 1) by tampering with the STARTTLS indicator, 2) by presenting
 * a self-signed certificate, 3) by presenting a certificate issued by an
 * untrusted certificate authority; or 4) by presenting a certificate that was
 * issued by a valid certificate authority to a domain other than the mail
 * server's. For proper mail transport encryption, see SMTPS or SMTP_TLS.
 *
 * @param host The mail host
 * @param port The port
 */

@JsonDeserialize
@JsonSerialize
@JsonTypeName("SMTP")
public record IdServerMailTransportSMTP(
  @JsonProperty(value = "Host", required = true)
  String host,
  @JsonProperty(value = "Port", required = true)
  int port)
  implements IdServerMailTransportConfigurationType
{
  /**
   * Vanilla SMTP with an insecure STARTTLS upgrade (if supported).
   *
   * This falls back to plaintext when a mail server does not indicate support for
   * STARTTLS. Additionally, even if a TLS session is negotiated, server
   * certificates are not validated in any way. This TransportStrategy only offers
   * protection against passive network eavesdroppers when the mail server
   * indicates support for STARTTLS. Active network attackers can trivially bypass
   * the encryption 1) by tampering with the STARTTLS indicator, 2) by presenting
   * a self-signed certificate, 3) by presenting a certificate issued by an
   * untrusted certificate authority; or 4) by presenting a certificate that was
   * issued by a valid certificate authority to a domain other than the mail
   * server's. For proper mail transport encryption, see SMTPS or SMTP_TLS.
   *
   * @param host The mail host
   * @param port The port
   */

  public IdServerMailTransportSMTP
  {
    Objects.requireNonNull(host, "host");
  }
}
