package com.company.itpearls.core;

import org.springframework.stereotype.Service;

@Service(StarsAndOtherService.NAME)
public class StarsAndOtherServiceBean implements StarsAndOtherService {
    @Override
    public String setStars(int stars) {
//        String retStr = "\u2605";
        String retStr = "";

        for (int i = 1; i <= 5; i++) {
            if (i <= stars)
                retStr = retStr + "\u2605";
            else
                retStr = retStr + "\u2606";
        }

        return retStr;
    }

    @Override
    public String noneStars() {
        String retStr = "";

        for (int i = 1; i <= 5; i++) {
            retStr = retStr + "\u25C7";
        }

        return retStr;
    }

    @Override
    public String setBlackRectangle(int blackRenctangle) {
        String retStr = "";

        for (int i = 1; i <= blackRenctangle; i++) {
                retStr = retStr + "\u25AE";
        }

        return retStr;
    }
}