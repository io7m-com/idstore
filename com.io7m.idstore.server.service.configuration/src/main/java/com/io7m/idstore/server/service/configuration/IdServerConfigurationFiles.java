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

import com.io7m.cxbutton.core.CxButtonColors;
import com.io7m.cxbutton.core.CxButtonStateColors;
import com.io7m.cxbutton.core.CxColor;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseCreate;
import com.io7m.idstore.database.api.IdDatabaseUpgrade;
import com.io7m.idstore.server.api.IdColor;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerColorScheme;
import com.io7m.idstore.server.api.IdServerConfiguration;
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
import com.io7m.idstore.server.api.IdServerOpenTelemetryConfiguration.IdLogs;
import com.io7m.idstore.server.api.IdServerOpenTelemetryConfiguration.IdMetrics;
import com.io7m.idstore.server.api.IdServerOpenTelemetryConfiguration.IdOTLPProtocol;
import com.io7m.idstore.server.api.IdServerOpenTelemetryConfiguration.IdTraces;
import com.io7m.idstore.server.api.IdServerPasswordExpirationConfiguration;
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
import com.io7m.idstore.server.service.configuration.jaxb.Logs;
import com.io7m.idstore.server.service.configuration.jaxb.Mail;
import com.io7m.idstore.server.service.configuration.jaxb.MailAuthentication;
import com.io7m.idstore.server.service.configuration.jaxb.Metrics;
import com.io7m.idstore.server.service.configuration.jaxb.OpenTelemetry;
import com.io7m.idstore.server.service.configuration.jaxb.OpenTelemetryProtocol;
import com.io7m.idstore.server.service.configuration.jaxb.PasswordExpiration;
import com.io7m.idstore.server.service.configuration.jaxb.RateLimiting;
import com.io7m.idstore.server.service.configuration.jaxb.SMTPSType;
import com.io7m.idstore.server.service.configuration.jaxb.SMTPTLSType;
import com.io7m.idstore.server.service.configuration.jaxb.SMTPType;
import com.io7m.idstore.server.service.configuration.jaxb.Sessions;
import com.io7m.idstore.server.service.configuration.jaxb.Traces;
import com.io7m.repetoir.core.RPServiceType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.server.api.IdServerDatabaseKind.POSTGRESQL;

/**
 * The configuration file parser.
 */

public final class IdServerConfigurationFiles
  implements RPServiceType
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
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(stream, "stream");

    try {
      final Schema schema =
        createSchema();
      final var context =
        JAXBContext.newInstance(
          "com.io7m.idstore.server.service.configuration.jaxb");
      final var unmarshaller =
        context.createUnmarshaller();

      unmarshaller.setSchema(schema);

      final var streamSource =
        new StreamSource(stream, source.toString());

      return parseConfiguration(
        (Configuration) unmarshaller.unmarshal(streamSource)
      );
    } catch (final SAXException | JAXBException | URISyntaxException e) {
      throw new IOException(e);
    }
  }

  private static Schema createSchema()
    throws SAXException
  {
    final var schemas =
      SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    return schemas.newSchema(
      IdServerConfigurationFiles.class.getResource(
        "/com/io7m/idstore/server/service/configuration/configuration.xsd")
    );
  }

  /**
   * Serialize a configuration file.
   *
   * @param output        The output stream
   * @param configuration The configuration
   *
   * @throws IOException On errors
   */

  public void serialize(
    final OutputStream output,
    final IdServerConfiguration configuration)
    throws IOException
  {
    Objects.requireNonNull(output, "output");
    Objects.requireNonNull(configuration, "configuration");

    try {
      final Schema schema =
        createSchema();

      final var context =
        JAXBContext.newInstance(
          "com.io7m.idstore.server.service.configuration.jaxb");
      final var marshaller =
        context.createMarshaller();
      final var types =
        DatatypeFactory.newInstance();

      marshaller.setSchema(schema);
      marshaller.marshal(serializeConfiguration(types, configuration), output);
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  private static Configuration serializeConfiguration(
    final DatatypeFactory types,
    final IdServerConfiguration configuration)
  {
    final var c = new Configuration();
    c.setBranding(
      serializeBranding(configuration.branding()));
    c.setMail(
      serializeMail(types, configuration.mailConfiguration()));
    c.setHTTPServices(
      serializeHTTP(
        configuration.adminApiAddress(),
        configuration.userApiAddress(),
        configuration.userViewAddress())
    );
    c.setDatabase(
      serializeDatabase(configuration.databaseConfiguration()));
    c.setHistory(
      serializeHistory(configuration.history()));
    c.setSessions(
      serializeSessions(types, configuration.sessions()));
    c.setRateLimiting(
      serializeRateLimiting(types, configuration.rateLimit()));
    c.setPasswordExpiration(
      serializePasswordExpiration(types, configuration.passwordExpiration()));
    c.setOpenTelemetry(
      serializeOpenTelemetry(configuration.openTelemetry()));
    return c;
  }

  private static OpenTelemetry serializeOpenTelemetry(
    final Optional<IdServerOpenTelemetryConfiguration> c)
  {
    if (c.isEmpty()) {
      return null;
    }

    final var cc = c.get();
    final var r = new OpenTelemetry();

    r.setTraces(
      cc.traces()
        .map(IdServerConfigurationFiles::serializeOpenTelemetryTraces)
        .orElse(null)
    );
    r.setMetrics(
      cc.metrics()
        .map(IdServerConfigurationFiles::serializeOpenTelemetryMetrics)
        .orElse(null)
    );
    r.setLogs(
      cc.logs()
        .map(IdServerConfigurationFiles::serializeOpenTelemetryLogs)
        .orElse(null)
    );
    r.setLogicalServiceName(cc.logicalServiceName());
    return r;
  }

  private static Logs serializeOpenTelemetryLogs(
    final IdLogs c)
  {
    final var r = new Logs();
    r.setEndpoint(c.endpoint().toString());
    r.setProtocol(serializeOTProtocol(c.protocol()));
    return r;
  }

  private static Metrics serializeOpenTelemetryMetrics(
    final IdMetrics c)
  {
    final var r = new Metrics();
    r.setEndpoint(c.endpoint().toString());
    r.setProtocol(serializeOTProtocol(c.protocol()));
    return r;
  }

  private static Traces serializeOpenTelemetryTraces(
    final IdTraces c)
  {
    final var r = new Traces();
    r.setEndpoint(c.endpoint().toString());
    r.setProtocol(serializeOTProtocol(c.protocol()));
    return r;
  }

  private static OpenTelemetryProtocol serializeOTProtocol(
    final IdOTLPProtocol protocol)
  {
    return switch (protocol) {
      case GRPC -> OpenTelemetryProtocol.GRPC;
      case HTTP -> OpenTelemetryProtocol.HTTP;
    };
  }

  private static PasswordExpiration serializePasswordExpiration(
    final DatatypeFactory types,
    final IdServerPasswordExpirationConfiguration c)
  {
    final var r = new PasswordExpiration();
    r.setAdminPasswordValidityDuration(
      c.adminPasswordValidityDuration()
        .map(d -> serializeDuration(types, d))
        .orElse(null)
    );
    r.setUserPasswordValidityDuration(
      c.userPasswordValidityDuration()
        .map(d -> serializeDuration(types, d))
        .orElse(null)
    );
    return r;
  }

  private static RateLimiting serializeRateLimiting(
    final DatatypeFactory types,
    final IdServerRateLimitConfiguration c)
  {
    final var r = new RateLimiting();
    r.setAdminLoginDelay(
      serializeDuration(types, c.adminLoginDelay()));
    r.setAdminLoginRateLimit(
      serializeDuration(types, c.adminLoginRateLimit()));
    r.setEmailVerificationRateLimit(
      serializeDuration(types, c.emailVerificationRateLimit()));
    r.setPasswordResetRateLimit(
      serializeDuration(types, c.passwordResetRateLimit()));
    r.setUserLoginRateLimit(
      serializeDuration(types, c.userLoginRateLimit()));
    r.setUserLoginDelay(
      serializeDuration(types, c.userLoginDelay()));
    return r;
  }

  private static Sessions serializeSessions(
    final DatatypeFactory types,
    final IdServerSessionConfiguration s)
  {
    final var r = new Sessions();
    r.setAdminSessionExpiration(
      serializeDuration(types, s.adminSessionExpiration()));
    r.setUserSessionExpiration(
      serializeDuration(types, s.userSessionExpiration()));
    return r;
  }

  private static History serializeHistory(
    final IdServerHistoryConfiguration history)
  {
    final var r = new History();
    r.setAdminLoginHistoryLimit(history.adminLoginHistoryLimit());
    r.setUserLoginHistoryLimit(history.userLoginHistoryLimit());
    return r;
  }

  private static Database serializeDatabase(
    final IdDatabaseConfiguration c)
  {
    final var r = new Database();
    r.setAddress(c.address());
    r.setCreate(c.create() == IdDatabaseCreate.CREATE_DATABASE);
    r.setKind("POSTGRESQL");
    r.setName(c.databaseName());
    r.setOwnerRoleName(c.ownerRoleName());
    r.setOwnerRolePassword(c.ownerRolePassword());
    r.setPort(c.port());
    r.setReaderRolePassword(c.readerRolePassword().orElse(null));
    r.setUpgrade(c.upgrade() == IdDatabaseUpgrade.UPGRADE_DATABASE);
    r.setWorkerRolePassword(c.workerRolePassword());
    return r;
  }

  private static HTTPServices serializeHTTP(
    final IdServerHTTPServiceConfiguration adminAPI,
    final IdServerHTTPServiceConfiguration userAPI,
    final IdServerHTTPServiceConfiguration userView)
  {
    final var r = new HTTPServices();
    r.setHTTPServiceAdminAPI(serializeHTTPService(adminAPI));
    r.setHTTPServiceUserAPI(serializeHTTPService(userAPI));
    r.setHTTPServiceUserView(serializeHTTPService(userView));
    return r;
  }

  private static HTTPServiceType serializeHTTPService(
    final IdServerHTTPServiceConfiguration s)
  {
    final var r = new HTTPServiceType();
    r.setExternalURI(s.externalAddress().toString());
    r.setListenAddress(s.listenAddress());
    r.setListenPort(s.listenPort());
    return r;
  }

  private static Mail serializeMail(
    final DatatypeFactory types,
    final IdServerMailConfiguration c)
  {
    final var m = new Mail();

    final var t = c.transportConfiguration();
    if (t instanceof final IdServerMailTransportSMTP ts) {
      m.setSMTP(serializeMailSMTP(ts));
    } else if (t instanceof final IdServerMailTransportSMTPS ts) {
      m.setSMTPS(serializeMailSMTPS(ts));
    } else if (t instanceof final IdServerMailTransportSMTP_TLS ts) {
      m.setSMTPTLS(serializeMailSMTPTLS(ts));
    }
    m.setSenderAddress(
      c.senderAddress());
    m.setVerificationExpiration(
      serializeDuration(types, c.verificationExpiration()));
    m.setMailAuthentication(
      serializeMailAuthentication(c.authenticationConfiguration()));

    return m;
  }

  private static MailAuthentication serializeMailAuthentication(
    final Optional<IdServerMailAuthenticationConfiguration> auth)
  {
    if (auth.isEmpty()) {
      return null;
    }

    final var a = auth.get();
    final var r = new MailAuthentication();
    r.setUsername(a.userName());
    r.setPassword(a.password());
    return r;
  }

  private static javax.xml.datatype.Duration serializeDuration(
    final DatatypeFactory types,
    final Duration duration)
  {
    return types.newDuration(duration.toString());
  }

  private static SMTPType serializeMailSMTP(
    final IdServerMailTransportSMTP ts)
  {
    final var s = new SMTPType();
    s.setHost(ts.host());
    s.setPort(ts.port());
    return s;
  }

  private static SMTPSType serializeMailSMTPS(
    final IdServerMailTransportSMTPS ts)
  {
    final var s = new SMTPSType();
    s.setHost(ts.host());
    s.setPort(ts.port());
    return s;
  }

  private static SMTPTLSType serializeMailSMTPTLS(
    final IdServerMailTransportSMTP_TLS ts)
  {
    final var s = new SMTPTLSType();
    s.setHost(ts.host());
    s.setPort(ts.port());
    return s;
  }

  private static Branding serializeBranding(
    final IdServerBrandingConfiguration branding)
  {
    final var b = new Branding();
    b.setColorScheme(
      serializeBrandingColorScheme(branding.scheme()));
    b.setLogo(
      serializeBrandingLogo(branding.logo()));
    b.setLoginExtra(
      serializeBrandingLoginExtra(branding.loginExtra()));
    b.setProductTitle(
      serializeBrandingProductTitle(branding.productTitle()));
    return b;
  }

  private static ColorScheme serializeBrandingColorScheme(
    final Optional<IdServerColorScheme> scheme)
  {
    if (scheme.isEmpty()) {
      return null;
    }

    final var s = scheme.get();
    final var c = new ColorScheme();
    c.setButtonColors(
      serializeBrandingButtonColors(s.buttonColors()));
    c.setErrorBorderColor(
      serializeColorType(s.errorBorderColor()));

    c.setHeaderBackgroundColor(
      serializeColorType(s.headerBackgroundColor()));
    c.setHeaderLinkColor(
      serializeColorType(s.headerLinkColor()));
    c.setHeaderBackgroundColor(
      serializeColorType(s.headerBackgroundColor()));
    c.setHeaderTextColor(
      serializeColorType(s.headerTextColor()));

    c.setMainBackgroundColor(
      serializeColorType(s.mainBackgroundColor()));
    c.setMainLinkColor(
      serializeColorType(s.mainLinkColor()));
    c.setMainBackgroundColor(
      serializeColorType(s.mainBackgroundColor()));
    c.setMainTextColor(
      serializeColorType(s.mainTextColor()));
    c.setMainTableBorderColor(
      serializeColorType(s.mainTableBorderColor()));
    c.setMainMessageBorderColor(
      serializeColorType(s.mainMessageBorderColor()));

    return c;
  }

  private static ButtonColors serializeBrandingButtonColors(
    final CxButtonColors cxButtonColors)
  {
    final var c = new ButtonColors();

    c.setDisabled(
      serializeBrandingButtonStateColors(cxButtonColors.disabled()));
    c.setEnabled(
      serializeBrandingButtonStateColors(cxButtonColors.enabled()));
    c.setHover(
      serializeBrandingButtonStateColors(cxButtonColors.hover()));
    c.setPressed(
      serializeBrandingButtonStateColors(cxButtonColors.pressed()));

    return c;
  }

  private static ButtonStateColors serializeBrandingButtonStateColors(
    final CxButtonStateColors s)
  {
    final var c = new ButtonStateColors();
    c.setBodyColor(serializeCxColorType(s.bodyColor()));
    c.setBorderColor(serializeCxColorType(s.borderColor()));
    c.setEmbossEColor(serializeCxColorType(s.embossEColor()));
    c.setEmbossNColor(serializeCxColorType(s.embossNColor()));
    c.setEmbossWColor(serializeCxColorType(s.embossWColor()));
    c.setEmbossSColor(serializeCxColorType(s.embossSColor()));
    c.setTextColor(serializeCxColorType(s.textColor()));
    return c;
  }

  private static ColorType serializeCxColorType(
    final CxColor cxColor)
  {
    final var c = new ColorType();
    c.setRed(cxColor.red());
    c.setGreen(cxColor.green());
    c.setBlue(cxColor.blue());
    return c;
  }

  private static ColorType serializeColorType(
    final IdColor idColor)
  {
    final var c = new ColorType();
    c.setRed(idColor.red());
    c.setGreen(idColor.green());
    c.setBlue(idColor.blue());
    return c;
  }

  private static String serializeBrandingLogo(
    final Optional<Path> logo)
  {
    return logo.map(Path::toString).orElse(null);
  }

  private static String serializeBrandingLoginExtra(
    final Optional<Path> path)
  {
    return path.map(Path::toString).orElse(null);
  }

  private static String serializeBrandingProductTitle(
    final String s)
  {
    return s;
  }

  private static IdServerConfigurationFile parseConfiguration(
    final Configuration input)
    throws URISyntaxException
  {
    return new IdServerConfigurationFile(
      parseBranding(input.getBranding()),
      parseMail(input.getMail()),
      parseHTTP(input.getHTTPServices()),
      parseDatabase(input.getDatabase()),
      parseHistory(input.getHistory()),
      parseSessions(input.getSessions()),
      parseRateLimit(input.getRateLimiting()),
      parsePasswordExpiration(input.getPasswordExpiration()),
      parseOpenTelemetry(input.getOpenTelemetry())
    );
  }

  private static IdServerPasswordExpirationConfiguration parsePasswordExpiration(
    final PasswordExpiration passwordExpiration)
  {
    if (passwordExpiration == null) {
      return new IdServerPasswordExpirationConfiguration(
        Optional.empty(),
        Optional.empty()
      );
    }

    return new IdServerPasswordExpirationConfiguration(
      Optional.ofNullable(passwordExpiration.getUserPasswordValidityDuration())
        .map(IdServerConfigurationFiles::parseDuration),
      Optional.ofNullable(passwordExpiration.getAdminPasswordValidityDuration())
        .map(IdServerConfigurationFiles::parseDuration)
    );
  }

  private static Optional<IdServerOpenTelemetryConfiguration> parseOpenTelemetry(
    final OpenTelemetry openTelemetry)
  {
    if (openTelemetry == null) {
      return Optional.empty();
    }

    final var metrics =
      Optional.ofNullable(openTelemetry.getMetrics())
        .map(m -> new IdMetrics(
          URI.create(m.getEndpoint()),
          parseProtocol(m.getProtocol())
        ));

    final var traces =
      Optional.ofNullable(openTelemetry.getTraces())
        .map(m -> new IdTraces(
          URI.create(m.getEndpoint()),
          parseProtocol(m.getProtocol())
        ));

    final var logs =
      Optional.ofNullable(openTelemetry.getLogs())
        .map(m -> new IdLogs(
          URI.create(m.getEndpoint()),
          parseProtocol(m.getProtocol())
        ));

    return Optional.of(
      new IdServerOpenTelemetryConfiguration(
        openTelemetry.getLogicalServiceName(),
        logs,
        metrics,
        traces
      )
    );
  }

  private static IdOTLPProtocol parseProtocol(
    final OpenTelemetryProtocol protocol)
  {
    return switch (protocol) {
      case GRPC -> IdOTLPProtocol.GRPC;
      case HTTP -> IdOTLPProtocol.HTTP;
    };
  }

  private static IdServerRateLimitConfiguration parseRateLimit(
    final RateLimiting rateLimiting)
  {
    return new IdServerRateLimitConfiguration(
      parseDuration(
        rateLimiting.getEmailVerificationRateLimit()),
      parseDuration(
        rateLimiting.getPasswordResetRateLimit()),
      parseDurationOrDefault(
        rateLimiting.getUserLoginRateLimit(),
        Duration.ofSeconds(5L)
      ),
      parseDurationOrDefault(
        rateLimiting.getUserLoginDelay(),
        Duration.ofSeconds(1L)
      ),
      parseDurationOrDefault(
        rateLimiting.getAdminLoginRateLimit(),
        Duration.ofSeconds(5L)
      ),
      parseDurationOrDefault(
        rateLimiting.getAdminLoginDelay(),
        Duration.ofSeconds(1L)
      )
    );
  }

  private static Duration parseDuration(
    final javax.xml.datatype.Duration duration)
  {
    return Duration.parse(duration.toString());
  }

  private static Duration parseDurationOrDefault(
    final javax.xml.datatype.Duration duration,
    final Duration defaultValue)
  {
    if (duration == null) {
      return defaultValue;
    }

    return Duration.parse(duration.toString());
  }

  private static IdServerSessionConfiguration parseSessions(
    final Sessions sessions)
  {
    return new IdServerSessionConfiguration(
      parseDuration(sessions.getUserSessionExpiration()),
      parseDuration(sessions.getAdminSessionExpiration())
    );
  }

  private static IdServerHistoryConfiguration parseHistory(
    final History history)
  {
    return new IdServerHistoryConfiguration(
      Math.toIntExact(history.getUserLoginHistoryLimit()),
      Math.toIntExact(history.getAdminLoginHistoryLimit())
    );
  }

  private static IdServerDatabaseConfiguration parseDatabase(
    final Database database)
  {
    return new IdServerDatabaseConfiguration(
      parseDatabaseKind(database.getKind()),
      database.getOwnerRoleName(),
      database.getOwnerRolePassword(),
      database.getWorkerRolePassword(),
      Optional.ofNullable(database.getReaderRolePassword()),
      database.getAddress(),
      Math.toIntExact(database.getPort()),
      database.getName(),
      database.isCreate(),
      database.isUpgrade()
    );
  }

  private static IdServerDatabaseKind parseDatabaseKind(
    final String kind)
  {
    return switch (kind.toLowerCase(Locale.ROOT)) {
      case "postgresql" -> POSTGRESQL;
      default -> {
        throw new IllegalArgumentException(
          "Unrecognized database kind: %s (must be one of %s)"
            .formatted(kind, List.of(IdServerDatabaseKind.values()))
        );
      }
    };
  }

  private static IdServerHTTPConfiguration parseHTTP(
    final HTTPServices httpServices)
    throws URISyntaxException
  {
    return new IdServerHTTPConfiguration(
      parseHTTPService(httpServices.getHTTPServiceAdminAPI()),
      parseHTTPService(httpServices.getHTTPServiceUserAPI()),
      parseHTTPService(httpServices.getHTTPServiceUserView())
    );
  }

  private static IdServerHTTPServiceConfiguration parseHTTPService(
    final HTTPServiceType service)
    throws URISyntaxException
  {
    return new IdServerHTTPServiceConfiguration(
      service.getListenAddress(),
      Math.toIntExact(service.getListenPort()),
      new URI(service.getExternalURI())
    );
  }

  private static IdServerMailConfiguration parseMail(
    final Mail mail)
  {
    final IdServerMailTransportConfigurationType transport;
    if (mail.getSMTP() != null) {
      transport = parseSMTP(mail.getSMTP());
    } else if (mail.getSMTPS() != null) {
      transport = parseSMTPS(mail.getSMTPS());
    } else if (mail.getSMTPTLS() != null) {
      transport = parseSMTPTLS(mail.getSMTPTLS());
    } else {
      throw new IllegalStateException();
    }

    return new IdServerMailConfiguration(
      transport,
      parseMailAuth(mail.getMailAuthentication()),
      mail.getSenderAddress(),
      parseDuration(mail.getVerificationExpiration())
    );
  }

  private static IdServerMailTransportConfigurationType parseSMTP(
    final SMTPType smtp)
  {
    return new IdServerMailTransportSMTP(
      smtp.getHost(),
      Math.toIntExact(smtp.getPort())
    );
  }

  private static IdServerMailTransportConfigurationType parseSMTPS(
    final SMTPSType smtp)
  {
    return new IdServerMailTransportSMTPS(
      smtp.getHost(),
      Math.toIntExact(smtp.getPort())
    );
  }

  private static IdServerMailTransportConfigurationType parseSMTPTLS(
    final SMTPTLSType smtp)
  {
    return new IdServerMailTransportSMTP_TLS(
      smtp.getHost(),
      Math.toIntExact(smtp.getPort())
    );
  }

  private static Optional<IdServerMailAuthenticationConfiguration> parseMailAuth(
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

  private static IdServerBrandingConfiguration parseBranding(
    final Branding branding)
  {
    return new IdServerBrandingConfiguration(
      branding.getProductTitle(),
      parseLogo(branding.getLogo()),
      parseLoginExtra(branding.getLoginExtra()),
      parseColorScheme(branding.getColorScheme())
    );
  }

  private static Optional<IdServerColorScheme> parseColorScheme(
    final ColorScheme colorScheme)
  {
    if (colorScheme == null) {
      return Optional.empty();
    }

    return Optional.of(
      new IdServerColorScheme(
        parseButtonColors(colorScheme.getButtonColors()),
        parseColor(colorScheme.getErrorBorderColor()),
        parseColor(colorScheme.getHeaderBackgroundColor()),
        parseColor(colorScheme.getHeaderLinkColor()),
        parseColor(colorScheme.getHeaderTextColor()),
        parseColor(colorScheme.getMainBackgroundColor()),
        parseColor(colorScheme.getMainLinkColor()),
        parseColor(colorScheme.getMainMessageBorderColor()),
        parseColor(colorScheme.getMainTableBorderColor()),
        parseColor(colorScheme.getMainTextColor())
      )
    );
  }

  private static CxButtonColors parseButtonColors(
    final ButtonColors buttonColors)
  {
    return new CxButtonColors(
      parseButtonStateColors(buttonColors.getEnabled()),
      parseButtonStateColors(buttonColors.getDisabled()),
      parseButtonStateColors(buttonColors.getPressed()),
      parseButtonStateColors(buttonColors.getHover())
    );
  }

  private static CxButtonStateColors parseButtonStateColors(
    final ButtonStateColors state)
  {
    return new CxButtonStateColors(
      parseCxColor(state.getTextColor()),
      parseCxColor(state.getBodyColor()),
      parseCxColor(state.getBorderColor()),
      parseCxColor(state.getEmbossEColor()),
      parseCxColor(state.getEmbossNColor()),
      parseCxColor(state.getEmbossSColor()),
      parseCxColor(state.getEmbossWColor())
    );
  }

  private static CxColor parseCxColor(
    final ColorType color)
  {
    return new CxColor(color.getRed(), color.getGreen(), color.getBlue());
  }

  private static IdColor parseColor(
    final ColorType color)
  {
    return new IdColor(
      color.getRed(),
      color.getGreen(),
      color.getBlue()
    );
  }

  private static Optional<Path> parseLoginExtra(
    final String loginExtra)
  {
    if (loginExtra == null) {
      return Optional.empty();
    }

    return Optional.of(Path.of(loginExtra));
  }

  private static Optional<Path> parseLogo(
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
