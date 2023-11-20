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
//        String retStr = "\u2605";
//        String retStr = "";
        StringBuilder sb = new StringBuilder("");

        for (int i = 1; i <= 5; i++) {
            if (i <= stars)
                sb.append("\u2605");
//                retStr = retStr + "\u2605";
            else
                sb.append("\u2606");
//                retStr = retStr + "\u2606";
        }

        return sb.toString();
    }

    @Override
    public String noneStars() {
//        String retStr = "";
        StringBuilder sb = new StringBuilder("");

        for (int i = 1; i <= 5; i++) {
//            retStr = retStr + "\u25C7";
            sb.append("\u25C7");
        }

        return sb.toString();
    }

    @Override
    public String setBlackRectangle(int blackRenctangle) {
//        String retStr = "";
        StringBuilder sb = new StringBuilder("");

        for (int i = 1; i <= blackRenctangle; i++) {
            sb.append("\u25AE");
//                retStr = retStr + "\u25AE";
        }

        return sb.toString();
    }

    public static final String CYRILLIC_TO_LATIN = "Cyrillic-Latin";

    @Override
    public String cyrillicToLatin(String inputString) {
            String st = "привет мир";
            Transliterator toLatinTrans = Transliterator.getInstance(CYRILLIC_TO_LATIN);
            String result = toLatinTrans.transliterate(inputString);
            return result;
    }

    @Override
    public String deleteSystemChar(String string) {
        return parseCVService.deleteSystemChar(string);
    }
}