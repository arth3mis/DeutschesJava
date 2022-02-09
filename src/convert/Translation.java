package convert;

import java.util.HashMap;

public class Translation {

    private String translationText;
    private HashMap<String, Translation> staticTranslations = new HashMap<>();


    Translation() {
        translationText = null;
    }

    Translation(String translationText) {
        this.translationText = translationText;
    }

    public void print(String preText) {
        staticTranslations.forEach((k, v) -> {
            System.out.println(preText + k + " -> " + v.translationText);
            v.print(preText + "\t");
        });
    }

    public String getTranslationText() {
        return translationText;
    }

    public HashMap<String, Translation> getStaticTranslations() {
        return staticTranslations;
    }

    public void setStaticTranslations(HashMap<String, Translation> staticTranslations) {
        this.staticTranslations = staticTranslations;
    }
}
