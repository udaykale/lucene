/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.queryparser.flexible.standard.nodes.intervalfn;

import java.util.Locale;
import java.util.Objects;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queries.intervals.Intervals;
import org.apache.lucene.queries.intervals.IntervalsSource;

/** Node that represents {@link Intervals#extend(IntervalsSource, int, int)}. */
public class Extend extends IntervalFunction {
  private final int before, after;
  private final IntervalFunction source;

  public Extend(IntervalFunction source, int before, int after) {
    this.source = Objects.requireNonNull(source);
    this.before = before;
    this.after = after;
  }

  @Override
  public IntervalsSource toIntervalSource(String field, Analyzer analyzer) {
    return Intervals.extend(source.toIntervalSource(field, analyzer), before, after);
  }

  @Override
  public String toString() {
    return String.format(Locale.ROOT, "fn:extend(%s %d %d)", source, before, after);
  }
}
