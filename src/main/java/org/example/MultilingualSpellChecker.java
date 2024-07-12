package org.example;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class MultilingualSpellChecker {
    private static final Map<String, String> DICTIONARY_PATHS = new HashMap<>();
    private static final int SUGGESTION_LIMIT = 5;
    private static final Map<String, TrieNode> languageRoots = new HashMap<>();
    private static final String DEFAULT_LANGUAGE = "hi";

    static {
        DICTIONARY_PATHS.put("en", "/usr/share/dict/words");
        DICTIONARY_PATHS.put("gu", "/Users/rosey/Desktop/gu.txt");
        DICTIONARY_PATHS.put("hi", "/Users/rosey/Desktop/hindi-hunspell-master/Hindi/hi_IN.txt");
        DICTIONARY_PATHS.put("mr", "/Users/rosey/Desktop/mr_IN.txt");
        DICTIONARY_PATHS.put("sa", "/Users/rosey/Desktop/sa_IN.txt");
        DICTIONARY_PATHS.put("kn", "/Users/rosey/Desktop/kn.txt");
        DICTIONARY_PATHS.put("ne", "/Users/rosey/Desktop/ne_NP.txt");
        // Add more languages and their dictionary paths here
    }

    public static void main(String[] args) throws LangDetectException, IOException {
        // Initialize the language detector
        DetectorFactory.loadProfile(Paths.get("profiles").toFile());

        try (Scanner scanner = new Scanner(System.in)) {
            // Continuously prompt the user for input until they decide to exit
            while (true) {
                System.out.print("Enter a word to spell check (type 'exit' to quit): ");
                String word = scanner.nextLine();

                if (word.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting spell checker...");
                    break;
                }

                // Detect the language of the input word
                String detectedLanguage = detectLanguage(word);
                System.out.println("Detected Language: " + detectedLanguage);

                // Use default language if detected language is not supported
                if (!DICTIONARY_PATHS.containsKey(detectedLanguage)) {
                    System.out.println("Unsupported language: " + detectedLanguage + ". Falling back to default language.");
                    detectedLanguage = DEFAULT_LANGUAGE;
                }

                // Load the appropriate dictionary based on the detected language
                loadDictionary(detectedLanguage);

                if (isValidWord(word, detectedLanguage)) {
                    System.out.println("The spelling of '" + word + "' is correct.");
                } else {
                    System.out.println("The spelling of '" + word + "' is incorrect. Suggestions:");

                    // Provide suggestions if the word is not found in the dictionary
                    List<String> suggestions = suggestCorrectSpelling(word, detectedLanguage);
                    for (int i = 0; i < Math.min(suggestions.size(), SUGGESTION_LIMIT); i++) {
                        System.out.println((i + 1) + ". " + suggestions.get(i));
                    }
                }
            }
        }
    }

    // Trie node class
    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord;
    }

    // Method to detect the language of a word
    private static String detectLanguage(String word) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        detector.append(word);
        return detector.detect();
    }

    // Method to load the dictionary from a file and build the trie for a specific language
    private static void loadDictionary(String language) {
        if (languageRoots.containsKey(language)) {
            return; // Dictionary already loaded
        }

        TrieNode root = new TrieNode();
        try (BufferedReader reader = new BufferedReader(new FileReader(DICTIONARY_PATHS.get(language)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    insertWord(root, line.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        languageRoots.put(language, root);
    }

    // Method to insert a word into the trie
    private static void insertWord(TrieNode root, String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEndOfWord = true;
    }

    // Method to check if a word is in the dictionary
    private static boolean isValidWord(String word, String language) {
        TrieNode root = languageRoots.get(language);
        TrieNode node = search(root, word);
        return node != null && node.isEndOfWord;
    }

    // Method to search for a word in the trie
    private static TrieNode search(TrieNode root, String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children.get(c);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    // Method to suggest the correct spelling of a word
    private static List<String> suggestCorrectSpelling(String word, String language) {
        List<String> suggestions = new ArrayList<>();
        StringBuilder prefix = new StringBuilder();
        TrieNode root = languageRoots.get(language);
        TrieNode node = root;

        // Traverse the trie until the end of the prefix
        for (char c : word.toCharArray()) {
            prefix.append(c);
            node = node.children.get(c);
            if (node == null) {
                return suggestions; // No suggestions if prefix not found
            }
        }

        // Collect suggestions using DFS from the end of the prefix
        collectSuggestions(node, prefix, suggestions);

        return suggestions;
    }

    // Depth-first search to collect suggestions from a trie node
    private static void collectSuggestions(TrieNode node, StringBuilder prefix, List<String> suggestions) {
        if (suggestions.size() >= SUGGESTION_LIMIT) {
            return; // Stop collecting suggestions if the limit is reached
        }
        if (node.isEndOfWord) {
            suggestions.add(prefix.toString()); // Add word to suggestions if it's valid
        }
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            prefix.append(entry.getKey());
            collectSuggestions(entry.getValue(), prefix, suggestions); // Recursive DFS
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }
}
