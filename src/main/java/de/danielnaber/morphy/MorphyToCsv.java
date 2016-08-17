package de.danielnaber.morphy;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Take Morphy XML format as found on http://danielnaber.de/morphologie/ and convert
 * it to a compact CSV format (e.g. for korrekturen.de).
 */
public class MorphyToCsv {

  @SuppressWarnings("Duplicates")
  private void run(File morphyFile) throws FileNotFoundException {
    try (Scanner scanner = new Scanner(morphyFile)) {
      String fullForm = "";
      int lineCount = 0;
      boolean inComment = false;
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        lineCount++;
        if (inComment) {
          System.err.println("Skipping comment: " + line);
          if (line.contains("-->")) {
            inComment = false;
          }
          continue;
        }
        if (line.startsWith("<form>")) {
          fullForm = line.substring("<form>".length(), line.indexOf("</form>"));
        } else if (line.contains("<!--") && line.contains("-->")) {
          System.err.println("Skipping comment: " + line);
        } else if (line.contains("<!--")) {
          inComment = true;
        } else if (line.contains("-->")) {
          inComment = false;
        } else if (line.startsWith("<item") || line.startsWith("</item")) {
          // skip
        } else if (line.startsWith("<lemma")) {
          if (!line.contains("*")) {  // seems to indicate a lowercase base form for an uppercase full form (e.g. Schussfesteren / schussfest)
            int startPos = line.indexOf(">");
            int endPos = line.indexOf("<", startPos);
            String baseForm = line.substring(startPos + 1, endPos);
            if (baseForm.contains("<") || baseForm.contains(">")) {
              throw new RuntimeException("Error extracting base form. Form '" + baseForm
                      + "' , line: " + line + ", startPos: "
                      + startPos + ", endPos: " + endPos);
            }
            String tags = line.substring("<lemma".length(), startPos)
                          .replaceAll("(abl|ableitung|zahl|wort)=\".*?\"", "")
                          .replaceAll("[a-z]+=", ":")
                          .replaceAll(" +:", ":")
                          .replaceAll("^:", "")
                          .replaceAll("\"", "")
                          .trim();
            if (tags.matches(".*[a-zöäüß ].*")) {
              throw new RuntimeException("Unexpected tags output: '" + tags + "' in line " + lineCount);
            }
            String cleanBaseForm = baseForm.replace("(", "").replace(")", "").replace("[", "").replace("]", "");
            if (cleanBaseForm.equals(baseForm)) {
              System.out.println(fullForm + "\t" + cleanBaseForm + "\t\t" + tags);
            } else {
              System.out.println(fullForm + "\t" + cleanBaseForm + "\t" + baseForm + "\t" + tags);
            }
          }
        } else {
          System.err.println("Warning: unknown line format: " + line);
        }
      }
    }
  }

  public static void main(String[] args) throws FileNotFoundException {
    if (args.length != 1) {
      System.out.println("Usage: " + MorphyToCsv.class.getSimpleName() + " <morphyXMLExportFile>");
      System.exit(1);
    }
    MorphyToCsv prg = new MorphyToCsv();
    File morphyFile = new File(args[0]);
    prg.run(morphyFile);
  }

}
