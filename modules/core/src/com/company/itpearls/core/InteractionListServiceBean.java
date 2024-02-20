package com.company.itpearls.core;

import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.core.global.DataManager;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.math.BigDecimal;

@Service(InteractionListService.NAME)
public class InteractionListServiceBean implements InteractionListService {

    @Inject
    private DataManager dataManager;

    @Override
    public BigDecimal getCountIteraction(IteractionList iteractionList) {
        BigDecimal countIteraction = dataManager.loadValue("select count(e.numberIteraction) from itpearls_IteractionList e where e.candidate = :candidate and e.vacancy = :vacancy",
                        BigDecimal.class)
                .parameter("candidate", iteractionList.getCandidate())
                .parameter("vacancy", iteractionList.getVacancy())
                .one();

        return countIteraction;
    }

    private final static String QUERY_GET_MAX_NUMBER_INTERACTION = "select e from itpearls_IteractionList e where e.numberIteraction = (select max(f.numberIteraction) from itpearls_IteractionList f)";


    @Override
    public BigDecimal getCountInteraction() {
        IteractionList e = null;

        try {
            e = dataManager.load(IteractionList.class)
                    .query(QUERY_GET_MAX_NUMBER_INTERACTION)
                    .view("iteractionList-view")
//                    .cacheable(true)
                    .one();
        } catch (IllegalStateException exception) {
            exception.printStackTrace();
        } finally {
            if (e != null) {
                return e.getNumberIteraction();
            } else {
                return BigDecimal.ZERO;
            }
        }
    }
}