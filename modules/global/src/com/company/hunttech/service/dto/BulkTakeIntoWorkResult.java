package com.company.hunttech.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BulkTakeIntoWorkResult implements Serializable {
    private static final long serialVersionUID = 9182379182739182L;

    private int totalSelected;
    private int addedCount;
    private int alreadyInWorkCount;
    private int errorCount;
    private List<String> errorDetails = new ArrayList<>();

    public int getTotalSelected() {
        return totalSelected;
    }

    public void setTotalSelected(int totalSelected) {
        this.totalSelected = totalSelected;
    }

    public int getAddedCount() {
        return addedCount;
    }

    public void setAddedCount(int addedCount) {
        this.addedCount = addedCount;
    }

    public int getAlreadyInWorkCount() {
        return alreadyInWorkCount;
    }

    public void setAlreadyInWorkCount(int alreadyInWorkCount) {
        this.alreadyInWorkCount = alreadyInWorkCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public List<String> getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(List<String> errorDetails) {
        this.errorDetails = errorDetails != null ? errorDetails : new ArrayList<>();
    }
}
