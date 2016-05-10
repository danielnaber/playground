package de.danielnaber.encog;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordEmbeddings {

  private final Map<String, float[]> lookupTable = new HashMap<>();
  
  int notNullCount;
  int nullCount;

  public WordEmbeddings() throws IOException {
    File file = new File("/media/Data/word-embeddings/glove/glove.6B.100d-top50K.txt");
    List<String> lines = Files.readAllLines(file.toPath());
    System.out.println("Loading embeddings from " + file);
    for (String line : lines) {
      String[] parts = line.split(" ");
      float[] values = new float[100];
      int i = 0;
      for (String part : parts) {
        if (i > 0) {  // skip word
          //System.out.println(i + " -> " + part);
          values[i-1] = Float.parseFloat(part);
        }
        i++;
      }
      lookupTable.put(parts[0], values);
    }
    System.out.println("Loaded " + lookupTable.size() + " embeddings");
  }

  public double[] getSentenceVector(int contextSize, String[] tokens, int wordPos) {
    double[] result = new double[5*100];
    int pos = 0;
    for (int j = wordPos - contextSize; j <= wordPos + contextSize; j++) {
      float[] vector = getTokenVector(tokens, j);
      if (vector == null) {
        vector = new float[100];
        //System.out.println("NULL! " + j + "@" + tokens[j].getToken());
        nullCount++;
      } else {
        notNullCount++;
      }
      //System.out.println(j + " -> " + Arrays.toString(vector));
      int i = 0; 
      for (float v : vector) {
        //System.out.println(pos + " " + i + "="+ ((pos*100)+i) + ", v=" +v);
        result[(pos*100)+i] = v;
        i++;
      }
      //System.out.println(Arrays.toString(result));
      pos++;
    }
    //System.out.println(Arrays.toString(result));
    return result;
  }

  // gets the word2vec representation of a word
  private float[] getTokenVector(String[] tokens, int j) {
    String token;
    if (j < 0) {
      token = "_START_";
    } else if (j >= tokens.length) {
      token = "_END_";
    } else {
      token = tokens[j];
    }
    float[] floats = lookupTable.get(token.toLowerCase());
    /*if (floats == null) {
      System.out.println(token + " -> null");
    } else {
      System.out.println(token + " -> " + floats.length + ": " + Arrays.toString(floats));
    }*/
    return floats;
  }

  @Nullable
  public float[] getTokenVector(String token) {
    return lookupTable.get(token.toLowerCase());
  }
}
