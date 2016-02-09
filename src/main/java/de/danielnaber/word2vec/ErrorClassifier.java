package de.danielnaber.word2vec;

import org.apache.commons.io.FileUtils;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.io.*;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("MagicNumber")
final class ErrorClassifier {

  ErrorClassifierTools tools;
  MultiLayerNetwork model;
  
  private ErrorClassifier() throws IOException {
    tools = new ErrorClassifierTools();
    model = loadModel();
  }

  private float eval(String sentence, String word, String replacement) {
    INDArray labels = Nd4j.create(1, 2);
    INDArray example = tools.getSentenceVector(2, sentence, word, replacement);
    DataSet testSet = new DataSet(example, labels);
    INDArray inputs = Nd4j.create(1, 5);
    inputs.putRow(0, testSet.getFeatureMatrix());
    labels.putRow(0, testSet.getLabels());
    DataSet curr = new DataSet(inputs, labels);
    INDArray output = model.output(curr.getFeatureMatrix(), false);
    float correctProbability = output.getFloat(0);
    return correctProbability;
  }

  private MultiLayerNetwork loadModel() throws IOException {
    MultiLayerConfiguration confFromJson = MultiLayerConfiguration.fromJson(FileUtils.readFileToString(new File(ErrorClassifierTrainer.JSON_FILE)));
    try (DataInputStream dis = new DataInputStream(new FileInputStream(ErrorClassifierTrainer.BIN_FILE))) {
      INDArray newParams = Nd4j.read(dis);
      MultiLayerNetwork model = new MultiLayerNetwork(confFromJson);
      model.init();
      model.setParameters(newParams);
      return model;
    }
  }

  public static void main(String[] args) throws Exception {
    ErrorClassifier errorClassifier = new ErrorClassifier();
    List<String> sentences = Arrays.asList("I've never been there before.",
            "I will look for a hotel to stay when I arrive there.",
            "There is a book on the table.",
            "It is freezing up there in mountains.",
            "I found this dollar sitting over there on the sidewalk."
    );
    for (String sentence : sentences) {
      float p1 = errorClassifier.eval(sentence, "there", "their");
      System.out.printf("%.3f <- " + sentence + "\n", p1);
      String wrongSentence = sentence.replaceAll("there", "their").replaceAll("There", "Their");
      float p2 = errorClassifier.eval(wrongSentence, "their", "there");
      System.out.printf("%.3f <- " + wrongSentence + "\n", p2);
    }
  }

}
