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

package com.io7m.idstore.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandType;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;

import static com.github.victools.jsonschema.generator.Option.EXTRA_OPEN_API_FORMAT_VALUES;
import static com.github.victools.jsonschema.generator.Option.FLATTENED_OPTIONALS;
import static com.github.victools.jsonschema.generator.Option.NULLABLE_FIELDS_BY_DEFAULT;
import static com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON;
import static com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12;

public final class IdAdminV1SchemaGeneration
{
  private IdAdminV1SchemaGeneration()
  {

  }

  public static String generate()
  {
    final var configBuilder =
      new SchemaGeneratorConfigBuilder(DRAFT_2020_12, PLAIN_JSON);

    configBuilder.without(Option.DEFINITION_FOR_MAIN_SCHEMA);
    configBuilder.without(Option.SCHEMA_VERSION_INDICATOR);

    configBuilder.with(new JacksonModule(
      JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS
    ));
    configBuilder.with(new IdOptionalsNotRequired());

    configBuilder.with(EXTRA_OPEN_API_FORMAT_VALUES);
    configBuilder.without(NULLABLE_FIELDS_BY_DEFAULT);
    configBuilder.without(FLATTENED_OPTIONALS);

    configBuilder.forMethods()
      .withNullableCheck(target -> {
        return Boolean.FALSE;
      });
    configBuilder.forFields()
      .withNullableCheck(target -> {
        return Boolean.FALSE;
      });
    configBuilder.forFields()
      .withRequiredCheck(f -> {
        return !f.toString().startsWith("Optional<");
      });

    final var config =
      configBuilder.build();
    final var generator =
      new SchemaGenerator(config);

    final var objectMapper = new ObjectMapper();
    final var mainSchema = objectMapper.createObjectNode();
    mainSchema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
    mainSchema.put("$id", "https://www.io7m.com/software/idstore/admin-1.0.schema.json");

    final var defs = objectMapper.createObjectNode();
    mainSchema.set("$defs", defs);

    for (final var c : IdA1CommandType.class.getPermittedSubclasses()) {
      final var jsonSchema = generator.generateSchema(c);
      defs.set(
        c.getSimpleName().replace("IdA1", ""),
        jsonSchema
      );
    }

    for (final var c : IdA1ResponseType.class.getPermittedSubclasses()) {
      final var jsonSchema = generator.generateSchema(c);
      defs.set(
        c.getSimpleName().replace("IdA1", ""),
        jsonSchema
      );
    }

    return mainSchema.toPrettyString();
  }

  public static void generatePrint()
  {
    System.out.println(generate());
  }

  public static void main(
    final String[] args)
  {
    generatePrint();
  }
}
