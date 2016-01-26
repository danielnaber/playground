/* LanguageTool, a natural language style checker 
 * Copyright (C) 2016 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package de.danielnaber.word2vec;

import org.deeplearning4j.berkeley.Pair;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class ManualEval {

  private void eval(File file) throws FileNotFoundException {
    Pair<InMemoryLookupTable, VocabCache> pair = WordVectorSerializer.loadTxt(file);
    Word2Vec vec = new Word2Vec();
    vec.setLookupTable(pair.getFirst());
    vec.setVocab(pair.getSecond());
    //System.out.println("Similarity: " + vec.similarity("montag", "freitag"));
    //System.out.println("Similarity: " + vec.similarity("montag", "berlin"));

    List<String> words = Arrays.asList(
            "berlin", "münchen", "gütersloh", "linux", "open source", "wikipedia", "tisch", "fahrrad", "handtuch",
            "essen", "traktor", "bauernhof", "landwirt", "kryptographie",
            "gehen", "trinken", "laufen", "rennen", "denken",
            "schön", "jung", "alt", "klug", "wichtig");
    for (String word : words) {
      Collection<String> similar = vec.wordsNearest(word, 10);
      System.out.println("Similar words to '" + word + "' : " + similar);
    }
  }

  public static void main(String[] args) throws FileNotFoundException {
    if (args.length != 1) {
      System.out.println("Usage: " + ManualEval.class.getSimpleName() + " <word2vec>");
      System.out.println("       <word2vec> is the output file of " + Indexer.class.getSimpleName());
      System.exit(1);
    }
    new ManualEval().eval(new File(args[0]));
  }
  
}
