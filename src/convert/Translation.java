package convert;

import main.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Translation {

    private final String translationText;
    public String priorityKey = null;
    private Map<String, Translation> staticTranslations = new HashMap<>();
    private Map<String, Translation> packageTranslations;


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

    public @NotNull Map<String, Translation> getStaticTranslations() {
        return staticTranslations;
    }
    public Map<String, Translation> getPackageTranslations() {
        return packageTranslations;
    }

    public void setStaticTranslations(@NotNull Map<String, Translation> staticTranslations) {
        this.staticTranslations = staticTranslations;
    }
    public void setPackageTranslations(@NotNull Map<String, Translation> packageTranslations) {
        this.packageTranslations = packageTranslations;
    }
    public void initPackageTranslations() {
        packageTranslations = new HashMap<>();
    }


    public static class Reverse {
        // contains statics and packages
        public final Map<String, Reverse> translations = new HashMap<>();
        String text;
        public final Set<String> extraTexts = new HashSet<>();

        // used to search if Reverse object for a translation has already been created
        private static final Map<Translation, Reverse> dictionary = new HashMap<>();

        private Reverse(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
        public int count() {
            return 1 + extraTexts.size();
        }

        static Reverse create(Translation translation, String de) {
            // check previous reversions
            if (dictionary.containsKey(translation))
                return dictionary.get(translation);
            // create new and save for later
            Reverse rt = new Reverse(de);
            dictionary.put(translation, rt);

            // combine statics and packages
            Set<Map.Entry<String, Translation>> set = new HashSet<>(translation.getStaticTranslations().entrySet());
            if (translation.getPackageTranslations() != null)
                set.addAll(translation.getPackageTranslations().entrySet());

            // loop through every de->en, put as en->de into translations
            for (var e : set) {
                Translation en = e.getValue();
                // known english word & german alternative?
                if (rt.translations.containsKey(en.getTranslationText())) {
                    // set as priority (= text)?
                    if (e.getKey().equals(en.priorityKey)) {
                        Reverse r = rt.translations.get(en.getTranslationText());
                        r.extraTexts.add(r.text);
                        r.text = e.getKey();
                    } else
                        rt.translations.get(en.getTranslationText()).extraTexts.add(e.getKey());
                }
                // new english word?
                else {
                    rt.translations.put(en.getTranslationText(), create(en, e.getKey()));
                }
            }
            return rt;
        }
    }
}
