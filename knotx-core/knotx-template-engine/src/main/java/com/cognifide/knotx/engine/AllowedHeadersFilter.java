/*
 * Knot.x - Reactive microservice assembler - Templating Engine Verticle
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
package com.cognifide.knotx.engine;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class AllowedHeadersFilter implements Predicate<String> {

  private final List<Pattern> patterns;

  public AllowedHeadersFilter(List<Pattern> patterns) {
    this.patterns = patterns;
  }

  @Override
  public boolean test(String header) {
    return patterns.stream().anyMatch(pattern -> pattern.matcher(header).matches());
  }
}
