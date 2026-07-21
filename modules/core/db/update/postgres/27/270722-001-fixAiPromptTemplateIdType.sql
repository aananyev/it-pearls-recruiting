-- Fix HUNTTECH_AI_PROMPT_TEMPLATE primary-key type for AiPromptTemplate.
--
-- AiPromptTemplate extends StandardEntity, therefore EclipseLink/CUBA expects
-- the ID attribute to be returned by PostgreSQL as java.util.UUID. The original
-- create migration used varchar(36), so entity materialization returned String
-- and failed in BaseUuidEntity._persistence_set with ClassCastException.
--
-- Existing seed values are canonical UUID strings and can be converted safely.
-- PostgreSQL also accepts this statement when the column is already uuid.
ALTER TABLE HUNTTECH_AI_PROMPT_TEMPLATE
    ALTER COLUMN ID TYPE uuid
    USING ID::uuid;
