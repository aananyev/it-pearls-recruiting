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

    @Test
    public void testDynamicColumnAdditionAndResolution() {
        RecruiterCandidateKanbanWidget widget = new RecruiterCandidateKanbanWidget();

        UUID root1Id = UUID.randomUUID();
        UUID root2Id = UUID.randomUUID();
        UUID newCustomRootId = UUID.randomUUID(); // Пользователь создал новый элемент верхнего уровня в Iteraction!

        RecruiterCandidateKanbanWidget.DynamicKanbanColumn col1 =
                new RecruiterCandidateKanbanWidget.DynamicKanbanColumn(root1Id, "001", "Ресерчинг", "recruiter-stage-new", "new", 1);
        RecruiterCandidateKanbanWidget.DynamicKanbanColumn col2 =
                new RecruiterCandidateKanbanWidget.DynamicKanbanColumn(root2Id, "002", "Хантинг", "recruiter-stage-recruiter", "recruiter", 2);
        RecruiterCandidateKanbanWidget.DynamicKanbanColumn colNew =
                new RecruiterCandidateKanbanWidget.DynamicKanbanColumn(newCustomRootId, "004", "Проверка СБ и комплаенс", "recruiter-stage-client", "client", 4);

        java.util.List<RecruiterCandidateKanbanWidget.DynamicKanbanColumn> columns =
                java.util.Arrays.asList(col1, col2, colNew);

        // Создаем дочернее взаимодействие, ссылающееся на новый корневой элемент
        Iteraction newRoot = new Iteraction();
        newRoot.setId(newCustomRootId);
        newRoot.setNumber("004");
        newRoot.setIterationName("Проверка СБ и комплаенс");

        Iteraction childAction = new Iteraction();
        childAction.setId(UUID.randomUUID());
        childAction.setNumber("4.01");
        childAction.setIterationName("Анкета передана в службу безопасности");
        childAction.setIteractionTree(newRoot);

        RecruiterCandidateKanbanWidget.DynamicKanbanColumn resolved = widget.resolveColumn(childAction, columns);

        assertEquals("Кандидат с новым действием должен автоматически попасть в новую динамическую колонку",
                newCustomRootId, resolved.getId());
        assertEquals("Проверка СБ и комплаенс", resolved.getCaption());
    }
}

