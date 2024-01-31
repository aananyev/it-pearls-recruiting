package com.company.itpearls.core;

import com.ibm.icu.text.Transliterator;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

@Service(StarsAndOtherService.NAME)
public class StarsAndOtherServiceBean implements StarsAndOtherService {
    @Inject
    private ParseCVService parseCVService;
    @Inject
    private StarsAndOtherService starsAndOtherService;

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

    @Override
    public Map<String, Integer> setStarMap() {
        Map<String, Integer> map = new LinkedHashMap<>();

        map.put(new StringBuilder()
                .append(starsAndOtherService.setStars(1))
                .append(" Полный негатив")
                .toString(), 0);
        map.put(new StringBuilder()
                .append(starsAndOtherService.setStars(2))
                .append(" Сомнительно")
                .toString(), 1);
        map.put(new StringBuilder()
                .append(starsAndOtherService.setStars(3))
                .append(" Нейтрально")
                .toString(), 2);
        map.put(new StringBuilder()
                .append(starsAndOtherService.setStars(4))
                .append(" Положительно")
                .toString(), 3);
        map.put(new StringBuilder()
                .append(starsAndOtherService.setStars(5))
                .append(" Отлично!")
                .toString(), 4);

/*        map.put(starsAndOtherService.setStars(1) + " Полный негатив", 0);
        map.put(starsAndOtherService.setStars(2) + " Сомнительно", 1);
        map.put(starsAndOtherService.setStars(3) + " Нейтрально", 2);
        map.put(starsAndOtherService.setStars(4) + " Положительно", 3);
        map.put(starsAndOtherService.setStars(5) + " Отлично!", 4); */

        return map;
    }
}