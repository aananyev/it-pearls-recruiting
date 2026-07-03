package com.company.hunttech.core;

public interface StarsAndOtherService {
    String NAME = "hunttech_StarsAndOtherService";

    String setStars(int stars);

    String setBlackRectangle(int blackRenctangle);

    String noneStars();

    String cyrillicToLatin(String inputString);

    String deleteSystemChar(String string);
}