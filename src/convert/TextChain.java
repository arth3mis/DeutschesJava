package convert;

import java.io.BufferedReader;
import java.io.IOException;

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
        private boolean inBlockComment = false, inStringLiteral = false;

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

                // Examines single Line
                for (; index < line.length(); index++) {
                    char c = line.charAt(index);
                    char cNext = line.length() > index + 1 ? line.charAt(index + 1) : ' ';

                    // Filter Comments
                    if (inBlockComment && !inStringLiteral) {
                        if (c == '*' && cNext == '/') {
                            index++;
                            inBlockComment = false;
                        }
                        continue;

                    } else if (c == '/' && cNext == '*') {
                        // Recognize Block-Comments using /*
                        index ++;
                        inBlockComment = true;

                        // Add previous Text
                        if (!text.isEmpty()) {
                            chainEnd = chainEnd.setAndGetNextChain(new TextChain(text.toString()));
                            text = new StringBuilder();
                        }
                        continue;

                    } else if (c == '/' && cNext == '/') {
                        // Recognize Line-Comments using //
                        break;
                    }


                    // Group String Literals
                    if (inStringLiteral) {
                        if (c == '\\') {
                            // Check for \'s to ignore \" but not ignore \\"
                            text.append(c).append(cNext);
                            index++;
                            continue;

                        } else if (c == '"') {
                            // End String
                            text.append(c);
                            chainEnd = chainEnd.setAndGetNextChain(new TextChain(text.toString()));
                            text = new StringBuilder();
                            inStringLiteral = false;
                            continue;

                        }
                        text.append(c);
                        continue;

                    } else if (c == '"') {
                        // Add previous Text
                        if (!text.isEmpty()) {
                            chainEnd = chainEnd.setAndGetNextChain(new TextChain(text.toString()));
                            text = new StringBuilder();
                        }
                        inStringLiteral = true;
                        text.append(c);
                        continue;
                    }


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

                if (!inBlockComment) {
                    if (!text.isEmpty()) {
                        chainEnd = chainEnd.setAndGetNextChain(new TextChain(text.toString()));
                        text = new StringBuilder();
                    }
                    chainEnd = chainEnd.setAndGetNextChain(new TextChain(LINE_SEPARATOR));
                }

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
