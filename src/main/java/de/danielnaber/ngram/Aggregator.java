package de.danielnaber.ngram;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Aggregate counts from Google ngram files.
 */
class Aggregator {

    private Aggregator() {
    }

    private static final int MIN_YEAR = 1980;

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length != 1) {
            System.out.println("Usage: " + Aggregator.class.getSimpleName() + " <googleNgramFile>");
            System.exit(1);
        }
        final Scanner scanner = new Scanner(new File(args[0]));
        String prevWord = null;
        int count = 0;
        while (scanner.hasNextLine()) {
            final String line = scanner.nextLine();
            final String[] parts = line.split("\t");
            final String word = parts[0];
            if (!word.equals(prevWord)) {
                System.out.println(count + "\t" + prevWord);
                count = 0;
            }
            final int year = Integer.parseInt(parts[1]);
            if (year >= MIN_YEAR) {
                final int wordCount = Integer.parseInt(parts[2]);
                count += wordCount;
            }

            prevWord = word;
        }
        scanner.close();
    }

}
