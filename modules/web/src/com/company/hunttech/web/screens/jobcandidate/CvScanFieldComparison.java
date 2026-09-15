package com.company.hunttech.web.screens.jobcandidate;

import java.io.Serializable;

/**
 * Модель строки сопоставления поля кандидата с данными, найденными в резюме.
 */
public class CvScanFieldComparison implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        NEW,        // В карточке пусто, в резюме найдено значение
        REPLACE,    // В карточке заполнено, в резюме найдено другое значение
        MATCH,      // Значения совпадают
        EMPTY       // В резюме значение не найдено
    }

    private String fieldId;
    private String fieldCaption;
    private String currentValue;
    private String foundValue;
    private Status status;
    private boolean selected;
    private boolean enabled;

    public CvScanFieldComparison() {
    }

    public CvScanFieldComparison(String fieldId, String fieldCaption, String currentValue, String foundValue, Status status) {
        this.fieldId = fieldId;
        this.fieldCaption = fieldCaption;
        this.currentValue = currentValue;
        this.foundValue = foundValue;
        this.status = status;
        this.selected = (status == Status.NEW || status == Status.REPLACE);
        this.enabled = (status != Status.EMPTY && foundValue != null && !foundValue.trim().isEmpty());
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public String getFieldCaption() {
        return fieldCaption;
    }

    public void setFieldCaption(String fieldCaption) {
        this.fieldCaption = fieldCaption;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public String getFoundValue() {
        return foundValue;
    }

    public void setFoundValue(String foundValue) {
        this.foundValue = foundValue;
        recomputeEnabled();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        recomputeEnabled();
    }

    private void recomputeEnabled() {
        this.enabled = (status == Status.NEW || status == Status.REPLACE)
                && foundValue != null
                && !foundValue.trim().isEmpty()
                && !"—".equals(foundValue.trim());
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
