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

import com.io7m.anethum.api.ParseSeverity;
import com.io7m.anethum.api.ParseStatus;
import com.io7m.anethum.api.ParsingException;
import com.io7m.blackthorne.core.BTException;
import com.io7m.blackthorne.core.BTParseError;
import com.io7m.blackthorne.core.BTPreserveLexical;
import com.io7m.blackthorne.jxe.BlackthorneJXE;
import com.io7m.idstore.server.api.IdServerConfigurationFile;
import com.io7m.idstore.server.service.configuration.v1.IdC1Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static com.io7m.blackthorne.core.BTPreserveLexical.PRESERVE_LEXICAL_INFORMATION;
import static com.io7m.idstore.server.service.configuration.v1.IdC1Names.qName;
import static java.util.Map.entry;

final class IdServerConfigurationParser
  implements IdServerConfigurationParserType
{
  private final BTPreserveLexical context;
  private final URI source;
  private final InputStream stream;
  private final Consumer<ParseStatus> statusConsumer;

  IdServerConfigurationParser(
    final BTPreserveLexical inContext,
    final URI inSource,
    final InputStream inStream,
    final Consumer<ParseStatus> inStatusConsumer)
  {
    this.context =
      Objects.requireNonNullElse(inContext, PRESERVE_LEXICAL_INFORMATION);
    this.source =
      Objects.requireNonNull(inSource, "source");
    this.stream =
      Objects.requireNonNull(inStream, "stream");
    this.statusConsumer =
      Objects.requireNonNull(inStatusConsumer, "statusConsumer");
  }

  @Override
  public String toString()
  {
    return "[IdServerConfigurationParser 0x%x]"
      .formatted(Integer.valueOf(this.hashCode()));
  }

  @Override
  public IdServerConfigurationFile execute()
    throws ParsingException
  {
    try {
      return BlackthorneJXE.parse(
        this.source,
        this.stream,
        Map.ofEntries(
          entry(qName("Configuration"), IdC1Configuration::new)
        ),
        IdServerConfigurationSchemas.schemas(),
        this.context
      );
    } catch (final BTException e) {
      final var statuses =
        e.errors()
          .stream()
          .map(IdServerConfigurationParser::mapParseError)
          .toList();

      for (final var status : statuses) {
        this.statusConsumer.accept(status);
      }

      final var ex =
        new ParsingException(e.getMessage(), List.copyOf(statuses));
      ex.addSuppressed(e);
      throw ex;
    }
  }

  @Override
  public void close()
    throws IOException
  {
    this.stream.close();
  }

  private static ParseStatus mapParseError(
    final BTParseError error)
  {
    return ParseStatus.builder("parse-error", error.message())
      .withSeverity(mapSeverity(error.severity()))
      .withLexical(error.lexical())
      .build();
  }

  private static ParseSeverity mapSeverity(
    final BTParseError.Severity severity)
  {
    return switch (severity) {
      case ERROR -> ParseSeverity.PARSE_ERROR;
      case WARNING -> ParseSeverity.PARSE_WARNING;
    };
  }
}
