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

package com.io7m.idstore.server.service.configuration;

import com.io7m.cxbutton.core.CxButtonColors;
import com.io7m.cxbutton.core.CxButtonStateColors;
import com.io7m.cxbutton.core.CxColor;
import com.io7m.idstore.server.api.IdColor;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerColorScheme;
import com.io7m.idstore.server.api.IdServerConfigurationFile;
import com.io7m.idstore.server.api.IdServerDatabaseConfiguration;
import com.io7m.idstore.server.api.IdServerDatabaseKind;
import com.io7m.idstore.server.api.IdServerHTTPConfiguration;
import com.io7m.idstore.server.api.IdServerHTTPServiceConfiguration;
import com.io7m.idstore.server.api.IdServerHistoryConfiguration;
import com.io7m.idstore.server.api.IdServerMailAuthenticationConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportConfigurationType;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.api.IdServerMailTransportSMTPS;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP_TLS;
import com.io7m.idstore.server.api.IdServerOpenTelemetryConfiguration;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import com.io7m.idstore.server.api.IdServerSessionConfiguration;
import com.io7m.idstore.server.service.configuration.jaxb.Branding;
import com.io7m.idstore.server.service.configuration.jaxb.ButtonColors;
import com.io7m.idstore.server.service.configuration.jaxb.ButtonStateColors;
import com.io7m.idstore.server.service.configuration.jaxb.ColorScheme;
import com.io7m.idstore.server.service.configuration.jaxb.ColorType;
import com.io7m.idstore.server.service.configuration.jaxb.Configuration;
import com.io7m.idstore.server.service.configuration.jaxb.Database;
import com.io7m.idstore.server.service.configuration.jaxb.HTTPServiceType;
import com.io7m.idstore.server.service.configuration.jaxb.HTTPServices;
import com.io7m.idstore.server.service.configuration.jaxb.History;
import com.io7m.idstore.server.service.configuration.jaxb.Mail;
import com.io7m.idstore.server.service.configuration.jaxb.MailAuthentication;
import com.io7m.idstore.server.service.configuration.jaxb.OpenTelemetry;
import com.io7m.idstore.server.service.configuration.jaxb.RateLimiting;
import com.io7m.idstore.server.service.configuration.jaxb.SMTPSType;
import com.io7m.idstore.server.service.configuration.jaxb.SMTPTLSType;
import com.io7m.idstore.server.service.configuration.jaxb.SMTPType;
import com.io7m.idstore.server.service.configuration.jaxb.Sessions;
import com.io7m.idstore.services.api.IdServiceType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.io7m.idstore.server.api.IdServerDatabaseKind.POSTGRESQL;

/**
 * The configuration file parser.
 */

public final class IdServerConfigurationFiles
  implements IdServiceType
{
  /**
   * The Public API v1 message protocol.
   */

  public IdServerConfigurationFiles()
  {

  }

  /**
   * Parse a configuration file.
   *
   * @param source The source URI
   * @param stream The input stream
   *
   * @return The file
   *
   * @throws IOException On errors
   */

  public IdServerConfigurationFile parse(
    final URI source,
    final InputStream stream)
    throws IOException
  {
    try {
      final var schemas =
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      final var schema =
        schemas.newSchema(
          IdServerConfigurationFiles.class.getResource(
            "/com/io7m/idstore/server/service/configuration/configuration.xsd")
        );

      final var context =
        JAXBContext.newInstance(
          "com.io7m.idstore.server.service.configuration.jaxb");
      final var unmarshaller =
        context.createUnmarshaller();

      unmarshaller.setSchema(schema);

      final var streamSource =
        new StreamSource(stream, source.toString());

      return processConfiguration(
        (Configuration) unmarshaller.unmarshal(streamSource)
      );
    } catch (final SAXException | JAXBException | URISyntaxException e) {
      throw new IOException(e);
    }
  }

  private static IdServerConfigurationFile processConfiguration(
    final Configuration input)
    throws URISyntaxException
  {
    return new IdServerConfigurationFile(
      processBranding(input.getBranding()),
      processMail(input.getMail()),
      processHTTP(input.getHTTPServices()),
      processDatabase(input.getDatabase()),
      processHistory(input.getHistory()),
      processSessions(input.getSessions()),
      processRateLimit(input.getRateLimiting()),
      processOpenTelemetry(input.getOpenTelemetry())
    );
  }

  private static Optional<IdServerOpenTelemetryConfiguration> processOpenTelemetry(
    final OpenTelemetry openTelemetry)
    throws URISyntaxException
  {
    if (openTelemetry == null) {
      return Optional.empty();
    }

    return Optional.of(
      new IdServerOpenTelemetryConfiguration(
        openTelemetry.getLogicalServiceName(),
        new URI(openTelemetry.getOTELCollectorAddress())
      )
    );
  }

  private static IdServerRateLimitConfiguration processRateLimit(
    final RateLimiting rateLimiting)
  {
    return new IdServerRateLimitConfiguration(
      processDuration(rateLimiting.getEmailVerificationRateLimit()),
      processDuration(rateLimiting.getPasswordResetRateLimit())
    );
  }

  private static Duration processDuration(
    final javax.xml.datatype.Duration duration)
  {
    return Duration.parse(duration.toString());
  }

  private static IdServerSessionConfiguration processSessions(
    final Sessions sessions)
  {
    return new IdServerSessionConfiguration(
      processDuration(sessions.getUserSessionExpiration()),
      processDuration(sessions.getAdminSessionExpiration())
    );
  }

  private static IdServerHistoryConfiguration processHistory(
    final History history)
  {
    return new IdServerHistoryConfiguration(
      Math.toIntExact(history.getUserLoginHistoryLimit()),
      Math.toIntExact(history.getAdminLoginHistoryLimit())
    );
  }

  private static IdServerDatabaseConfiguration processDatabase(
    final Database database)
  {
    return new IdServerDatabaseConfiguration(
      processDatabaseKind(database.getKind()),
      database.getUser(),
      database.getPassword(),
      database.getAddress(),
      Math.toIntExact(database.getPort()),
      database.getName(),
      database.isCreate(),
      database.isUpgrade()
    );
  }

  private static IdServerDatabaseKind processDatabaseKind(
    final String kind)
  {
    return switch (kind.toLowerCase()) {
      case "postgresql" -> POSTGRESQL;
      default -> {
        throw new IllegalArgumentException(
          "Unrecognized database kind: %s (must be one of %s)"
            .formatted(kind, List.of(IdServerDatabaseKind.values()))
        );
      }
    };
  }

  private static IdServerHTTPConfiguration processHTTP(
    final HTTPServices httpServices)
    throws URISyntaxException
  {
    return new IdServerHTTPConfiguration(
      processHTTPService(httpServices.getHTTPServiceAdminAPI()),
      processHTTPService(httpServices.getHTTPServiceUserAPI()),
      processHTTPService(httpServices.getHTTPServiceUserView())
    );
  }

  private static IdServerHTTPServiceConfiguration processHTTPService(
    final HTTPServiceType service)
    throws URISyntaxException
  {
    return new IdServerHTTPServiceConfiguration(
      service.getListenAddress(),
      Math.toIntExact(service.getListenPort()),
      new URI(service.getExternalURI())
    );
  }

  private static IdServerMailConfiguration processMail(
    final Mail mail)
  {
    final IdServerMailTransportConfigurationType transport;
    if (mail.getSMTP() != null) {
      transport = processSMTP(mail.getSMTP());
    } else if (mail.getSMTPS() != null) {
      transport = processSMTPS(mail.getSMTPS());
    } else if (mail.getSMTPTLS() != null) {
      transport = processSMTPTLS(mail.getSMTPTLS());
    } else {
      throw new IllegalStateException();
    }

    return new IdServerMailConfiguration(
      transport,
      processMailAuth(mail.getMailAuthentication()),
      mail.getSenderAddress(),
      processDuration(mail.getVerificationExpiration())
    );
  }

  private static IdServerMailTransportConfigurationType processSMTP(
    final SMTPType smtp)
  {
    return new IdServerMailTransportSMTP(
      smtp.getHost(),
      Math.toIntExact(smtp.getPort())
    );
  }

  private static IdServerMailTransportConfigurationType processSMTPS(
    final SMTPSType smtp)
  {
    return new IdServerMailTransportSMTPS(
      smtp.getHost(),
      Math.toIntExact(smtp.getPort())
    );
  }

  private static IdServerMailTransportConfigurationType processSMTPTLS(
    final SMTPTLSType smtp)
  {
    return new IdServerMailTransportSMTP_TLS(
      smtp.getHost(),
      Math.toIntExact(smtp.getPort())
    );
  }

  private static Optional<IdServerMailAuthenticationConfiguration> processMailAuth(
    final MailAuthentication mailAuthentication)
  {
    if (mailAuthentication == null) {
      return Optional.empty();
    }

    return Optional.of(
      new IdServerMailAuthenticationConfiguration(
        mailAuthentication.getUsername(),
        mailAuthentication.getPassword()
      )
    );
  }

  private static IdServerBrandingConfiguration processBranding(
    final Branding branding)
  {
    return new IdServerBrandingConfiguration(
      branding.getProductTitle(),
      processLogo(branding.getLogo()),
      processLoginExtra(branding.getLoginExtra()),
      processColorScheme(branding.getColorScheme())
    );
  }

  private static Optional<IdServerColorScheme> processColorScheme(
    final ColorScheme colorScheme)
  {
    if (colorScheme == null) {
      return Optional.empty();
    }

    return Optional.of(
      new IdServerColorScheme(
        processButtonColors(colorScheme.getButtonColors()),
        processColor(colorScheme.getErrorBorderColor()),
        processColor(colorScheme.getHeaderBackgroundColor()),
        processColor(colorScheme.getHeaderLinkColor()),
        processColor(colorScheme.getHeaderTextColor()),
        processColor(colorScheme.getMainBackgroundColor()),
        processColor(colorScheme.getMainLinkColor()),
        processColor(colorScheme.getMainMessageBorderColor()),
        processColor(colorScheme.getMainTableBorderColor()),
        processColor(colorScheme.getMainTextColor())
      )
    );
  }

  private static CxButtonColors processButtonColors(
    final ButtonColors buttonColors)
  {
    return new CxButtonColors(
      processButtonStateColors(buttonColors.getEnabled()),
      processButtonStateColors(buttonColors.getDisabled()),
      processButtonStateColors(buttonColors.getPressed()),
      processButtonStateColors(buttonColors.getHover())
    );
  }

  private static CxButtonStateColors processButtonStateColors(
    final ButtonStateColors state)
  {
    return new CxButtonStateColors(
      processCxColor(state.getTextColor()),
      processCxColor(state.getBodyColor()),
      processCxColor(state.getBorderColor()),
      processCxColor(state.getEmbossEColor()),
      processCxColor(state.getEmbossNColor()),
      processCxColor(state.getEmbossSColor()),
      processCxColor(state.getEmbossWColor())
    );
  }

  private static CxColor processCxColor(
    final ColorType color)
  {
    return new CxColor(color.getRed(), color.getGreen(), color.getBlue());
  }

  private static IdColor processColor(
    final ColorType color)
  {
    return new IdColor(
      color.getRed(),
      color.getGreen(),
      color.getBlue()
    );
  }

  private static Optional<Path> processLoginExtra(
    final String loginExtra)
  {
    if (loginExtra == null) {
      return Optional.empty();
    }

    return Optional.of(Path.of(loginExtra));
  }

  private static Optional<Path> processLogo(
    final String logo)
  {
    if (logo == null) {
      return Optional.empty();
    }

    return Optional.of(Path.of(logo));
  }

  /**
   * Parse a configuration file.
   *
   * @param file The input file
   *
   * @return The file
   *
   * @throws IOException On errors
   */

  public IdServerConfigurationFile parse(
    final Path file)
    throws IOException
  {
    try (var stream = Files.newInputStream(file)) {
      return this.parse(file.toUri(), stream);
    }
  }

  @Override
  public String description()
  {
    return "Server configuration elements.";
  }

  @Override
  public String toString()
  {
    return "[IdServerConfigurationFiles 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}
