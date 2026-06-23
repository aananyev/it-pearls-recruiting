package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.core.*;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.StandartRoles;
import com.company.itpearls.web.screens.fragments.Skillsbar;
import com.company.itpearls.web.screens.openposition.openpositionfragments.OpenPositionDetailScreenFragment;
import com.company.itpearls.web.screens.openposition.openpositionviews.QuickViewOpenPositionDescription;
import com.company.itpearls.web.screens.openpositioncomment.OpenPositionCommentEdit;
import com.company.itpearls.web.screens.openpositioncomment.OpenPositionCommentsView;
import com.company.itpearls.web.screens.recrutiestasks.RecrutiesTasksGroupSubscribeBrowse;
import com.company.itpearls.web.screens.hrmasters.suggestjobcandidates.Suggestjobcandidate;
import com.company.itpearls.web.screens.simplebrowsers.JobCandidateSimpleBrowse;
import com.company.itpearls.web.screens.simplebrowsers.JobCandidateSimpleMailBrowse;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.reports.entity.Report;
import com.haulmont.reports.gui.ReportGuiManager;
import com.haulmont.reports.gui.actions.list.ListPrintFormAction;
import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicReference;

@UiController("itpearls_OpenPosition.browse")
@UiDescriptor("open-position-browse.xml")
@LookupComponent("openPositionsTable")
@LoadDataBeforeShow
public class OpenPositionBrowse extends StandardLookup<OpenPosition> {

    @Inject
    private MessageBundle messageBundle;
    @Inject
    private PopupButton setRatingButton;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private Metadata metadata;
    @Inject
    private SendNotificationsService sendNotificationsService;

    private final static String h4_tag = "<h4>";
    private final static String h4_tag_a = "</h4>";
    private final static String br_tag_a = "</br>";
    private final static String width_20px = "20px";
    private final static String width_30px = "30px";
    private final static String width_50px = "50px";
    private final static String style_icon_no_border_50px = "icon-no-border-50px";
    private final static String style_table_wordwrap = "table-wordwrap";
    private final static String style_circle_30px = "circle-30px";

    private final static String icons_no_company_png = "icons/no-company.png";
    private final static String icons_traffic_lights_gray_png = "icons/traffic-lights_gray.png";
    private final static String icons_traffic_lights_blue_png = "icons/traffic-lights_blue.png";
    private final static String icons_traffic_lights_green_png = "icons/traffic-lights_green.png";
    private final static String icons_traffic_lights_yellow_png = "icons/traffic-lights_yellow.png";
    private final static String icons_traffic_lights_red_png = "icons/traffic-lights_red.png";
    private final static String icons_remove_png = "icons/remove.png";

    private static final String QUERY_URGENTLY_POSITIONS =
            "select e from itpearls_OpenPosition e where e.openClose = false and e.priority >= :priority";
    private static final String QUERY_PARENT_OPENPOSITION =
            "select e from itpearls_OpenPosition e where e.parentOpenPosition = :parentOpenPosition";
    private static final String QUERY_ACTIVE_RECRUITERS_COUNT_BY_POSITIONS =
            "select e.openPosition.id as openPositionId, count(e.reacrutier) as cnt from itpearls_RecrutiesTasks e "
                    + "where e.openPosition.id in :ids and e.closed = false and e.endDate >= :currentDate "
                    + "group by e.openPosition.id";
    private static final String QUERY_SENT_CV_COUNT_BY_POSITIONS =
            "select e.vacancy.id as vacancyId, count(e) as cnt from itpearls_IteractionList e "
                    + "where e.vacancy.id in :ids and e.iteractionType.signSendToClient = true "
                    + "and e.vacancy.lastOpenDate < e.dateIteraction "
                    + "group by e.vacancy.id";
    private static final String QUERY_AVG_RATING_BY_POSITIONS =
            "select e.openPosition.id as openPositionId, avg(e.rating) as avgRating from itpearls_OpenPositionComment e "
                    + "where e.openPosition.id in :ids and not e.rating is null "
                    + "group by e.openPosition.id";

    private final static String separatorChar = "⎯";
    private final static String separator = separatorChar.repeat(22);
    @Inject
    private OpenPositionService openPositionService;
    @Inject
    private TelegramService telegramService;
    @Inject
    private ApplicationSetupService applicationSetupService;

    @Install(to = "openPositionsTable.projectLogoColumn", subject = "columnGenerator")
    private Object openPositionsTableProjectLogoColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        Image image = uiComponents.create(Image.class);
        image.setDescriptionAsHtml(true);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth(width_50px);
        image.setHeight(width_50px);
        image.setStyleName(style_icon_no_border_50px);
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (event.getItem().getProjectName() != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(h4_tag);
            sb.append(event.getItem().getProjectName().getProjectName());
            sb.append(h4_tag_a);
            sb.append(br_tag_a);
            sb.append(br_tag_a);
            if (event.getItem().getProjectName().getId() != null) {
                UUID projectId = event.getItem().getProjectName().getId();
                if (projectDescriptionExistsCache.getOrDefault(projectId, false)) {
                    String description = getLazyProjectDescription(projectId);
                    if (description != null) {
                        sb.append(description);
                        image.setDescription(sb.toString());
                    }
                }
            }

            if (event.getItem().getProjectName().getProjectLogo() != null) {
                image.setSource(FileDescriptorResource.class)
                        .setFileDescriptor(event
                                .getItem()
                                .getProjectName()
                                .getProjectLogo());
            } else {
                image.setSource(ThemeResource.class).setPath(icons_no_company_png);
            }
        }

        retBox.add(image);
        return retBox;
    }

    @Install(to = "openPositionsTable.companyLogoColumn", subject = "columnGenerator")
    private Object openPositionsTableCompanyLogoColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        Image image = uiComponents.create(Image.class);
        image.setDescriptionAsHtml(true);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth(width_50px);
        image.setHeight(width_50px);
        image.setStyleName(style_icon_no_border_50px);
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (event.getItem().getProjectName() != null) {
            if (event.getItem().getProjectName().getProjectDepartment() != null) {
                if (event.getItem().getProjectName().getProjectDepartment().getCompanyName() != null) {
                    UUID companyId = event.getItem().getProjectName().getProjectDepartment().getCompanyName().getId();
                    if (companyId != null && companyDescriptionExistsCache.getOrDefault(companyId, false)) {
                        String companyDescription = getLazyCompanyDescription(companyId);
                        if (companyDescription != null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(h4_tag);
                            sb.append(event.getItem().getProjectName().getProjectDepartment().getCompanyName().getComanyName());
                            sb.append(h4_tag_a);
                            sb.append(br_tag_a);
                            sb.append(br_tag_a);
                            sb.append(companyDescription);

                            image.setDescription(sb.toString());
                        }
                    }

                    if (event.getItem().getProjectName().getProjectDepartment().getCompanyName().getFileCompanyLogo() != null) {
                        image.setSource(FileDescriptorResource.class)
                                .setFileDescriptor(event
                                        .getItem()
                                        .getProjectName()
                                        .getProjectDepartment()
                                        .getCompanyName()
                                        .getFileCompanyLogo());
                    } else {
                        image.setSource(ThemeResource.class).setPath(String.valueOf(StdPictures.NO_COMPANY));
                    }
                }
            }
        }

        retBox.add(image);
        return retBox;
    }


    private static final String NULL_SALARY = "0 т.р./0 т.р.";
    @Inject
    private CollectionLoader<OpenPosition> openPositionsDl;

    private static final String QUERY_OPEN_POSITION_COMMENT_EXISTS =
            "select e.id from itpearls_OpenPosition e "
                    + "where e.id in :ids and e.comment is not null and length(e.comment) > 0";
    private static final String QUERY_OPEN_POSITION_COMMENT_POSITIVE =
            "select e.id from itpearls_OpenPosition e "
                    + "where e.id in :ids and e.comment is not null and length(e.comment) > 0 "
                    + "and lower(e.comment) not like :negPrefix";
    private static final String QUERY_OPEN_POSITION_EXERCISE_EXISTS =
            "select e.id from itpearls_OpenPosition e "
                    + "where e.id in :ids and e.exercise is not null and length(e.exercise) > 0";
    private static final String QUERY_OPEN_POSITION_MEMO_EXISTS =
            "select e.id from itpearls_OpenPosition e "
                    + "where e.id in :ids and e.memoForInterview is not null and length(e.memoForInterview) > 0";
    private static final String QUERY_OPEN_POSITION_TEMPLATE_EXISTS =
            "select e.id from itpearls_OpenPosition e "
                    + "where e.id in :ids and e.templateLetter is not null and length(e.templateLetter) > 0";
    private static final String QUERY_TEMPLATE_LETTER_EXISTS =
            "select op.id from itpearls_OpenPosition op "
                    + "left join op.projectName p "
                    + "left join p.projectDepartment cd "
                    + "where op.id in :ids and ("
                    + "(op.templateLetter is not null and length(op.templateLetter) > 0) "
                    + "or (p.templateLetter is not null and length(p.templateLetter) > 0) "
                    + "or (cd.templateLetter is not null and length(cd.templateLetter) > 0))";
    private static final String QUERY_PROJECT_DESCRIPTION_EXISTS =
            "select e.id from itpearls_Project e "
                    + "where e.id in :ids and e.projectDescription is not null and length(e.projectDescription) > 0";
    private static final String QUERY_COMPANY_DESCRIPTION_EXISTS =
            "select e.id from itpearls_Company e "
                    + "where e.id in :ids and e.companyDescription is not null and length(e.companyDescription) > 0";

    private Map<UUID, Boolean> commentExistsCache = Collections.emptyMap();
    private Map<UUID, Boolean> commentPositiveCache = Collections.emptyMap();
    private Map<UUID, Boolean> exerciseExistsCache = Collections.emptyMap();
    private Map<UUID, Boolean> memoExistsCache = Collections.emptyMap();
    private Map<UUID, Boolean> templateLetterExistsCache = Collections.emptyMap();
    private Map<UUID, Boolean> projectDescriptionExistsCache = Collections.emptyMap();
    private Map<UUID, Boolean> companyDescriptionExistsCache = Collections.emptyMap();
    private Map<UUID, String> positionEnNameCache = Collections.emptyMap();
    private Map<UUID, String> positionRuNameCache = Collections.emptyMap();
    private Map<UUID, Integer> activeRecruitersCountByPosition = Collections.emptyMap();
    private Map<UUID, Integer> sentCvCountByPosition = Collections.emptyMap();
    private Map<UUID, BigDecimal> avgRatingByPosition = Collections.emptyMap();

    private final Map<UUID, String> lazyCommentTextCache = new HashMap<>();
    private final Map<UUID, String> lazyExerciseTextCache = new HashMap<>();
    private final Map<UUID, String> lazyMemoTextCache = new HashMap<>();
    private final Map<UUID, String> lazyTemplateLetterTextCache = new HashMap<>();
    private final Map<UUID, String> lazyProjectDescriptionCache = new HashMap<>();
    private final Map<UUID, String> lazyCompanyDescriptionCache = new HashMap<>();

    @Subscribe(id = "openPositionsDl", target = Target.DATA_LOADER)
    private void onOpenPositionsDlPostLoad(CollectionLoader.PostLoadEvent<OpenPosition> event) {
        List<OpenPosition> positions = event.getLoadedEntities();
        clearLazyLobTextCaches();
        refreshBrowseLobExistsCaches(positions);
        refreshBrowseAggregateCaches(positions);
    }

    private void clearLazyLobTextCaches() {
        lazyCommentTextCache.clear();
        lazyExerciseTextCache.clear();
        lazyMemoTextCache.clear();
        lazyTemplateLetterTextCache.clear();
        lazyProjectDescriptionCache.clear();
        lazyCompanyDescriptionCache.clear();
    }

    private void refreshBrowseLobExistsCaches(List<OpenPosition> positions) {
        List<UUID> ids = new ArrayList<>();
        for (OpenPosition position : positions) {
            if (position.getId() != null) {
                ids.add(position.getId());
            }
        }
        if (ids.isEmpty()) {
            commentExistsCache = Collections.emptyMap();
            commentPositiveCache = Collections.emptyMap();
            exerciseExistsCache = Collections.emptyMap();
            memoExistsCache = Collections.emptyMap();
            templateLetterExistsCache = Collections.emptyMap();
            projectDescriptionExistsCache = Collections.emptyMap();
            companyDescriptionExistsCache = Collections.emptyMap();
            positionEnNameCache = Collections.emptyMap();
            positionRuNameCache = Collections.emptyMap();
            return;
        }

        Map<UUID, Boolean> commentExists = toExistsCache(
                loadIdsByQuery(QUERY_OPEN_POSITION_COMMENT_EXISTS, ids));
        Map<UUID, Boolean> commentPositive = toExistsCache(
                loadIdsByQuery(QUERY_OPEN_POSITION_COMMENT_POSITIVE, ids, "negPrefix", "нет%"));
        Map<UUID, Boolean> exerciseExists = toExistsCache(
                loadIdsByQuery(QUERY_OPEN_POSITION_EXERCISE_EXISTS, ids));
        Map<UUID, Boolean> memoExists = toExistsCache(
                loadIdsByQuery(QUERY_OPEN_POSITION_MEMO_EXISTS, ids));
        Set<UUID> templateExistsIds = new HashSet<>(
                loadIdsByQuery(QUERY_OPEN_POSITION_TEMPLATE_EXISTS, ids));
        templateExistsIds.addAll(loadIdsByQuery(QUERY_TEMPLATE_LETTER_EXISTS, ids));
        Map<UUID, Boolean> templateExists = toExistsCache(templateExistsIds);
        commentExistsCache = commentExists;
        commentPositiveCache = commentPositive;
        exerciseExistsCache = exerciseExists;
        memoExistsCache = memoExists;
        templateLetterExistsCache = templateExists;

        Set<UUID> projectIds = new HashSet<>();
        Set<UUID> companyIds = new HashSet<>();
        for (OpenPosition position : positions) {
            if (position.getProjectName() != null && position.getProjectName().getId() != null) {
                projectIds.add(position.getProjectName().getId());
                if (position.getProjectName().getProjectDepartment() != null
                        && position.getProjectName().getProjectDepartment().getCompanyName() != null
                        && position.getProjectName().getProjectDepartment().getCompanyName().getId() != null) {
                    companyIds.add(position.getProjectName().getProjectDepartment().getCompanyName().getId());
                }
            }
        }

        Map<UUID, Boolean> projectDescriptions = toExistsCache(
                loadIdsByQuery(QUERY_PROJECT_DESCRIPTION_EXISTS, new ArrayList<>(projectIds)));
        projectDescriptionExistsCache = projectDescriptions;

        Map<UUID, Boolean> companyDescriptions = toExistsCache(
                loadIdsByQuery(QUERY_COMPANY_DESCRIPTION_EXISTS, new ArrayList<>(companyIds)));
        companyDescriptionExistsCache = companyDescriptions;

        Set<UUID> positionTypeIds = new HashSet<>();
        for (OpenPosition position : positions) {
            if (position.getPositionType() != null && position.getPositionType().getId() != null) {
                positionTypeIds.add(position.getPositionType().getId());
            }
        }
        Map<UUID, String> enNames = new HashMap<>();
        Map<UUID, String> ruNames = new HashMap<>();
        if (!positionTypeIds.isEmpty()) {
            for (KeyValueEntity row : dataManager.loadValues(
                    "select e.id, e.positionEnName, e.positionRuName from itpearls_Position e where e.id in :ids")
                    .properties("id", "positionEnName", "positionRuName")
                    .parameter("ids", positionTypeIds)
                    .list()) {
                UUID id = row.getValue("id");
                if (id != null) {
                    enNames.put(id, row.getValue("positionEnName"));
                    ruNames.put(id, row.getValue("positionRuName"));
                }
            }
        }
        positionEnNameCache = enNames;
        positionRuNameCache = ruNames;
    }

    private List<UUID> loadIdsByQuery(String query, List<UUID> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return dataManager.loadValue(query, UUID.class)
                .parameter("ids", ids)
                .list();
    }

    private List<UUID> loadIdsByQuery(String query, List<UUID> ids, String extraParam, Object extraValue) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return dataManager.loadValue(query, UUID.class)
                .parameter("ids", ids)
                .parameter(extraParam, extraValue)
                .list();
    }

    private static Map<UUID, Boolean> toExistsCache(Collection<UUID> idsWithFlag) {
        Map<UUID, Boolean> cache = new HashMap<>();
        for (UUID id : idsWithFlag) {
            if (id != null) {
                cache.put(id, Boolean.TRUE);
            }
        }
        return cache;
    }

    private static BigDecimal toNumberToBigDecimal(Number value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return BigDecimal.valueOf(value.doubleValue());
    }

    private static Integer toNumberToInt(Number value) {
        if (value == null) {
            return null;
        }
        return value.intValue();
    }

    private void refreshBrowseAggregateCaches(List<OpenPosition> positions) {
        List<UUID> ids = new ArrayList<>();
        for (OpenPosition position : positions) {
            if (position.getId() != null) {
                ids.add(position.getId());
            }
        }
        if (ids.isEmpty()) {
            activeRecruitersCountByPosition = Collections.emptyMap();
            sentCvCountByPosition = Collections.emptyMap();
            avgRatingByPosition = Collections.emptyMap();
            return;
        }

        Date currentDate = new Date();

        Map<UUID, Integer> recruitersCount = new HashMap<>();
        for (KeyValueEntity row : dataManager.loadValues(QUERY_ACTIVE_RECRUITERS_COUNT_BY_POSITIONS)
                .properties("openPositionId", "cnt")
                .parameter("ids", ids)
                .parameter("currentDate", currentDate)
                .list()) {
            UUID id = row.getValue("openPositionId");
            Integer count = toNumberToInt(row.getValue("cnt"));
            if (id != null && count != null) {
                recruitersCount.put(id, count);
            }
        }
        activeRecruitersCountByPosition = recruitersCount;

        Map<UUID, Integer> sentCvCount = new HashMap<>();
        for (KeyValueEntity row : dataManager.loadValues(QUERY_SENT_CV_COUNT_BY_POSITIONS)
                .properties("vacancyId", "cnt")
                .parameter("ids", ids)
                .list()) {
            UUID id = row.getValue("vacancyId");
            Integer count = toNumberToInt(row.getValue("cnt"));
            if (id != null && count != null) {
                sentCvCount.put(id, count);
            }
        }
        sentCvCountByPosition = sentCvCount;

        Map<UUID, BigDecimal> avgRatings = new HashMap<>();
        for (KeyValueEntity row : dataManager.loadValues(QUERY_AVG_RATING_BY_POSITIONS)
                .properties("openPositionId", "avgRating")
                .parameter("ids", ids)
                .list()) {
            UUID id = row.getValue("openPositionId");
            BigDecimal avg = toNumberToBigDecimal(row.getValue("avgRating"));
            if (id != null && avg != null) {
                avgRatings.put(id, avg);
            }
        }
        avgRatingByPosition = avgRatings;
    }

    private Position ensurePositionTypeLoaded(Position position) {
        if (position == null || position.getId() == null) {
            return position;
        }
        if (PersistenceHelper.isLoaded(position, "positionEnName")
                && PersistenceHelper.isLoaded(position, "positionRuName")) {
            return position;
        }
        return dataManager.reload(position, "position-picker-view");
    }

    private String getPositionEnName(Position position) {
        if (position == null || position.getId() == null) {
            return null;
        }
        String cached = positionEnNameCache.get(position.getId());
        if (cached != null || positionEnNameCache.containsKey(position.getId())) {
            return cached;
        }
        return ensurePositionTypeLoaded(position).getPositionEnName();
    }

    private String getPositionRuName(Position position) {
        if (position == null || position.getId() == null) {
            return null;
        }
        String cached = positionRuNameCache.get(position.getId());
        if (cached != null || positionRuNameCache.containsKey(position.getId())) {
            return cached;
        }
        return ensurePositionTypeLoaded(position).getPositionRuName();
    }

    private String formatPositionTypeColumnText(Position position) {
        String notName = messageBundle.getMessage("msgNotNamePosition");
        if (position == null) {
            return notName + " / " + notName;
        }
        String enName = getPositionEnName(position);
        if (enName != null) {
            return enName;
        }
        String ruName = getPositionRuName(position);
        return notName + " / " + (ruName != null ? ruName : notName);
    }

    private String formatPositionTypeDescription(Position position) {
        if (position == null) {
            return "";
        }
        String ruName = getPositionRuName(position);
        String enName = getPositionEnName(position);
        if (ruName == null) {
            return enName != null ? enName : "";
        }
        return enName != null ? ruName + "/" + enName : ruName;
    }

    private String buildTemplateLetterText(String openPositionLetter, String projectLetter, String departmentLetter) {
        StringBuilder sb = new StringBuilder();
        if (openPositionLetter != null && !openPositionLetter.isEmpty()) {
            sb.append("Требования к вакансии: ")
                    .append(Jsoup.parse(openPositionLetter).wholeText())
                    .append("\n\n");
        }
        if (projectLetter != null && !projectLetter.isEmpty()) {
            sb.append("Требования проекта: ")
                    .append(Jsoup.parse(projectLetter).wholeText())
                    .append("\n\n");
        }
        if (departmentLetter != null && !departmentLetter.isEmpty()) {
            sb.append("Требования департамента: ")
                    .append(Jsoup.parse(departmentLetter).wholeText());
        }
        return sb.toString();
    }

    private boolean hasComment(OpenPosition openPosition) {
        if (openPosition == null || openPosition.getId() == null) {
            return false;
        }
        return commentExistsCache.getOrDefault(openPosition.getId(), false);
    }

    private boolean hasPositiveComment(OpenPosition openPosition) {
        if (openPosition == null || openPosition.getId() == null) {
            return false;
        }
        return commentPositiveCache.getOrDefault(openPosition.getId(), false);
    }

    private boolean hasExerciseText(OpenPosition openPosition) {
        if (openPosition == null || openPosition.getId() == null) {
            return false;
        }
        return exerciseExistsCache.getOrDefault(openPosition.getId(), false);
    }

    private boolean hasMemoForInterview(OpenPosition openPosition) {
        if (openPosition == null || openPosition.getId() == null) {
            return false;
        }
        return memoExistsCache.getOrDefault(openPosition.getId(), false);
    }

    private boolean hasTemplateLetter(OpenPosition openPosition) {
        if (openPosition == null || openPosition.getId() == null) {
            return false;
        }
        return templateLetterExistsCache.getOrDefault(openPosition.getId(), false);
    }

    private String getLazyCommentText(OpenPosition openPosition) {
        if (openPosition == null || openPosition.getId() == null) {
            return null;
        }
        UUID id = openPosition.getId();
        if (!lazyCommentTextCache.containsKey(id)) {
            dataManager.loadValues("select e.id, e.comment from itpearls_OpenPosition e where e.id = :id")
                    .properties("id", "comment")
                    .parameter("id", id)
                    .optional()
                    .ifPresent(row -> lazyCommentTextCache.put(id, row.getValue("comment")));
            lazyCommentTextCache.putIfAbsent(id, null);
        }
        return lazyCommentTextCache.get(id);
    }

    private String getLazyExerciseText(OpenPosition openPosition) {
        if (openPosition == null || openPosition.getId() == null) {
            return null;
        }
        UUID id = openPosition.getId();
        if (!lazyExerciseTextCache.containsKey(id)) {
            dataManager.loadValues("select e.id, e.exercise from itpearls_OpenPosition e where e.id = :id")
                    .properties("id", "exercise")
                    .parameter("id", id)
                    .optional()
                    .ifPresent(row -> lazyExerciseTextCache.put(id, row.getValue("exercise")));
            lazyExerciseTextCache.putIfAbsent(id, null);
        }
        return lazyExerciseTextCache.get(id);
    }

    private String getLazyMemoText(OpenPosition openPosition) {
        if (openPosition == null || openPosition.getId() == null) {
            return null;
        }
        UUID id = openPosition.getId();
        if (!lazyMemoTextCache.containsKey(id)) {
            dataManager.loadValues("select e.id, e.memoForInterview from itpearls_OpenPosition e where e.id = :id")
                    .properties("id", "memoForInterview")
                    .parameter("id", id)
                    .optional()
                    .ifPresent(row -> lazyMemoTextCache.put(id, row.getValue("memoForInterview")));
            lazyMemoTextCache.putIfAbsent(id, null);
        }
        return lazyMemoTextCache.get(id);
    }

    private String getLazyTemplateLetterText(OpenPosition openPosition) {
        if (openPosition == null || openPosition.getId() == null) {
            return "";
        }
        UUID id = openPosition.getId();
        if (!lazyTemplateLetterTextCache.containsKey(id)) {
            String text = dataManager.loadValues(
                    "select op.id, op.templateLetter, p.templateLetter, cd.templateLetter "
                            + "from itpearls_OpenPosition op "
                            + "left join op.projectName p "
                            + "left join p.projectDepartment cd "
                            + "where op.id = :id")
                    .properties("id", "op.templateLetter", "p.templateLetter", "cd.templateLetter")
                    .parameter("id", id)
                    .optional()
                    .map(row -> buildTemplateLetterText(
                            row.getValue("op.templateLetter"),
                            row.getValue("p.templateLetter"),
                            row.getValue("cd.templateLetter")))
                    .orElse("");
            lazyTemplateLetterTextCache.put(id, text);
        }
        String cached = lazyTemplateLetterTextCache.get(id);
        return cached != null ? cached : "";
    }

    private String getLazyProjectDescription(UUID projectId) {
        if (projectId == null) {
            return null;
        }
        if (!lazyProjectDescriptionCache.containsKey(projectId)) {
            dataManager.loadValues("select e.id, e.projectDescription from itpearls_Project e where e.id = :id")
                    .properties("id", "projectDescription")
                    .parameter("id", projectId)
                    .optional()
                    .ifPresent(row -> lazyProjectDescriptionCache.put(projectId, row.getValue("projectDescription")));
            lazyProjectDescriptionCache.putIfAbsent(projectId, null);
        }
        return lazyProjectDescriptionCache.get(projectId);
    }

    private String getLazyCompanyDescription(UUID companyId) {
        if (companyId == null) {
            return null;
        }
        if (!lazyCompanyDescriptionCache.containsKey(companyId)) {
            dataManager.loadValues("select e.id, e.companyDescription from itpearls_Company e where e.id = :id")
                    .properties("id", "companyDescription")
                    .parameter("id", companyId)
                    .optional()
                    .ifPresent(row -> lazyCompanyDescriptionCache.put(companyId, row.getValue("companyDescription")));
            lazyCompanyDescriptionCache.putIfAbsent(companyId, null);
        }
        return lazyCompanyDescriptionCache.get(companyId);
    }

    private OpenPosition loadOpenPositionWithDescriptionLobs(OpenPosition openPosition) {
        if (openPosition == null || openPosition.getId() == null) {
            return openPosition;
        }
        return dataManager.reload(openPosition, ViewBuilder.of(OpenPosition.class)
                .add("comment")
                .add("commentEn")
                .add("templateLetter")
                .add("projectName", project -> project
                        .add("projectDescription")
                        .add("templateLetter")
                        .add("projectDepartment", dept -> dept
                                .add("templateLetter")
                                .add("companyName", company -> company
                                        .add("companyDescription")
                                        .add("workingConditions"))))
                .build());
    }

    @Inject
    private CheckBox checkBoxOnlyOpenedPosition;
    @Inject
    private DataManager dataManager;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private CheckBox checkBoxOnlyMySubscribe;
    @Inject
    private UserSession userSession;
    @Inject
    private Button buttonExcel;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private DataGrid<OpenPosition> openPositionsTable;
    @Inject
    private UiComponents uiComponents;

    private static final String MANAGEMENT_GROUP = "Менеджмент";
    private static final String ACCOUNTING_GROUP = "Аккаунтинг";
    private static final String HUNTING_GROUP = "Хантинг";
    private Map<String, Integer> remoteWork = new LinkedHashMap<>();
    private Map<String, Integer> priorityMap = new LinkedHashMap<>();
    private Map<String, Integer> mapWorkExperience = new LinkedHashMap<>();
    private List<User> users = new ArrayList<>();

    private final static String QUERY_SELECT_COMMAND
            = "select e from itpearls_OpenPosition e where e.parentOpenPosition = :parentOpenPosition and e.openClose = false";
    private final static String QUERY_RECRUTIER_TASK
            = "select e from itpearls_RecrutiesTasks e where e.endDate > current_date and e.closed = false and e.openPosition = :openPosition";

    public final static int PRIORITY_NONE = -2;
    public final static int PRIORITY_DRAFT = -1;
    public final static int PRIORITY_PAUSED = 0;
    public final static int PRIORITY_LOW = 1;
    public final static int PRIORITY_NORMAL = 2;
    public final static int PRIORITY_HIGH = 3;
    public final static int PRIORITY_CRITICAL = 4;

    private final static String parameter_subscriber = "subscriber";
    private final static String parameter_notsubscriber = "notsubscriber";
    private final static String parameter_freesubscriber = "freesubscriber";
    private final static String parameter_newOpenPosition = "newOpenPosition";
    private final static String parameter_priority = "priority";

    private static final String QUERY_COUNT_ITERACTIONS
            = "select e.iteractionType, count(e.iteractionType) from itpearls_IteractionList e where e.dateIteraction between :startDate and :endDate and (e.vacancy = :vacancy or (e.vacancy in (select f from itpearls_OpenPosition f where f.parentOpenPosition = :vacancy))) and e.iteractionType.statistics = true group by e.iteractionType";
    private static final String QUERY_USER
            = "select e from sec$User e where e.active = true";
    static final String QUERY_GET_SUBSCRIBER =
            "select e from itpearls_RecrutiesTasks e where e.openPosition = :openPosition and e.reacrutier = :reacrutier and :current_date between e.startDate and e.endDate";
    final static String QUERY_CANDIDATES_FROM_CONSIDERATION = "select e from itpearls_JobCandidate e " +
            "where e.iteractionList in (select f from itpearls_IteractionList f where f.candidate = e and f.iteractionType.signSendToClient = true and f.vacancy = :vacancy)";

    private final static String font_icon_PLUS_CIRCLE = "font-icon:PLUS_CIRCLE";
    private final static String font_icon_MINUS_CIRCLE = "font-icon:MINUS_CIRCLE";
    private final static String font_icon_QUESTION_CIRCLE = "font-icon:QUESTION_CIRCLE";

    private final static String cuba_icon_PLUS_CIRCLE = "PLUS_CIRCLE";
    private final static String cuba_icon_MINUS_CIRCLE = "MINUS_CIRCLE";
    private final static String cuba_icon_QUESTION_CIRCLE = "QUESTION_CIRCLE";

    @Inject
    private Button groupSubscribe;
    @Inject
    private Screens screens;
    @Inject
    private CollectionContainer<OpenPosition> openPositionsDc;
    @Inject
    private CheckBox checkBoxOnlyNotPaused;
    @Inject
    private LookupField notLowerRatingLookupField;
    @Inject
    private GroupBoxLayout urgentlyPositons;
    @Inject
    private HBoxLayout urgentlyHBox;
    @Inject
    private LookupField remoteWorkLookupField;
    @Inject
    private Events events;
    @Inject
    private Notifications notifications;
    @Inject
    private Button buttonSubscribe;
    @Inject
    private Button suggestCandidateButton;
    @Inject
    private RadioButtonGroup subscribeRadioButtonGroup;
    @Inject
    private ReportGuiManager reportGuiManager;
    @Inject
    private Actions actions;
    @Inject
    private Dialogs dialogs;
    @Inject
    private PopupButton openCloseButton;
    @Inject
    private PopupButton reportsPopupButton;

    @Subscribe
    protected void onInit(InitEvent event) {
        initRemoteWorkMap();

        initTableGenerator();
        initGroupSubscribeButton();

        setMapOfPriority();
        setWorkExperienceMap();

        Action listPrintFormAction = actions.create(ListPrintFormAction.class, "listPrintFormAction");
        openPositionsTable.addAction(listPrintFormAction);

        users = dataManager.load(User.class)
                .query(QUERY_USER)
                .list();
    }

    @Install(to = "subscribeRadioButtonGroup", subject = "optionDescriptionProvider")
    private String subscribeRadioButtonGroupOptionDescriptionProvider(Object object) {
//        String retStr = "";
        StringBuilder sb = new StringBuilder();

        switch ((Integer) object) {
            case 0:
                sb.append(messageBundle.getMessage("msgNeedSubscribe"));
//                retStr = messageBundle.getMessage("msgNeedSubscribe");
//                retStr = "Я не подписан на эти вакансии. Для работы с ними надо подписаться";
                break;
            case 1:
                sb.append(messageBundle.getMessage("msgInMyWork"));
//                retStr = messageBundle.getMessage("msgInMyWork");
//                retStr = "Находится в работе у меня на определенный период времени";
                break;
            case 2:
                sb.append(messageBundle.getMessage("msgAllOpenedVacancy"));
//                retStr = messageBundle.getMessage("msgAllOpenedVacancy");
//                retStr = "Все открытые вакансии";
                break;
            case 3:
                sb.append(messageBundle.getMessage("msgFreeVacancy"));
//                retStr = "Не находится ни у кого в работе. Свободная вакансия";
                break;
            case 4:
                sb.append(messageBundle.getMessage("msgOpenedInLast3days"));
//                retStr = messageBundle.getMessage("msgOpenedInLast3days");
//                retStr = "Новые вакансии окрытые за последние 3 дня";
                break;
            case 5:
                sb.append(messageBundle.getMessage("msgOpenedInLast7days"));
//                retStr = messageBundle.getMessage("msgOpenedInLast7days");
//                retStr = "Новые вакансии окрытые за последнюю неделю. За 7 дней";
                break;
            case 6:
                sb.append(messageBundle.getMessage("msgOpenedInLastMonth"));
//                retStr = messageBundle.getMessage("msgOpenedInLastMonth");
//                retStr = "Новые вакансии окрытые за последний месяц. За 30 дней";
                break;
            default:
                break;
        }

        return sb.toString();
    }

    private void initCheckBoxOnlyOpenedPosition() {
        Map<String, Integer> onlyOpenedPositionMap = new LinkedHashMap<>();

        onlyOpenedPositionMap.put("Все вакансии", 2);
        onlyOpenedPositionMap.put("В подписке", 1);
        onlyOpenedPositionMap.put("Не в подписке", 0);
        onlyOpenedPositionMap.put("Свободные", 3);
        onlyOpenedPositionMap.put("Открытые за 3 дня", 4);
        onlyOpenedPositionMap.put("Открытые за неделю", 5);
        onlyOpenedPositionMap.put("Открытые за месяц", 6);
        onlyOpenedPositionMap.put("На паузе", 7);

        subscribeRadioButtonGroup.setOptionsMap(onlyOpenedPositionMap);

        subscribeRadioButtonGroup.addValueChangeListener(e -> {
            setOpenPositionBrowseFilter();
        });

        if (userSession.getUser().getGroup().getName().equals(MANAGEMENT_GROUP) ||
                userSession.getUser().getGroup().getName().equals(ACCOUNTING_GROUP)) {
            subscribeRadioButtonGroup.setValue(2);
        } else {
            subscribeRadioButtonGroup.setValue(1);
        }

        setOpenPositionBrowseFilter();
        openPositionsTable.repaint();
    }

    private void setOpenPositionBrowseFilter() {
        reportsPopupButton.setEnabled(openPositionsTable.getSingleSelected() != null);
        buttonSubscribe.setEnabled(openPositionsTable.getSingleSelected() != null);

        suggestCandidateButton.setVisible(((Integer) subscribeRadioButtonGroup.getValue()) == 1);

        switch ((Integer) subscribeRadioButtonGroup.getValue()) {
            case 1:
                openPositionsDl.setParameter(parameter_subscriber, userSession.getUser());
                openPositionsDl.removeParameter(parameter_notsubscriber);
                openPositionsDl.removeParameter(parameter_freesubscriber);
                openPositionsDl.removeParameter(parameter_newOpenPosition);
                openPositionsDl.removeParameter(parameter_priority);
                checkBoxOnlyNotPaused.setValue(true);
                break;
            case 0:
                openPositionsDl.removeParameter(parameter_subscriber);
                openPositionsDl.setParameter(parameter_notsubscriber, userSession.getUser());
                openPositionsDl.removeParameter(parameter_freesubscriber);
                openPositionsDl.removeParameter(parameter_newOpenPosition);
                openPositionsDl.removeParameter(parameter_priority);
                checkBoxOnlyNotPaused.setValue(true);
                break;
            case 2:
                openPositionsDl.removeParameter(parameter_subscriber);
                openPositionsDl.removeParameter(parameter_notsubscriber);
                openPositionsDl.removeParameter(parameter_freesubscriber);
                openPositionsDl.removeParameter(parameter_newOpenPosition);
                openPositionsDl.removeParameter(parameter_priority);
                checkBoxOnlyNotPaused.setValue(false);
                break;
            case 3:
                openPositionsDl.removeParameter(parameter_subscriber);
                openPositionsDl.removeParameter(parameter_notsubscriber);
                openPositionsDl.setParameter(parameter_freesubscriber, false);
                openPositionsDl.removeParameter(parameter_newOpenPosition);
                openPositionsDl.removeParameter(parameter_priority);
                checkBoxOnlyNotPaused.setValue(true);
                break;
            case 4:
                openPositionsDl.removeParameter(parameter_subscriber);
                openPositionsDl.removeParameter(parameter_notsubscriber);
                openPositionsDl.removeParameter(parameter_freesubscriber);
                openPositionsDl.setParameter(parameter_newOpenPosition, 3);
                openPositionsDl.removeParameter(parameter_priority);
                checkBoxOnlyNotPaused.setValue(true);
                break;
            case 5:
                openPositionsDl.removeParameter(parameter_subscriber);
                openPositionsDl.removeParameter(parameter_notsubscriber);
                openPositionsDl.removeParameter(parameter_freesubscriber);
                openPositionsDl.setParameter(parameter_newOpenPosition, 7);
                openPositionsDl.removeParameter(parameter_priority);
                checkBoxOnlyNotPaused.setValue(true);
                break;
            case 6:
                openPositionsDl.removeParameter(parameter_subscriber);
                openPositionsDl.removeParameter(parameter_notsubscriber);
                openPositionsDl.removeParameter(parameter_freesubscriber);
                openPositionsDl.setParameter(parameter_newOpenPosition, 30);
                openPositionsDl.removeParameter(parameter_priority);
                checkBoxOnlyNotPaused.setValue(true);
                break;
            case 7:
                openPositionsDl.removeParameter(parameter_subscriber);
                openPositionsDl.removeParameter(parameter_notsubscriber);
                openPositionsDl.removeParameter(parameter_freesubscriber);
                openPositionsDl.removeParameter(parameter_newOpenPosition);
                openPositionsDl.setParameter(parameter_priority, 0);
                checkBoxOnlyNotPaused.setValue(false);
                break;
            default:
                break;
        }

        openPositionsDl.load();
    }

    private void initGroupSubscribeButton() {
        if (userSession.getUser().getGroup().getName().equals(MANAGEMENT_GROUP) ||
                userSession.getUser().getGroup().getName().equals(HUNTING_GROUP)) {
            groupSubscribe.setEnabled(true);
        } else {
            groupSubscribe.setEnabled(false);
        }
    }

    private void initTableGenerator() {
        openPositionsTable.setItemClickAction(new BaseAction("itemClickAction")
                .withHandler(actionPerformedEvent ->
                        openPositionsTable.setDetailsVisible(openPositionsTable.getSingleSelected(), true)));

    }

    protected String getPriorityIcon(int priority) {
        String icon = null;

        switch (priority) {
            case PRIORITY_NONE:
                icon = CubaIcon.CANCEL.source();
                break;
            case PRIORITY_DRAFT:
                icon = icons_traffic_lights_gray_png;
                break;
            case PRIORITY_PAUSED: //"Paused"
                icon = icons_remove_png;
                break;
            case PRIORITY_LOW: //"Low"
                icon = icons_traffic_lights_blue_png;
                break;
            case PRIORITY_NORMAL: //"Normal"
                icon = icons_traffic_lights_green_png;
                break;
            case PRIORITY_HIGH: //"High"
                icon = icons_traffic_lights_yellow_png;
                break;
            case PRIORITY_CRITICAL: //"Critical"
                icon = icons_traffic_lights_red_png;
                break;
            default:
                break;
        }

        return icon;
    }

    @Install(to = "notLowerRatingLookupField", subject = "optionIconProvider")
    private String notLowerRatingLookupFieldOptionIconProvider(Object object) {
        String icon = null;

        switch ((int) object) {
            case PRIORITY_NONE:
                icon = CubaIcon.CANCEL.source();
                break;
            case PRIORITY_DRAFT:
                icon = icons_traffic_lights_gray_png;
                break;
            case PRIORITY_PAUSED: //"Paused"
                icon = icons_remove_png;
                break;
            case PRIORITY_LOW: //"Low"
                icon = icons_traffic_lights_blue_png;
                break;
            case PRIORITY_NORMAL: //"Normal"
                icon = icons_traffic_lights_green_png;
                break;
            case PRIORITY_HIGH: //"High"
                icon = icons_traffic_lights_yellow_png;
                break;
            case PRIORITY_CRITICAL: //"Critical"
                icon = icons_traffic_lights_red_png;
                break;
            default:
                break;
        }

        return icon;
    }

    @Install(to = "remoteWorkLookupField", subject = "optionIconProvider")
    private String remoteWorkLookupFieldOptionIconProvider(Object object) {
        String returnIcon = "";

        switch ((int) object) {
            case -1:
                returnIcon = CubaIcon.CANCEL.source();
                break;
            case 1:
                returnIcon = font_icon_PLUS_CIRCLE;
                break;
            case 0:
                returnIcon = font_icon_MINUS_CIRCLE;
                break;
            case 2:
                returnIcon = font_icon_QUESTION_CIRCLE;
                break;
            default:
                returnIcon = font_icon_QUESTION_CIRCLE;
                break;
        }

        return returnIcon;
    }


    @Install(to = "openPositionsTable.remoteWork", subject = "descriptionProvider")
    private String openPositionsTableRemoteWorkDescriptionProvider(OpenPosition openPosition) {
//        String retStr = String.valueOf(remoteWork.get(openPosition.getRemoteWork()));
        StringBuilder sb = new StringBuilder(String
                .valueOf(remoteWork.get(openPosition.getRemoteWork())));

        switch (openPosition.getRemoteWork()) {
            case -1:
                sb = new StringBuilder();
                sb.append(messageBundle.getMessage("msgUndefined"));
//                retStr = messageBundle.getMessage("msgUndefined");
                break;
            case 0:
                sb = new StringBuilder();
                sb.append(messageBundle.getMessage("msgWorkInOffice"));
//                retStr = messageBundle.getMessage("msgWorkInOffice");
                break;
            case 1:
                sb = new StringBuilder();
                sb.append(messageBundle.getMessage("msgRemoteWork"));
//                retStr = messageBundle.getMessage("msgRemoteWork");
                break;
            case 2:
                sb = new StringBuilder();
                sb.append(messageBundle.getMessage("msgHybridWorkDesc"));
//                retStr = messageBundle.getMessage("msgHybridWorkDesc");
                break;
        }

        if (openPosition.getCityPosition() == null) {
            return sb.toString();
        } else {
//            StringBuilder sb = new StringBuilder(retStr);
            sb.append("\nЖелаемая локация: ");
            sb.append(openPosition.getCityPosition().getCityRuName());
            sb.append(openPosition.getRemoteComment() != null ?
                    "\nКомментарий: " + openPosition.getRemoteComment() : "");
            return sb.toString();
        }
    }

    private void initRemoteWorkMap() {
        remoteWork.put(messageBundle.getMessage("msgUndefined"), -1);
        remoteWork.put(messageBundle.getMessage("msgWorkInOffice"), 0);
        remoteWork.put(messageBundle.getMessage("msgRemoteWork"), 1);
        remoteWork.put(messageBundle.getMessage("msgHybridWork"), 2);
    }

    @Install(to = "openPositionsTable.remoteWork", subject = "columnGenerator")
    private Object openPositionsTableRemoteWorkColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout retHbox = setPlusMinusIcon(setRemoteWorkIcon(event));
        return retHbox;
    }

    private HBoxLayout setPlusMinusIcon(Icons.Icon setRemoteWorkIcon) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        Label label = uiComponents.create(Label.class);

        retHBox.setWidthFull();
        retHBox.setHeightFull();
        retHBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retHBox.setStyleName(style_table_wordwrap);

        label.setHeightAuto();
        label.setAlignment(Component.Alignment.MIDDLE_CENTER);
        label.setStyleName(style_table_wordwrap);
        label.setIconFromSet(setRemoteWorkIcon);

        retHBox.add(label);

        return retHBox;
    }

    private Icons.Icon setRemoteWorkIcon(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String returnIcon = "";

        switch (event.getItem().getRemoteWork()) {
            case -1:
                returnIcon = CubaIcon.CANCEL.source();
                break;
            case 1:
                returnIcon = cuba_icon_PLUS_CIRCLE;
                break;
            case 0:
                returnIcon = cuba_icon_MINUS_CIRCLE;
                break;
            case 2:
                returnIcon = cuba_icon_QUESTION_CIRCLE;
                break;
            default:
                returnIcon = cuba_icon_QUESTION_CIRCLE;
                break;
        }

        return CubaIcon.valueOf(returnIcon);
    }

    @Install(to = "openPositionsTable.testExserice", subject = "columnGenerator")
    private Object openPositionsTableTestExsericeColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String returnIcon = "";

        if (event.getItem().getNeedExercise() != null) {
            if (event.getItem().getNeedExercise())
                returnIcon = cuba_icon_PLUS_CIRCLE;
            else
                returnIcon = cuba_icon_MINUS_CIRCLE;
        } else
            returnIcon = cuba_icon_MINUS_CIRCLE;

        return setPlusMinusIcon(CubaIcon.valueOf(returnIcon));
    }

    @Install(to = "openPositionsTable.testExserice", subject = "descriptionProvider")
    private String openPositionsTableTestExsericeDescriptionProvider(OpenPosition openPosition) {
        String exercise = getLazyExerciseText(openPosition);
        if (exercise != null)
            return Jsoup.parse(exercise).wholeText();
        else
            return "";
    }

    @Install(to = "openPositionsTable.description", subject = "columnGenerator")
    private Object openPositionsTableDescriptionColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String returnIcon = hasPositiveComment(event.getItem()) ? "FILE_TEXT" : "FILE";
        return setPlusMinusIcon(CubaIcon.valueOf(returnIcon));
    }

    @Install(to = "openPositionsTable.description", subject = "descriptionProvider")
    private String openPositionsTableDescriptionDescriptionProvider(OpenPosition openPosition) {
        String comment = getLazyCommentText(openPosition);
        if (comment != null) {
            return Jsoup.parse(comment).wholeText();
        }
        return "";
    }

    final static String open_position_pic_center_large_green = "open-position-pic-center-large-green";
    final static String open_position_pic_center_large_yellow = "open-position-pic-center-large-yellow";
    final static String open_position_pic_center_large_maroon = "open-position-pic-center-large-maroon";
    final static String open_position_pic_center_large_orange = "open-position-pic-center-large-orange";
    final static String open_position_pic_center_large_lime = "open-position-pic-center-large-lime";
    final static String open_position_pic_center_large_red = "open-position-pic-center-large-red";

    @Install(to = "openPositionsTable.description", subject = "styleProvider")
    private String openPositionsTableDescriptionStyleProvider(OpenPosition openPosition) {
        if (hasPositiveComment(openPosition)) {
            return open_position_pic_center_large_green;
        }
        return open_position_pic_center_large_red;
    }

    @Install(to = "openPositionsTable.testExserice", subject = "styleProvider")
    private String openPositionsTableTestExsericeStyleProvider(OpenPosition openPosition) {
        if (openPosition.getNeedExercise() != null) {
            if (openPosition.getNeedExercise()) {
                return open_position_pic_center_large_green;
            } else {
                return open_position_pic_center_large_red;
            }
        } else
            return open_position_pic_center_large_red;
    }


    @Install(to = "openPositionsTable.icon", subject = "styleProvider")
    private String openPositionsTableIconStyleProvider(OpenPosition openPosition) {
        return "open-position-pic-center";
    }

    @Install(to = "openPositionsTable.remoteWork", subject = "styleProvider")
    private String openPositionsTableRemoteWorkStyleProvider(OpenPosition openPosition) {
        String style = "";

        switch (openPosition.getRemoteWork()) {
            case 1:
                style = (openPosition.getRemoteComment() == null ?
                        open_position_pic_center_large_green :
                        open_position_pic_center_large_lime);
                break;
            case 2:
                style = (openPosition.getRemoteComment() == null ?
                        open_position_pic_center_large_red :
                        open_position_pic_center_large_maroon);
                break;
            case 0:
                style = (openPosition.getRemoteComment() == null ?
                        open_position_pic_center_large_yellow :
                        open_position_pic_center_large_orange);
                break;
        }

        return style;
    }

    @Install(to = "openPositionsTable.numberPosition", subject = "styleProvider")
    private String openPositionsTableNumberPositionStyleProvider(OpenPosition openPosition) {
        return "open-position-pic-center";
    }

    @Install(to = "openPositionsTable.icon", subject = "columnGenerator")
    private Object openPositionsTableIconColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();
        retHBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

        Image image = uiComponents.create(Image.class);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth("35px");
        image.setHeight("35px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);

        String iconPath = getIcon(event.getItem());
        if (iconPath != null) {
            image.setSource(ThemeResource.class).setPath(iconPath);
        }

        retHBox.add(image);
        return retHBox;
    }

    @Install(to = "openPositionsTable.vacansyName", subject = "descriptionProvider")
    private String openPositionsTableVacansyNameDescriptionProvider(OpenPosition openPosition) {
        String returnData = "";
        StringBuilder sb = new StringBuilder();

        List<RecrutiesTasks> recrutiesTasks = dataManager.load(RecrutiesTasks.class)
                .view("recrutiesTasks-view")
                .query(QUERY_RECRUTIER_TASK)
                .parameter("openPosition", openPosition)
                .list();

        if (openPosition.getShortDescription() != null) {
            sb.append("\n<b>Кратко: </b><i>");
            sb.append(openPosition.getShortDescription());
            sb.append("</i><br><br>");
        }

        if (openPosition.getProjectName() != null) {
            if (openPosition.getProjectName().getProjectOwner() != null) {
                if (openPosition.getProjectName().getProjectOwner().getSecondName() != null &&
                        openPosition.getProjectName().getProjectOwner().getFirstName() != null) {
                    sb.append("\n</br><b>");
                    sb.append(messageBundle.getMessage("msgProjectOwner"));
                    sb.append(":</b> ");
                    sb.append(openPosition.getProjectName().getProjectOwner().getSecondName());
                    sb.append(" ");
                    sb.append(openPosition.getProjectName().getProjectOwner().getFirstName());
                    sb.append("<br>");
                }
            }
        }

        if (recrutiesTasks.size() != 0) {
            sb.append("\n<b><br>В работе у:</b><br>");

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

            for (RecrutiesTasks a : recrutiesTasks) {
                sb.append("\n");
                sb.append(a.getReacrutier().getName());
                sb.append(" до <i>");
                sb.append(sdf.format(a.getEndDate()));
                sb.append("</i><br>");
            }

            returnData = sb.toString().substring(0, sb.toString().length() - 1);
        }

        returnData = returnData != null ? Jsoup.parse(returnData).wholeText() : "";

        return openPosition.getVacansyName() + "\n" + returnData;
    }

    @Install(to = "openPositionsTable.projectName", subject = "descriptionProvider")
    private String openPositionsTableProjectNameDescriptionProvider(OpenPosition openPosition) {
        String a = "";
        if (openPosition.getProjectName() != null && openPosition.getProjectName().getId() != null) {
            UUID projectId = openPosition.getProjectName().getId();
            if (projectDescriptionExistsCache.getOrDefault(projectId, false)) {
                String textReturn = getLazyProjectDescription(projectId);
                a = textReturn != null ? Jsoup.parse(textReturn).wholeText() : "";
            }
        }
        String projectOwner = "";
        StringBuilder sb = new StringBuilder();
        StringBuilder sb1 = new StringBuilder();

        try {
            sb.append(openPosition.getProjectName().getProjectOwner().getSecondName());
            sb.append(" ");
            sb.append(openPosition.getProjectName().getProjectOwner().getFirstName());
            projectOwner = sb.toString();
        } catch (NullPointerException e) {
            return a;
        }

        return sb1.append(openPosition.getProjectName().getProjectName())
                .append("\n\nВладелец проекта: ")
                .append(projectOwner)
                .append("\n\n<br><br>")
                .append(a)
                .toString();
    }

    @Inject
    private Fragments fragments;

    @Install(to = "openPositionsTable", subject = "detailsGenerator")
    protected Component openPositionsTableDetailsGenerator(OpenPosition entity) {

        OpenPositionDetailScreenFragment openPositionDetailScreenFragment =
                fragments.create(this, OpenPositionDetailScreenFragment.class);

        openPositionDetailScreenFragment.setOpenPosition(entity);
        openPositionDetailScreenFragment.setLabels();
        openPositionDetailScreenFragment.setDefaultCompanyLogo();

        GroupBoxLayout mainLayout = detailsGenerator(entity, openPositionDetailScreenFragment);

        return mainLayout;
    }

    protected GroupBoxLayout detailsGenerator(OpenPosition entity, OpenPositionDetailScreenFragment openPositionDetailScreenFragment) {
        GroupBoxLayout mainLayout = uiComponents.create(GroupBoxLayout.class);
        mainLayout.setSpacing(true);

        mainLayout.setWidthFull();

        HBoxLayout titleBox = uiComponents.create(HBoxLayout.NAME);
        titleBox.setSpacing(true);
        HBoxLayout buttonsHBox = uiComponents.create(HBoxLayout.NAME);

        buttonsHBox.setSpacing(true);
        buttonsHBox.setWidthAuto();
        buttonsHBox.setHeightFull();
        buttonsHBox.setAlignment(Component.Alignment.TOP_RIGHT);

        Component suitableButton = findSuitableButton(entity);
        Component fragmentTitle = createTitleFragment(entity);
        Component closeButton = createCloseButton(entity);
        Component editButton = createEditButton(entity);
        Component priorityField = createPriorityField(entity);
        Component commentButton = createCommentButton(entity);
        Component openCloseButton = createOpenCloseButton(entity);
        Component viewDescriptionButton = createViewDescriptionButton(entity);
        Component gigachatButton = createGigaChatLetterButton(entity);
        Component sendedCandidatesButton = createSendedCandidatesButton(entity);

        closeButton.setAlignment(Component.Alignment.TOP_RIGHT);
        commentButton.setAlignment(Component.Alignment.TOP_RIGHT);
        openCloseButton.setAlignment(Component.Alignment.TOP_RIGHT);
        editButton.setAlignment(Component.Alignment.TOP_RIGHT);
        priorityField.setAlignment(Component.Alignment.TOP_RIGHT);

        buttonsHBox.setAlignment(Component.Alignment.BOTTOM_RIGHT);

        buttonsHBox.add(priorityField);
        buttonsHBox.add(openCloseButton);
        buttonsHBox.add(editButton);
        buttonsHBox.add(viewDescriptionButton);
        buttonsHBox.add(sendedCandidatesButton);
        buttonsHBox.add(commentButton);

        if (suitableButton != null)
            buttonsHBox.add(suitableButton);

        buttonsHBox.add(closeButton);

        titleBox.setWidthFull();
        titleBox.add(fragmentTitle);
        titleBox.add(buttonsHBox);

        mainLayout.add(titleBox);

        openPositionDetailScreenFragment.setSubscribersRecruters();
        Fragment fragment = openPositionDetailScreenFragment.getFragment();
        fragment.setWidthFull();
        mainLayout.add(fragment);

        Skillsbar skillBoxFragment = fragments.create(this, Skillsbar.class);
        if (skillBoxFragment.generateSkillLabels(
                openPositionsTable.getSingleSelected().getCommentEn() != null ?
                        openPositionsTable.getSingleSelected().getCommentEn() :
                        openPositionsTable.getSingleSelected().getComment())) {
            mainLayout.add(skillBoxFragment.getFragment());
        }

        closeAllAnoterDetailsScreenFragments();


        return mainLayout;
    }

    private Component createGigaChatLetterButton(OpenPosition entity) {
        Button retButton = uiComponents.create(Button.class);
        retButton.setIcon(CubaIcon.MAGIC.source());
        retButton.setDescription(messageBundle.getMessage("msgGigaChatLetter"));
        retButton.setEnabled(true);
        retButton.setVisible(false);

        retButton.addClickListener(addClickListenerEvent -> {
            openPositionCommentViewInvoke();
        });

        return retButton;

    }

    private Component createViewCommentButton(OpenPosition entity) {
        Button retButton = uiComponents.create(Button.class);
        retButton.setIcon(CubaIcon.STAR.source());
        retButton.setDescription(messageBundle.getMessage("msgComment"));
        retButton.setEnabled(true);

        retButton.addClickListener(addClickListenerEvent -> {
            openPositionCommentViewInvoke();
        });

        return retButton;
    }

    public void openPositionCommentViewInvoke() {
        Screen screen = screens.create(OpenPositionCommentsView.class);
        ((OpenPositionCommentsView) screen).setOpenPosition(openPositionsTable.getSingleSelected());
        screen.show();
    }

    private Component createCommentButton(OpenPosition entity) {
        PopupButton retButton = uiComponents.create(PopupButton.class);
        retButton.setIcon(CubaIcon.COMMENT.source());
        retButton.setDescription(messageBundle.getMessage("msgComment"));
        retButton.setEnabled(true);

        retButton.addAction(new BaseAction("setRatingComment")
                .withCaption(messageBundle.getMessage("msgOpenPositionComment"))
                .withIcon(CubaIcon.STAR_O.source())
                .withHandler(actionPerformedEvent -> setRatingComment()));
        retButton.addAction(new BaseAction("viewRatingComment")
                .withCaption(messageBundle.getMessage("msgViewOpenPositionComment"))
                .withIcon(CubaIcon.VIEW_ACTION.source())
                .withHandler(actionPerformedEvent -> openPositionCommentViewInvoke()));

        return retButton;
    }

    private Component createSendedCandidatesButton(OpenPosition entity) {
        Button retButton = uiComponents.create(Button.class);
        retButton.setIcon(CubaIcon.USER_CIRCLE.source());
        retButton.setDescription("Отправленные кандидаты заказчику");
        retButton.setEnabled(true);

        retButton.addClickListener(e -> {
            if (openPositionsTable.getSingleSelected() != null) {
                JobCandidateSimpleBrowse jobCandidateSimpleBrowse
                        = screens.create(JobCandidateSimpleBrowse.class);
                jobCandidateSimpleBrowse.setOpenPosition(entity);
                jobCandidateSimpleBrowse.setHeader(entity);

                screens.show(jobCandidateSimpleBrowse);

            } else {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withDescription("Кандидатов отправленных заказчику на какую вакансию Вы хотите посмотреть?")
                        .show();
            }
        });

        return retButton;
    }

    // TODO: закрыть все остальные Details
    private void closeAllAnoterDetailsScreenFragments() {
        for (OpenPosition op : openPositionsTable.getItems().getItems(0,
                openPositionsDc.getItems().size())) {
            openPositionsTable.setDetailsVisible(op, false);
        }
    }

    private Component createViewDescriptionButton(OpenPosition entity) {
        Button retButton = uiComponents.create(Button.NAME);
        retButton.setIcon(CubaIcon.STREET_VIEW.iconName());
        retButton.setEnabled(openPositionsTable.getSingleSelected() != null);
        retButton.setCaption("Описание");
        retButton.setDescription("Просмотр описания вакансии и описания проекта");

        retButton.addClickListener(e -> {
            OpenPosition selected = openPositionsTable.getSingleSelected();
            if (selected != null) {
                OpenPosition loaded = loadOpenPositionWithDescriptionLobs(selected);
                QuickViewOpenPositionDescription quickViewOpenPositionDescription = screens.create(QuickViewOpenPositionDescription.class);
                quickViewOpenPositionDescription.setJobDescription(loaded.getComment() != null ? loaded.getComment() : "");

                if (loaded.getTemplateLetter() != null) {
                    quickViewOpenPositionDescription.setCvRequirement(loaded.getTemplateLetter());
                }

                if (loaded.getProjectName() != null && loaded.getProjectName().getProjectDescription() != null) {
                    quickViewOpenPositionDescription.setProjectDescription(loaded.getProjectName().getProjectDescription());
                }

                if (loaded.getCommentEn() != null) {
                    quickViewOpenPositionDescription.setJobDescriptionEng(loaded.getCommentEn());
                }

                if (loaded.getProjectName() != null
                        && loaded.getProjectName().getProjectDepartment() != null
                        && loaded.getProjectName().getProjectDepartment().getCompanyName() != null
                        && loaded.getProjectName().getProjectDepartment().getCompanyName().getWorkingConditions() != null) {
                    quickViewOpenPositionDescription.setCompanyWorkConditions(loaded.getProjectName()
                            .getProjectDepartment()
                            .getCompanyName()
                            .getWorkingConditions());
                }

                if (loaded.getProjectName() != null
                        && loaded.getProjectName().getProjectDepartment() != null
                        && loaded.getProjectName().getProjectDepartment().getCompanyName() != null) {
                    quickViewOpenPositionDescription.setCompanyDescription(loaded.getProjectName()
                            .getProjectDepartment()
                            .getCompanyName()
                            .getCompanyDescription());
                }

                quickViewOpenPositionDescription.reloadDescriptions();
                screens.show(quickViewOpenPositionDescription);
            } else {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withDescription("Куакую вакансию вы хотите просмотреть?")
                        .show();
            }
        });

        return retButton;
    }

    private Component findSuitableButton(OpenPosition entity) {

        if (getSubscubeOpenPosition(entity)) {

            if (dataManager.load(CandidateCV.class)
                    .query("select e from itpearls_CandidateCV e where e.resumePosition = :resumePosition")
                    .parameter("resumePosition", entity.getPositionType())
                    .cacheable(true)
                    .view("candidateCV-view")
                    .list().size() != 0) {

                Button suitableButton = uiComponents.create(Button.class);
                suitableButton.setDescription("Подобрать резюме по вакансии");
                suitableButton.setIconFromSet(CubaIcon.EYE);

                suitableButton.setAction(new BaseAction("Suggestjobcandidate")
                        .withHandler(actionPerformedEvent -> {
                            Suggestjobcandidate suggestjobcandidate = screens.create(Suggestjobcandidate.class);
                            suggestjobcandidate.setOpenPosition(entity);

                            suggestjobcandidate.show();
                        }));

                return suitableButton;
            } else {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withDescription("Нет кандидатов в базе для выбранной вакансии")
                        .show();

                return null;
            }
        } else {
            return null;
        }
    }

    private boolean getSubscubeOpenPosition(OpenPosition entity) {
        if (dataManager.load(RecrutiesTasks.class)
                .query(QUERY_GET_SUBSCRIBER)
                .parameter("openPosition", entity)
                .parameter("reacrutier", (ExtUser) userSession.getUser())
                .parameter("current_date", new Date())
                .view("recrutiesTasks-view")
                .list().size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    private Component createOpenCloseButton(OpenPosition entity) {
        Button retButton = uiComponents.create(Button.NAME);

        retButton.setIcon(!entity.getOpenClose() ? CubaIcon.REMOVE_ACTION.iconName() : CubaIcon.ADD_ACTION.iconName());

        retButton.setCaption(!entity.getOpenClose()
                ? messageBundle.getMessage("msgClose")
                : messageBundle.getMessage("msgOpen"));
        retButton.setDescription(!entity.getOpenClose()
                ? messageBundle.getMessage("msgCloseVacancy")
                : messageBundle.getMessage("msgOpenVacancy"));

        retButton.addClickListener(e -> {
            removeCandidatesWithConsideration();
            openCloseButtonClickListener(entity, retButton);
        });

        return retButton;
    }

    private void openCloseButtonClickListener(OpenPosition entity, PopupButton retButton) {
        if (!openCloseChildVacancy(entity)) {
            entity.setOpenClose(!entity.getOpenClose());

            retButton.getAction("closeOpenPositionAction").setCaption(!entity.getOpenClose()
                    ? messageBundle.getMessage("msgClose")
                    : messageBundle.getMessage("msgOpen"));
            retButton.setDescription(!entity.getOpenClose()
                    ? messageBundle.getMessage("msgCloseVacamcy")
                    : messageBundle.getMessage("msgOpenVacancy"));

            openCloseVacancy(entity);
        } else {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withDescription(
                            messageBundle.getMessage("msgCanNotCloseWithout"))
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .show();
        }
    }

    private void openCloseVacancy(OpenPosition entity) {
        if (entity.getOpenClose() || entity.getOpenClose() == null) {

            events.publish(new UiNotificationEvent(this,
                    openPositionService.getOpenPositionCloseShortMessage(entity,
                            userSession.getUser())));

            openPositionService.setOpenPositionNewsAutomatedMessage(openPositionsTable.getSingleSelected(),
                    "Закрылась вакансия",
                    "Закрыта вакансия",
                    new Date(),
                    (ExtUser) userSession.getUser());

            telegramService.sendMessageToChat(applicationSetupService.getTelegramChatOpenPosition(),
                    openPositionService.getOpenPositionCloseLongMessage(entity, userSession.getUser()));

            entity.setOwner(null);
            entity.setLastOpenDate(null);

            openPositionsTable.setDetailsVisible(entity, false);
            openPositionsDl.load();

        } else {

            events.publish(new UiNotificationEvent(this,
                    openPositionService.getOpenPositionOpenShortMessage(entity, userSession.getUser())));

            openPositionService.setOpenPositionNewsAutomatedMessage(openPositionsTable.getSingleSelected(),
                    "Открылась вакансия",
                    "Открыта вакансия",
                    new Date(),
                    (ExtUser) userSession.getUser());

            telegramService.sendMessageToChat(applicationSetupService
                            .getTelegramChatOpenPosition(),
                    openPositionService
                                    .getOpenPositionOpenLongMessage(entity, userSession.getUser()));

            entity.setOwner((ExtUser) userSession.getUser());
            entity.setLastOpenDate(new Date());
            entity.setPriority(PRIORITY_NORMAL);

            if (entity.getParentOpenPosition() != null) {
                if (entity.getParentOpenPosition().getOpenClose()) {
                    entity.getParentOpenPosition().setOpenClose(false);
                    openPositionService.setOpenPositionNewsAutomatedMessage(openPositionsTable.getSingleSelected().getParentOpenPosition(),
                            "Открыта дочерней вакансией "
                                    + openPositionsTable.getSingleSelected().getVacansyName(),
                            "Открыта ввиду открытия дочерней вакансии "
                                    + openPositionsTable.getSingleSelected().getVacansyName(),
                            new Date(),
                            (ExtUser) userSession.getUser());
                }
            }

            openPositionsTable.setDetailsVisible(entity, false);
            openPositionsDl.load();

            openPositionsTable.repaint();

            if (entity.getOpenClose() != null) {
                if (!entity.getOpenClose()) {
                    openPositionsTable.setSelected(entity);
                    openPositionsTable.scrollTo(entity);
                }
            } else {
                openPositionsTable.setSelected(entity);
                openPositionsTable.scrollTo(entity);
            }
        }

        dataManager.commit(entity);

        if (!entity.getOpenClose()) {
            entity.getProjectName().setProjectIsClosed(false);
        }

        openPositionsDl.load();
    }

    private void openCloseButtonClickListener(OpenPosition entity, Button retButton) {
        if (!openCloseChildVacancy(entity)) {
            entity.setOpenClose(!entity.getOpenClose());

            retButton.setCaption(!entity.getOpenClose()
                    ? messageBundle.getMessage("msgClose")
                    : messageBundle.getMessage("msgOpen"));
            retButton.setDescription(!entity.getOpenClose()
                    ? messageBundle.getMessage("msgCloseVacamcy") :
                    messageBundle.getMessage("msgOpenVacancy"));

            openCloseChildVacancy(entity);
        } else {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withDescription(
                            messageBundle.getMessage("msgCanNotCloseWithout"))
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .show();
        }
    }

    private Boolean openCloseChildVacancy(OpenPosition event) {
        List<OpenPosition> openPositions = dataManager.load(OpenPosition.class)
                .query(QUERY_SELECT_COMMAND)
                .parameter("parentOpenPosition", event)
                .view("openPosition-view")
                .list();

        String magPos = "";
        StringBuilder sb = new StringBuilder();

        AtomicReference<Boolean> flagDialog = new AtomicReference<>(false);

        if (openPositions.size() != 0) {
            for (OpenPosition a : openPositions) {
                sb.append("<li><i>")
                        .append(a.getVacansyName())
                        .append("</i></li>");
            }

            sb.insert(0, (event.getOpenClose()
                    ? messageBundle.getMessage("msgOpen")
                    : messageBundle.getMessage("msgClose")));
            sb.insert((event.getOpenClose()
                            ? messageBundle.getMessage("msgOpen")
                            : messageBundle.getMessage("msgClose")).length(),
                    " вакансии группы?<br><ul>");
            sb.append("</ul>");

            dialogs.createOptionDialog()
                    .withType(Dialogs.MessageType.WARNING)
                    .withContentMode(ContentMode.HTML)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withMessage(sb.toString())
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                        for (OpenPosition a : openPositions) {
                            a.setOpenClose(event.getOpenClose());
                        }

                        flagDialog.set(true);
                    }), new DialogAction(DialogAction.Type.NO))
                    .show();
        }

        return flagDialog.get();
    }

    @Install(to = "openPositionsTable.salaryMinMax", subject = "columnGenerator")
    private Object openPositionsTableSalaryMinMaxColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout retObject = setComponentsToOpenPositionsTable(event, getSalaryMinMaxStr(event));
        return retObject;
    }

    private String getSalaryMinMaxStr(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
//        String retStr = "";
        StringBuilder sb = new StringBuilder();

        try {
            sb.append(getSalaryString(event.getItem()));
            sb.append((event.getItem().getSalaryComment() != null ? " \ud83d\udcc3" : ""));

            return sb.toString();
        } catch (NullPointerException e) {
            if (event.getItem().getOutstaffingCost() != null) {
                return messageBundle.getMessage("msgUndefined");
            } else {
                return messageBundle.getMessage("msgNotPrice");
            }
        }

//        return retStr;
    }

    private String getSalaryString(OpenPosition openPosition) {
        BigDecimal salaryMin = openPosition.getSalaryMin().divide(BigDecimal.valueOf(1000));
        BigDecimal salaryMax = openPosition.getSalaryMax().divide(BigDecimal.valueOf(1000));

        StringBuilder sb = new StringBuilder();

        if (!(openPosition.getSalaryCandidateRequest() != null
                ? openPosition.getSalaryCandidateRequest() : false)) {
            try {
                int salMin = salaryMin.divide(BigDecimal.valueOf(1000)).intValue();
                if (salMin != 0) {

                    sb.append(salaryMin.toString().substring(0, salaryMin.toString().length() - 3))
                            .append(messageBundle.getMessage("msgThausendRubles"))
                            .append("/")
                            .append(salaryMax.toString().substring(0, salaryMax.toString().length() - 3))
                            .append(messageBundle.getMessage("msgThausendRubles"));
                } else {
                    sb.append("До ")
                            .append(salaryMax.toString().substring(0, salaryMax.toString().length() - 3))
                            .append(messageBundle.getMessage("msgThausendRubles"));
                }
            } catch (NullPointerException | StringIndexOutOfBoundsException e) {
                sb = new StringBuilder();
            }

            if (openPosition.getSalaryMin() == null || openPosition.getSalaryMin().equals("")) {
                if (openPosition.getSalaryMax() == null || openPosition.getSalaryMax().equals("")) {
                    if (openPosition.getOutstaffingCost() != null && !openPosition.getOutstaffingCost().equals("")) {
                        sb = new StringBuilder();
                        sb.append(messageBundle.getMessage("msgUndefined"));
                    } else {
                        sb = new StringBuilder();
                        sb.append(messageBundle.getMessage("msgNotPrice"));
                    }
                }
            }

        } else {
            sb = new StringBuilder();
            sb.append(messageBundle.getMessage("msgAtRequestOfCandidate"));
        }

        return sb.toString();
    }

    private String getSalaryStringCaption(OpenPosition openPosition) {
        BigDecimal salaryMin = openPosition.getSalaryMin().divide(BigDecimal.valueOf(1000));
        BigDecimal salaryMax = openPosition.getSalaryMax().divide(BigDecimal.valueOf(1000));

        StringBuilder sb = new StringBuilder();

        try {
            sb.append(salaryMin.toString().substring(0, salaryMin.toString().length() - 3))
                    .append(" т.р./")
                    .append(salaryMax.toString().substring(0, salaryMax.toString().length() - 3))
                    .append(" т.р.");
        } catch (NullPointerException | StringIndexOutOfBoundsException e) {
            sb = new StringBuilder();
        }

        if (salaryMin.intValue() == 0) {
            sb = new StringBuilder();
            sb.append("неопределено");
        }

        return sb.toString();
    }

    @Install(to = "openPositionsTable.salaryMinMax", subject = "descriptionProvider")
    private String openPositionsTableSalaryMinMaxDescriptionProvider(OpenPosition openPosition) {
        StringBuilder sb = new StringBuilder();

        if (openPosition.getSalaryFixLimit() != null) {
            if (openPosition.getSalaryFixLimit()) {
                sb.append("Фиксированное запрлатное предложение.\n");
            }
        }

        try {
            sb.append(getSalaryStringCaption(openPosition))
                    .append((openPosition.getOutstaffingCost() != null ?
                            "\nПредельная ставка заказчика: "
                                    + openPosition.getOutstaffingCost()
                                    + " руб./час" : ""));
        } catch (NullPointerException e) {
            sb = new StringBuilder();
        }

        if (openPosition.getSalaryCandidateRequest() != null
                ? openPosition.getSalaryCandidateRequest() : false) {
            sb = new StringBuilder();
            sb.append(messageBundle.getMessage("msgSalaryExpectation"));
        }

        return sb.append((openPosition.getSalaryComment() != null ?
                        "\n\n" + openPosition.getSalaryComment()
                        : ""))
                .toString();
    }

    @Install(to = "openPositionsTable.salaryMinMax", subject = "styleProvider")
    private String openPositionsTableSalaryMinMaxStyleProvider(OpenPosition openPosition) {
        String retStr = "";

        if (openPosition.getSalaryCandidateRequest() != null
                ? !openPosition.getSalaryCandidateRequest() : true) {
            if (openPosition.getSalaryFixLimit() != null) {
                if (openPosition.getSalaryFixLimit()) {
                    retStr = "salary-fix-limit";
                }
            }
        } else {
            retStr = style_table_wordwrap;
        }

        return retStr;
    }

    @Install(to = "openPositionsTable.cityPositionList", subject = "columnGenerator")
    private Object openPositionsTableCityPositionListColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout retObject = setComponentsToOpenPositionsTable(event, getCityPositions(event));
        return retObject;
    }

    private String getCityPositions(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String mainCity = "";
        StringBuilder sb = new StringBuilder();

        if (event.getItem().getCityPosition() != null) {
            if (event.getItem().getCityPosition().getCityRuName() != null) {
                mainCity = event.getItem().getCityPosition().getCityRuName();
            }
        }

        sb.append(mainCity)
                .append(((event.getItem().getCities().size() != 0) ? " [+]" : ""));

        return sb.toString();
    }

    Boolean flagPriority = true;

    private Component createPriorityField(OpenPosition openPosition) {
        LookupField retField = uiComponents.create(LookupField.NAME);

        retField.setOptionsMap(priorityMap);
        retField.setValue(openPosition.getPriority());
        retField.setAlignment(Component.Alignment.TOP_RIGHT);
        retField.addValueChangeListener(f -> {
            int value = (int) retField.getValue();
            Optional<String> result = priorityMap.entrySet()
                    .stream()
                    .filter(entry -> value == entry.getValue())
                    .map(Map.Entry::getKey)
                    .findFirst();

            openPosition.setPriority((int) retField.getValue());

            if (retField.getValue().equals(OpenPositionPriority.LOW.getId())) {
                setClosingWeek(openPosition);
            } else {
                dataManager.commit(openPosition);
                openPositionsDl.load();
                openPositionsTable.repaint();
                openPositionsTable.scrollTo(openPosition);
            }

            StringBuilder sb = new StringBuilder()
                    .append("Изменен приоритет вакансии <b>")
                    .append(openPosition.getVacansyName())
                    .append("</b> на <b>")
                    .append(result.get())
                    .append("</b><br><svg align=\"right\" width=\"100%\"><i>")
                    .append(userSession.getUser().getName())
                    .append("</i></svg>");

            events.publish(new UiNotificationEvent(this,
                    sb.toString()));

            telegramService.sendMessageToChat(applicationSetupService.getTelegramChatOpenPosition(),
                    sb.toString());

            if (flagPriority) {
                openPositionService.setOpenPositionNewsAutomatedMessage(openPosition,
                        "Изменен приоритет вакансии на " + result.get(),
                        "Изменен приоритет вакансии на " + result.get(),
                        new Date(),
                        (ExtUser) userSession.getUser());
            } else {
                flagPriority = true;
            }
        });

        retField.setLookupSelectHandler(e -> {
            if (retField.getValue().equals(OpenPositionPriority.LOW.getId())) {
                setClosingWeek(openPosition);
            } else {
                openPositionsTable.getSingleSelected().setPriority((int) retField.getValue());
                dataManager.commit(openPosition);
                openPositionsDl.load();
                openPositionsTable.repaint();
                openPositionsTable.scrollTo(openPosition);
            }
        });

        retField.setOptionIconProvider(g -> {
            switch (g.hashCode()) {
                case PRIORITY_DRAFT:
                    return icons_traffic_lights_gray_png;
                case PRIORITY_PAUSED: //"Paused"
                    return icons_remove_png;
                case PRIORITY_LOW: //"Low"
                    return icons_traffic_lights_blue_png;
                case PRIORITY_NORMAL: //"Normal"
                    return icons_traffic_lights_green_png;
                case PRIORITY_HIGH: //"High"
                    return icons_traffic_lights_yellow_png;
                case PRIORITY_CRITICAL: //"Critical"
                    return icons_traffic_lights_red_png;
                default:
                    return null;
            }
        });

        return retField;
    }

    private void setClosingWeek(OpenPosition openPosition) {
        if (openPosition.getClosingDate() == null) {
            dialogs.createOptionDialog()
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withMessage(messageBundle.getMessage("msgSetClosingVacancyWeek"))
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                            .withHandler(e -> {
                                GregorianCalendar calendar = new GregorianCalendar();
                                calendar.add(7, Calendar.DAY_OF_WEEK);

                                openPosition.setClosingDate(calendar.getTime());
                                dataManager.commit(openPosition);

                                openPositionsDl.load();
                                openPositionsTable.repaint();
                                openPositionsTable.setSelected(openPosition);
                                openPositionsTable.scrollTo(openPosition);

                                notifications.create(Notifications.NotificationType.WARNING)
                                        .withDescription(messageBundle.getMessage("msgSetClosingVacancyWeek"))
                                        .withCaption(messageBundle.getMessage("msgWarning"))
                                        .withHideDelayMs(5000)
                                        .withPosition(Notifications.Position.DEFAULT)
                                        .show();
                            }), new DialogAction(DialogAction.Type.NO))
                    .show();
        }
    }

    private Component createTitleFragment(OpenPosition entity) {
        Label<String> titleLabel = uiComponents.create(Label.NAME);
        titleLabel.addStyleName("h4");
        titleLabel.addStyleName(style_table_wordwrap);
        titleLabel.setDescription(entity.getVacansyName());
        titleLabel.setValue(entity.getVacansyName());
        titleLabel.setAlignment(Component.Alignment.BOTTOM_LEFT);
        titleLabel.setWidthFull();

        return titleLabel;
    }

    private Component createEditButton(OpenPosition entity) {
        Button editButton = uiComponents.create(Button.class);
        editButton.setCaption("Изменить");
        editButton.setIcon(CubaIcon.EDIT.iconName());
        editButton.setAlignment(Component.Alignment.TOP_RIGHT);

        BaseAction editAction = new BaseAction("edit")
                .withHandler(actionPerformedEvent -> {
                    screenBuilders.editor(OpenPosition.class, this)
                            .editEntity(entity)
                            .build()
                            .show();
                })
                .withCaption("");
        editButton.setAction(editAction);
        return editButton;
    }

    private Component createCloseButton(OpenPosition entity) {
        Button closeButton = uiComponents.create(Button.class);
        closeButton.setIcon("icons/close.png");
        BaseAction closeAction = new BaseAction("closeAction")
                .withHandler(actionPerformedEvent -> {
                    openPositionsTable.setDetailsVisible(entity, false);
                    openPositionsTable.repaint();
                    openPositionsTable.setSelected(entity);
                    openPositionsTable.scrollTo(entity);
                })
                .withCaption("");
        closeButton.setAction(closeAction);
        return closeButton;
    }


    @Install(to = "openPositionsTable", subject = "rowStyleProvider")
    private String openPositionsTableRowStyleProvider(OpenPosition openPosition) {
        int s = openPosition.getId() != null
                ? activeRecruitersCountByPosition.getOrDefault(openPosition.getId(), 0)
                : 0;

        if (openPosition.getSignDraft() == null ? true : !openPosition.getSignDraft()) {
            if (openPosition.getInternalProject() != null) {
                if (openPosition.getInternalProject()) {
                    if (s == 0) {
                        return "open-position-internal-project";
                    } else {
                        return "open-position-internal-project-job-recrutier";
                    }
                } else {
                    if (s == 0)
                        if ((openPosition.getCommandCandidate() != null ? openPosition.getCommandCandidate() : 2) != 1)
                            return "open-position-empty-recrutier";
                        else
                            return "open-position-job-command";
                    else
                        return "open-position-job-recruitier";
                }
            } else {
                if (openPosition.getCommandCandidate() != 1) {
                    if (s == 0)
                        return "open-position-empty-recrutier";
                    else
                        return "open-position-job-recruitier";
                } else
                    return "open-position-job-command";
            }
        } else {
            return "open-position-draft";
        }
    }

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {
        initCheckBoxOnlyOpenedPosition();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        checkBoxOnlyOpenedPosition.setValue(true); // только открытые позиции
        buttonExcel.setEnabled(getRoleService.isUserRoles(userSession.getUser(), StandartRoles.MANAGER));

        setInternalProjectFilter();
        setSubcribersFilter();
        setOpenPositionNotPaused();
        setStatusNotLower();
        setStatusRemoteWork();

        if (!getRoleService.isUserRoles(userSession.getUser(), StandartRoles.MANAGER)) {
            notLowerRatingLookupField.setValue(PRIORITY_NORMAL);
        }

        setUrgentlyPositios(3);

        clearUrgentFilter();
    }

    @Subscribe
    public void onAfterShow2(AfterShowEvent event) {
        initOpenCloseButton();
        setButtonsEnableDisable();
    }

    private void initOpenCloseButton() {
        openCloseButton.addAction(new BaseAction("closeOpenPositionAction")
                .withCaption(messageBundle.getMessage("msgOpenClose"))
                .withHandler(e -> openCloseButtonInvoke(e))
                .withIcon(CubaIcon.CLOSE.source()));
        openCloseButton.addAction(new BaseAction("closeOpenPositionWithCommentAction")
                .withCaption(messageBundle.getMessage("msgCloseOpenPositionWithComment"))
                .withHandler(e -> openCloseButtonWithCommentInvoke(e))
                .withIcon(CubaIcon.COMMENTING.source()));

        initActionButtonSeparator(openCloseButton, "separator1");
        final String actionStr = "Action";

        for (Map.Entry entry : priorityMap.entrySet()) {
            openCloseButton.addAction(
                    new BaseAction(new StringBuilder().append(
                                    entry.getKey().toString())
                            .append(actionStr)
                            .toString())
                            .withHandler(e -> {

                                if (openPositionsTable.getSingleSelected() != null) {
                                    int newPriority = 0;

                                    String action = e.getSource().getId();
                                    // openPositionsTable.getSingleSelected().setPriority();

                                    for (Map.Entry entry1 : priorityMap.entrySet()) {
                                        if (action.startsWith(entry1.getKey().toString())) {
                                            newPriority = (int) entry1.getValue();
                                            break;
                                        }
                                    }

                                    OpenPosition openPosition = openPositionsTable.getSingleSelected();
                                    openPosition.setPriority(newPriority);

                                    if (newPriority == OpenPositionPriority.LOW.getId()) {
                                        setClosingWeek(openPosition);
                                    } else {
                                        dataManager.commit(openPosition);
                                        openPositionsDl.load();
                                        openPositionsTable.repaint();
                                        openPositionsTable.setSelected(openPosition);
                                        openPositionsTable.scrollTo(openPosition);
                                    }
                                }
                            })
                            .withIcon(getPriorityIcon((int) entry.getValue()))
                            .withCaption(entry.getKey().toString()));
        }

        openCloseButton.setEnabled(false);

    }

    private void setButtonsEnableDisable() {
        buttonSubscribe.setEnabled(false);
        reportsPopupButton.setEnabled(false);

        openPositionsTable.addSelectionListener(e -> {
            if (e.getSelected() == null) {
                buttonSubscribe.setEnabled(false);
                reportsPopupButton.setEnabled(false);
                setRatingButton.setEnabled(false);

                openCloseButton.setEnabled(false);
                openCloseButton.setIconFromSet(CubaIcon.CLOSE);
                openCloseButton.setCaption(messageBundle.getMessage("msgOpenClose"));
            } else {
                setRatingButton.setEnabled(true);
            }
        });

        openPositionsTable.addItemClickListener(e -> {
            buttonSubscribe.setEnabled(true);
            reportsPopupButton.setEnabled(true);

            if (getRoleService.isUserRoles(userSession.getUser(), StandartRoles.MANAGER)) {
                if (e.getItem().getOpenClose()) {
                    openCloseButton.setEnabled(true);
                    openCloseButton.setIconFromSet(CubaIcon.YES);
                    openCloseButton.setCaption(messageBundle.getMessage("msgOpen"));
                    openCloseButton.getAction("closeOpenPositionAction")
                            .setCaption(messageBundle.getMessage("msgOpen"));
                    openCloseButton.getAction("closeOpenPositionAction")
                            .setIcon(CubaIcon.OPENID.source());
                    openCloseButton.getAction("closeOpenPositionWithCommentAction")
                            .setCaption(messageBundle.getMessage("msgOpenOpenPositionWithComment"));
                } else {
                    openCloseButton.setEnabled(true);
                    openCloseButton.setIconFromSet(CubaIcon.CLOSE);
                    openCloseButton.setCaption(messageBundle.getMessage("msgClose"));
                    openCloseButton.getAction("closeOpenPositionAction")
                            .setCaption(messageBundle.getMessage("msgClose"));
                    openCloseButton.getAction("closeOpenPositionAction")
                            .setIcon(CubaIcon.CLOSE.source());
                    openCloseButton.getAction("closeOpenPositionWithCommentAction")
                            .setCaption(messageBundle.getMessage("msgCloseOpenPositionWithComment"));
                }
            }
        });
    }

    @Subscribe("remoteWorkLookupField")
    public void onRemoteWorkLookupFieldValueChange(HasValue.ValueChangeEvent event) {
        if ((int) event.getValue() >= 0) {
            openPositionsDl.setParameter("remoteWork", remoteWorkLookupField.getValue());
        } else {
            openPositionsDl.removeParameter("remoteWork");
        }

        openPositionsDl.load();
    }

    @Subscribe("notLowerRatingLookupField")
    public void onNotLowerRatingLookupFieldValueChange1(HasValue.ValueChangeEvent event) {
        removeUrgentlyLists();

        if (notLowerRatingLookupField.getValue() != null) {
            if (((int) notLowerRatingLookupField.getValue()) != PRIORITY_DRAFT) {
                if (((int) notLowerRatingLookupField.getValue()) != PRIORITY_NONE) {
                    setUrgentlyPositios(notLowerRatingLookupField.getValue() == null ? 0 : (int) notLowerRatingLookupField.getValue());
                } else {
                    openPositionsDl.removeParameter("rating");
                }
            } else {
                openPositionsDl.removeParameter("rating");
                openPositionsDl.setParameter("signDraft", true);
            }
        } else {
            openPositionsDl.removeParameter("rating");
        }

        openPositionsDl.load();
    }

    private void removeUrgentlyLists() {
        for (Component component : urgentlyHBox.getComponents()) {
            urgentlyHBox.remove(component);
        }
    }

    private void setUrgentlyPositios(int priority) {

        List<OpenPosition> openPositions = dataManager.load(OpenPosition.class)
                .query(QUERY_URGENTLY_POSITIONS)
                .parameter("priority", priority)
                .cacheable(true)
                .view("openPosition-view")
                .list();

        HashMap<String, Integer> opList = new HashMap<>();

        // перемешать коллекцию случайным образом
        Collections.shuffle(openPositions);

        for (OpenPosition op : openPositions) {
            if (op.getPositionType() != null) {
                if (op.getPositionType().getPositionRuName() != null) {
                    String positionName = op.getPositionType().getPositionRuName();

                    if (!opList.containsKey(positionName)) {
                        opList.put(positionName, op.getPriority());
                    } else {
                        if (opList.get(positionName) < op.getPriority()) {
                            opList.remove(positionName);
                            if (positionName != null) {
                                opList.put(positionName, op.getPriority());
                            }
                        }
                    }
                }
            }
        }

        if (opList.size() == 0) {
            urgentlyPositons.setVisible(false);
        }

        for (HashMap.Entry op : opList.entrySet()) {
            Integer countOp = 0;

            LinkButton label = uiComponents.create(LinkButton.NAME);

            StringBuilder sb = new StringBuilder("<b><u>Проекты:</u></b><br>");

            for (OpenPosition op1 : openPositions) {
                if (op1.getPositionType() != null) {
                    if (op1.getPositionType().getPositionRuName() != null) {

                        if (op.getKey().equals(op1.getPositionType().getPositionRuName()) &&
                                (!op1.getOpenClose() || op1.getOpenClose() == null)) {
                            sb.append(op1.getProjectName().getProjectName()).append("<br>");

                            if (notLowerRatingLookupField.getValue() != null) {
                                if ((int) notLowerRatingLookupField.getValue() >= 0) {
                                    if (op1.getPriority() >= (int) notLowerRatingLookupField.getValue() &&
                                            !op1.getOpenClose()) {
                                        if (op1.getNumberPosition() != null) {
                                            countOp += op1.getNumberPosition();
                                        }
                                    }
                                }
                            } else if (!op1.getOpenClose()) {
                                if (op1.getNumberPosition() != null) {
                                    if (op1.getNumberPosition() != null) {
                                        countOp += op1.getNumberPosition();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            label.setCaption(op.getKey().toString() + " (" + countOp.toString() + ")");
            label.setDescriptionAsHtml(true);
            label.setDescription(sb.toString());
            label.addClickListener(clickEvent -> {
                OpenPosition opRet = null;

                for (OpenPosition ops : openPositions) {
                    if (ops.getPositionType() != null) {
                        if (ops.getPositionType().getPositionRuName() != null) {
                            if (ops.getPositionType().getPositionRuName().equals(op.getKey())) {
                                opRet = ops;
                                break;
                            }
                        }
                    }

                }

                openPositionsDl.setParameter("rating", opRet.getPriority());
                openPositionsDl.setParameter("positionType", opRet.getPositionType());
                openPositionsDl.load();
            });

            switch ((int) op.getValue()) {
                case 1:
                    label.setStyleName("label_button_blue");
                    break;
                case 2:
                    label.setStyleName("label_button_green");
                    break;
                case 3:
                    label.setStyleName("label_button_orange");
                    break;
                case 4:
                    label.setStyleName("label_button_red");
                    break;
                default:
                    label.setStyleName("label_button_grey");
                    break;
            }

            Label label1 = uiComponents.create(Label.NAME);
            label1.setValue(" ");

            urgentlyHBox.add(label);
            urgentlyHBox.add(label1);
        }
    }

    private void setMapOfPriority() {
        priorityMap.put("None", PRIORITY_NONE);
        priorityMap.put("Draft", PRIORITY_DRAFT);
        priorityMap.put("Paused", PRIORITY_PAUSED);
        priorityMap.put("Low", PRIORITY_LOW);
        priorityMap.put("Normal", PRIORITY_NORMAL);
        priorityMap.put("High", PRIORITY_HIGH);
        priorityMap.put("Critical", PRIORITY_CRITICAL);
    }

    private void setStatusNotLower() {
        notLowerRatingLookupField.setOptionsMap(priorityMap);
    }

    private void setStatusRemoteWork() {
        remoteWorkLookupField.setOptionsMap(remoteWork);
    }

    @Subscribe("notLowerRatingLookupField")
    public void onNotLowerRatingLookupFieldValueChange(HasValue.ValueChangeEvent event) {
        if (notLowerRatingLookupField.getValue() != null) {
            openPositionsDl.setParameter("rating", notLowerRatingLookupField.getValue());
        } else {
            openPositionsDl.removeParameter("rating");
        }

        openPositionsDl.load();
    }

    private void setOpenPositionNotPaused() {
        checkBoxOnlyNotPaused.setValue(true);
    }

    private void setInternalProjectFilter() {
        if (!getRoleService.isUserRoles(userSession.getUser(), StandartRoles.RESEARCHER)) {
            openPositionsDl.removeParameter("subscriber");
        } else {
            openPositionsDl.setParameter("subscriber", userSession.getUser());
        }

        openPositionsDl.load();
    }

    private void setSubcribersFilter() {
        if (checkBoxOnlyMySubscribe.getValue()) {
            openPositionsDl.setParameter("subscriber", userSession.getUser());
            openPositionsDl.removeParameter("notsubscriber");
            openPositionsDl.removeParameter("freesubscriber");
            openPositionsDl.removeParameter("newOpenPosition");
        } else {
            openPositionsDl.removeParameter("subscriber");
            openPositionsDl.setParameter("notsubscriber", userSession.getUser());
            openPositionsDl.removeParameter("freesubscriber");
            openPositionsDl.removeParameter("newOpenPosition");
        }

        openPositionsDl.load();
    }

    @Subscribe("checkBoxOnlyMySubscribe")
    public void onCheckBoxOnlyMySubscribeValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        setSubcribersFilter();
    }

    @Subscribe("checkBoxOnlyNotPaused")
    public void onCheckBoxOnlyNotPausedValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxOnlyNotPaused.getValue()) {
            openPositionsDl.setParameter("paused", 0);
        } else {
            openPositionsDl.removeParameter("paused");
        }

        openPositionsDl.load();
    }


    @Subscribe("checkBoxOnlyOpenedPosition")
    public void onCheckBoxOnlyOpenedPositionValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxOnlyOpenedPosition.getValue()) {
            openPositionsDl.setParameter("openClosePos", false);
        } else {
            openPositionsDl.removeParameter("openClosePos");
        }

        openPositionsDl.load();
    }

    private String getIcon(OpenPosition openPosition) {
        String icon = null;

        Integer priority = openPosition.getPriority();

        if (priority != null) {
            switch (priority) {
                case PRIORITY_NONE:
                    icon = CubaIcon.MINUS.source();
                    break;
                case PRIORITY_DRAFT:
                    icon = "icons/traffic-lights_gray.png";
                    break;
                case PRIORITY_PAUSED: //"Paused"
                    icon = "icons/traffic-lights_gray.png";
                    break;
                case PRIORITY_LOW: //"Low"
                    icon = "icons/traffic-lights_blue.png";
                    break;
                case PRIORITY_NORMAL: //"Normal"
                    icon = "icons/traffic-lights_green.png";
                    break;
                case PRIORITY_HIGH: //"High"
                    icon = "icons/traffic-lights_yellow.png";
                    break;
                case PRIORITY_CRITICAL: //"Critical"
                    icon = "icons/traffic-lights_red.png";
                    break;
            }
        } else {
            icon = "icons/question-white.png";
        }

        return icon;
    }

    public void subscribePosition() {
        Screen opScreen = screenBuilders
                .editor(RecrutiesTasks.class, this)
                .newEntity()
                .withInitializer(data -> {
                    data.setOpenPosition(openPositionsTable.getSingleSelected());
                })
                .withScreenId("itpearls_RecrutiesTasks.edit")
                .withLaunchMode(OpenMode.DIALOG)
                .build();

        opScreen.addAfterCloseListener(e -> {
            this.openPositionsDl.load();
        });

        opScreen.show();
    }

    public void groupSubscribe() {
        screens.create(RecrutiesTasksGroupSubscribeBrowse.class).show();
    }

    public void clearUrgentFilter() {
        if (notLowerRatingLookupField.getValue() != null) {
            openPositionsDl.setParameter("rating", (int) notLowerRatingLookupField.getValue());
        } else {
            openPositionsDl.removeParameter("rating");
        }

        openPositionsDl.removeParameter("positionType");
        openPositionsDl.load();
    }

    @Install(to = "openPositionsTable.positionType", subject = "descriptionProvider")
    private String openPositionsTablePositionTypeDescriptionProvider(OpenPosition openPosition) {
        return formatPositionTypeDescription(openPosition.getPositionType());
    }

    @Install(to = "openPositionsTable.cityPositionList", subject = "descriptionProvider")
    private String openPositionsTableCityPositionListDescriptionProvider(OpenPosition openPosition) {
        StringBuilder sb = new StringBuilder();

        if (openPosition.getCityPosition() != null) {
            sb.append(openPosition.getCityPosition().getCityRuName());
        }

        if (openPosition.getCities() != null) {
            for (City s : openPosition.getCities()) {
                if (sb.length() != 0) {
                    sb.append(",");
                }
                sb.append(s.getCityRuName());
            }
        }

        return sb.toString();
    }

    @Subscribe("openPositionsTable")
    public void onOpenPositionsTableEditorClose(DataGrid.EditorCloseEvent event) {
        openPositionsDl.load();
    }

    @Install(to = "openPositionsTable.queryQuestion", subject = "columnGenerator")
    private Object openPositionsTableQueryQuestionColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String returnIcon = "";

        switch (getQueryQuestion(event)) {
            case 1:
                returnIcon = "PLUS_CIRCLE";
                break;
            case 0:
                returnIcon = "MINUS_CIRCLE";
                break;
            case 2:
                returnIcon = "QUESTION_CIRCLE";
                break;
            default:
                returnIcon = "QUESTION_CIRCLE";
                break;
        }

        return setPlusMinusIcon(CubaIcon.valueOf(returnIcon));
    }

    private int getQueryQuestion(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        return hasTemplateLetter(event.getItem()) ? 1 : 0;
    }

    private String getTemplateLetter(OpenPosition openPosition) {
        return getLazyTemplateLetterText(openPosition);
    }

    @Install(to = "openPositionsTable.queryQuestion", subject = "descriptionProvider")
    private String openPositionsTableQueryQuestionDescriptionProvider(OpenPosition openPosition) {
        return getTemplateLetter(openPosition);
    }

    @Install(to = "openPositionsTable.queryQuestion", subject = "styleProvider")
    private String openPositionsTableQueryQuestionStyleProvider(OpenPosition openPosition) {
        if (hasTemplateLetter(openPosition)) {
            return open_position_pic_center_large_green;
        } else {
            return open_position_pic_center_large_red;
        }
    }

    @Subscribe("openPositionsTable")
    public void onOpenPositionsTableSelection(DataGrid.SelectionEvent<OpenPosition> event) {
        if (openPositionsTable.getSingleSelected() != null) {
            suggestCandidateButton.setEnabled(true);
        } else {
            suggestCandidateButton.setEnabled(false);
        }
    }

    public void suggestCandidateButton() {
        Suggestjobcandidate suggestjobcandidate = screens.create(Suggestjobcandidate.class);
        suggestjobcandidate.setOpenPosition(openPositionsTable.getSingleSelected());

        suggestjobcandidate.show();
    }

    @Install(to = "openPositionsTable.memoForCandidateColumn", subject = "descriptionProvider")
    private String openPositionsTableMemoForCandidateColumnDescriptionProvider(OpenPosition openPosition) {
        String memo = getLazyMemoText(openPosition);
        if (memo != null)
            return Jsoup.parse(memo).wholeText();
        else
            return null;
    }

    @Install(to = "openPositionsTable.memoForCandidateColumn", subject = "styleProvider")
    private String openPositionsTableMemoForCandidateColumnStyleProvider(OpenPosition openPosition) {
        if (hasMemoForInterview(openPosition)) {
            return open_position_pic_center_large_green;
        }
        return open_position_pic_center_large_red;
    }

    @Install(to = "openPositionsTable.lastOpenCloseColumn", subject = "columnGenerator")
    private Object openPositionsTableLastOpenCloseColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout retObject = uiComponents.create(HBoxLayout.class);
        Label label = uiComponents.create(Label.class);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yy");
        Date lastDate = event.getItem().getLastOpenDate() != null
                ? event.getItem().getLastOpenDate() : event.getItem().getCreateTs();

        retObject.setWidthFull();
        retObject.setHeightFull();
        retObject.setAlignment(Component.Alignment.MIDDLE_CENTER);

        label.setHeightAuto();
        label.setWidthAuto();
        label.setAlignment(Component.Alignment.MIDDLE_CENTER);
        label.setValue(lastDate != null ?
                simpleDateFormat.format(lastDate) : "");

        retObject.add(label);

        return retObject;
    }

    @Install(to = "openPositionsTable.lastOpenCloseColumn", subject = "styleProvider")
    private String openPositionsTableLastOpenCloseColumnStyleProvider(OpenPosition openPosition) {
        Date lastDate = openPosition.getLastOpenDate() != null
                ? openPosition.getLastOpenDate() : openPosition.getCreateTs();
        if (lastDate != null) {
            Date date = new Date();

            if (date.before(DateUtils.addMonths(lastDate, 1))) {
                return "pic-center-small-red";
            } else {
                if (date.before(DateUtils.addMonths(lastDate, 2))) {
                    return "pic-center-small-orange";
                } else {
                    if (date.before(DateUtils.addMonths(lastDate, 3))) {
                        return "pic-center-small-green";
                    } else {
                        return "pic-center-small-grey";

                    }
                }
            }
        } else {
            return "pic-center-small-gray";
        }
    }

    @Install(to = "openPositionsTable.lastOpenCloseColumn", subject = "descriptionProvider")
    private String openPositionsTableLastOpenCloseColumnDescriptionProvider(OpenPosition openPosition) {
        if (openPosition.getLastOpenDate() != null) {
            Date date = new Date();

            if (date.before(DateUtils.addMonths(openPosition.getLastOpenDate(), 1))) {
                return "Открыта более месяца назад";
            } else {
                if (date.before(DateUtils.addMonths(openPosition.getLastOpenDate(), 2))) {
                    return "Открыта более 2-х месяцев назад";
                } else {
                    if (date.before(DateUtils.addMonths(openPosition.getLastOpenDate(), 3))) {
                        return "Открыта более 3-х месяцев назад";
                    } else {
                        return "Открыта очень давно";
                    }
                }
            }
        } else {
            return "Открыта недавно";
        }
    }

    @Install(to = "openPositionsTable.memoForCandidateColumn", subject = "columnGenerator")
    private Object openPositionsTableMemoForCandidateColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String returnIcon = hasMemoForInterview(event.getItem())
                ? CubaIcon.PLUS_CIRCLE.iconName()
                : CubaIcon.MINUS_CIRCLE.iconName();
        return setPlusMinusIcon(CubaIcon.valueOf(returnIcon));
    }

    Integer montOfStat = 3;

    @Install(to = "openPositionsTable.idStatistics", subject = "columnGenerator")
    private Object openPositionsTableIdStatisticsColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout retObject = setComponentsToOpenPositionsTable(event, getIDStatistics(event));
        return retObject;
    }


    private String getIDStatistics(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
//        String retStr = "";
        StringBuilder sb = new StringBuilder();

        GregorianCalendar gregorianCalendar = (GregorianCalendar) GregorianCalendar.getInstance();
        Date endDate = gregorianCalendar.getTime();
        gregorianCalendar.add(Calendar.MONTH, -montOfStat);
        Date startDate = gregorianCalendar.getTime();

        List<KeyValueEntity> iteractionIntegerKeyValue =
                dataManager.loadValues(QUERY_COUNT_ITERACTIONS)
                        .properties("iteractionType", "sum")
                        .parameter("startDate", startDate)
                        .parameter("vacancy", event.getItem())
                        .parameter("endDate", endDate)
                        .list();

        if (iteractionIntegerKeyValue.size() != 0) {
            for (KeyValueEntity entity : iteractionIntegerKeyValue) {
//                retStr += entity.getValue("sum") + " / ";
                sb.append(entity.getValue("sum").toString()).append(" / ");

            }

//            retStr = retStr.substring(0, retStr.length() - 3);
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    @Install(to = "openPositionsTable.idStatistics", subject = "descriptionProvider")
    private String openPositionsTableIdStatisticsDescriptionProvider(OpenPosition openPosition) {
//        String retStr = "Статистика за " + montOfStat + " месяца\n";
        StringBuilder sb = new StringBuilder();
        sb.append("Статистика за ")
                .append(montOfStat)
                .append(" месяца\n");

        GregorianCalendar gregorianCalendar = (GregorianCalendar) GregorianCalendar.getInstance();
        Date endDate = gregorianCalendar.getTime();
        gregorianCalendar.add(Calendar.MONTH, -montOfStat);
        Date startDate = gregorianCalendar.getTime();

        List<KeyValueEntity> iteractionIntegerKeyValue =
                dataManager.loadValues(QUERY_COUNT_ITERACTIONS)
                        .properties("iteractionType", "sum")
                        .parameter("startDate", startDate)
                        .parameter("vacancy", openPosition)
                        .parameter("endDate", endDate)
                        .list();

        if (iteractionIntegerKeyValue.size() != 0) {
            for (KeyValueEntity entity : iteractionIntegerKeyValue) {
                String iteractionName = ((Iteraction) entity.getValue("iteractionType"))
                        .getIterationName();
//                retStr += iteractionName + " : " + entity.getValue("sum") + "\n";

                sb.append(iteractionName)
                        .append(" : ")
                        .append(entity.getValue("sum").toString())
                        .append("\n");
            }

            sb.deleteCharAt(sb.length() - 1);

//            retStr = retStr.substring(0, retStr.length() - 1);
        }

        return sb.toString();
    }

    String more_10_msg = "<font color=red>10</font>";
    String clarification_required = "<font color=blue>???</font>";

    @Install(to = "openPositionsTable.numberPosition", subject = "columnGenerator")
    private Object openPositionsTableNumberPositionColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {

        String labelStr = "";
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        Label label = uiComponents.create(Label.class);

        retHBox.setWidthFull();
        retHBox.setHeightFull();
        retHBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retHBox.setStyleName(style_table_wordwrap);

        label.setHeightAuto();
        label.setAlignment(Component.Alignment.MIDDLE_LEFT);
        label.setStyleName(style_table_wordwrap);
        label.setHtmlEnabled(true);

        if (event.getItem().getMore10NumberPosition() == null) {
            if (event.getItem().getNumberPosition() != null) {
                if (event.getItem().getNumberPosition() < 10) {
                    labelStr = event.getItem().getNumberPosition().toString();
                } else {
                    labelStr = more_10_msg;
                }
            } else {
                labelStr = clarification_required;
            }
        } else {
            if (event.getItem().getMore10NumberPosition()) {
                labelStr = more_10_msg;
            } else {
                if (event.getItem().getNumberPosition() < 10) {
                    labelStr = event.getItem().getNumberPosition().toString();
                } else {
                    labelStr = more_10_msg;
                }
            }
        }

        label.setValue(labelStr);
        retHBox.add(label);
        retHBox.expand(label);

        return retHBox;

    }

    @Install(to = "openPositionsTable.numberPosition", subject = "descriptionProvider")
    private String openPositionsTableNumberPositionDescriptionProvider(OpenPosition openPosition) {
        if (openPosition.getMore10NumberPosition() != null) {
            if (openPosition.getMore10NumberPosition()) {
                if (openPosition.getNumberPosition() == null)
                    return ">10";
                else
                    return openPosition.getNumberPosition().toString();
            } else {
                if (openPosition.getNumberPosition() != null) {
                    return openPosition.getNumberPosition().toString();
                } else {
                    return null;
                }
            }
        } else {
            return openPosition.getNumberPosition() == null ? "" : openPosition.getNumberPosition().toString();
        }
    }

    @Install(to = "openPositionsTable.owner", subject = "descriptionProvider")
    private String openPositionsTableOwnerDescriptionProvider(OpenPosition openPosition) {
        if (openPosition.getOwner() != null) {
            return (openPosition.getOwner().getName() != null ?
                    openPosition.getOwner().getName() : openPosition.getCreatedBy());
        } else {
            return null;
        }
    }

    @Install(to = "openPositionsTable.owner", subject = "columnGenerator")
    private Object openPositionsTableOwnerColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout retObject = setComponentsToOpenPositionsTable(event, whoOwner(event));

        return retObject;
    }

    private String whoOwner(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String userName = null;
        String a, b, c, e;

        if (event.getItem().getOwner() == null) {
            for (User user : users) {
                a = user.getLogin();
                b = user.getName();
                c = event.getItem().getCreatedBy();
                userName = c.equals(a) ? b : null;

                if (userName != null)
                    break;

//                userName = user.getLogin().equals(event.getItem().getCreatedBy()) ? user.getName() : null;
            }
        } else {
            e = event.getItem().getOwner().getName();
            userName = event.getItem().getOwner().getName();
        }

        return userName;
    }

    @Subscribe("signDraftCheckBox")
    public void onSignDraftCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        openPositionsDl.setParameter("signDraft", (event.getValue() != null ? event.getValue() : false));
    }

    @Install(to = "openPositionsTable.workExperience", subject = "columnGenerator")
    private Object openPositionsTableWorkExperienceColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout retObject = setComponentsToOpenPositionsTable(event, getWorkExperience(event));

        return retObject;
    }

    private String getWorkExperience(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        Set<Map.Entry<String, Integer>> entrySet = mapWorkExperience.entrySet();
        Integer desiredObject = event.getItem().getWorkExperience();

        for (Map.Entry<String, Integer> pair : entrySet) {
            if (desiredObject.equals(pair.getValue())) {
                return pair.getKey();// нашли наше значение и возвращаем  ключ
            }
        }

        return null;
    }

    private void setWorkExperienceMap() {
        mapWorkExperience.put("Нет требований", 0);
        mapWorkExperience.put("Без опыта", 1);
        mapWorkExperience.put("1 год", 2);
        mapWorkExperience.put("2 года", 3);
        mapWorkExperience.put("3 года", 4);
        mapWorkExperience.put("4 года", 6);
        mapWorkExperience.put("5 лет и более", 5);
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        checkBoxOnlyMySubscribe.setValue(true);
    }

    public void getMemoForCandidate() {
        Map<String, Object> reportParams = new HashMap<>();
        reportParams.put("openPosition", openPositionsTable.getSingleSelected());

        LoadContext<Report> loadContext = LoadContext.create(Report.class)
                .setQuery(LoadContext
                        .createQuery("select p from report$Report p where p.code = 'memoForCandidates'"))
                .setView("report.edit");
        Report report = dataManager.load(loadContext);
        reportGuiManager.printReport(report, reportParams);
    }

    @Install(to = "openPositionsTable.folder", subject = "columnGenerator")
    private Object openPositionsTableFolderColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> columnGeneratorEvent) {
        String retStr = "QUESTION_CIRCLE";
        String styleRetLabel = "open-position-pic-center-x-large-gray";
        String descriptionRetLabel = "";

        VBoxLayout retHbox = uiComponents.create(VBoxLayout.NAME);
        Label retLabel = uiComponents.create(Label.NAME);


        if (dataManager.load(OpenPosition.class)
                .query(QUERY_PARENT_OPENPOSITION)
                .parameter("parentOpenPosition", columnGeneratorEvent.getItem())
                .view("openPosition-view")
                .list().size() > 0) {
            retStr = "FOLDER";
            styleRetLabel = "open-position-pic-center-x-large-gray";

        } else {
            Boolean positionIsClosed = columnGeneratorEvent.getItem().getOpenClose() != null
                    ? columnGeneratorEvent.getItem().getOpenClose() : false;


            if (columnGeneratorEvent.getItem() != null) {
                if (!positionIsClosed) {
                    if (columnGeneratorEvent.getItem().getPriority() != null) {
                        switch (columnGeneratorEvent.getItem().getPriority()) {
                            case PRIORITY_DRAFT:
                                retStr = "REFRESH_ACTION";
                                descriptionRetLabel = messageBundle.getMessage("msgDraftPriority");
                                styleRetLabel = "open-position-pic-center-x-large-gray";
                                break;
                            case PRIORITY_PAUSED:
                                retStr = "PAUSE_CIRCLE";
                                descriptionRetLabel = messageBundle.getMessage("msgPausePriority");
                                styleRetLabel = "open-position-pic-center-x-large-gray";
                                break;
                            case PRIORITY_LOW:
                                retStr = "ARROW_CIRCLE_DOWN";
                                descriptionRetLabel = messageBundle.getMessage("msgLowPriority");
                                styleRetLabel = "open-position-pic-center-x-large-blue";
                                break;
                            case PRIORITY_NORMAL:
                                retStr = "LOOKUP_OK";
                                descriptionRetLabel = messageBundle.getMessage("msgNormalPriority");
                                styleRetLabel = "open-position-pic-center-x-large-green";
                                break;
                            case PRIORITY_HIGH:
                                retStr = "ARROW_CIRCLE_UP";
                                descriptionRetLabel = messageBundle.getMessage("msgHighPriority");
                                styleRetLabel = "open-position-pic-center-x-large-orange";
                                break;
                            case PRIORITY_CRITICAL:
                                retStr = "EXCLAMATION_CIRCLE";
                                descriptionRetLabel = messageBundle.getMessage("msgCriticalPriority");
                                styleRetLabel = "open-position-pic-center-x-large-red";
                                break;
                            default:
                                retStr = "LOOKUP_OK";
                                descriptionRetLabel = messageBundle.getMessage("msgPausePriority");
                                styleRetLabel = "open-position-pic-center-x-large-gray";
                                break;
                        }
                    }
                } else {
                    retStr = "STOP_CIRCLE";
                }
            }
        }

        retLabel.setIconFromSet(CubaIcon.valueOf(retStr));
        retLabel.setStyleName(styleRetLabel);
        retLabel.setDescription(descriptionRetLabel);
        retLabel.setWidthAuto();
        retLabel.setHeightAuto();
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);

        retHbox.setWidth("100px");
        retHbox.setHeight("60px");
        retHbox.setAlignment(Component.Alignment.MIDDLE_LEFT);

        HBoxLayout signsHBox = uiComponents.create(HBoxLayout.class);
        signsHBox.setSpacing(false);
        signsHBox.setMargin(true);
        signsHBox.setWidthAuto();
        signsHBox.setHeightAuto();

        signsHBox.add(setSignClosingVecency(columnGeneratorEvent));
        signsHBox.add(setSignAttachments(columnGeneratorEvent));
        signsHBox.add(setSignTestCase(columnGeneratorEvent));
        signsHBox.add(setSignComment(columnGeneratorEvent));
        signsHBox.add(setSignNeetLetter(columnGeneratorEvent));
        signsHBox.add(setSignMemo(columnGeneratorEvent));
        signsHBox.add(setSignRecrutersComment(columnGeneratorEvent));

        retHbox.add(retLabel);
        retHbox.add(signsHBox);
        return retHbox;
    }

    private String getTimerClosingVacancyValue(Date closingDate) {
        StringBuilder sb = new StringBuilder();

        if (closingDate != null) {
            long diffDate = closingDate.getTime() - new Date().getTime();
            int days = (int) diffDate / (24 * 60 * 60 * 1000);
            int hours = (int) ((diffDate - days * (24 * 60 * 60 * 1000)) / (60 * 60 * 1000));
            int minutes = (int) ((diffDate - (days * (24 * 60 * 60 * 1000) + hours * (60 * 60 * 1000))) / (60 * 1000));
            int seconds = (int) ((diffDate - (days * (24 * 60 * 60 * 1000) + hours * (60 * 60 * 1000) + minutes * (60 * 1000))) / 1000);

            sb.append(days)
                    .append(" ")
                    .append(messageBundle.getMessage("msgDays"))
                    .append(" ")
                    .append(hours)
                    .append(" ")
                    .append(messageBundle.getMessage("msgHours"))
                    .append(" ")
                    .append(minutes)
                    .append(" ")
                    .append(messageBundle.getMessage("msgMinutes"))
                    .append(" ")
                    .append(seconds)
                    .append(" ")
                    .append(messageBundle.getMessage("msgSeconds"));

            return sb.toString();
        }

        return "";
    }

    private Component setSignClosingVecency(DataGrid.ColumnGeneratorEvent<OpenPosition> columnGeneratorEvent) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidth(width_20px);
        retLabel.setHeight(width_20px);
        retLabel.setStyleName("open_position_sign_label_black");

        if (columnGeneratorEvent.getItem().getClosingDate() != null) {
            if (columnGeneratorEvent.getItem().getClosingDate().after(new Date())) {
                retLabel.setIcon(CubaIcon.CLOCK_O.source());
                retLabel.setDescription(getTimerClosingVacancyValue(columnGeneratorEvent.getItem().getClosingDate()));
            } else {
                retLabel.setIcon(CubaIcon.STOP_CIRCLE.source());
                retLabel.setDescription(messageBundle.getMessage("msgVacancyExpired"));
            }

            retLabel.setVisible(true);
        } else {
            retLabel.setVisible(false);
        }

        return retLabel;
    }

    private Label setSignRecrutersComment(DataGrid.ColumnGeneratorEvent<OpenPosition> columnGeneratorEvent) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidth(width_20px);
        retLabel.setHeight(width_20px);
        retLabel.setStyleName("open_position_sign_label_black");
        retLabel.setDescription(messageBundle.getMessage("msgSignRecruitersComment"));

        retLabel.setIcon(CubaIcon.COMMENT.source());

        if (columnGeneratorEvent.getItem().getOpenPositionComments() != null) {
            if (columnGeneratorEvent.getItem().getOpenPositionComments().size() > 0) {
                retLabel.setVisible(true);
            } else {
                retLabel.setVisible(false);
            }
        } else {
            retLabel.setVisible(false);
        }

        return retLabel;
    }

    private Label setSignMemo(DataGrid.ColumnGeneratorEvent<OpenPosition> columnGeneratorEvent) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidth(width_20px);
        retLabel.setHeight(width_20px);
        retLabel.setStyleName("open_position_sign_label_black");
        retLabel.setDescription(messageBundle.getMessage("msgSignMemo"));

        retLabel.setIcon(CubaIcon.MEDKIT.source());

        if (columnGeneratorEvent.getItem().getNeedMemoForInterview() != null) {
            if (columnGeneratorEvent.getItem().getNeedMemoForInterview()) {
                retLabel.setVisible(true);
            } else {
                retLabel.setVisible(false);
            }
        } else {
            retLabel.setVisible(false);
        }

        return retLabel;
    }

    private Label setSignTestCase(DataGrid.ColumnGeneratorEvent<OpenPosition> columnGeneratorEvent) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidth(width_20px);
        retLabel.setHeight(width_20px);
        retLabel.setStyleName("open_position_sign_label_black");
        retLabel.setDescription(messageBundle.getMessage("msgSignTestCase"));

        retLabel.setIcon(CubaIcon.BRIEFCASE.source());

        if (columnGeneratorEvent.getItem().getNeedExercise() != null) {
            if (columnGeneratorEvent.getItem().getNeedExercise()) {
                retLabel.setVisible(true);
            } else {
                retLabel.setVisible(false);
            }
        } else {
            retLabel.setVisible(false);
        }

        return retLabel;
    }

    private Label setSignNeetLetter(DataGrid.ColumnGeneratorEvent<OpenPosition> columnGeneratorEvent) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidth(width_20px);
        retLabel.setHeight(width_20px);
        retLabel.setStyleName("open_position_sign_label_black");
        retLabel.setDescription(messageBundle.getMessage("msgSignNeedLetter"));

        retLabel.setIcon(CubaIcon.ENVELOPE_SQUARE.source());

        if (columnGeneratorEvent.getItem().getNeedLetter() != null) {
            if (columnGeneratorEvent.getItem().getNeedLetter()) {
                retLabel.setVisible(true);
            } else {
                retLabel.setVisible(false);
            }
        } else {
            retLabel.setVisible(false);
        }

        return retLabel;
    }

    private Label setSignComment(DataGrid.ColumnGeneratorEvent<OpenPosition> columnGeneratorEvent) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidth(width_20px);
        retLabel.setHeight(width_20px);
        retLabel.setStyleName("open_position_sign_label_black");
        retLabel.setDescription(messageBundle.getMessage("msgSignComment"));

        retLabel.setIcon(CubaIcon.FILE_WORD_O.source());

        if (columnGeneratorEvent.getItem() != null) {
            if (hasPositiveComment(columnGeneratorEvent.getItem())) {
                retLabel.setVisible(true);
            } else {
                retLabel.setVisible(false);
            }
        } else {
            retLabel.setVisible(false);
        }

        return retLabel;
    }

    private Label setSignAttachments(DataGrid.ColumnGeneratorEvent<OpenPosition> events) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidth(width_20px);
        retLabel.setHeight(width_20px);
        retLabel.setStyleName("open_position_sign_label_black");
        retLabel.setDescription(messageBundle.getMessage("msgSignAttachments"));

        retLabel.setIcon(CubaIcon.PAPERCLIP.source());

        if (events.getItem().getSomeFiles() != null) {
            if (events.getItem().getSomeFiles().size() > 0) {
                retLabel.setVisible(true);
            } else {
                retLabel.setVisible(false);
            }
        } else {
            retLabel.setVisible(false);
        }

        return retLabel;
    }

    @Install(to = "openPositionsTable.positionType", subject = "columnGenerator")
    private Object openPositionsTablePositionTypeColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        Position positionType = event.getItem().getPositionType();
        return setComponentsToOpenPositionsTable(event, formatPositionTypeColumnText(positionType));
    }

    @Install(to = "openPositionsTable.projectName", subject = "columnGenerator")
    private Object openPositionsTableProjectNameColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String projectTitle = event.getItem().getProjectName() != null
                ? event.getItem().getProjectName().getProjectName() : "";
        return setComponentsToOpenPositionsTable(event, projectTitle);
    }

    private HBoxLayout setComponentsToOpenPositionsTable(DataGrid.ColumnGeneratorEvent<OpenPosition> event,
                                                         String dataStr) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        Label label = uiComponents.create(Label.class);

        retHBox.setWidthFull();
        retHBox.setHeightFull();
        retHBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retHBox.setStyleName(style_table_wordwrap);

        label.setHeightAuto();
        label.setAlignment(Component.Alignment.MIDDLE_LEFT);
        label.setStyleName(style_table_wordwrap);

        label.setValue(dataStr);

        retHBox.add(label);

        return retHBox;
    }

    @Install(to = "openPositionsTable.vacansyName", subject = "columnGenerator")
    private Object openPositionsTableVacansyNameColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
        retHBox.setHeightFull();
        retHBox.setSpacing(true);

        Label newVacancyLabel = uiComponents.create(Label.class);
        if (event.getItem().getSignDraft() != null) {
            if (!event.getItem().getSignDraft()) {
                newVacancyLabel.setValue(messageBundle.getMessage("msgNewReserve"));
                newVacancyLabel.setStyleName("button_table_red");
            } else {
                newVacancyLabel.setValue(messageBundle.getMessage("msgDraft"));
                newVacancyLabel.setStyleName("button_table_gray");
            }
        }

        if (event.getItem().getOpenClose() != null) {
            if (event.getItem().getOpenClose()) {
                newVacancyLabel.setValue(messageBundle.getMessage("msgClose"));
                newVacancyLabel.setStyleName("button_table_gray");
            }
        }

        newVacancyLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        newVacancyLabel.setWidthAuto();
        newVacancyLabel.setHeightAuto();

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        gregorianCalendar.add(Calendar.DAY_OF_MONTH, -3);
        if (event.getItem().getLastOpenDate() != null) {
            if (event.getItem().getSignDraft() != null) {
                if (!event.getItem().getSignDraft()) {
                    if (event.getItem().getLastOpenDate().before(gregorianCalendar.getTime())) {
                        newVacancyLabel.setVisible(false);
                    } else {
                        newVacancyLabel.setVisible(true);
                    }
                } else {
                    newVacancyLabel.setVisible(true);
                }
            } else {
                if (event.getItem().getLastOpenDate().before(gregorianCalendar.getTime())) {
                    newVacancyLabel.setVisible(false);
                } else {
                    newVacancyLabel.setVisible(true);
                }
            }
        }

        HBoxLayout retObject = setComponentsToOpenPositionsTable(event, event.getItem().getVacansyName());
        retObject.setWidthFull();

        Person projectOwner = event.getItem().getProjectName() != null
                ? event.getItem().getProjectName().getProjectOwner() : null;
        Image image = setProjectOwnerImage(projectOwner);
        retHBox.add(newVacancyLabel);
        retHBox.add(retObject);
        retHBox.add(image);
        retHBox.expand(retObject);

        return retHBox;
    }

    private Image setProjectOwnerImage(Person projectOwner) {
        StringBuilder sb = new StringBuilder();

        Image retImage = uiComponents.create(Image.class);
        retImage.setWidth(width_30px);
        retImage.setHeight(width_30px);
        retImage.setStyleName(style_circle_30px);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setAlignment(Component.Alignment.MIDDLE_LEFT);
        retImage.setDescription(sb.toString());
        retImage.setVisible(false);

        if (projectOwner != null) {
            sb.append(projectOwner.getFirstName())
                    .append(" ")
                    .append(projectOwner.getSecondName());
            if (projectOwner.getPersonPosition() != null) {
                sb.append(" / ")
                        .append(projectOwner.getPersonPosition().getPositionRuName());
            }
            if (projectOwner.getCompanyDepartment() != null) {
                sb.append(" / ")
                        .append(projectOwner.getCompanyDepartment().getDepartamentRuName());
                if (projectOwner.getCompanyDepartment().getCompanyName() != null) {
                    sb.append(" / ")
                            .append(projectOwner.getCompanyDepartment().getCompanyName().getComanyName());
                }
            }

            if (projectOwner.getCityOfResidence() != null) {
                sb.append(" / ")
                        .append(projectOwner.getCityOfResidence().getCityRuName());
            }

            if (projectOwner.getFileImageFace() != null) {
                retImage
                        .setSource(FileDescriptorResource.class)
                        .setFileDescriptor(projectOwner.getFileImageFace());
                retImage.setVisible(true);
            }
        }

        return retImage;
    }

    public void openCloseButtonInvoke(Action.ActionPerformedEvent e) {
        if (openPositionsTable.getSingleSelected() != null) {
            if (((PopupButton) e.getComponent()).getAction("closeOpenPositionAction").getCaption()
                    .equals(messageBundle.getMessage("msgClose"))) {

                removeCandidatesWithConsideration();
            }

            OpenPosition openPosition = openPositionsTable.getSingleSelected();
            openCloseButtonClickListener(openPosition, openCloseButton);
            openPositionsDl.load();
            openPositionsTable.repaint();
            openCloseButton.setEnabled(false);
            buttonSubscribe.setEnabled(false);

            if (openPosition.getOpenClose() != null) {
                if (!openPosition.getOpenClose()) {
                    openPositionsTable.setSelected(openPosition); // ERROR: IllegalStateException: Datasource doesn't contain items
                    openPositionsTable.scrollTo(openPosition);
                    openCloseButton.setEnabled(true);
                    buttonSubscribe.setEnabled(true);
                }
            } else {
                openPositionsTable.setSelected(openPosition); // ERROR: IllegalStateException: Datasource doesn't contain items
                openPositionsTable.scrollTo(openPosition);
                openCloseButton.setEnabled(true);
                buttonSubscribe.setEnabled(true);
            }
        }
    }

    private void removeCandidatesWithConsideration() {
        OpenPosition closeVacancy = openPositionsTable.getSingleSelected();

        if (!closeVacancy.getOpenClose()) {

            List<JobCandidate> jobCandidates = dataManager.load(JobCandidate.class)
                    .query(QUERY_CANDIDATES_FROM_CONSIDERATION)
                    .parameter("vacancy", closeVacancy)
                    .view("jobCandidate-view")
                    .list();

            List<JobCandidate> jobCandidatesNotEnded = new ArrayList<>();

            for (JobCandidate jc : jobCandidates) {
                Boolean sendCV = false;
                Boolean endCase = false;

                for (IteractionList il : jc.getIteractionList()) {
                    if (il.getVacancy() != null) {
                        if (il.getVacancy().equals(closeVacancy)) {
                            if (!sendCV) {
                                if (il.getIteractionType() != null) {
                                    if (il.getIteractionType().getSignSendToClient() != null) {
                                        if (il.getIteractionType().getSignSendToClient()) {
                                            sendCV = true;
                                        }
                                    }
                                }
                            } else {
                                if (!endCase) {
                                    if (il.getIteractionType().getSignEndCase() != null) {
                                        if (il.getIteractionType().getSignEndCase()) {
                                            endCase = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (sendCV && !endCase) {
                    jobCandidatesNotEnded.add(jc);
                }
            }

            StringBuffer dialogMessage = new StringBuffer(
                    messageBundle.getMessage("msgDialogMessageCandidateConsideration"));
            dialogMessage.append(" ");

            if (jobCandidatesNotEnded.size() > 0) {
                for (JobCandidate jc : jobCandidatesNotEnded) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(messageBundle.getMessage("msgInConsideration"))
                            .append(" ")
                            .append(closeVacancy.getVacansyName());
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withType(Notifications.NotificationType.TRAY)
                            .withPosition(Notifications.Position.BOTTOM_RIGHT)
                            .withCaption(jc.getFullName())
                            .withHideDelayMs(15000)
                            .withDescription(sb.toString())
                            .show();
                    dialogMessage.append(jc.getFullName());
                    dialogMessage.append(", ");
                }

                dialogMessage.delete(dialogMessage.length() - 2, dialogMessage.length());
                dialogMessage.append(". ");
                dialogMessage.append(messageBundle.getMessage("msgDeleteFromConsideration"));

                dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                        .withMessage(dialogMessage.toString())
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withType(Dialogs.MessageType.CONFIRMATION)
                        .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                                        .withHandler(e -> {
                                            Iteraction iteractionSignEndProcessVacancyClosed = null;

                                            BigDecimal iteractionMaxNumber = dataManager
                                                    .loadValue("select max(e.numberIteraction) from itpearls_IteractionList e", BigDecimal.class)
                                                    .one();

                                            try {
                                                iteractionSignEndProcessVacancyClosed = dataManager.load(Iteraction.class)
                                                        .query("select e from itpearls_Iteraction e where e.signEndProcessVacancyClosed = true")
                                                        .one();
                                            } catch (IllegalStateException exception) {
                                                notifications.create(Notifications.NotificationType.ERROR)
                                                        .withCaption(messageBundle.getMessage("msgError"))
                                                        .withDescription(messageBundle.getMessage("msgDoNotSignEndProcessVacancyClosed"))
                                                        .withType(Notifications.NotificationType.ERROR)
                                                        .show();

                                                exception.printStackTrace();
                                            }

                                            if (iteractionSignEndProcessVacancyClosed != null) {
                                                CommitContext commitContext = new CommitContext();

                                                for (JobCandidate jc : jobCandidatesNotEnded) {
                                                    IteractionList iteractionList = metadata.create(IteractionList.class);

                                                    iteractionList.setVacancy(closeVacancy);
                                                    iteractionList.setCandidate(jc);
                                                    iteractionList.setDateIteraction(new Date());
                                                    iteractionList.setRating(4);
                                                    iteractionList.setComment(messageBundle.getMessage("msgVacancyCloded"));
                                                    iteractionList.setRecrutierName(userSession.getUser().getName());
                                                    iteractionList.setRecrutier((ExtUser) userSession.getUser());
                                                    iteractionMaxNumber.add(BigDecimal.ONE);
                                                    iteractionList.setNumberIteraction(iteractionMaxNumber);
                                                    iteractionList.setIteractionType(iteractionSignEndProcessVacancyClosed);

                                                    commitContext.addInstanceToCommit(iteractionList);
                                                }

                                                dataManager.commit(commitContext);

                                                sendNotificationsService.SendEmail(
                                                        messageBundle.getMessage("msgEmailSubjCloseOfVacancy"),
                                                        dialogMessage.toString());
                                            }
                                        }),
                                new DialogAction(DialogAction.Type.NO))
                        .show();
            }
        }
    }

    @Install(to = "openPositionsTable.lastCVSend", subject = "columnGenerator")
    private Object openPositionsTableLastCVSendColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> columnGeneratorEvent) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);

        retHBox.setWidthFull();
        retHBox.setHeightFull();
        retHBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retHBox.setStyleName(style_table_wordwrap);

        Label labelRet = uiComponents.create(Label.NAME);
        OpenPosition position = columnGeneratorEvent.getItem();
        int countCVsend = position.getId() != null
                ? sentCvCountByPosition.getOrDefault(position.getId(), 0)
                : 0;

        labelRet.setWidthAuto();
        labelRet.setHeightAuto();
        labelRet.setAlignment(Component.Alignment.MIDDLE_CENTER);
        labelRet.setStyleName(style_table_wordwrap);

        labelRet.setValue(countCVsend);

        switch (countCVsend) {
            case 0:
            case 1:
            case 2:
                labelRet.setStyleName("label_table_blue");
                break;
            case 3:
            case 4:
                labelRet.setStyleName("label_table_green");
                break;
            case 5:
            case 6:
                labelRet.setStyleName("label_table_orange");
                break;
            case 7:
            case 8:
            case 9:
                labelRet.setStyleName("label_table_red");
                break;
            case 10:
            default:
                labelRet.setStyleName("label_table_gray");
                break;
        }

        retHBox.add(labelRet);

        return retHBox;
    }

    @Install(to = "openPositionsTable.projectName", subject = "styleProvider")
    private String openPositionsTableProjectNameStyleProvider(OpenPosition openPosition) {
        return style_table_wordwrap;
    }

    @Install(to = "openPositionsTable.vacansyName", subject = "styleProvider")
    private String openPositionsTableVacansyNameStyleProvider(OpenPosition openPosition) {
        return style_table_wordwrap;
    }

    @Install(to = "openPositionsTable.cityPositionList", subject = "styleProvider")
    private String openPositionsTableCityPositionListStyleProvider(OpenPosition openPosition) {
        return style_table_wordwrap;
    }

    @Install(to = "openPositionsTable.workExperience", subject = "styleProvider")
    private String openPositionsTableWorkExperienceStyleProvider(OpenPosition openPosition) {
        return style_table_wordwrap;
    }

    @Install(to = "openPositionsTable.owner", subject = "styleProvider")
    private String openPositionsTableOwnerStyleProvider(OpenPosition openPosition) {
        return style_table_wordwrap;
    }

    @Install(to = "openPositionsTable.positionType", subject = "styleProvider")
    private String openPositionsTablePositionTypeStyleProvider(OpenPosition openPosition) {
        return style_table_wordwrap;
    }


    private HBoxLayout setSubscribersRecruters(OpenPosition openPosition) {
        final String QUERY_SUBSCRIBERS = "select e from itpearls_RecrutiesTasks e where e.endDate >= :currentDate and e.openPosition = :openPosition";

        HBoxLayout recrutersHBox = uiComponents.create(HBoxLayout.class);

        List<RecrutiesTasks> tasks = dataManager.load(RecrutiesTasks.class)
                .query(QUERY_SUBSCRIBERS)
                .parameter("openPosition", openPosition)
                .parameter("currentDate", new Date())
                .view("recrutiesTasks-view")
                .list();

        for (RecrutiesTasks user : tasks) {
            Image image = uiComponents.create(Image.class);

            image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            image.setWidth("30px");
            image.setStyleName(style_circle_30px);
            image.setDescription(user.getReacrutier().getName());

            try {
                ExtUser extUser = (ExtUser) user.getReacrutier();
                image.setSource(FileDescriptorResource.class)
                        .setFileDescriptor(extUser.getFileImageFace());
            } catch (Exception e) {
                e.printStackTrace();
            }

            recrutersHBox.add(image);
        }

        return recrutersHBox;
    }

    @Install(to = "openPositionsTable.candidateSendedColumn", subject = "columnGenerator")
    private Object openPositionsTableCandidateSendedColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout hBoxLayout = uiComponents.create(HBoxLayout.class);

        hBoxLayout.setWidthFull();
        hBoxLayout.setHeightFull();

        Button retButton = uiComponents.create(Button.class);
        retButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retButton.setIconFromSet(CubaIcon.USER_CIRCLE);
        retButton.addClickListener(event1 -> {
            JobCandidateSimpleBrowse jobCandidateSimpleBrowse =
                    screens.create(JobCandidateSimpleBrowse.class);
            jobCandidateSimpleBrowse.setOpenPosition(event.getItem());
            jobCandidateSimpleBrowse.show();
        });

        hBoxLayout.add(retButton);
        return hBoxLayout;
    }

    private HBoxLayout setCenteredCell(String value, String style) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);

        retHBox.setWidthFull();
        retHBox.setHeightFull();
        retHBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retHBox.setStyleName(style_table_wordwrap);

        Label labelRet = uiComponents.create(Label.NAME);

        labelRet.setWidthAuto();
        labelRet.setHeightAuto();
        labelRet.setAlignment(Component.Alignment.MIDDLE_CENTER);
        labelRet.setStyleName(style_table_wordwrap);

        labelRet.setValue(value);
        labelRet.setStyleName(style);

        retHBox.add(labelRet);

        return retHBox;
    }

    @Install(to = "openPositionsTable.vacansyID", subject = "columnGenerator")
    private Object openPositionsTableVacansyIDColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        return setCenteredCell(event.getItem().getVacansyID(), "label_table_black");
    }

    public void setRatingComment() {
        screenBuilders.editor(OpenPositionComment.class, this)
                .withScreenClass(OpenPositionCommentEdit.class)
                .withInitializer(e -> {
                    e.setOpenPosition(openPositionsTable.getSingleSelected() != null
                            ? openPositionsTable.getSingleSelected() : null);
                    e.setUser((ExtUser) userSession.getUser());
                })
                .withAfterCloseListener(e1 -> {
                    OpenPosition selected = openPositionsTable.getSingleSelected();
                    openPositionsDl.load();
                    openPositionsTable.repaint();

                    if (selected != null)
                        openPositionsTable.setSelected(selected);

                    try {
                        if (selected != null)
                            openPositionsTable.scrollTo(selected);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                })
                .withOpenMode(OpenMode.DIALOG)
                .newEntity()
                .build()
                .show();
    }

    @Install(to = "openPositionsTable.rating", subject = "columnGenerator")
    private Object openPositionsTableRatingColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        return avgRating(event.getItem());
    }

    private Object avgRating(OpenPosition openPosition) {
        BigDecimal avgRating = openPosition.getId() != null
                ? avgRatingByPosition.get(openPosition.getId())
                : null;

        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        Label starLabel = uiComponents.create(Label.class);

        if (avgRating != null) {
            int avgRatingInt = Integer.valueOf(avgRating.intValue()) + 1;

            starLabel.setValue(starsAndOtherService.setStars(avgRatingInt));
            starLabel.setDescription(messageBundle.getMessage("msgAvgRating") + ":" + avgRatingInt);

        } else {
            starLabel.setDescription(messageBundle.getMessage("msgNotAvgRating"));
        }

        starLabel.setWidthAuto();
        starLabel.setHeightAuto();
        starLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        retBox.add(starLabel);

        return retBox;
    }

    public void openCloseButtonWithCommentInvoke(Action.ActionPerformedEvent e) {
        setRatingComment();
        openCloseButtonInvoke(e);
    }

    @Install(to = "openPositionsTable.openPositionActionButtonColumn", subject = "columnGenerator")
    private Object openPositionsTableOpenPositionActionButtonColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        PopupButton actionPopupButton = uiComponents.create(PopupButton.class);
        actionPopupButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        actionPopupButton.setWidthAuto();
        actionPopupButton.setHeightAuto();
        actionPopupButton.setIconFromSet(CubaIcon.BARS);
        actionPopupButton.setShowActionIcons(true);
        actionPopupButton.addPopupVisibilityListener(e -> {
            openPositionsTable.setSelected(event.getItem());

            try {
                openPositionsTable.scrollTo(event.getItem());
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
        });

        initActionButton(actionPopupButton, event);
        retHBox.add(actionPopupButton);

        return retHBox;
    }

    private void initActionButton(PopupButton actionPopupButton, DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        initActionButtonJobCandidateSimpleBrowse(actionPopupButton, event);

        initActionButtonSeparator(actionPopupButton, event);

        initActionButtonSubscribe(actionPopupButton, event);

        initActionButtonSeparator(actionPopupButton, "separator2Action");

        initActionButtonReports(actionPopupButton, event);

        initActionButtonSeparator(actionPopupButton, "separator3Action");

        initActionButtonComments(actionPopupButton, event);
    }

    private void initActionButtonComments(PopupButton actionPopupButton, DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        actionPopupButton.addAction(new BaseAction("openPositionCommentAction")
                .withIcon(CubaIcon.STAR_O.source())
                .withCaption(messageBundle.getMessage("msgOpenPositionComment"))
                .withHandler(event1 -> {
                    setRatingComment();
                }));

        actionPopupButton.addAction(new BaseAction("viewRatingCommentAction")
                .withIcon(CubaIcon.VIEW_ACTION.source())
                .withCaption(messageBundle.getMessage("msgViewOpenPositionComment"))
                .withHandler(event1 -> {
                    openPositionCommentViewInvoke();
                }));
    }

    private void initActionButtonReports(PopupButton actionPopupButton, DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        actionPopupButton.addAction(new BaseAction("msgReport")
                .withIcon(CubaIcon.FILE_TEXT.source())
                .withCaption(messageBundle.getMessage("msgReport"))
                .withHandler(event1 -> {
                    getMemoForCandidate();
                }));
    }

    private void initActionButtonSubscribe(PopupButton actionPopupButton, DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        actionPopupButton.addAction(new BaseAction("subscribePositionAction")
                .withIcon(CubaIcon.BOLT.source())
                .withCaption(messageBundle.getMessage("msgSubscribeOpenPosition"))
                .withHandler(event1 -> {
                    subscribePosition();
                }));

        actionPopupButton.addAction(new BaseAction("subscribeListAction")
                .withIcon(CubaIcon.CLONE.source())
                .withCaption(messageBundle.getMessage("msgRecrutersTasks"))
                .withHandler(event1 -> {
                    screenBuilders.lookup(RecrutiesTasks.class, this)
                            .withOpenMode(OpenMode.DIALOG)
                            .build()
                            .show();
                }));
    }

    private void initActionButtonSeparator(PopupButton actionPopupButton, DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        actionPopupButton.addAction(new BaseAction("separator1Action")
                .withCaption(separator));
    }


    private void initActionButtonSeparator(PopupButton actionPopupButton, String baseActionID) {
        actionPopupButton.addAction(new BaseAction(baseActionID)
                .withCaption(separator));
    }

    private void initActionButtonJobCandidateSimpleBrowse(PopupButton actionPopupButton, DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        actionPopupButton.addAction(new BaseAction("jobCandidateSimpleAction")
                .withCaption(messageBundle.getMessage("msgJobCandidateSimpleBrowse"))
                .withIcon(CubaIcon.USER_CIRCLE.source())
                .withHandler(e -> {
                    JobCandidateSimpleMailBrowse jobCandidateSimpleBrowse
                            = screens.create(JobCandidateSimpleMailBrowse.class);
                    jobCandidateSimpleBrowse.setOpenPosition(event.getItem());
                    jobCandidateSimpleBrowse.setSignSendToClent(true);
                    jobCandidateSimpleBrowse.setHeader(event.getItem());

                    screens.show(jobCandidateSimpleBrowse);
                }));
    }
}


