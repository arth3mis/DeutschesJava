package convert;

import main.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class Translation {

    private String translationText;
    private HashMap<String, Translation> staticTranslations = new HashMap<>();
    private HashMap<String, Translation> packageTranslations;


    Translation() {
        translationText = null;
    }

    Translation(String translationText) {
        this.translationText = translationText;
    }

    public void print(String preText) {
        staticTranslations.forEach((k, v) -> {
            Logger.debug(preText + k + " -> " + v.translationText);
            v.print(preText + "\t");
        });
        if (packageTranslations == null)
            return;
        packageTranslations.forEach((k, v) -> {
            Logger.debug(preText + k + " (paket) -> " + v.translationText);
            v.print(preText + "\t");
        });
    }

    public String getTranslationText() {
        return translationText;
    }

    public @NotNull HashMap<String, Translation> getStaticTranslations() {
        return staticTranslations;
    }
    public HashMap<String, Translation> getPackageTranslations() {
        return packageTranslations;
    }

    public void setStaticTranslations(@NotNull HashMap<String, Translation> staticTranslations) {
        this.staticTranslations = staticTranslations;
    }
    public void initPackageTranslations() {
        packageTranslations = new HashMap<>();
    }
}
