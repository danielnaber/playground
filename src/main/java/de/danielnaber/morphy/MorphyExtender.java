package de.danielnaber.morphy;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Command line tool that reads a Morphy export file in XML-like format and writes 
 * new entries for those entries for which a rewrite rule is given in a replacement file.
 * Useful to extend the original Morphy data with post-spelling-reform entries. 
 */
public class MorphyExtender {
    
    private MorphyExtender() {
    }

    private void applyAdditions(File morphyFile, File mappingFile) throws FileNotFoundException {
        System.out.println("<!-- forms added by " + MorphyExtender.class.getSimpleName() + " on " + new Date() + ": -->");
        final Map<String, StringReplacement> oldToNew = getMapping(mappingFile);
        final Scanner scanner = new Scanner(morphyFile);
        try {
            int count = 0;
            String wordForm = null;
            final List<String> inflections = new ArrayList<String>();
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                if (line.startsWith("<form>")) {
                    if (wordForm != null && oldToNew.containsKey(wordForm)) {
                        count++;
                        final StringReplacement replacement = oldToNew.get(wordForm);
                        final String newForm = replace(wordForm, replacement);
                        System.out.println("<form>" + newForm + "</form>");
                        for (String inflection : inflections) {
                            System.out.println(replace(inflection, replacement));
                        }
                    }
                    wordForm = line.substring("<form>".length(), line.indexOf("</form>"));
                    inflections.clear();
                } else if (line.startsWith("<lemma")) {
                    inflections.add(line);
                } else if (line.startsWith("#")) {
                    // ignore
                } else {
                    throw new RuntimeException("Unknown line format: " + line);
                }
            }
            System.err.println("Forms from input file: " + oldToNew.size() + ", forms found in Morphy file: " + count);
        } finally {
            scanner.close();
        }
    }

    private Map<String, StringReplacement> getMapping(File file) throws FileNotFoundException {
        final Map<String, StringReplacement> oldToNew = new HashMap<String, StringReplacement>();
        final Scanner scanner = new Scanner(file);
        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                if (line.startsWith("#")) {
                    continue;
                }
                final String[] parts = line.split("\t");
                if (parts.length != 3) {
                    throw new RuntimeException("Unexpected format: " + line);
                }
                oldToNew.put(parts[0], new StringReplacement(parts[1], parts[2]));
            }
        } finally {
            scanner.close();
        }
        return oldToNew;
    }

    private String replace(String str, StringReplacement replacement) {
        return str.replaceAll(replacement.from, replacement.to);
    }

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length != 2) {
            System.out.println("Usage: " + MorphyExtender.class.getSimpleName() + " <morphyFile> <rewriteRuleFile>");
            System.out.println("   <morphyFile> is a Morphy export in a format similar to XML");
            System.out.println("   <rewriteRuleFile> tab-separated file: original word form, string to be replaced, replacement string");
            System.out.println("                     example: Abschluß\tß\tss");
            System.exit(1);
        }
        final MorphyExtender extender = new MorphyExtender();
        extender.applyAdditions(new File(args[0]), new File(args[1]));
    }

    private class StringReplacement {
        private final String from;
        private final String to;
        private StringReplacement(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }
}
