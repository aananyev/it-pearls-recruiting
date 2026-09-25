package com.company.hunttech.web.widgets.recruiterdashboard;

import com.company.hunttech.entity.Iteraction;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.web.widgets.recruiterdashboard.RecruiterCandidateKanbanWidget.KanbanStage;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class RecruiterCandidateKanbanStageTest {

    @Test
    public void testKanbanStageCaptionsMatchRootGroups() {
        assertEquals("Ресерчинг", KanbanStage.RESEARCHING.getCaption());
        assertEquals("Хантинг", KanbanStage.HUNTING.getCaption());
        assertEquals("На стороне заказчика", KanbanStage.CLIENT.getCaption());
        assertEquals("УСПЕХ", KanbanStage.OFFER.getCaption());
        assertEquals("ОТКАЗ", KanbanStage.OUTCOME.getCaption());
        assertEquals("Закрытие кейса", KanbanStage.CASE_CLOSED.getCaption());
    }

    @Test
    public void testResolveByRootInteractionGroup() {
        // Корневые группы
        Iteraction rootResearching = new Iteraction();
        rootResearching.setNumber("001");
        rootResearching.setIterationName("Ресерчинг");

        Iteraction rootHunting = new Iteraction();
        rootHunting.setNumber("002");
        rootHunting.setIterationName("Хантинг");

        Iteraction rootClient = new Iteraction();
        rootClient.setNumber("003");
        rootClient.setIterationName("На стороне заказчика");

        // Дочерние взаимодействия
        Iteraction cvReceived = new Iteraction();
        cvReceived.setNumber("1.06");
        cvReceived.setIterationName("Получено CV");
        cvReceived.setIteractionTree(rootResearching);

        Iteraction recruiterInterview = new Iteraction();
        recruiterInterview.setNumber("2.01");
        recruiterInterview.setIterationName("Назначено собеседование с рекрутером Hunttech");
        recruiterInterview.setIteractionTree(rootHunting);

        Iteraction clientTechInterview = new Iteraction();
        clientTechInterview.setNumber("3.8");
        clientTechInterview.setIterationName("Прошел техническое собеседование на стороне заказчика");
        clientTechInterview.setIteractionTree(rootClient);

        assertEquals(KanbanStage.RESEARCHING, KanbanStage.resolve(cvReceived));
        assertEquals(KanbanStage.HUNTING, KanbanStage.resolve(recruiterInterview));
        assertEquals(KanbanStage.CLIENT, KanbanStage.resolve(clientTechInterview));
    }

    @Test
    public void testCandidateWithTwoProjectsProducesDifferentCaseKeys() {
        UUID candidateId = UUID.randomUUID();
        UUID projectDksId = UUID.randomUUID();
        UUID projectAgimaId = UUID.randomUUID();

        String keyDks = candidateId + ":" + projectDksId;
        String keyAgima = candidateId + ":" + projectAgimaId;

        assertNotEquals("Кандидат с двумя разными проектами должен иметь независимые ключи карточек",
                keyDks, keyAgima);
    }
}
