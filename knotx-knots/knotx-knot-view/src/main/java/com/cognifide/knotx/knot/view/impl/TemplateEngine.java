/*
 * Knot.x - Reactive microservice assembler - View Knot
 *
 * Copyright (C) 2016 Cognifide Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cognifide.knotx.knot.view.impl;

import com.cognifide.knotx.dataobjects.KnotContext;
import com.cognifide.knotx.knot.view.ViewKnotConfiguration;
import com.cognifide.knotx.knot.view.parser.HtmlFragment;
import com.cognifide.knotx.knot.view.parser.RawHtmlFragment;
import com.cognifide.knotx.knot.view.parser.TemplateHtmlFragment;
import com.cognifide.knotx.fragments.Fragment;
import com.cognifide.knotx.handlebars.CustomHandlebarsHelper;
import com.github.jknack.handlebars.Handlebars;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ServiceLoader;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.eventbus.EventBus;
import rx.Observable;

public class TemplateEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(TemplateEngine.class);

  private Handlebars handlebars;

  private TemplateSnippetProcessor snippetProcessor;

  public TemplateEngine(EventBus eventBus, ViewKnotConfiguration configuration) {
    this.snippetProcessor = new TemplateSnippetProcessor(eventBus, configuration);
    initHandlebars();
  }

  public Observable<String> process(KnotContext knotContext) {
    return knotContext.fragments()
        .map(
            fragments -> Observable.from(fragments)
                .doOnNext(this::traceFragment)
                .flatMap(this::compileHtmlFragment)
                .concatMapEager(compiledFragment -> snippetProcessor.processSnippet(compiledFragment, knotContext))
                // eager will buffer faster processing to emit items in proper order, keeping concurrency.
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
        )
        .orElse(Observable.just(StringUtils.EMPTY));
  }

  private void initHandlebars() {
    handlebars = new Handlebars();
    DefaultHandlebarsHelpers.registerFor(handlebars);

    ServiceLoader.load(CustomHandlebarsHelper.class)
        .iterator().forEachRemaining(helper -> {
      handlebars.registerHelper(helper.getName(), helper);
      LOGGER.info("Registered custom Handlebars helper: {}", helper.getName());
    });
  }

  private Observable<HtmlFragment> compileHtmlFragment(Fragment fragment) {
    if (!fragment.isRaw()) {
      return Observable.create(subscriber -> {
        try {
          subscriber.onNext(new TemplateHtmlFragment(fragment).compileWith(handlebars));
          subscriber.onCompleted();
        } catch (IOException e) {
          subscriber.onError(e);
        }
      });
    } else {
      return Observable.just(new RawHtmlFragment(fragment));
    }
  }

  private void traceFragment(Fragment fragment) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Processing fragment {}", fragment.toJson().encodePrettily());
    }
  }
}