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
package org.apache.lucene.analysis.ja;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.tests.analysis.BaseTokenStreamTestCase;
import org.junit.Ignore;
import org.junit.Test;

public class TestJapaneseNumberFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    analyzer =
        new Analyzer() {
          @Override
          protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer =
                new JapaneseTokenizer(
                    newAttributeFactory(), null, false, false, JapaneseTokenizer.Mode.SEARCH);
            return new TokenStreamComponents(tokenizer, new JapaneseNumberFilter(tokenizer));
          }
        };
  }

  @Override
  public void tearDown() throws Exception {
    analyzer.close();
    super.tearDown();
  }

  @Test
  public void testBasics() throws IOException {

    assertAnalyzesTo(
        analyzer,
        "???????????????????????????????????????????????????",
        new String[] {"??????", "102500", "???", "???", "?????????", "???", "??????", "???"},
        new int[] {0, 2, 8, 9, 10, 13, 14, 16},
        new int[] {2, 8, 9, 10, 13, 14, 16, 17});

    assertAnalyzesTo(
        analyzer,
        "?????????????????????????????????????????????",
        new String[] {"??????", "???", "???", "??????", "???", "100000", "???", "??????", "???", "???"},
        new int[] {0, 2, 3, 4, 6, 7, 10, 11, 13, 14},
        new int[] {2, 3, 4, 6, 7, 10, 11, 13, 14, 15});

    assertAnalyzesTo(
        analyzer,
        "???????????????????????????????????????????????????",
        new String[] {"???????????????", "???", "??????", "???", "???", "6000000", "???", "??????"},
        new int[] {0, 5, 6, 8, 9, 10, 14, 15},
        new int[] {5, 6, 8, 9, 10, 14, 15, 17});
  }

  @Test
  public void testVariants() throws IOException {
    // Test variants of three
    assertAnalyzesTo(analyzer, "3", new String[] {"3"});
    assertAnalyzesTo(analyzer, "???", new String[] {"3"});
    assertAnalyzesTo(analyzer, "???", new String[] {"3"});

    // Test three variations with trailing zero
    assertAnalyzesTo(analyzer, "03", new String[] {"3"});
    assertAnalyzesTo(analyzer, "??????", new String[] {"3"});
    assertAnalyzesTo(analyzer, "??????", new String[] {"3"});
    assertAnalyzesTo(analyzer, "003", new String[] {"3"});
    assertAnalyzesTo(analyzer, "?????????", new String[] {"3"});
    assertAnalyzesTo(analyzer, "?????????", new String[] {"3"});

    // Test thousand variants
    assertAnalyzesTo(analyzer, "???", new String[] {"1000"});
    assertAnalyzesTo(analyzer, "1???", new String[] {"1000"});
    assertAnalyzesTo(analyzer, "??????", new String[] {"1000"});
    assertAnalyzesTo(analyzer, "??????", new String[] {"1000"});
    assertAnalyzesTo(analyzer, "????????????", new String[] {"1000"});
    assertAnalyzesTo(analyzer, "?????????", new String[] {"1000"}); // Strange, but supported
  }

  @Test
  public void testLargeVariants() throws IOException {
    // Test large numbers
    assertAnalyzesTo(analyzer, "???????????????", new String[] {"35789"});
    assertAnalyzesTo(analyzer, "?????????????????????", new String[] {"6025001"});
    assertAnalyzesTo(analyzer, "?????????????????????", new String[] {"1000006005001"});
    assertAnalyzesTo(analyzer, "????????????????????????", new String[] {"10000006005001"});
    assertAnalyzesTo(analyzer, "?????????", new String[] {"10000000000000001"});
    assertAnalyzesTo(analyzer, "?????????", new String[] {"100000000000000010"});
    assertAnalyzesTo(analyzer, "???????????????????????????", new String[] {"100010001000100011111"});
  }

  @Test
  public void testNegative() throws IOException {
    assertAnalyzesTo(analyzer, "-100???", new String[] {"-", "1000000"});
  }

  @Test
  public void testMixed() throws IOException {
    // Test mixed numbers
    assertAnalyzesTo(analyzer, "??????2????????????", new String[] {"3223"});
    assertAnalyzesTo(analyzer, "????????????", new String[] {"3223"});
  }

  @Test
  public void testNininsankyaku() throws IOException {
    // Unstacked tokens
    assertAnalyzesTo(analyzer, "???", new String[] {"2"});
    assertAnalyzesTo(analyzer, "??????", new String[] {"2", "???"});
    assertAnalyzesTo(analyzer, "?????????", new String[] {"2", "???", "3"});
    // Stacked tokens - emit tokens as they are
    assertAnalyzesTo(analyzer, "????????????", new String[] {"???", "????????????", "???", "???", "???"});
  }

  @Test
  public void testFujiyaichinisanu() throws IOException {
    // Stacked tokens with a numeral partial
    assertAnalyzesTo(analyzer, "??????????????????", new String[] {"???", "?????????", "???", "???", "123"});
  }

  @Test
  public void testFunny() throws IOException {
    // Test some oddities for inconsistent input
    assertAnalyzesTo(analyzer, "??????", new String[] {"20"}); // 100?
    assertAnalyzesTo(analyzer, "?????????", new String[] {"300"}); // 10,000?
    assertAnalyzesTo(analyzer, "????????????", new String[] {"4000"}); // 1,000,000,000,000?
  }

  @Test
  public void testKanjiArabic() throws IOException {
    // Test kanji numerals used as Arabic numbers (with head zero)
    assertAnalyzesTo(analyzer, "????????????????????????????????????????????????????????????", new String[] {"1234567899876543210"});

    // I'm Bond, James "normalized" Bond...
    assertAnalyzesTo(analyzer, "?????????", new String[] {"7"});
  }

  @Test
  public void testDoubleZero() throws IOException {
    assertAnalyzesTo(
        analyzer, "??????", new String[] {"0"}, new int[] {0}, new int[] {2}, new int[] {1});
  }

  @Test
  public void testName() throws IOException {
    // Test name that normalises to number
    assertAnalyzesTo(
        analyzer,
        "????????????",
        new String[] {"??????", "10000000000000001"}, // ?????? is normalized to a number
        new int[] {0, 2},
        new int[] {2, 4},
        new int[] {1, 1});

    // An analyzer that marks ?????? as a keyword
    Analyzer keywordMarkingAnalyzer =
        new Analyzer() {
          @Override
          protected TokenStreamComponents createComponents(String fieldName) {
            CharArraySet set = new CharArraySet(1, false);
            set.add("??????");

            Tokenizer tokenizer =
                new JapaneseTokenizer(
                    newAttributeFactory(), null, false, JapaneseTokenizer.Mode.SEARCH);
            return new TokenStreamComponents(
                tokenizer, new JapaneseNumberFilter(new SetKeywordMarkerFilter(tokenizer, set)));
          }
        };

    assertAnalyzesTo(
        keywordMarkingAnalyzer,
        "????????????",
        new String[] {"??????", "??????"}, // ?????? is not normalized
        new int[] {0, 2},
        new int[] {2, 4},
        new int[] {1, 1});
    keywordMarkingAnalyzer.close();
  }

  @Test
  public void testDecimal() throws IOException {
    // Test Arabic numbers with punctuation, i.e. 3.2 thousands
    assertAnalyzesTo(analyzer, "??????????????????????????????", new String[] {"12345.67"});
  }

  @Test
  public void testDecimalPunctuation() throws IOException {
    // Test Arabic numbers with punctuation, i.e. 3.2 thousands yen
    assertAnalyzesTo(analyzer, "???????????????", new String[] {"3200", "???"});
  }

  @Test
  public void testThousandSeparator() throws IOException {
    assertAnalyzesTo(analyzer, "4,647", new String[] {"4647"});
  }

  @Test
  public void testDecimalThousandSeparator() throws IOException {
    assertAnalyzesTo(analyzer, "4,647.0010", new String[] {"4647.001"});
  }

  @Test
  public void testCommaDecimalSeparator() throws IOException {
    assertAnalyzesTo(analyzer, "15,7", new String[] {"157"});
  }

  @Test
  public void testTrailingZeroStripping() throws IOException {
    assertAnalyzesTo(analyzer, "1000.1000", new String[] {"1000.1"});
    assertAnalyzesTo(analyzer, "1000.0000", new String[] {"1000"});
  }

  @Test
  public void testEmpty() throws IOException {
    assertAnalyzesTo(analyzer, "", new String[] {});
  }

  @Test
  public void testRandomHugeStrings() throws Exception {
    checkRandomData(random(), analyzer, RANDOM_MULTIPLIER, 4096);
  }

  @Test
  @Nightly
  public void testRandomHugeStringsAtNight() throws Exception {
    checkRandomData(random(), analyzer, 3 * RANDOM_MULTIPLIER, 8192);
  }

  @Test
  public void testRandomSmallStrings() throws Exception {
    checkRandomData(random(), analyzer, 100 * RANDOM_MULTIPLIER, 128);
  }

  @Test
  public void testFunnyIssue() throws Exception {
    BaseTokenStreamTestCase.checkAnalysisConsistency(
        random(), analyzer, true, "??????\u302f\u3029\u3039\u3023\u3033\u302bB", true);
  }

  @Ignore(
      "This test is used during development when analyze normalizations in large amounts of text")
  @Test
  public void testLargeData() throws IOException {
    Path input = Paths.get("/tmp/test.txt");
    Path tokenizedOutput = Paths.get("/tmp/test.tok.txt");
    Path normalizedOutput = Paths.get("/tmp/test.norm.txt");

    Analyzer plainAnalyzer =
        new Analyzer() {
          @Override
          protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer =
                new JapaneseTokenizer(
                    newAttributeFactory(), null, false, JapaneseTokenizer.Mode.SEARCH);
            return new TokenStreamComponents(tokenizer);
          }
        };

    analyze(
        plainAnalyzer,
        Files.newBufferedReader(input, StandardCharsets.UTF_8),
        Files.newBufferedWriter(tokenizedOutput, StandardCharsets.UTF_8));

    analyze(
        analyzer,
        Files.newBufferedReader(input, StandardCharsets.UTF_8),
        Files.newBufferedWriter(normalizedOutput, StandardCharsets.UTF_8));
    plainAnalyzer.close();
  }

  public void analyze(Analyzer analyzer, Reader reader, Writer writer) throws IOException {
    TokenStream stream = analyzer.tokenStream("dummy", reader);
    stream.reset();

    CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);

    while (stream.incrementToken()) {
      writer.write(termAttr.toString());
      writer.write("\n");
    }

    reader.close();
    writer.close();
  }
}
