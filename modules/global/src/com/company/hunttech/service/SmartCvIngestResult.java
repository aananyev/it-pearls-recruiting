package com.company.hunttech.service;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.JobCandidate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Результат обработки и загрузки резюме.
 */
public class SmartCvIngestResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        SUCCESS,
        DUPLICATE_DETECTED,
        ERROR
    }

    private Status status;
    private String message;
    private JobCandidate candidate;
    private CandidateCV candidateCv;
    private JobCandidate duplicateCandidate;
    private SmartCvParsedData parsedData;
    private List<String> missingFields = new ArrayList<>();
    private AiExecutionResult aiExecution;

    public static SmartCvIngestResult success(JobCandidate candidate, CandidateCV cv, SmartCvParsedData parsedData, List<String> missingFields, AiExecutionResult aiExecution) {
        SmartCvIngestResult res = new SmartCvIngestResult();
        res.setStatus(Status.SUCCESS);
        res.setCandidate(candidate);
        res.setCandidateCv(cv);
        res.setParsedData(parsedData);
        res.setMissingFields(missingFields != null ? missingFields : Collections.emptyList());
        res.setAiExecution(aiExecution);
        return res;
    }

    public static SmartCvIngestResult duplicate(JobCandidate existingCandidate, SmartCvParsedData parsedData, AiExecutionResult aiExecution) {
        SmartCvIngestResult res = new SmartCvIngestResult();
        res.setStatus(Status.DUPLICATE_DETECTED);
        res.setDuplicateCandidate(existingCandidate);
        res.setParsedData(parsedData);
        res.setAiExecution(aiExecution);
        return res;
    }

    public static SmartCvIngestResult error(String errorMessage) {
        SmartCvIngestResult res = new SmartCvIngestResult();
        res.setStatus(Status.ERROR);
        res.setMessage(errorMessage);
        return res;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public JobCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(JobCandidate candidate) {
        this.candidate = candidate;
    }

    public CandidateCV getCandidateCv() {
        return candidateCv;
    }

    public void setCandidateCv(CandidateCV candidateCv) {
        this.candidateCv = candidateCv;
    }

    public JobCandidate getDuplicateCandidate() {
        return duplicateCandidate;
    }

    public void setDuplicateCandidate(JobCandidate duplicateCandidate) {
        this.duplicateCandidate = duplicateCandidate;
    }

    public SmartCvParsedData getParsedData() {
        return parsedData;
    }

    public void setParsedData(SmartCvParsedData parsedData) {
        this.parsedData = parsedData;
    }

    public List<String> getMissingFields() {
        return missingFields == null ? Collections.emptyList() : missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public AiExecutionResult getAiExecution() {
        return aiExecution;
    }

    public void setAiExecution(AiExecutionResult aiExecution) {
        this.aiExecution = aiExecution;
    }
}
