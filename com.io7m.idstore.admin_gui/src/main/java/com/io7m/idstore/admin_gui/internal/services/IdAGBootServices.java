/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.idstore.admin_gui.internal.services;

import com.io7m.idstore.admin_client.IdAClients;
import com.io7m.idstore.admin_gui.IdAGConfiguration;
import com.io7m.idstore.admin_gui.internal.IdAGApplication;
import com.io7m.idstore.admin_gui.internal.IdAGBackgroundSchedulerService;
import com.io7m.idstore.admin_gui.internal.IdAGStrings;
import com.io7m.idstore.admin_gui.internal.client.IdAGClientService;
import com.io7m.idstore.admin_gui.internal.errors.IdAGErrorDialogs;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventBus;
import com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferencesService;
import com.io7m.idstore.admin_gui.internal.preferences.IdAGPreferencesServiceType;
import com.io7m.repetoir.core.RPServiceDirectory;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.repetoir.core.RPServiceType;
import com.io7m.taskrecorder.core.TRTask;
import com.io7m.taskrecorder.core.TRTaskRecorder;
import com.io7m.taskrecorder.core.TRTaskSucceeded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The main service directory.
 */

public final class IdAGBootServices
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAGBootServices.class);

  private IdAGBootServices()
  {

  }

  /**
   * Create a service directory.
   *
   * @param configuration The application configuration
   * @param strings       The strings
   * @param bootEvents    A receiver of boot events
   *
   * @return A new service directory
   */

  public static CompletableFuture<TRTask<RPServiceDirectoryType>> create(
    final IdAGConfiguration configuration,
    final IdAGStrings strings,
    final Consumer<IdAGBootEvent> bootEvents)
  {
    final var future =
      new CompletableFuture<TRTask<RPServiceDirectoryType>>();

    final var thread = new Thread(() -> {
      try {
        future.complete(createServices(configuration, strings, bootEvents));
      } catch (final Throwable e) {
        future.completeExceptionally(e);
      }
    });

    thread.setName("com.io7m.idstore.boot[%d]".formatted(thread.getId()));
    thread.setDaemon(true);
    thread.start();
    return future;
  }

  private static TRTask<RPServiceDirectoryType> createServices(
    final IdAGConfiguration configuration,
    final IdAGStrings strings,
    final Consumer<IdAGBootEvent> bootEvents)
    throws Exception
  {
    final var services =
      new RPServiceDirectory();
    final var creators =
      new ArrayList<EIBootService<? extends RPServiceType>>();
    final var eventBus =
      new IdAGEventBus();
    final var clients =
      new IdAClients();

    creators.add(new EIBootService<>(
      "Loading string resources...",
      IdAGEventBus.class,
      () -> eventBus
    ));

    creators.add(new EIBootService<>(
      "Loading string resources...",
      IdAGStrings.class,
      () -> strings
    ));

    creators.add(new EIBootService<>(
      "Creating client...",
      IdAGClientService.class,
      () -> {
        return IdAGClientService.create(
          eventBus,
          clients,
          configuration.locale()
        );
      }
    ));

    creators.add(new EIBootService<>(
      "Loading preferences...",
      IdAGPreferencesServiceType.class,
      () -> {
        final var prefs =
          IdAGPreferencesService.openOrDefault(
            configuration.directories()
              .configurationDirectory()
              .resolve("preferences.xml")
          );
        prefs.update(Function.identity());
        return prefs;
      }
    ));

    creators.add(new EIBootService<>(
      "Loading event bus...",
      IdAGEventBus.class,
      IdAGEventBus::new
    ));

    creators.add(new EIBootService<>(
      "Loading background scheduler service...",
      IdAGBackgroundSchedulerService.class,
      IdAGBackgroundSchedulerService::new
    ));

    creators.add(new EIBootService<>(
      "Loading error dialogs...",
      IdAGErrorDialogs.class,
      () -> new IdAGErrorDialogs(strings, configuration)
    ));

    final var recorder =
      TRTaskRecorder.<RPServiceDirectoryType>create(
        LOG,
        "Booting application..."
      );

    final var size = creators.size();
    for (var index = 0; index < size; ++index) {
      final var creator = creators.get(index);
      final var progress = (double) index / (double) size;

      recorder.beginStep(creator.message);
      bootEvents.accept(new IdAGBootEvent(creator.message(), progress));

      try {
        final var clazz = (Class<RPServiceType>) creator.clazz;
        final var service = creator.creator.create();
        services.register(clazz, service);
      } catch (final Exception e) {
        recorder.setTaskFailed(e.getMessage(), Optional.of(e));
        throw e;
      }
    }

    if (debugFailBoot()) {
      recorder.setTaskFailed("Failed due to debug option!");
      return recorder.toTask();
    }

    bootEvents.accept(
      new IdAGBootEvent(IdAGApplication.appVersionedTitle(strings), 1.0)
    );
    recorder.setTaskResolution(new TRTaskSucceeded<>("OK", services));
    return recorder.toTask();
  }

  private static boolean debugFailBoot()
    throws IOException
  {
    final var property =
      System.getProperty("com.io7m.idstore.debug.boot_fail", "FALSE")
        .toUpperCase(Locale.ROOT);

    if ("EXCEPTION".equals(property)) {
      throw new IOException("Failed due to debug option!");
    }

    return Objects.equals(property, "TASK");
  }

  private interface EIBootServiceCreatorType<T extends RPServiceType>
  {
    T create()
      throws Exception;
  }

  private record EIBootService<T extends RPServiceType>(
    String message,
    Class<T> clazz,
    EIBootServiceCreatorType<T> creator)
  {

  }
}
