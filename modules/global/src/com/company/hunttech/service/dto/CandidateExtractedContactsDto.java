package com.company.hunttech.service.dto;

import java.io.Serializable;

/**
 * Структурированные контактные данные кандидата, извлеченные нейросетью из резюме.
 */
public class CandidateExtractedContactsDto implements Serializable {
    private static final long serialVersionUID = 819203948571029384L;

    private String phone;
    private String mobilePhone;
    private String email;
    private String telegramName;
    private String skypeName;
    private String whatsupName;
    private String wiberName;
    private String city;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelegramName() {
        return telegramName;
    }

    public void setTelegramName(String telegramName) {
        this.telegramName = telegramName;
    }

    public String getSkypeName() {
        return skypeName;
    }

    public void setSkypeName(String skypeName) {
        this.skypeName = skypeName;
    }

    public String getWhatsupName() {
        return whatsupName;
    }

    public void setWhatsupName(String whatsupName) {
        this.whatsupName = whatsupName;
    }

    public String getWiberName() {
        return wiberName;
    }

    public void setWiberName(String wiberName) {
        this.wiberName = wiberName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean hasAnyContact() {
        return isNotEmpty(phone) || isNotEmpty(mobilePhone) || isNotEmpty(email)
                || isNotEmpty(telegramName) || isNotEmpty(skypeName)
                || isNotEmpty(whatsupName) || isNotEmpty(wiberName)
                || isNotEmpty(city);
    }

    private static boolean isNotEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
