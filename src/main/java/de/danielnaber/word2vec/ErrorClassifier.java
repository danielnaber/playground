package de.danielnaber.word2vec;

import org.deeplearning4j.berkeley.Pair;
import org.deeplearning4j.datasets.fetchers.BaseDataFetcher;
import org.deeplearning4j.datasets.iterator.BaseDatasetIterator;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.nd4j.linalg.util.FeatureUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("MagicNumber")
final class ErrorClassifier {

  private static final String WORD_EMBEDDINGS = "/media/Data/word-embeddings/glove/glove.6B.100d-top50K.txt";
  private static final String CORRECT_SENTENCES = "/home/dnaber/sentences-there.txt";
  private static final String INCORRECT_SENTENCES = "/home/dnaber/sentences-there-incorrect.txt";

  private ErrorClassifier() {}

  public static void main(String[] args) throws Exception {
    System.out.println("Loading embeddings...");
    Pair<InMemoryLookupTable, VocabCache> pair = WordVectorSerializer.loadTxt(new File(WORD_EMBEDDINGS));
    System.out.println("Done.");
    InMemoryLookupTable lookupTable = pair.getFirst();

    Nd4j.ENFORCE_NUMERICAL_STABILITY = true;
    final int numRows = 1;
    final int numColumns = 5;
    int outputNum = 2;
    int numSamples = 1000;
    int batchSize = 50;
    int iterations = 3;
    int seed = 123;
    int listenerFreq = iterations/5;
    int splitTrainNum = (int) (batchSize*.8);

    System.out.println("Load data....");
    File correctSentenceFile = new File(CORRECT_SENTENCES);
    File incorrectSentenceFile = new File(INCORRECT_SENTENCES);
    ErrorDataFetcher fetcher = new ErrorDataFetcher(correctSentenceFile, incorrectSentenceFile, lookupTable);
    DataSetIterator dataIter = new BaseDatasetIterator(batchSize, numSamples, fetcher);

    System.out.println("Build model....");
    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(seed)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .iterations(iterations)
            //.gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
            .learningRate(1e-1f)
            //.momentum(0.5)
            //.momentumAfter(Collections.singletonMap(3, 0.9))
            //.useDropConnect(true)
            .list(2)
            .layer(0, new DenseLayer.Builder()
                    .nIn(numRows * numColumns)
                    .nOut(100)
                    .activation("relu")
                    .weightInit(WeightInit.XAVIER)
                            //.weightInit(WeightInit.NORMALIZED)
                    .build())
            .layer(1, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
                    .nIn(100)
                    .nOut(outputNum)
                    .activation("softmax")
                    .weightInit(WeightInit.XAVIER)
                    .build())
            .build();

    MultiLayerNetwork model = new MultiLayerNetwork(conf);
    model.init();
    model.setListeners(Arrays.asList((IterationListener) new ScoreIterationListener(listenerFreq)));

    System.out.println("Train model....");
    model.setListeners(Arrays.asList((IterationListener) new ScoreIterationListener(listenerFreq)));
    List<INDArray> testInput = new ArrayList<>();
    List<INDArray> testLabels = new ArrayList<>();
    while(dataIter.hasNext()) {
      DataSet errorData = dataIter.next();
      SplitTestAndTrain trainTest = errorData.splitTestAndTrain(splitTrainNum, new Random(seed));
      //trainTest = errorData.splitTestAndTrain(splitTrainNum);
      DataSet trainInput = trainTest.getTrain();
      testInput.add(trainTest.getTest().getFeatureMatrix());
      testLabels.add(trainTest.getTest().getLabels());
      model.fit(trainInput);
    }

    // TODO: real cross-validation is needed here...
    System.out.println("Evaluate model....");
    Evaluation eval = new Evaluation(outputNum);
    for(int i = 0; i < testInput.size(); i++) {
      INDArray output = model.output(testInput.get(i));
      eval.eval(testLabels.get(i), output);
    }

    System.out.println(eval.stats());
  }

  private static class ErrorDataFetcher extends BaseDataFetcher {

    private final InMemoryLookupTable lookupTable;
    private final Iterator<String> correctIterator;
    private final Iterator<String> incorrectIterator;

    ErrorDataFetcher(File correct, File incorrect, InMemoryLookupTable lookupTable) throws IOException {
      this.lookupTable = lookupTable;
      List<String> correctSentences = Files.readAllLines(correct.toPath(), Charset.forName("utf-8"));
      List<String> incorrectSentences = Files.readAllLines(incorrect.toPath(), Charset.forName("utf-8"));
      System.out.println("Loaded correct/incorrect sentences: " + correctSentences.size() + ", " + incorrectSentences.size());
      totalExamples = correctSentences.size() + incorrectSentences.size();
      numOutcomes = 2;
      correctIterator = correctSentences.iterator();
      incorrectIterator = incorrectSentences.iterator();
    }

    @Override
    public void fetch(int numExamples) {
      List<DataSet> toConvert = new ArrayList<>();
      int contextSize = 2;
      for (int i = 0; i < numExamples; ) {
        String sentence;
        boolean correct;
        String word;
        if (i % 2 == 0) {
          sentence = correctIterator.next();
          correct = true;
          word = "there";
          cursor++;
        } else {
          sentence = incorrectIterator.next();
          correct = false;
          word = "their";
          cursor++;
        }
        List<String> origTokens = Arrays.asList(sentence.split("[,.;:\\s]"));
        List<String> tokens = origTokens.stream().filter(t -> !t.trim().isEmpty()).collect(Collectors.toList());
        int wordPos = tokens.indexOf(word);
        if (wordPos == -1) {
          System.err.println("Skipping, '" + word + "' not found: " + sentence);
        } else {
          INDArray in = Nd4j.createComplex(1, 5);
          int pos = 0;
          for (int j = wordPos - contextSize; j <= wordPos + contextSize; j++) {
            INDArray vector = getTokenVector(tokens, j);
            in.put(pos, vector);
            pos++;
          }
          INDArray out = FeatureUtil.toOutcomeVector(correct ? 1 : 0, 2);
          toConvert.add(new DataSet(in, out));
          i++;
        }
      }
      inputColumns = contextSize*2 + 1;   // token + 2 links und rechts
      initializeCurrFromList(toConvert);
    }

    // gets the word2vec representation of a word
    private INDArray getTokenVector(List<String> tokens, int j) {
      String token;
      if (j < 0) {
        token = "_START_";
      } else if (j >= tokens.size()) {
        token = "_END_";
      } else {
        token = tokens.get(j);
      }
      return lookupTable.vector(token);
    }

  }

}
