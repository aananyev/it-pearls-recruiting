package com.company.itpearls.core;

import java.util.Map;

public interface StarsAndOtherService {
    String NAME = "itpearls_StarsAndOtherService";

    String setStars(int stars);

    String setBlackRectangle(int blackRenctangle);

    String noneStars();

    String cyrillicToLatin(String inputString);

    String deleteSystemChar(String string);

    Map<String, Integer> setStarMap();
}