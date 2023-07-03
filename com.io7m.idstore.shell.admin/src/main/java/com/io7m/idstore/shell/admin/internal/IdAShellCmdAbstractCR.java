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


package com.io7m.idstore.shell.admin.internal;

import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.protocol.admin.IdACommandType;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QException;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.util.Objects;

import static com.io7m.quarrel.core.QCommandStatus.SUCCESS;

/**
 * The abstract command implementation.
 *
 * @param <C> The command type
 * @param <R> The response type
 */

public abstract class IdAShellCmdAbstractCR<
  C extends IdACommandType<R>,
  R extends IdAResponseType>
  extends IdAShellCmdAbstract
{
  private final Class<R> responseClass;

  /**
   * The abstract command implementation.
   *
   * @param inServices      The services
   * @param inMetadata      The command metadata
   * @param inCommandClass  The command type
   * @param inResponseClass The response type
   */

  protected IdAShellCmdAbstractCR(
    final RPServiceDirectoryType inServices,
    final QCommandMetadata inMetadata,
    final Class<C> inCommandClass,
    final Class<R> inResponseClass)
  {
    super(inServices, inMetadata);

    Objects.requireNonNull(inCommandClass, "commandClass");
    this.responseClass =
      Objects.requireNonNull(inResponseClass, "responseClass");
  }

  protected abstract C onCreateCommand(
    QCommandContextType context)
    throws IdPasswordException, Exception;

  protected abstract void onFormatResponse(
    QCommandContextType context,
    R response)
    throws QException;

  @Override
  public final QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    final var r =
      this.client().executeOrElseThrow(
        this.onCreateCommand(context),
        IdAClientException::ofError
      );

    this.onFormatResponse(context, this.responseClass.cast(r));
    return SUCCESS;
  }
}
