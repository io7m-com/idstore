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


package com.io7m.idstore.tests.integration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class IdDocuments
{
  private IdDocuments()
  {

  }

  public static String dump(
    final Document document)
  {
    try {
      final var output = new ByteArrayOutputStream();
      final var factory = TransformerFactory.newInstance();
      final var transformer = factory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      final var source = new DOMSource(document);
      final var result = new StreamResult(output);
      transformer.transform(source, result);
      return output.toString();
    } catch (final TransformerException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<Element> elementsWithName(
    final Document document,
    final String name)
  {
    final var results = new ArrayList<Element>();
    elementsWithNameRecurse(results, document.getDocumentElement(), name);
    return results;
  }

  private static void elementsWithNameRecurse(
    final ArrayList<Element> results,
    final Element element,
    final String name)
  {
    if (Objects.equals(element.getNodeName(), name)) {
      results.add(element);
    }

    final var nodes = element.getChildNodes();
    for (var index = 0; index < nodes.getLength(); ++index) {
      final var node = nodes.item(index);
      if (node instanceof Element subElement) {
        elementsWithNameRecurse(results, subElement, name);
      }
    }
  }
}
