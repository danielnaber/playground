package de.danielnaber.morphy;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Iterate over the Morphy export file and print fullform to baseform mapping.
 * Send the result of this through "sort -u" to avoid duplicates.
 */
public class MorphyMappingToSql {

    private void run(File morphyFile) throws FileNotFoundException {
        final Scanner scanner = new Scanner(morphyFile);
        String fullForm = "";
        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                if (line.startsWith("<form>")) {
                    fullForm = line.substring("<form>".length(), line.indexOf("</form>"));
                } else if (line.startsWith("<lemma")) {
                    if (!line.contains("*")) {
                        final int startPos = line.indexOf(">");
                        final int endPos = line.indexOf("<", startPos);
                        final String baseForm = line.substring(startPos + 1, endPos);
                        if (baseForm.contains("<") || baseForm.contains(">")) {
                            throw new RuntimeException("Error extracting base form. Form '" + baseForm 
                                    + "' , line: " + line + ", startPos: "
                                    + startPos + ", endPos: " + endPos);
                        }
                        final String cleanBaseForm = baseForm.replace("(", "").replace(")", "")
                                .replace("[", "").replace("]", "");
                        if (!fullForm.equals(cleanBaseForm)) {
                            System.out.println(fullForm + "\t" + cleanBaseForm);
                        }
                    }
                } else if (line.startsWith("#")) {
                    // ignore
                } else {
                    System.err.println("Warning: unknown line format: " + line);
                }
            }
        } finally {
            scanner.close();
        }
    }
    
    public static void main(String[] args) throws FileNotFoundException {
        if (args.length != 1) {
            System.out.println("Usage: " + MorphyMappingToSql.class.getSimpleName() + " <morphyExportFile>");
            System.exit(1);
        }
        final MorphyMappingToSql prg = new MorphyMappingToSql();
        final File morphyFile = new File(args[0]);
        prg.run(morphyFile);
    }

}
