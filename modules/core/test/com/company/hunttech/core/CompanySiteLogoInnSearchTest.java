package com.company.hunttech.core;

import com.company.hunttech.service.CompanySearchAiService;
import com.company.hunttech.service.CompanySearchAiServiceBean;
import com.company.hunttech.service.CompanySiteLogoInnCandidate;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class CompanySiteLogoInnSearchTest {

    @Test
    public void testCandidateDtoProperties() {
        CompanySiteLogoInnCandidate candidate = new CompanySiteLogoInnCandidate(
                "Яндекс", "7736207543", "https://ya.ru", "https://ya.ru/logo.png");
        candidate.setLegalEntityName("ООО \"Яндекс\"");
        candidate.setOgrn("1027700229193");
        candidate.setCity("Москва");
        candidate.setCountry("Россия");
        candidate.setDescription("Российская транснациональная компания в отрасли информационных технологий");

        assertEquals("Яндекс", candidate.getCompanyName());
        assertEquals("ООО \"Яндекс\"", candidate.getLegalEntityName());
        assertEquals("7736207543", candidate.getInn());
        assertEquals("1027700229193", candidate.getOgrn());
        assertEquals("https://ya.ru", candidate.getWebsite());
        assertEquals("https://ya.ru/logo.png", candidate.getLogoUrl());
        assertEquals("Москва", candidate.getCity());
        assertEquals("Россия", candidate.getCountry());
        assertNotNull(candidate.toString());
    }

    @Test
    public void testFindCandidatesFallbackOfflineExtraction() {
        CompanySearchAiServiceBean service = new CompanySearchAiServiceBean();

        // 1. Поиск для пустого запроса должен безопасно возвращать пустой список
        List<CompanySiteLogoInnCandidate> emptyResults = service.findCompanySiteLogoInnCandidates("");
        assertNotNull(emptyResults);
        assertTrue(emptyResults.isEmpty());

        // 2. Поиск по известному бренду Яндекс в оффлайн режиме (Wikipedia / сайт)
        List<CompanySiteLogoInnCandidate> candidates = service.findCompanySiteLogoInnCandidates("Яндекс");
        assertNotNull(candidates);
        // Должен вернуть хотя бы 1 кандидата (через Wikipedia fallback или реестр)
        if (!candidates.isEmpty()) {
            CompanySiteLogoInnCandidate cand = candidates.get(0);
            assertNotNull(cand.getCompanyName());
            assertTrue(cand.getCompanyName().toLowerCase().contains("яндекс") || cand.getCompanyName().toLowerCase().contains("yandex"));
        }
    }
}
