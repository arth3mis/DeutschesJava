package convert.translation;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

public class TextChain {

    String germanWord;
    TextChain nextChain;
    TextChain subChain;

    public TextChain(String germanWord) {
        this.germanWord = germanWord;
    }

    public void print() {
        System.out.print("'" + germanWord + "'");
        if (subChain != null) {
            System.out.print("  v  ");
            subChain.print();
            System.out.print("  ^ ");
        }
        if (nextChain != null) {
            System.out.print(" > ");
            nextChain.print();
        }
    }

    public TextChain setAndGetNextChain(TextChain nextChain) {
        return this.nextChain = nextChain;
    }

    public TextChain setAndGetSubChain(TextChain subChain) {
        return this.subChain = subChain;
    }

    public String getGermanWord() {
        return germanWord;
    }

    public TextChain getNextChain() {
        return nextChain;
    }

    public TextChain getSubChain() {
        return subChain;
    }


    public static class Generator extends TextChain {

        private static final char[] SPLITTERS = new char[] {' ', '\t', '.', ',', ':', ';', '=', '+', '+', '-', '*', '%', '<', '>', '&', '|', '!', '?', '^', '~', '@', '{', '}'};
        private static final char[] SUB_OPENER = new char[] {'(', '['};
        private static final char[] SUB_CLOSER = new char[] {')', ']'};
        private static final String LINE_SEPARATOR = "\n";

        private BufferedReader br;
        private String line;
        private int index = 0;
        private boolean inBlockComment = false;

        public Generator(BufferedReader br) {
            super("");
            this.br = br;
        }

        public TextChain generate() throws IOException {
            readNextLine();
            generate(this);
            return nextChain;
        }

        private void generate(TextChain chainEnd) throws IOException {
            StringBuilder text = new StringBuilder();

            // Read new Line if necessary
            while (line != null) {

                for (; index < line.length(); index++) {
                    char c = line.charAt(index);

                    // Create new chain part, if isSplitter
                    if (isSymbol(c, SPLITTERS)) {
                        // Add previous Text
                        if (!text.isEmpty()) {
                            chainEnd = chainEnd.setAndGetNextChain(new TextChain(text.toString()));
                            text = new StringBuilder();
                        }

                        // Add splitter
                        chainEnd = chainEnd.setAndGetNextChain(new TextChain(String.valueOf(c)));

                    } else if (isSymbol(c, SUB_OPENER)) {
                        // Add previous Text
                        if (!text.isEmpty()) {
                            chainEnd = chainEnd.setAndGetNextChain(new TextChain(text.toString()));
                            text = new StringBuilder();
                        }

                        // Recursive call
                        index++;
                        generate(chainEnd.setAndGetSubChain(new TextChain(String.valueOf(c))));

                    } else if (isSymbol(c, SUB_CLOSER)) {
                        // Add previous Text
                        if (!text.isEmpty()) {
                            chainEnd = chainEnd.setAndGetNextChain(new TextChain(text.toString()));
                            text = new StringBuilder();
                        }

                        // Add splitter
                        chainEnd = chainEnd.setAndGetNextChain(new TextChain(String.valueOf(c)));

                        // End sub-chain here
                        return;

                    } else {
                        // Append Character
                        text.append(c);
                    }
                }


                if (!text.isEmpty()) {
                    chainEnd = chainEnd.setAndGetNextChain(new TextChain(text.toString()));
                    text = new StringBuilder();
                }
                chainEnd = chainEnd.setAndGetNextChain(new TextChain(LINE_SEPARATOR));

                // Reads next line
                readNextLine();
                index = 0;
            }
            return;
        }

        /**
         * Reads next line with information. Comments get filtered here.
         * @throws IOException
         * @return success
         */
        private boolean readNextLine() throws IOException {
            line = br.readLine();

            // Return false if null
            if (line == null) return false;

            // Filter comments
            if (inBlockComment) {
                // Recognize end of comments
                if (line.contains("*/")) {
                    line = line.substring(line.indexOf("*/") + 2);
                    inBlockComment = false;
                } else {
                    // Repeat until end of comment is found
                    readNextLine();
                }

            } else {
                // Recognize new comments
                if (line.contains("//")) {
                    line = line.substring(0, line.indexOf("//"));
                } else if (line.contains("/*")) {
                    int indexOfBegin = line.indexOf("/*");
                    String beginLine = line.substring(0, indexOfBegin);
                    String endLine = line.substring(indexOfBegin + 2);

                    // If BlockComment ends in same line
                    if (endLine.contains("*/")) {
                        line = beginLine + endLine.substring(endLine.indexOf("*/") + 2);

                        // If BlockComment goes beyond this line
                    } else {
                        line = beginLine;
                        inBlockComment = true;
                    }
                }
            }

            return true;
        }



        private static boolean isSymbol(char c, char[] symbols) {
            for (int i = 0; i < symbols.length; i++) {
                if (symbols[i] == c) return true;
            }
            return false;
        }
    }
}
