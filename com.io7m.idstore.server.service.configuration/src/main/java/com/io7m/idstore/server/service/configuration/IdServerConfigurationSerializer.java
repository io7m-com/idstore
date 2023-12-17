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


package com.io7m.idstore.server.service.configuration;

import com.io7m.anethum.api.SerializationException;
import com.io7m.cxbutton.core.CxButtonColors;
import com.io7m.cxbutton.core.CxButtonStateColors;
import com.io7m.cxbutton.core.CxColor;
import com.io7m.idstore.server.api.IdColor;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerColorScheme;
import com.io7m.idstore.server.api.IdServerConfigurationFile;
import com.io7m.idstore.server.api.IdServerDatabaseConfiguration;
import com.io7m.idstore.server.api.IdServerHTTPConfiguration;
import com.io7m.idstore.server.api.IdServerHTTPServiceConfiguration;
import com.io7m.idstore.server.api.IdServerHistoryConfiguration;
import com.io7m.idstore.server.api.IdServerMailAuthenticationConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.api.IdServerMailTransportSMTPS;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP_TLS;
import com.io7m.idstore.server.api.IdServerMaintenanceConfiguration;
import com.io7m.idstore.server.api.IdServerOpenTelemetryConfiguration;
import com.io7m.idstore.server.api.IdServerPasswordExpirationConfiguration;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import com.io7m.idstore.server.api.IdServerSessionConfiguration;
import com.io7m.idstore.tls.IdTLSConfigurationType;
import com.io7m.idstore.tls.IdTLSDisabled;
import com.io7m.idstore.tls.IdTLSEnabled;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import static java.lang.Integer.toUnsignedString;

final class IdServerConfigurationSerializer
  implements IdServerConfigurationSerializerType
{
  private final OutputStream stream;
  private final XMLStreamWriter output;

  IdServerConfigurationSerializer(
    final URI inTarget,
    final OutputStream inStream)
  {
    Objects.requireNonNull(inTarget, "target");

    this.stream =
      Objects.requireNonNull(inStream, "stream");

    try {
      this.output =
        XMLOutputFactory.newFactory()
          .createXMLStreamWriter(this.stream, "UTF-8");
    } catch (final XMLStreamException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String findNS()
  {
    return IdServerConfigurationSchemas.schema1().namespace().toString();
  }

  @Override
  public String toString()
  {
    return "[IdServerConfigurationSerializer 0x%x]"
      .formatted(Integer.valueOf(this.hashCode()));
  }

  @Override
  public void execute(
    final IdServerConfigurationFile value)
    throws SerializationException
  {
    try {
      this.output.writeStartDocument("UTF-8", "1.0");
      this.serializeFile(value);
      this.output.writeEndDocument();
    } catch (final XMLStreamException e) {
      throw new SerializationException(e.getMessage(), e);
    }
  }

  private void serializeFile(
    final IdServerConfigurationFile value)
    throws XMLStreamException
  {
    this.output.writeStartElement("Configuration");
    this.output.writeDefaultNamespace(findNS());
    this.output.writeNamespace("tls", findTLSNS());

    this.serializeBranding(value.brandingConfiguration());
    this.serializeDatabase(value.databaseConfiguration());
    this.serializeHTTP(value.httpConfiguration());
    this.serializeHistory(value.historyConfiguration());
    this.serializeMail(value.mailConfiguration());
    this.serializeMaintenance(value.maintenanceConfiguration());
    this.serializeOpenTelemetryOpt(value.openTelemetry());
    this.serializePasswordExpiration(value.passwordExpiration());
    this.serializeRateLimit(value.rateLimit());
    this.serializeSessions(value.sessionConfiguration());

    this.output.writeEndElement();
  }

  private void serializeMaintenance(
    final IdServerMaintenanceConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("Maintenance");
    if (c.tlsReloadInterval().isPresent()) {
      final var r = c.tlsReloadInterval().get();
      this.output.writeAttribute("TLSReloadInterval", r.toString());
    }
    this.output.writeEndElement();
  }

  private void serializeHTTP(
    final IdServerHTTPConfiguration http)
    throws XMLStreamException
  {
    this.output.writeStartElement("HTTPServices");

    this.serializeHTTPAdminAPI(http.adminAPIService());
    this.serializeHTTPUserAPI(http.userAPIService());
    this.serializeHTTPUserView(http.userViewService());

    this.output.writeEndElement();
  }

  private void serializeHTTPUserView(
    final IdServerHTTPServiceConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("HTTPServiceUserView");
    this.output.writeAttribute("ListenAddress", c.listenAddress());
    this.output.writeAttribute("ListenPort", toUnsignedString(c.listenPort()));
    this.output.writeAttribute("ExternalURI", c.externalAddress().toString());
    this.serializeTLS(c.tlsConfiguration());
    this.output.writeEndElement();
  }

  private void serializeTLS(
    final IdTLSConfigurationType c)
    throws XMLStreamException
  {
    final var tlsNs = findTLSNS();

    switch (c) {
      case final IdTLSDisabled ignored -> {
        this.output.writeStartElement("tls", "TLSDisabled", tlsNs);
        this.output.writeEndElement();
      }
      case final IdTLSEnabled e -> {
        this.output.writeStartElement("tls", "TLSEnabled", tlsNs);

        final var ks = e.keyStore();
        this.output.writeStartElement("tls", "KeyStore", tlsNs);
        this.output.writeAttribute("Type", ks.storeType());
        this.output.writeAttribute("Provider", ks.storeProvider());
        this.output.writeAttribute("Password", ks.storePassword());
        this.output.writeAttribute("File", ks.storePath().toString());
        this.output.writeEndElement();

        final var ts = e.trustStore();
        this.output.writeStartElement("tls", "TrustStore", tlsNs);
        this.output.writeAttribute("Type", ts.storeType());
        this.output.writeAttribute("Provider", ts.storeProvider());
        this.output.writeAttribute("Password", ts.storePassword());
        this.output.writeAttribute("File", ts.storePath().toString());
        this.output.writeEndElement();

        this.output.writeEndElement();
      }
    }
  }

  private static String findTLSNS()
  {
    return IdServerConfigurationSchemas.tls1().namespace().toString();
  }

  private void serializeHTTPUserAPI(
    final IdServerHTTPServiceConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("HTTPServiceUserAPI");
    this.output.writeAttribute("ListenAddress", c.listenAddress());
    this.output.writeAttribute("ListenPort", toUnsignedString(c.listenPort()));
    this.output.writeAttribute("ExternalURI", c.externalAddress().toString());
    this.serializeTLS(c.tlsConfiguration());
    this.output.writeEndElement();
  }

  private void serializeHTTPAdminAPI(
    final IdServerHTTPServiceConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("HTTPServiceAdminAPI");
    this.output.writeAttribute("ListenAddress", c.listenAddress());
    this.output.writeAttribute("ListenPort", toUnsignedString(c.listenPort()));
    this.output.writeAttribute("ExternalURI", c.externalAddress().toString());
    this.serializeTLS(c.tlsConfiguration());
    this.output.writeEndElement();
  }

  private void serializeMail(
    final IdServerMailConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("Mail");
    this.output.writeAttribute(
      "SenderAddress",
      c.senderAddress()
    );
    this.output.writeAttribute(
      "VerificationExpiration",
      c.verificationExpiration().toString()
    );

    switch (c.transportConfiguration()) {
      case final IdServerMailTransportSMTP s -> {
        this.output.writeStartElement("SMTP");
        this.output.writeAttribute("Host", s.host());
        this.output.writeAttribute("Port", toUnsignedString(s.port()));
        this.output.writeEndElement();
      }
      case final IdServerMailTransportSMTPS s -> {
        this.output.writeStartElement("SMTPS");
        this.output.writeAttribute("Host", s.host());
        this.output.writeAttribute("Port", toUnsignedString(s.port()));
        this.output.writeEndElement();
      }
      case final IdServerMailTransportSMTP_TLS s -> {
        this.output.writeStartElement("SMTPTLS");
        this.output.writeAttribute("Host", s.host());
        this.output.writeAttribute("Port", toUnsignedString(s.port()));
        this.output.writeEndElement();
      }
    }

    if (c.authenticationConfiguration().isPresent()) {
      this.serializeMailAuthentication(c.authenticationConfiguration().get());
    }

    this.output.writeEndElement();
  }

  private void serializeMailAuthentication(
    final IdServerMailAuthenticationConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("MailAuthentication");
    this.output.writeAttribute("Username", c.userName());
    this.output.writeAttribute("Password", c.password());
    this.output.writeEndElement();
  }

  private void serializeOpenTelemetryOpt(
    final Optional<IdServerOpenTelemetryConfiguration> c)
    throws XMLStreamException
  {
    if (c.isPresent()) {
      this.serializeOpenTelemetry(c.get());
    }
  }

  private void serializeOpenTelemetry(
    final IdServerOpenTelemetryConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("OpenTelemetry");
    this.output.writeAttribute("LogicalServiceName", c.logicalServiceName());

    if (c.logs().isPresent()) {
      final var e = c.logs().get();
      this.output.writeStartElement("Logs");
      this.output.writeAttribute("Endpoint", e.endpoint().toString());
      this.output.writeAttribute("Protocol", e.protocol().toString());
      this.output.writeEndElement();
    }

    if (c.metrics().isPresent()) {
      final var e = c.metrics().get();
      this.output.writeStartElement("Metrics");
      this.output.writeAttribute("Endpoint", e.endpoint().toString());
      this.output.writeAttribute("Protocol", e.protocol().toString());
      this.output.writeEndElement();
    }

    if (c.traces().isPresent()) {
      final var e = c.traces().get();
      this.output.writeStartElement("Traces");
      this.output.writeAttribute("Endpoint", e.endpoint().toString());
      this.output.writeAttribute("Protocol", e.protocol().toString());
      this.output.writeEndElement();
    }

    this.output.writeEndElement();
  }

  private void serializeSessions(
    final IdServerSessionConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("Sessions");
    this.output.writeAttribute(
      "UserSessionExpiration",
      c.userSessionExpiration().toString()
    );
    this.output.writeAttribute(
      "AdminSessionExpiration",
      c.adminSessionExpiration().toString()
    );
    this.output.writeEndElement();
  }

  private void serializePasswordExpiration(
    final IdServerPasswordExpirationConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("PasswordExpiration");

    if (c.userPasswordValidityDuration().isPresent()) {
      this.output.writeAttribute(
        "UserPasswordValidityDuration",
        c.userPasswordValidityDuration().get().toString()
      );
    }

    if (c.adminPasswordValidityDuration().isPresent()) {
      this.output.writeAttribute(
        "AdminPasswordValidityDuration",
        c.adminPasswordValidityDuration().get().toString()
      );
    }

    this.output.writeEndElement();
  }

  private void serializeRateLimit(
    final IdServerRateLimitConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("RateLimiting");
    this.output.writeAttribute(
      "UserLoginDelay",
      c.userLoginDelay().toString()
    );
    this.output.writeAttribute(
      "UserLoginRateLimit",
      c.userLoginRateLimit().toString()
    );
    this.output.writeAttribute(
      "AdminLoginDelay",
      c.adminLoginDelay().toString()
    );
    this.output.writeAttribute(
      "AdminLoginRateLimit",
      c.adminLoginRateLimit().toString()
    );
    this.output.writeAttribute(
      "EmailVerificationRateLimit",
      c.emailVerificationRateLimit().toString()
    );
    this.output.writeAttribute(
      "PasswordResetRateLimit",
      c.passwordResetRateLimit().toString()
    );
    this.output.writeEndElement();
  }

  private void serializeHistory(
    final IdServerHistoryConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("History");
    this.output.writeAttribute(
      "UserLoginHistoryLimit",
      toUnsignedString(c.userLoginHistoryLimit())
    );
    this.output.writeAttribute(
      "AdminLoginHistoryLimit",
      toUnsignedString(c.adminLoginHistoryLimit())
    );
    this.output.writeEndElement();
  }

  private void serializeDatabase(
    final IdServerDatabaseConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("Database");
    this.output.writeAttribute(
      "OwnerRoleName",
      c.ownerRoleName()
    );
    this.output.writeAttribute(
      "OwnerRolePassword",
      c.ownerRolePassword()
    );
    this.output.writeAttribute(
      "WorkerRolePassword",
      c.workerRolePassword()
    );

    if (c.readerRolePassword().isPresent()) {
      final var r = c.readerRolePassword().get();
      this.output.writeAttribute("ReaderRolePassword", r);
    }

    this.output.writeAttribute(
      "Kind",
      c.kind().name()
    );
    this.output.writeAttribute(
      "Name",
      c.databaseName()
    );
    this.output.writeAttribute(
      "Address",
      c.address()
    );
    this.output.writeAttribute(
      "Port",
      toUnsignedString(c.port())
    );
    this.output.writeAttribute(
      "Create",
      Boolean.toString(c.create())
    );
    this.output.writeAttribute(
      "Upgrade",
      Boolean.toString(c.upgrade())
    );
    this.output.writeEndElement();
  }

  private void serializeBranding(
    final IdServerBrandingConfiguration c)
    throws XMLStreamException
  {
    this.output.writeStartElement("Branding");
    this.output.writeAttribute("ProductTitle", c.productTitle());

    final var logoOpt = c.logo();
    if (logoOpt.isPresent()) {
      final var logo = logoOpt.get();
      this.output.writeAttribute("Logo", logo.toString());
    }

    final var loginExtraOpt = c.loginExtra();
    if (loginExtraOpt.isPresent()) {
      final var loginExtra = loginExtraOpt.get();
      this.output.writeAttribute("LoginExtra", loginExtra.toString());
    }

    final var schemeOpt = c.scheme();
    if (schemeOpt.isPresent()) {
      final var scheme = schemeOpt.get();
      this.serializeScheme(scheme);
    }

    this.output.writeEndElement();
  }

  private void serializeScheme(
    final IdServerColorScheme scheme)
    throws XMLStreamException
  {
    this.output.writeStartElement("ColorScheme");
    this.serializeButtonColors(scheme.buttonColors());
    this.serializeColor(
      "ErrorBorderColor",
      scheme.errorBorderColor()
    );
    this.serializeColor(
      "HeaderBackgroundColor",
      scheme.headerBackgroundColor()
    );
    this.serializeColor(
      "HeaderLinkColor",
      scheme.headerLinkColor()
    );
    this.serializeColor(
      "HeaderTextColor",
      scheme.headerTextColor()
    );
    this.serializeColor(
      "MainBackgroundColor",
      scheme.mainBackgroundColor()
    );
    this.serializeColor(
      "MainLinkColor",
      scheme.mainLinkColor()
    );
    this.serializeColor(
      "MainMessageBorderColor",
      scheme.mainMessageBorderColor()
    );
    this.serializeColor(
      "MainTableBorderColor",
      scheme.mainTableBorderColor()
    );
    this.serializeColor(
      "MainTextColor",
      scheme.mainTextColor()
    );
    this.output.writeEndElement();
  }

  private void serializeColor(
    final String name,
    final IdColor idColor)
    throws XMLStreamException
  {
    this.output.writeStartElement(name);
    this.output.writeAttribute(
      "Red",
      String.format("%.03f", Double.valueOf(idColor.red()))
    );
    this.output.writeAttribute(
      "Green",
      String.format("%.03f", Double.valueOf(idColor.green()))
    );
    this.output.writeAttribute(
      "Blue",
      String.format("%.03f", Double.valueOf(idColor.blue()))
    );
    this.output.writeEndElement();
  }

  private void serializeButtonColors(
    final CxButtonColors colors)
    throws XMLStreamException
  {
    this.output.writeStartElement("ButtonColors");
    this.serializeButtonColorsDisabled(colors.disabled());
    this.serializeButtonColorsEnabled(colors.enabled());
    this.serializeButtonColorsHover(colors.hover());
    this.serializeButtonColorsPressed(colors.pressed());
    this.output.writeEndElement();
  }

  private void serializeButtonColorsPressed(
    final CxButtonStateColors c)
    throws XMLStreamException
  {
    this.serializeButtonStateColors("Pressed", c);
  }

  private void serializeButtonColorsHover(
    final CxButtonStateColors c)
    throws XMLStreamException
  {
    this.serializeButtonStateColors("Hover", c);
  }

  private void serializeButtonColorsEnabled(
    final CxButtonStateColors c)
    throws XMLStreamException
  {
    this.serializeButtonStateColors("Enabled", c);
  }

  private void serializeButtonColorsDisabled(
    final CxButtonStateColors c)
    throws XMLStreamException
  {
    this.serializeButtonStateColors("Disabled", c);
  }

  private void serializeButtonStateColors(
    final String name,
    final CxButtonStateColors c)
    throws XMLStreamException
  {
    this.output.writeStartElement(name);
    this.serializeCxColor("BodyColor", c.bodyColor());
    this.serializeCxColor("BorderColor", c.borderColor());
    this.serializeCxColor("EmbossEColor", c.embossEColor());
    this.serializeCxColor("EmbossNColor", c.embossNColor());
    this.serializeCxColor("EmbossSColor", c.embossSColor());
    this.serializeCxColor("EmbossWColor", c.embossWColor());
    this.serializeCxColor("TextColor", c.textColor());
    this.output.writeEndElement();
  }

  private void serializeCxColor(
    final String name,
    final CxColor c)
    throws XMLStreamException
  {
    this.output.writeStartElement(name);
    this.output.writeAttribute(
      "Red",
      String.format("%.03f", Double.valueOf(c.red()))
    );
    this.output.writeAttribute(
      "Green",
      String.format("%.03f", Double.valueOf(c.green()))
    );
    this.output.writeAttribute(
      "Blue",
      String.format("%.03f", Double.valueOf(c.blue()))
    );
    this.output.writeEndElement();
  }

  @Override
  public void close()
    throws IOException
  {
    this.stream.close();
  }
}
