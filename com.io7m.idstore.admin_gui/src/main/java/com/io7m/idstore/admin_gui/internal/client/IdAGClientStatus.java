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

package com.io7m.idstore.admin_gui.internal.client;

/**
 * The client status.
 */

public enum IdAGClientStatus
{
  /**
   * The client is disconnected.
   */

  DISCONNECTED,

  /**
   * The client is connecting.
   */

  CONNECTING,

  /**
   * The client failed to connect to the server.
   */

  CONNECTION_FAILED,

  /**
   * The client managed to connect to the server.
   */

  CONNECTION_SUCCEEDED,

  /**
   * The client is connected to the server and is idle.
   */

  CONNECTED,

  /**
   * The client is requesting data.
   */

  REQUESTING,

  /**
   * The client failed to complete a request.
   */

  REQUEST_FAILED,
}
