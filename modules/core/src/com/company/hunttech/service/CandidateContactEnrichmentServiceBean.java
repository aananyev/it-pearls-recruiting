package com.company.hunttech.service;

import com.company.hunttech.config.HunttechContactEnrichmentConfig;
import com.company.hunttech.core.PdfParserService;
import com.company.hunttech.entity.*;
import com.company.hunttech.entity.ai.AiFunctionConfiguration;
import com.company.hunttech.service.dto.CandidateContactsEnrichmentKpiDto;
import com.company.hunttech.service.dto.CandidateContactsScanResult;
import com.company.hunttech.service.dto.CandidateExtractedContactsDto;
import com.company.hunttech.core.ai.AiCostCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.*;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Единая сервисная реализация оркестрации определения контактов и фото кандидатов (Candidate Contact Enrichment).
 */
@Service(CandidateContactEnrichmentService.NAME)
public class CandidateContactEnrichmentServiceBean implements CandidateContactEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(CandidateContactEnrichmentServiceBean.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    private Persistence persistence;
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private Configuration configuration;
    @Inject
    private AiExecutionService aiExecutionService;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private PdfParserService pdfParserService;

    @Nullable
    @Inject
    private CandidateContactsEnrichmentWorker enrichmentWorker;

    @Override
    public void enqueueCandidateCvPriority(UUID candidateCvId) {
        enqueueCandidateCv(candidateCvId, PRIORITY_HIGH);
        if (enrichmentWorker != null) {
            enrichmentWorker.triggerImmediateProcessing();
        }
    }

    @Override
    public void enqueueCandidateCv(UUID candidateCvId, int priority) {
        if (candidateCvId == null) {
            return;
        }

        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();

            CandidateCV cv = em.find(CandidateCV.class, candidateCvId);
            if (cv == null || cv.getCandidate() == null) {
                return;
            }

            CandidateCvContactAnalysis existing = loadAnalysisRecord(em, candidateCvId);
            if (existing != null) {
                if (priority > (existing.getPriority() != null ? existing.getPriority() : 0)) {
                    existing.setPriority(priority);
                }
                if (existing.getStatus() == CandidateCvAnalysisStatus.FRESH) {
                    existing.setStatus(CandidateCvAnalysisStatus.STALE);
                } else if (existing.getStatus() == CandidateCvAnalysisStatus.PROCESSING) {
                    // Если воркер сейчас обрабатывает старую версию резюме, помечаем необходимость пересканирования
                    existing.setStatus(CandidateCvAnalysisStatus.STALE);
                }
            } else {
                CandidateCvContactAnalysis analysis = metadata.create(CandidateCvContactAnalysis.class);
                analysis.setCandidate(cv.getCandidate());
                analysis.setCandidateCv(cv);
                analysis.setStatus(CandidateCvAnalysisStatus.NOT_ANALYZED);
                analysis.setPriority(priority);
                em.persist(analysis);
            }

            tx.commit();
        } catch (Exception e) {
            log.warn("Ошибка постановки CandidateCV {} в очередь анализа контактов: {}", candidateCvId, e.getMessage());
        }
    }

    @Override
    public CandidateContactsScanResult scanAndEnrich(JobCandidate candidate, CandidateCV cv, String aiFunctionCode, boolean isBackground) {
        return scanAndEnrich(candidate, cv, aiFunctionCode, isBackground, false);
    }

    @Override
    public CandidateContactsScanResult scanAndEnrich(JobCandidate candidate, CandidateCV cv, String aiFunctionCode,
                                                     boolean isBackground, boolean forceScan) {
        long startTime = System.currentTimeMillis();
        CandidateContactsScanResult result = new CandidateContactsScanResult();

        if (candidate == null || cv == null) {
            result.setSuccess(false);
            result.setRawError("Кандидат или резюме не заданы");
            return result;
        }

        HunttechContactEnrichmentConfig cfg = configuration.getConfig(HunttechContactEnrichmentConfig.class);
        String effectiveFunctionCode = (aiFunctionCode != null && !aiFunctionCode.trim().isEmpty())
                ? aiFunctionCode : cfg.getAiFunctionCode();

        // 1. Извлечение текста и картинок из оригинала файла (DOCX, PDF и др.) или из textCV
        ExtractedDocumentData docData = extractDocumentData(cv);
        String rawText = docData.text;
        List<byte[]> candidateImages = docData.images;

        if (rawText == null || rawText.trim().isEmpty()) {
            log.info("Текст резюме кандидата {} (CV ID: {}) пуст -> SKIPPED", candidate.getFullName(), cv.getId());
            saveSkippedAnalysisRecord(candidate, cv, effectiveFunctionCode);
            result.setSuccess(true);
            result.setDurationMs(System.currentTimeMillis() - startTime);
            return result;
        }

        String contentHash = calculateNormalizedCvHash(rawText);
        Integer configVersion = loadFunctionVersion(effectiveFunctionCode);

        // Проверка неизменности хэша и версии AI
        CandidateCvContactAnalysis existingAnalysis = loadAnalysisRecord(cv.getId());
        if (existingAnalysis != null && existingAnalysis.getStatus() == CandidateCvAnalysisStatus.FRESH && !forceScan) {
            if (Objects.equals(existingAnalysis.getCvContentHash(), contentHash)
                    && Objects.equals(existingAnalysis.getContactsConfigurationVersion(), configVersion)) {
                log.info("Контакты резюме кандидата {} (CV ID: {}) уже проанализированы актуальной версией, пропуск",
                        candidate.getFullName(), cv.getId());
                result.setSuccess(true);
                result.setDurationMs(System.currentTimeMillis() - startTime);
                return result;
            } else {
                existingAnalysis.setStatus(CandidateCvAnalysisStatus.STALE);
                dataManager.commit(existingAnalysis);
            }
        }

        boolean freeOnly = isBackground && cfg.getFreeOnly();
        log.info("AI-анализ контактов кандидата {} (CV ID: {}, background={}, freeOnly={})",
                candidate.getFullName(), cv.getId(), isBackground, freeOnly);

        // 2. Детекция и сохранение фотографии кандидата (отсечение логотипов)
        byte[] bestPhotoBytes = detectCandidatePhoto(candidateImages);
        boolean photoFound = (bestPhotoBytes != null);
        result.setPhotoExtracted(photoFound);

        // 3. Подготовка текста для AI: экономия токенов (контактная шапка)
        String contactHeaderText = extractContactHeaderSnippet(rawText, 3000);

        // 4. Вызов AI через AiExecutionService (ВНЕ транзакции БД!)
        AiExecutionResult aiExecution = null;
        CandidateExtractedContactsDto extracted = null;

        try {
            Map<String, Object> aiContext = new HashMap<>();
            aiContext.put("sourceText", contactHeaderText);

            aiExecution = aiExecutionService.executeText(effectiveFunctionCode, aiContext);
            result.setAiExecution(aiExecution);

            if (aiExecution != null && aiExecution.getText() != null) {
                extracted = parseContactsJson(aiExecution.getText());
                result.setExtractedContacts(extracted);
            }

            if (extracted == null) {
                extracted = new CandidateExtractedContactsDto();
            }

            // 5. Безопасное сохранение контактов и фото в JobCandidate (защита от блокировок)
            List<String> updatedFields = applyContactsAndPhotoSafe(candidate, cv, extracted, bestPhotoBytes);
            result.setUpdatedFields(updatedFields);
            result.setTotalContactsFound(calculateContactsCount(extracted));
            result.setTotalContactsUpdated(updatedFields.size());

            // 6. Фиксация результата в CandidateCvContactAnalysis
            saveSuccessAnalysisRecord(candidate, cv, contentHash, configVersion, effectiveFunctionCode,
                    aiExecution, extracted, updatedFields, photoFound, System.currentTimeMillis() - startTime);

            result.setSuccess(true);
            result.setDurationMs(System.currentTimeMillis() - startTime);
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Ошибка при AI-анализе контактов кандидата {} (CV ID: {}): {}",
                    candidate.getFullName(), cv.getId(), e.getMessage(), e);
            result.setSuccess(false);
            result.setRawError(e.getMessage());
            result.setDurationMs(duration);

            handleAnalysisError(candidate, cv, contentHash, configVersion, effectiveFunctionCode, e, duration);
            return result;
        }
    }

    /**
     * Извлечение текста и изображений из резюме. В первую очередь проверяется оригинальный файл.
     */
    private ExtractedDocumentData extractDocumentData(CandidateCV cv) {
        ExtractedDocumentData data = new ExtractedDocumentData();

        FileDescriptor fd = cv.getOriginalFileCV();
        if (fd == null) {
            fd = cv.getFileCV();
        }

        if (fd != null && fd.getExtension() != null) {
            String ext = fd.getExtension().toLowerCase();
            try (InputStream is = fileLoader.openStream(fd)) {
                if ("pdf".equals(ext)) {
                    byte[] bytes = is.readAllBytes();
                    try (PDDocument pdDoc = new PDFParser(new RandomAccessReadBuffer(bytes)).parse()) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        data.text = stripper.getText(pdDoc);

                        for (PDPage page : pdDoc.getPages()) {
                            extractImagesFromResources(page.getResources(), data.images);
                        }
                    }
                } else if ("docx".equals(ext)) {
                    try (XWPFDocument doc = new XWPFDocument(is)) {
                        POITextExtractor extractor = new XWPFWordExtractor(doc);
                        data.text = extractor.getText();

                        for (XWPFPictureData pic : doc.getAllPictures()) {
                            if (pic.getData() != null && pic.getData().length > 0) {
                                data.images.add(pic.getData());
                            }
                        }
                    }
                } else if ("doc".equals(ext)) {
                    WordExtractor extractor = new WordExtractor(is);
                    data.text = extractor.getText();
                } else if ("rtf".equals(ext)) {
                    RTFEditorKit kit = new RTFEditorKit();
                    javax.swing.text.Document rtfDoc = kit.createDefaultDocument();
                    try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        kit.read(reader, rtfDoc, 0);
                        data.text = rtfDoc.getText(0, rtfDoc.getLength());
                    }
                } else if ("txt".equals(ext)) {
                    data.text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                log.warn("Ошибка при чтении файла {} (CV ID: {}): {}", fd.getName(), cv.getId(), e.getMessage());
            }
        }

        // Если из файла текст не извлечен, берем сохраненный textCV
        if (data.text == null || data.text.trim().isEmpty()) {
            data.text = cv.getTextCV();
        }

        return data;
    }

    private void extractImagesFromResources(PDResources resources, List<byte[]> targetImages) {
        if (resources == null) return;
        try {
            for (COSName name : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(name);
                if (xObject instanceof PDFormXObject) {
                    extractImagesFromResources(((PDFormXObject) xObject).getResources(), targetImages);
                } else if (xObject instanceof PDImageXObject) {
                    BufferedImage bImg = ((PDImageXObject) xObject).getImage();
                    if (bImg != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bImg, "png", baos);
                        targetImages.add(baos.toByteArray());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Ошибка извлечения изображений из PDF ресурсов: {}", e.getMessage());
        }
    }

    /**
     * Алгоритм распознавания фотографии кандидата и отсечения посторонних логотипов (HeadHunter, Хабр и т.д.).
     */
    @Nullable
    private byte[] detectCandidatePhoto(List<byte[]> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }

        byte[] bestCandidate = null;
        int maxPixels = 0;

        for (byte[] imgBytes : images) {
            if (imgBytes == null || imgBytes.length < 5000) {
                continue; // слишком маленький размер файла
            }

            try (ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes)) {
                BufferedImage img = ImageIO.read(bais);
                if (img == null) continue;

                int width = img.getWidth();
                int height = img.getHeight();

                // 1. Минимальные размеры (отсекаем иконки соцсетей, буллеты, разделители)
                if (width < 100 || height < 100 || (width * height < 15000)) {
                    continue;
                }

                // 2. Aspect Ratio: портретная фотография или квадрат (0.60 .. 1.35)
                double ratio = (double) width / (double) height;
                if (ratio < 0.60 || ratio > 1.35) {
                    // Логотипы hh.ru обычно сильно вытянуты по горизонтали (ratio 3.0 .. 6.0)
                    continue;
                }

                // 3. Отсекаем монохромные плашки и векторные логотипы с малым числом цветов
                if (isMonochromeOrLogo(img)) {
                    continue;
                }

                int pixels = width * height;
                if (pixels > maxPixels) {
                    maxPixels = pixels;
                    bestCandidate = imgBytes;
                }
            } catch (Exception e) {
                log.debug("Ошибка проверки кандидата на фото: {}", e.getMessage());
            }
        }

        return bestCandidate;
    }

    /**
     * Проверка изображения на признаки логотипа (малое разнообразие цветов или прозрачные баннеры).
     */
    private boolean isMonochromeOrLogo(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        Set<Integer> uniqueColors = new HashSet<>();

        // Семплируем 100 точек
        int stepX = Math.max(1, width / 10);
        int stepY = Math.max(1, height / 10);

        for (int x = 0; x < width; x += stepX) {
            for (int y = 0; y < height; y += stepY) {
                int rgb = img.getRGB(x, y);
                uniqueColors.add(rgb);
                if (uniqueColors.size() > 30) {
                    return false; // Достаточное цветовое разнообразие реальной фотографии
                }
            }
        }

        return uniqueColors.size() <= 16; // Меньше 16 цветов на сетке 10x10 -> искусственный логотип/иконка
    }

    /**
     * Выделение контактной шапки документа (первые N символов).
     */
    private String extractContactHeaderSnippet(String rawText, int maxChars) {
        if (rawText == null) return "";
        String clean = rawText.replaceAll("\\r", "").trim();
        if (clean.length() <= maxChars) {
            return clean;
        }
        return clean.substring(0, maxChars);
    }

    /**
     * Парсинг JSON-ответа AI с контактами кандидата.
     */
    private CandidateExtractedContactsDto parseContactsJson(String rawAiText) {
        if (rawAiText == null || rawAiText.trim().isEmpty()) {
            return new CandidateExtractedContactsDto();
        }

        String json = rawAiText.trim();
        if (json.startsWith("```")) {
            int firstNewline = json.indexOf('\n');
            int lastBackticks = json.lastIndexOf("```");
            if (firstNewline != -1 && lastBackticks > firstNewline) {
                json = json.substring(firstNewline + 1, lastBackticks).trim();
            }
        }

        try {
            JsonNode root = MAPPER.readTree(json);
            CandidateExtractedContactsDto dto = new CandidateExtractedContactsDto();
            dto.setPhone(getTextOrNull(root, "phone"));
            dto.setMobilePhone(getTextOrNull(root, "mobilePhone"));
            dto.setEmail(getTextOrNull(root, "email"));
            dto.setTelegramName(cleanTelegram(getTextOrNull(root, "telegramName")));
            dto.setSkypeName(getTextOrNull(root, "skypeName"));
            dto.setWhatsupName(getTextOrNull(root, "whatsupName"));
            dto.setWiberName(getTextOrNull(root, "wiberName"));
            dto.setCity(getTextOrNull(root, "city"));
            return dto;
        } catch (Exception e) {
            log.warn("Не удалось распарсить JSON контактов AI: {} (исходный текст: {})", e.getMessage(), rawAiText);
            return extractContactsFallbackRegex(rawAiText);
        }
    }

    private String getTextOrNull(JsonNode node, String field) {
        if (node.hasNonNull(field)) {
            String val = node.get(field).asText().trim();
            return (!val.isEmpty() && !"null".equalsIgnoreCase(val) && !"—".equals(val) && !"-".equals(val)) ? val : null;
        }
        return null;
    }

    private String cleanTelegram(String tg) {
        if (tg == null) return null;
        String cleaned = tg.trim();
        if (cleaned.startsWith("@")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("https://t.me/")) {
            cleaned = cleaned.substring("https://t.me/".length());
        }
        if (cleaned.startsWith("t.me/")) {
            cleaned = cleaned.substring("t.me/".length());
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    private CandidateExtractedContactsDto extractContactsFallbackRegex(String text) {
        CandidateExtractedContactsDto dto = new CandidateExtractedContactsDto();
        if (text == null) return dto;

        // Поиск email
        Matcher emailMatcher = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}").matcher(text);
        if (emailMatcher.find()) {
            dto.setEmail(emailMatcher.group());
        }

        // Поиск телефона
        Matcher phoneMatcher = Pattern.compile("(\\+?[78][\\s-]?)?(\\(?\\d{3}\\)?[\\s-]?)?\\d{3}[\\s-]?\\d{2}[\\s-]?\\d{2}").matcher(text);
        if (phoneMatcher.find()) {
            dto.setPhone(phoneMatcher.group().trim());
        }

        // Поиск Telegram
        Matcher tgMatcher = Pattern.compile("@([a-zA-Z0-9_]{5,32})").matcher(text);
        if (tgMatcher.find()) {
            dto.setTelegramName(tgMatcher.group(1));
        }

        return dto;
    }

    /**
     * Безопасное сохранение контактов и фото в карточку кандидата с защитой от блокировок.
     */
    private List<String> applyContactsAndPhotoSafe(JobCandidate candidate, CandidateCV cv,
                                                   CandidateExtractedContactsDto extracted,
                                                   @Nullable byte[] photoBytes) {
        List<String> updatedFields = new ArrayList<>();

        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();

            JobCandidate target = em.find(JobCandidate.class, candidate.getId());
            if (target == null) {
                return updatedFields;
            }
            em.getDelegate().refresh(target); // актуализация версии

            CandidateCV targetCv = em.find(CandidateCV.class, cv.getId());

            // 1. Применение контактов (не затираем существующие значения пустыми)
            if (isNotBlank(extracted.getEmail()) && isBlank(target.getEmail())) {
                target.setEmail(extracted.getEmail());
                updatedFields.add("email");
            }

            if (isNotBlank(extracted.getPhone()) && isBlank(target.getPhone())) {
                target.setPhone(extracted.getPhone());
                updatedFields.add("phone");
            }

            if (isNotBlank(extracted.getMobilePhone()) && isBlank(target.getMobilePhone())) {
                target.setMobilePhone(extracted.getMobilePhone());
                updatedFields.add("mobilePhone");
            } else if (isNotBlank(extracted.getPhone()) && isBlank(target.getMobilePhone())) {
                target.setMobilePhone(extracted.getPhone());
                updatedFields.add("mobilePhone");
            }

            if (isNotBlank(extracted.getTelegramName()) && isBlank(target.getTelegramName())) {
                target.setTelegramName(extracted.getTelegramName());
                updatedFields.add("telegramName");
            }

            if (isNotBlank(extracted.getSkypeName()) && isBlank(target.getSkypeName())) {
                target.setSkypeName(extracted.getSkypeName());
                updatedFields.add("skypeName");
            }

            if (isNotBlank(extracted.getWhatsupName()) && isBlank(target.getWhatsupName())) {
                target.setWhatsupName(extracted.getWhatsupName());
                updatedFields.add("whatsupName");
            }

            if (isNotBlank(extracted.getWiberName()) && isBlank(target.getWiberName())) {
                target.setWiberName(extracted.getWiberName());
                updatedFields.add("wiberName");
            }

            // Город проживания (если не заполнен)
            if (isNotBlank(extracted.getCity()) && target.getCityOfResidence() == null) {
                List<City> cities = em.createQuery(
                        "select c from hunttech_City c where lower(c.cityRu) = :cName or lower(c.cityEn) = :cName", City.class)
                        .setParameter("cName", extracted.getCity().toLowerCase())
                        .setMaxResults(1)
                        .getResultList();
                if (!cities.isEmpty()) {
                    target.setCityOfResidence(cities.get(0));
                    updatedFields.add("cityOfResidence");
                }
            }

            // 2. Применение фото кандидата (требование 11: FileDescriptor + BLOB imageByteArray)
            if (photoBytes != null && target.getFileImageFace() == null && target.getImageByteArray() == null) {
                FileDescriptor photoFd = metadata.create(FileDescriptor.class);
                photoFd.setName("candidate_face_" + target.getId() + ".png");
                photoFd.setExtension("png");
                photoFd.setSize((long) photoBytes.length);
                photoFd.setCreateDate(new Date());

                try {
                    fileLoader.saveStream(photoFd, () -> new ByteArrayInputStream(photoBytes));
                    em.persist(photoFd);

                    target.setFileImageFace(photoFd);
                    target.setImageByteArray(photoBytes);
                    updatedFields.add("fileImageFace");
                    updatedFields.add("imageByteArray");

                    if (targetCv != null) {
                        targetCv.setFileImageFace(photoFd);
                        targetCv.setImageByteArray(photoBytes);
                    }
                } catch (Exception e) {
                    log.warn("Не удалось сохранить файл фото кандидата {}: {}", target.getId(), e.getMessage());
                }
            }

            if (targetCv != null) {
                targetCv.setContactInfoChecked(true);
            }

            em.merge(target);
            tx.commit();

        } catch (Exception e) {
            log.warn("Конфликт или ошибка блокировки при сохранении контактов кандидата {}: {}",
                    candidate.getId(), e.getMessage());
            throw new RuntimeException("Блокировка сохранения контактов: " + e.getMessage(), e);
        }

        return updatedFields;
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private int calculateContactsCount(CandidateExtractedContactsDto dto) {
        if (dto == null) return 0;
        int count = 0;
        if (isNotBlank(dto.getPhone())) count++;
        if (isNotBlank(dto.getMobilePhone())) count++;
        if (isNotBlank(dto.getEmail())) count++;
        if (isNotBlank(dto.getTelegramName())) count++;
        if (isNotBlank(dto.getSkypeName())) count++;
        if (isNotBlank(dto.getWhatsupName())) count++;
        if (isNotBlank(dto.getWiberName())) count++;
        if (isNotBlank(dto.getCity())) count++;
        return count;
    }

    private void saveSuccessAnalysisRecord(JobCandidate candidate, CandidateCV cv,
                                           String contentHash, Integer configVersion,
                                           String effectiveFunctionCode,
                                           AiExecutionResult aiExecution,
                                           CandidateExtractedContactsDto extracted,
                                           List<String> updatedFields,
                                           boolean photoFound,
                                           long durationMs) {
        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();
            CandidateCvContactAnalysis a = loadAnalysisRecord(em, cv.getId());
            if (a == null) {
                a = metadata.create(CandidateCvContactAnalysis.class);
                a.setCandidate(candidate);
                a.setCandidateCv(cv);
                em.persist(a);
            }

            a.setStatus(CandidateCvAnalysisStatus.FRESH);
            a.setCvContentHash(contentHash);
            a.setContactsAnalyzedAt(new Date());
            a.setProcessingFinishedAt(new Date());
            a.setDurationMs(durationMs);
            a.setContactsConfigurationVersion(configVersion);
            a.setAiFunctionCode(effectiveFunctionCode);
            a.setLastError(null);
            a.setRetryCount(0);
            a.setNextRetryAt(null);
            a.setPriority(CandidateContactEnrichmentService.PRIORITY_DEFAULT);

            if (aiExecution != null) {
                a.setProviderCode(aiExecution.getProviderCode());
                a.setModelName(aiExecution.getModelName());
                a.setExecutionSource(CandidateCvContactAnalysis.EXECUTION_SOURCE_AI);
                a.setPromptTokens(aiExecution.getPromptTokens());
                a.setCompletionTokens(aiExecution.getCompletionTokens());
                a.setTotalTokens(aiExecution.getTotalTokens());
                AiCostCalculator.CostResult costResult = AiCostCalculator.calculateCost(
                        aiExecution.getProviderCode(), aiExecution.getModelName(),
                        aiExecution.getPromptTokens(), aiExecution.getCompletionTokens());
                a.setEstimatedCost(costResult != null ? costResult.getCost() : null);
            }

            a.setContactsFoundCount(calculateContactsCount(extracted));
            a.setContactsUpdatedCount(updatedFields.size());
            a.setPhotoExtracted(photoFound);
            a.setExtractedPhone(extracted.getPhone());
            a.setExtractedEmail(extracted.getEmail());
            a.setExtractedTelegram(extracted.getTelegramName());
            a.setExtractedCity(extracted.getCity());

            Map<String, Object> deltaMap = new LinkedHashMap<>();
            deltaMap.put("extracted", extracted);
            deltaMap.put("updatedFields", updatedFields);
            deltaMap.put("photoFound", photoFound);
            try {
                a.setDeltaDetailsJson(MAPPER.writeValueAsString(deltaMap));
            } catch (Exception ignored) {
            }

            tx.commit();
        } catch (Exception e) {
            log.error("Ошибка сохранения успешного анализа контактов: {}", e.getMessage(), e);
        }
    }

    private void handleAnalysisError(JobCandidate candidate, CandidateCV cv,
                                    String contentHash, Integer configVersion,
                                    String effectiveFunctionCode,
                                    Exception error, long durationMs) {
        HunttechContactEnrichmentConfig cfg = configuration.getConfig(HunttechContactEnrichmentConfig.class);
        int maxRetries = Math.max(1, cfg.getMaxRetries());

        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();
            CandidateCvContactAnalysis a = loadAnalysisRecord(em, cv.getId());
            if (a == null) {
                a = metadata.create(CandidateCvContactAnalysis.class);
                a.setCandidate(candidate);
                a.setCandidateCv(cv);
                em.persist(a);
            }

            int currentRetries = a.getRetryCount() != null ? a.getRetryCount() : 0;
            currentRetries++;
            a.setRetryCount(currentRetries);
            a.setProcessingFinishedAt(new Date());
            a.setDurationMs(durationMs);
            a.setCvContentHash(contentHash);
            a.setContactsConfigurationVersion(configVersion);
            a.setAiFunctionCode(effectiveFunctionCode);
            a.setLastError(error.getMessage() != null && error.getMessage().length() > 1900
                    ? error.getMessage().substring(0, 1900) : error.getMessage());

            if (currentRetries >= maxRetries) {
                a.setStatus(CandidateCvAnalysisStatus.ERROR);
                a.setNextRetryAt(null);
                log.warn("CandidateCV ID: {} исчерпал лимит попыток ({}/{}) -> статус ERROR",
                        cv.getId(), currentRetries, maxRetries);
            } else {
                a.setStatus(CandidateCvAnalysisStatus.RETRY);
                // Экспоненциальная пауза: 1м, 5м, 15м, 60м
                long delayMs = (long) Math.pow(currentRetries, 2) * 60_000L;
                a.setNextRetryAt(new Date(System.currentTimeMillis() + delayMs));
                log.info("CandidateCV ID: {} переведен в RETRY (попытка {}/{}, следующий запуск через {} сек)",
                        cv.getId(), currentRetries, maxRetries, delayMs / 1000);
            }

            tx.commit();
        } catch (Exception e) {
            log.error("Ошибка сохранения статуса ошибки анализа контактов: {}", e.getMessage(), e);
        }
    }

    private void saveSkippedAnalysisRecord(JobCandidate candidate, CandidateCV cv, String aiFunctionCode) {
        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();
            CandidateCvContactAnalysis a = loadAnalysisRecord(em, cv.getId());
            if (a == null) {
                a = metadata.create(CandidateCvContactAnalysis.class);
                a.setCandidate(candidate);
                a.setCandidateCv(cv);
                em.persist(a);
            }
            a.setStatus(CandidateCvAnalysisStatus.SKIPPED);
            a.setLastError("Текст резюме пуст или файл не содержит текстовой информации");
            a.setProcessingFinishedAt(new Date());
            a.setAiFunctionCode(aiFunctionCode);
            tx.commit();
        } catch (Exception e) {
            log.warn("Ошибка сохранения SKIPPED записи анализа контактов: {}", e.getMessage());
        }
    }

    @Override
    public String calculateNormalizedCvHash(String rawCvText) {
        if (rawCvText == null || rawCvText.trim().isEmpty()) {
            return null;
        }
        String normalized = rawCvText.replaceAll("\\s+", " ").trim().toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public void reprocessCv(UUID candidateCvId) {
        if (candidateCvId == null) return;
        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();
            CandidateCvContactAnalysis a = loadAnalysisRecord(em, candidateCvId);
            if (a != null) {
                a.setStatus(CandidateCvAnalysisStatus.NOT_ANALYZED);
                a.setPriority(PRIORITY_HIGH);
                a.setRetryCount(0);
                a.setLastError(null);
                a.setNextRetryAt(null);
            }
            tx.commit();
        }
        if (enrichmentWorker != null) {
            enrichmentWorker.triggerImmediateProcessing();
        }
    }

    @Override
    public CandidateContactsEnrichmentKpiDto getKpiMetrics() {
        CandidateContactsEnrichmentKpiDto kpi = new CandidateContactsEnrichmentKpiDto();
        HunttechContactEnrichmentConfig cfg = configuration.getConfig(HunttechContactEnrichmentConfig.class);

        kpi.setWorkerEnabled(cfg.getEnabled());
        if (!cfg.getEnabled()) {
            kpi.setWorkerStatus("ВЫКЛЮЧЕН");
            kpi.setWorkerStatusDetail("Служба CandidateContactsEnrichmentWorker выключена");
        } else {
            kpi.setWorkerStatus("АКТИВЕН");
            kpi.setWorkerStatusDetail("Служба CandidateContactsEnrichmentWorker активна");
        }

        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();

            // Счетчики статусов
            List<Object[]> statusCounts = em.createQuery(
                    "select a.status, count(a) from hunttech_CandidateCvContactAnalysis a group by a.status")
                    .getResultList();

            long totalAnalyzedRecords = 0;
            for (Object[] row : statusCounts) {
                Integer st = (Integer) row[0];
                long cnt = ((Number) row[1]).longValue();
                totalAnalyzedRecords += cnt;
                if (st != null) {
                    CandidateCvAnalysisStatus status = CandidateCvAnalysisStatus.fromId(st);
                    if (status != null) {
                        switch (status) {
                            case FRESH: kpi.setFreshCount(cnt); break;
                            case NOT_ANALYZED: kpi.setNotAnalyzedCount(cnt); break;
                            case STALE: kpi.setStaleCount(cnt); break;
                            case RETRY: kpi.setRetryCount(cnt); break;
                            case ERROR: kpi.setErrorCount(cnt); break;
                            case SKIPPED: kpi.setSkippedCount(cnt); break;
                            case PROCESSING: kpi.setProcessingCount(cnt); break;
                        }
                    }
                }
            }

            // Общее количество пригодных CV
            Number totalCv = (Number) em.createQuery(
                    "select count(cv) from hunttech_CandidateCV cv where cv.textCV is not null or cv.fileCV is not null or cv.originalFileCV is not null")
                    .getSingleResult();
            kpi.setTotalEligibleCvCount(totalCv != null ? totalCv.longValue() : 0);

            // Временные срезы
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            Date startOfToday = cal.getTime();

            Number todayCount = (Number) em.createQuery(
                    "select count(a) from hunttech_CandidateCvContactAnalysis a " +
                            "where a.status = :fresh and a.processingFinishedAt >= :startToday")
                    .setParameter("fresh", CandidateCvAnalysisStatus.FRESH.getId())
                    .setParameter("startToday", startOfToday)
                    .getSingleResult();
            kpi.setProcessedToday(todayCount != null ? todayCount.longValue() : 0);

            Date past24h = new Date(System.currentTimeMillis() - 24 * 3600_000L);
            Number count24h = (Number) em.createQuery(
                    "select count(a) from hunttech_CandidateCvContactAnalysis a " +
                            "where a.status = :fresh and a.processingFinishedAt >= :past24h")
                    .setParameter("fresh", CandidateCvAnalysisStatus.FRESH.getId())
                    .setParameter("past24h", past24h)
                    .getSingleResult();
            kpi.setProcessed24h(count24h != null ? count24h.longValue() : 0);

            Date past7d = new Date(System.currentTimeMillis() - 7 * 24 * 3600_000L);
            Number count7d = (Number) em.createQuery(
                    "select count(a) from hunttech_CandidateCvContactAnalysis a " +
                            "where a.status = :fresh and a.processingFinishedAt >= :past7d")
                    .setParameter("fresh", CandidateCvAnalysisStatus.FRESH.getId())
                    .setParameter("past7d", past7d)
                    .getSingleResult();
            kpi.setProcessed7d(count7d != null ? count7d.longValue() : 0);

            // Контакты и фото
            Number contactsToday = (Number) em.createQuery(
                    "select coalesce(sum(a.contactsFoundCount), 0) from hunttech_CandidateCvContactAnalysis a " +
                            "where a.processingFinishedAt >= :startToday")
                    .setParameter("startToday", startOfToday)
                    .getSingleResult();
            kpi.setTotalContactsExtractedToday(contactsToday != null ? contactsToday.longValue() : 0);

            Number photosToday = (Number) em.createQuery(
                    "select count(a) from hunttech_CandidateCvContactAnalysis a " +
                            "where a.photoExtracted = true and a.processingFinishedAt >= :startToday")
                    .setParameter("startToday", startOfToday)
                    .getSingleResult();
            kpi.setTotalPhotosExtractedToday(photosToday != null ? photosToday.longValue() : 0);

            Number contactsAll = (Number) em.createQuery(
                    "select coalesce(sum(a.contactsFoundCount), 0) from hunttech_CandidateCvContactAnalysis a")
                    .getSingleResult();
            kpi.setTotalContactsExtractedAllTime(contactsAll != null ? contactsAll.longValue() : 0);

            Number photosAll = (Number) em.createQuery(
                    "select count(a) from hunttech_CandidateCvContactAnalysis a where a.photoExtracted = true")
                    .getSingleResult();
            kpi.setTotalPhotosExtractedAllTime(photosAll != null ? photosAll.longValue() : 0);

            // Токены и стоимость за сегодня
            Object[] tokenSums = (Object[]) em.createQuery(
                    "select count(a), coalesce(sum(a.promptTokens), 0), coalesce(sum(a.completionTokens), 0), " +
                            "coalesce(sum(a.totalTokens), 0), coalesce(sum(a.estimatedCost), 0) " +
                            "from hunttech_CandidateCvContactAnalysis a where a.processingFinishedAt >= :startToday")
                    .setParameter("startToday", startOfToday)
                    .getSingleResult();

            if (tokenSums != null) {
                kpi.setAiRequestsToday(((Number) tokenSums[0]).longValue());
                kpi.setPromptTokensToday(((Number) tokenSums[1]).longValue());
                kpi.setCompletionTokensToday(((Number) tokenSums[2]).longValue());
                kpi.setTotalTokensToday(((Number) tokenSums[3]).longValue());
                kpi.setEstimatedCostToday((BigDecimal) tokenSums[4]);
            }

            // Скорость и ETA
            double speed = kpi.getProcessed24h() / 24.0;
            kpi.setProcessingSpeedPerHour(BigDecimal.valueOf(speed).setScale(1, RoundingMode.HALF_UP).doubleValue());

            long remaining = kpi.getTotalEligibleCvCount() - kpi.getFreshCount();
            if (remaining <= 0) {
                kpi.setEstimatedEtaText("Очередь завершена");
            } else if (speed > 0) {
                double hoursLeft = remaining / speed;
                if (hoursLeft < 1.0) {
                    kpi.setEstimatedEtaText(String.format("~%d мин", Math.max(1, (int) (hoursLeft * 60))));
                } else if (hoursLeft < 48.0) {
                    kpi.setEstimatedEtaText(String.format("~%.1f ч", hoursLeft));
                } else {
                    kpi.setEstimatedEtaText(String.format("~%.1f дн", hoursLeft / 24.0));
                }
            } else {
                kpi.setEstimatedEtaText("расчет...");
            }
        } catch (Exception e) {
            log.warn("Ошибка расчета KPI контактов: {}", e.getMessage());
        }

        return kpi;
    }

    @Override
    public void setWorkerEnabled(boolean enabled) {
        HunttechContactEnrichmentConfig cfg = configuration.getConfig(HunttechContactEnrichmentConfig.class);
        cfg.setEnabled(enabled);
    }

    @Override
    public boolean isWorkerEnabled() {
        return configuration.getConfig(HunttechContactEnrichmentConfig.class).getEnabled();
    }

    @Override
    public void runSingleCycleNow() {
        if (enrichmentWorker != null) {
            enrichmentWorker.triggerImmediateProcessing();
        }
    }

    private CandidateCvContactAnalysis loadAnalysisRecord(UUID cvId) {
        return dataManager.load(CandidateCvContactAnalysis.class)
                .query("select a from hunttech_CandidateCvContactAnalysis a where a.candidateCv.id = :cvId")
                .parameter("cvId", cvId)
                .optional()
                .orElse(null);
    }

    private CandidateCvContactAnalysis loadAnalysisRecord(EntityManager em, UUID cvId) {
        List<CandidateCvContactAnalysis> list = em.createQuery(
                "select a from hunttech_CandidateCvContactAnalysis a where a.candidateCv.id = :cvId", CandidateCvContactAnalysis.class)
                .setParameter("cvId", cvId)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    private Integer loadFunctionVersion(String functionCode) {
        if (functionCode == null) return 1;
        try {
            return dataManager.loadValue(
                    "select f.configurationVersion from hunttech_AiFunctionConfiguration f where f.code = :code", Integer.class)
                    .parameter("code", functionCode)
                    .optional()
                    .orElse(1);
        } catch (Exception e) {
            return 1;
        }
    }

    private static class ExtractedDocumentData {
        String text;
        List<byte[]> images = new ArrayList<>();
    }
}
