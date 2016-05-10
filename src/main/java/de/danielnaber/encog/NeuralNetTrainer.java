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
package de.danielnaber.encog;

import org.encog.Encog;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.mathutil.randomize.ConsistentRandomizer;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

@SuppressWarnings("MagicNumber")
public class NeuralNetTrainer {

  //private static final String CORRECT_SENTENCES = "/home/dnaber/sentences-there10K.txt";
  //private static final String INCORRECT_SENTENCES = "/home/dnaber/sentences-there-incorrect10K.txt";
  //private static final String CORRECT_SENTENCES = "/home/dnaber/sentences-there1K.txt";
  //private static final String INCORRECT_SENTENCES = "/home/dnaber/sentences-there-incorrect1K.txt";
  //private static final String CORRECT_SENTENCES_TEST = "/home/dnaber/sentences-there1K.txt";
  //private static final String INCORRECT_SENTENCES_TEST = "/home/dnaber/sentences-there-incorrect1K.txt";
  
  private static final String CORRECT_SENTENCES = "/media/Data/mix-corpus/there.txt";
  private static final String INCORRECT_SENTENCES = "/media/Data/mix-corpus/their-incorrect.txt";
  private static final String CORRECT_SENTENCES_TEST = "/media/Data/mix-corpus/there-test.txt";
  private static final String INCORRECT_SENTENCES_TEST = "/media/Data/mix-corpus/their-incorrect-test.txt";
  private static final double THRESHOLD = 0.5;

  private final String word1;
  private final String word2;
  private final WordEmbeddings wordEmbeddings;
          
  protected NeuralNetTrainer(String word1, String word2) throws IOException {
    this.word1 = word1;
    this.word2 = word2;
    wordEmbeddings = new WordEmbeddings();
  }
  
  public void trainAndEval() throws IOException {
    BasicNetwork network = new BasicNetwork();
    network.addLayer(new BasicLayer(null, true, 500));
    //network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 100));
    network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 1));
    network.getStructure().finalizeStructure();
    network.reset();
    new ConsistentRandomizer(-1,1,500).randomize(network);
    //System.out.println(network.dumpWeights());

    MLDataSet trainingSet = getSet(CORRECT_SENTENCES, INCORRECT_SENTENCES);
    //Backpropagation train = new Backpropagation(network, trainingSet, 0.7, 0.3);
    Backpropagation train = new Backpropagation(network, trainingSet, 0.1, 0.1);
    //train.fixFlatSpot(false);

    int epoch = 1;
    do {
      train.iteration();
      System.out.printf("Epoch #" + epoch + " Error: %.10f\n", train.getError());
      epoch++;
    } while(train.getError() > 0.01);
    //} while(train.getError() > 0.02);

    System.out.println("Neural Network Results:");
    for (MLDataPair pair: trainingSet) {
      MLData output = network.compute(pair.getInput());
      if (pair.getInput().getData(0) != 0.0) {
        //System.out.printf("%.9f, %.9f, actual=%.9f, ideal=%.2f\n", pair.getInput().getData(0), pair.getInput().getData(1), output.getData(0), pair.getIdeal().getData(0));
      }
    }

    double error = calculateError(network, trainingSet);
    System.out.printf("error: %.9f\n", error);

    MLDataSet testSet = getSet(CORRECT_SENTENCES_TEST, INCORRECT_SENTENCES_TEST);
    double testError = calculateError(network, testSet);
    System.out.println("test error: " + testError);
    
    Encog.getInstance().shutdown();  
  }

  private MLDataSet getSet(String correctSentences, String incorrectSentences) throws IOException {
    List<String> correctLines = Files.readAllLines(new File(correctSentences).toPath(), Charset.forName("utf-8"));
    List<String> incorrectLines = Files.readAllLines(new File(incorrectSentences).toPath(), Charset.forName("utf-8"));
    int inputSentences = correctLines.size() + incorrectLines.size();
    double[][] input = new double[inputSentences][5*100];
    double[][] expected = new double[inputSentences][1];
    int count = initSentences(0, 1.0f, correctLines, input, expected);
    initSentences(count, 0.0f, incorrectLines, input, expected);
    //System.out.println("wordEmbeddings.notNullCount: " + wordEmbeddings.notNullCount);
    //System.out.println("wordEmbeddings.nullCount: " + wordEmbeddings.nullCount);
    return new BasicMLDataSet(input, expected);
  }

  private double calculateError(BasicNetwork method, MLDataSet data) {
    int total = 0;
    int correct = 0;
    int zeros = 0;
    int ones = 0;
    int idealZero = 0;
    int idealOne = 0;
    int truePositives = 0;
    int falsePositives = 0;
    int falseNegatives = 0;
    for(MLDataPair pair : data ) {
      int ideal = (int)pair.getIdeal().getData(0);
      MLData actual = method.compute(pair.getInput());
      if (ideal == 0) {  // sentence is actually incorrect
        idealZero++;
        if (actual.getData(0) < THRESHOLD) {
          correct++;
          zeros++;
          truePositives++;
        } else {
          falseNegatives++;
        }
      } else if (ideal == 1) {  // sentence is actually correct
        idealOne++;
        if (actual.getData(0) >= THRESHOLD) {
          correct++;
          ones++;
        } else {
          falsePositives++;
        }
      } else {
        throw new RuntimeException();
      }
      total++;
    }
    float precision = (float)truePositives / (truePositives + falsePositives);
    float recall = (float) truePositives / (truePositives + falseNegatives);
    System.out.println("total: " + total + ", correct: " + correct + ", 0: " + zeros + ", 1: " + ones + ", ideal0: " + idealZero + ", ideal1: " + idealOne);
    System.out.printf("precision=%.2f, recall=%.2f\n", precision, recall);
    return (double)(total-correct) / (double)total;
  }

  private int initSentences(int startCount, float label, List<String> correctLines, double[][] input, double[][] expected) throws IOException {
    int i = startCount;
    for (String correctLine : correctLines) {
      String[] tokens = correctLine.split("[…\\s.,;:“\"'’!?()\\[\\]/—-]");
      int pos = getTokenPos(word1, tokens);
      if (pos == -1) {
        pos = getTokenPos(word2, tokens);
        if (pos == -1) {
          throw new RuntimeException("Words not found: " + correctLine);
        }
      }
      //System.out.println("#"+pos + "->"+ tokens[pos]);
      double[] vector = wordEmbeddings.getSentenceVector(2, tokens, pos);
      input[i] = vector;
      expected[i] = new double[] { label };
      i++;
    }
    return i;
  }

  private int getTokenPos(String word, String[] tokens) {
    int i = 0;
    for (String token : tokens) {
      if (token.equalsIgnoreCase(word)) {
        return i;
      }
      i++;
    }
    return -1;
  }

  public static void main(String[] args) throws IOException {
    NeuralNetTrainer trainer = new NeuralNetTrainer("their", "there");
    trainer.trainAndEval();
  }
}
