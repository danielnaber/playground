package de.danielnaber.levenshtein;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

// Results of test with 1,801,660 lines (Jan's German dictionary):
// call:
//   java -cp playground-1.0-jar-with-dependencies.jar de.danielnaber.levenshtein.LevenshteinTest german.dic
// locally: 360ms when run from JAR (not IDE, Java 1.8.0_102)
// old OT server (.169, Java 1.8.0_45, no load): 370ms
// LT server (.131, Java 1.8.0_121, load 0.9): 500ms
// new OT server (.105, Java 1.8.0_121, load 0.8): 400ms
public class LevenshteinTest {

  public static void main(String[] args) throws IOException {
    List<String> lines = Files.readAllLines(Paths.get(args[0]));
    System.out.println("Loaded " + lines.size() + " lines"); 
    List<String> words = Arrays.asList("Haus", "auto", "gehen", "tisch", "gut", "gebirge", "selber", "tüte");
    long time = System.currentTimeMillis();
    for (String word : words) {
      System.out.println("Checking word " + word);
      for (String word2 : lines) {
        StringUtils.getLevenshteinDistance(word, word2, 3);
      }
    }
    long endTime = System.currentTimeMillis();
    long runTime = endTime - time;
    System.out.println(runTime / words.size() + "ms");
  }
  
}
