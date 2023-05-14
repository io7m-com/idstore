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

package com.io7m.idstore.tests.server.main;

import com.io7m.idstore.main.IdMain;
import com.io7m.idstore.tests.IdTestDirectories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class IdServerMainTest
{
  private Path directory;
  private ByteArrayOutputStream outLog;
  private PrintStream outPrint;
  private PrintStream outSaved;
  private ByteArrayOutputStream errLog;
  private PrintStream errPrint;
  private PrintStream errSaved;
  private Path directoryOutput;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.directory =
      IdTestDirectories.createTempDirectory();
    this.directoryOutput =
      this.directory.resolve("outputs");

    this.errLog = new ByteArrayOutputStream();
    this.errPrint = new PrintStream(this.errLog, true, UTF_8);
    this.outLog = new ByteArrayOutputStream();
    this.outPrint = new PrintStream(this.outLog, true, UTF_8);

    this.errSaved = System.err;
    this.outSaved = System.out;
    System.setOut(this.outPrint);
    System.setErr(this.errPrint);
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    this.outPrint.flush();
    this.errPrint.flush();

    System.setOut(this.outSaved);
    System.setErr(this.errSaved);

    IdTestDirectories.deleteDirectory(this.directory);

    System.out.println("OUT: ");
    System.out.println(this.outLog.toString(UTF_8));
    System.out.println();
    System.out.println("ERR: ");
    System.out.println(this.errLog.toString(UTF_8));
    System.out.println();
    System.out.flush();
  }

  @Test
  public void testNoArguments()
  {
    final var r = IdMain.mainExitless(new String[]{

    });
    assertEquals(0, r);
  }

  @Test
  public void testVersion()
  {
    final var r = IdMain.mainExitless(new String[]{
      "version"
    });
    assertEquals(0, r);
  }

  @Test
  public void testHelpHelp()
  {
    final var r = IdMain.mainExitless(new String[]{
      "help", "help"
    });
    assertEquals(0, r);
  }

  @Test
  public void testHelpServer()
  {
    final var r = IdMain.mainExitless(new String[]{
      "help", "server"
    });
    assertEquals(0, r);
  }

  @Test
  public void testHelpInitialize()
  {
    final var r = IdMain.mainExitless(new String[]{
      "help", "initialize"
    });
    assertEquals(0, r);
  }
}
