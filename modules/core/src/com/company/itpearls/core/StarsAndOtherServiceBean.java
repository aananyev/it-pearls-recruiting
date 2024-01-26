package com.company.itpearls.core;

import com.ibm.icu.text.Transliterator;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(StarsAndOtherService.NAME)
public class StarsAndOtherServiceBean implements StarsAndOtherService {
    @Inject
    private ParseCVService parseCVService;

    @Override
    public String setStars(int stars) {
        StringBuilder sb = new StringBuilder("");

        for (int i = 1; i <= 5; i++) {
            if (i <= stars)
                sb.append("\u2605");
            else
                sb.append("\u2606");
        }

        return sb.toString();
    }

    @Override
    public String noneStars() {
        StringBuilder sb = new StringBuilder("");

        for (int i = 1; i <= 5; i++) {
            sb.append("\u25C7");
        }

        return sb.toString();
    }

    @Override
    public String setBlackRectangle(int blackRenctangle) {
        StringBuilder sb = new StringBuilder("");

        for (int i = 1; i <= blackRenctangle; i++) {
            sb.append("\u25AE");
        }

        return sb.toString();
    }

    public static final String CYRILLIC_TO_LATIN = "Cyrillic-Latin";

    @Override
    public String cyrillicToLatin(String inputString) {
            Transliterator toLatinTrans = Transliterator.getInstance(CYRILLIC_TO_LATIN);
            String result = toLatinTrans.transliterate(inputString);
            return result;
    }

    @Override
    public String deleteSystemChar(String string) {
        return parseCVService.deleteSystemChar(string);
    }
}