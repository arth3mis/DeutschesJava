package convert.translation;

public class TextChain {

    String germanWord;
    TextChain nextChain;
    TextChain subChain;

    public TextChain(String germanWord) {
        this.germanWord = germanWord;
    }

    public void print() {
        System.out.print(germanWord);
        if (subChain != null) subChain.print();
        if (nextChain != null) nextChain.print();
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
}
