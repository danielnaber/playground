package de.danielnaber.wikipedia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Merge a semi-automatically generated list of incoming link counts with another
 * list of articles. This was a list of articles can then be sorted by their
 * number of incoming links.
 */
public class MergeIncomingLinkCount {

    private MergeIncomingLinkCount() {
    }

    private void run(String filename, String articleFile) throws FileNotFoundException, UnsupportedEncodingException {
        Map<String,Integer> articleToLinkCount = loadLinkCounts(filename);
        Scanner sc = new Scanner(new File(articleFile));
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] parts = line.trim().split(" ");
            String article = URLDecoder.decode(parts[0], "UTF-8");
            Integer count = articleToLinkCount.get(article);
            System.out.println((count == null ? "0" : count)  + " " + article);
        }
    }

    private Map<String, Integer> loadLinkCounts(String filename) throws FileNotFoundException {
        Scanner sc = new Scanner(new File(filename));
        Map<String, Integer> articleToLinkCount = new HashMap<String, Integer>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] parts = line.trim().split(" ");
            articleToLinkCount.put(parts[1], Integer.parseInt(parts[0]));
        }
        sc.close();
        return articleToLinkCount;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: " + MergeIncomingLinkCount.class.getSimpleName() + " <incomingLinkCountFile> <relevantClassFile>");
            System.exit(1);
        }
        MergeIncomingLinkCount prg = new MergeIncomingLinkCount();
        prg.run(args[0], args[1]);
    }

}
