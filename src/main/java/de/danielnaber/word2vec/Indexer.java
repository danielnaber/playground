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

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;

class Indexer {

  private Word2Vec index(File inputFile) {
    SentenceIterator iter = new LineSentenceIterator(inputFile);
    iter.setPreProcessor(String::toLowerCase);

    //EndingPreProcessor preProcessor = new EndingPreProcessor();  //useful only for English
    TokenizerFactory tokenizer = new DefaultTokenizerFactory();
    tokenizer.setTokenPreProcessor(new CommonPreprocessor());
    /*tokenizer.setTokenPreProcessor(token -> {
      //String base = preProcessor.preProcess(token.toLowerCase());
      String base = token.toLowerCase();
      return base.replaceAll("\\d", "d");
    });*/

    int batchSize = 1000;
    int iterations = 15;
    int layerSize = 300;
    System.out.println("Starting: " + new Date());
    Word2Vec vec = new Word2Vec.Builder()
            .batchSize(batchSize)
            .sampling(1e-5) // negative sampling. drops words out
            .minWordFrequency(5)
            .useAdaGrad(false)
            .layerSize(layerSize) // word feature vector size
            .iterations(iterations) // # iterations to train
            .learningRate(0.025)
            .minLearningRate(1e-2) // learning rate decays wrt # words. floor learning
            .negativeSample(10) // sample size 10 words
            .iterate(iter)
            .tokenizerFactory(tokenizer)
            .build();
    vec.fit();
    System.out.println("Done: " + new Date());
    return vec;
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.out.println("Usage: " + Indexer.class.getSimpleName() + " <input> <output>");
      System.exit(1);
    }
    File inputFile = new File(args[0]);
    File outputFile = new File(args[1]);
    Word2Vec vec = new Indexer().index(inputFile);
    WordVectorSerializer.writeWordVectors(vec, outputFile.getAbsolutePath());
  }

}
