package com.company.hunttech.web.screens.mainscreen;

import com.company.hunttech.entity.UserSettings;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Выбирает фоновое изображение главного экрана для текущего пользователя.
 * Пользовательский файл имеет абсолютный приоритет; при его отсутствии сервис
 * создаёт одну из десяти нейтральных SVG-композиций для активной темы.
 */
@Component(MainScreenBackgroundService.NAME)
public class MainScreenBackgroundService {

    public static final String NAME = "hunttech_MainScreenBackgroundService";
    public static final String CUSTOM_BACKGROUND_PREFIX = "hrm-main-background-";
    public static final int VARIANT_COUNT = 10;

    private static final Logger log = LoggerFactory.getLogger(MainScreenBackgroundService.class);
    private static final String QUERY_USER_SETTINGS =
            "select e from hunttech_UserSettings e where e.user = :currentUser";
    private static final String DEFAULT_THEME = "hover";
    private static final String LAST_VARIANT_ATTRIBUTE = "hrm.main.background.lastVariant.";
    private static final Map<String, Palette> PALETTES = createPalettes();

    @Inject
    private DataManager dataManager;
    @Inject
    private FileLoader fileLoader;

    /**
     * Разрешает фон при каждом создании или обновлении главного экрана. Для системного
     * каталога сессия исключает немедленное повторение предыдущего варианта темы.
     */
    public Resource resolveForUser(User currentUser, String themeName, UserSession session) {
        FileDescriptor customBackground = loadUserBackground(currentUser);
        Optional<Resource> customResource = createCustomResource(customBackground);
        if (customResource.isPresent()) {
            return customResource.get();
        }

        String normalizedTheme = normalizeTheme(themeName);
        int variant = nextVariant(session, normalizedTheme);
        return createGeneratedResource(normalizedTheme, variant);
    }

    /**
     * Маркер имени отделяет пользовательский фон от legacy-фотографии,
     * которая исторически могла находиться в UserSettings.fileImageFace.
     */
    public boolean isCustomBackground(FileDescriptor descriptor) {
        return descriptor != null
                && descriptor.getName() != null
                && descriptor.getName().startsWith(CUSTOM_BACKGROUND_PREFIX);
    }

    private int nextVariant(UserSession session, String normalizedTheme) {
        String attributeName = LAST_VARIANT_ATTRIBUTE + normalizedTheme;
        Integer previous = session.getAttribute(attributeName);
        int variant;

        if (previous == null || previous < 0 || previous >= VARIANT_COUNT) {
            variant = ThreadLocalRandom.current().nextInt(VARIANT_COUNT);
        } else {
            int candidate = ThreadLocalRandom.current().nextInt(VARIANT_COUNT - 1);
            variant = candidate >= previous ? candidate + 1 : candidate;
        }

        session.setAttribute(attributeName, variant);
        return variant;
    }

    private FileDescriptor loadUserBackground(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        return dataManager.load(UserSettings.class)
                .query(QUERY_USER_SETTINGS)
                .parameter("currentUser", currentUser)
                .view("userSettings-view")
                .optional()
                .map(UserSettings::getFileImageFace)
                .orElse(null);
    }

    private Optional<Resource> createCustomResource(FileDescriptor descriptor) {
        if (!isCustomBackground(descriptor)
                || !FileDescriptorImageHelper.fileExists(fileLoader, descriptor)) {
            return Optional.empty();
        }

        try (InputStream inputStream = fileLoader.openStream(descriptor)) {
            byte[] bytes = inputStream.readAllBytes();
            StreamResource resource = byteResource(bytes, descriptor.getName(), mimeType(descriptor));
            resource.setCacheTime(0);
            return Optional.of(resource);
        } catch (Exception e) {
            log.warn("Cannot load custom main screen background id={}: {}",
                    descriptor.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    private Resource createGeneratedResource(String themeName, int variant) {
        Palette palette = PALETTES.getOrDefault(themeName, PALETTES.get(DEFAULT_THEME));
        String svg = createSvg(palette, variant);
        StreamResource resource = byteResource(
                svg.getBytes(StandardCharsets.UTF_8),
                "hrm-main-" + themeName + "-" + variant + ".svg",
                "image/svg+xml");
        resource.setCacheTime(0);
        return resource;
    }

    private StreamResource byteResource(byte[] bytes, String fileName, String mimeType) {
        StreamResource resource = new StreamResource(
                (StreamResource.StreamSource) () -> new ByteArrayInputStream(bytes), fileName);
        resource.setMIMEType(mimeType);
        return resource;
    }

    private String normalizeTheme(String themeName) {
        if (themeName == null) {
            return DEFAULT_THEME;
        }
        String normalized = themeName.toLowerCase(Locale.ROOT);
        return PALETTES.containsKey(normalized) ? normalized : DEFAULT_THEME;
    }

    private String mimeType(FileDescriptor descriptor) {
        String extension = descriptor.getExtension();
        if (extension == null) {
            return "application/octet-stream";
        }
        switch (extension.toLowerCase(Locale.ROOT)) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }

    private String createSvg(Palette palette, int variant) {
        String shapes;
        switch (variant) {
            case 0:
                shapes = circles(palette);
                break;
            case 1:
                shapes = diagonalBands(palette);
                break;
            case 2:
                shapes = waves(palette);
                break;
            case 3:
                shapes = cornerArcs(palette);
                break;
            case 4:
                shapes = softGrid(palette);
                break;
            case 5:
                shapes = floatingCards(palette);
                break;
            case 6:
                shapes = dottedFlow(palette);
                break;
            case 7:
                shapes = layeredHills(palette);
                break;
            case 8:
                shapes = crossingLines(palette);
                break;
            case 9:
            default:
                shapes = balancedBlobs(palette);
                break;
        }

        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1920\" height=\"1080\" "
                + "viewBox=\"0 0 1920 1080\" preserveAspectRatio=\"xMidYMid slice\">"
                + "<defs><linearGradient id=\"bg\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
                + "<stop offset=\"0\" stop-color=\"" + palette.base + "\"/>"
                + "<stop offset=\"1\" stop-color=\"" + palette.surface + "\"/>"
                + "</linearGradient></defs>"
                + "<rect width=\"1920\" height=\"1080\" fill=\"url(#bg)\"/>"
                + shapes
                + "</svg>";
    }

    private String circles(Palette p) {
        return "<circle cx=\"260\" cy=\"210\" r=\"240\" fill=\"" + p.accent + "\" opacity=\".16\"/>"
                + "<circle cx=\"1580\" cy=\"860\" r=\"330\" fill=\"" + p.secondary + "\" opacity=\".18\"/>"
                + "<circle cx=\"1160\" cy=\"260\" r=\"115\" fill=\"" + p.line + "\" opacity=\".14\"/>";
    }

    private String diagonalBands(Palette p) {
        return "<path d=\"M-180 960 L460 1080 L1240 0 L600 0 Z\" fill=\"" + p.accent + "\" opacity=\".10\"/>"
                + "<path d=\"M620 1080 L1110 1080 L1840 0 L1350 0 Z\" fill=\"" + p.secondary + "\" opacity=\".12\"/>"
                + "<path d=\"M1500 1080 L1790 1080 L2100 520 L1810 520 Z\" fill=\"" + p.line + "\" opacity=\".10\"/>";
    }

    private String waves(Palette p) {
        return "<path d=\"M0 760 C330 630 610 900 980 755 C1320 625 1550 730 1920 610 L1920 1080 L0 1080 Z\" fill=\"" + p.accent + "\" opacity=\".12\"/>"
                + "<path d=\"M0 875 C380 720 680 990 1110 825 C1450 700 1690 770 1920 710 L1920 1080 L0 1080 Z\" fill=\"" + p.secondary + "\" opacity=\".13\"/>";
    }

    private String cornerArcs(Palette p) {
        return "<path d=\"M0 340 A340 340 0 0 1 340 0\" fill=\"none\" stroke=\"" + p.accent + "\" stroke-width=\"90\" opacity=\".12\"/>"
                + "<path d=\"M1920 720 A360 360 0 0 0 1560 1080\" fill=\"none\" stroke=\"" + p.secondary + "\" stroke-width=\"120\" opacity=\".13\"/>"
                + "<path d=\"M1920 210 A210 210 0 0 0 1710 0\" fill=\"none\" stroke=\"" + p.line + "\" stroke-width=\"46\" opacity=\".10\"/>";
    }

    private String softGrid(Palette p) {
        StringBuilder grid = new StringBuilder();
        for (int x = 120; x < 1920; x += 240) {
            grid.append("<line x1=\"").append(x).append("\" y1=\"0\" x2=\"")
                    .append(x).append("\" y2=\"1080\" stroke=\"").append(p.line)
                    .append("\" stroke-width=\"1\" opacity=\".12\"/>");
        }
        for (int y = 120; y < 1080; y += 180) {
            grid.append("<line x1=\"0\" y1=\"").append(y).append("\" x2=\"1920\" y2=\"")
                    .append(y).append("\" stroke=\"").append(p.line)
                    .append("\" stroke-width=\"1\" opacity=\".10\"/>");
        }
        return grid + "<rect x=\"1240\" y=\"150\" width=\"420\" height=\"260\" rx=\"48\" fill=\"" + p.accent + "\" opacity=\".08\"/>";
    }

    private String floatingCards(Palette p) {
        return "<rect x=\"180\" y=\"160\" width=\"420\" height=\"250\" rx=\"54\" fill=\"" + p.accent + "\" opacity=\".10\" transform=\"rotate(-6 390 285)\"/>"
                + "<rect x=\"1190\" y=\"580\" width=\"500\" height=\"300\" rx=\"62\" fill=\"" + p.secondary + "\" opacity=\".12\" transform=\"rotate(5 1440 730)\"/>"
                + "<rect x=\"770\" y=\"210\" width=\"250\" height=\"150\" rx=\"38\" fill=\"" + p.line + "\" opacity=\".08\"/>";
    }

    private String dottedFlow(Palette p) {
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < 18; i++) {
            int cx = 120 + i * 105;
            int cy = 260 + (int) (120 * Math.sin(i * 0.65));
            int radius = 10 + i % 4 * 4;
            dots.append("<circle cx=\"").append(cx).append("\" cy=\"").append(cy)
                    .append("\" r=\"").append(radius).append("\" fill=\"")
                    .append(i % 2 == 0 ? p.accent : p.secondary)
                    .append("\" opacity=\".14\"/>");
        }
        return dots + "<path d=\"M80 780 C560 570 1010 930 1840 670\" fill=\"none\" stroke=\"" + p.line + "\" stroke-width=\"3\" opacity=\".12\"/>";
    }

    private String layeredHills(Palette p) {
        return "<path d=\"M0 850 C350 600 650 760 980 660 C1300 560 1580 640 1920 460 L1920 1080 L0 1080 Z\" fill=\"" + p.line + "\" opacity=\".08\"/>"
                + "<path d=\"M0 920 C400 710 740 900 1110 770 C1430 660 1660 760 1920 690 L1920 1080 L0 1080 Z\" fill=\"" + p.accent + "\" opacity=\".11\"/>"
                + "<path d=\"M0 1000 C420 850 810 1030 1240 900 C1510 820 1730 860 1920 820 L1920 1080 L0 1080 Z\" fill=\"" + p.secondary + "\" opacity=\".12\"/>";
    }

    private String crossingLines(Palette p) {
        return "<path d=\"M-120 160 L1980 910\" stroke=\"" + p.accent + "\" stroke-width=\"54\" opacity=\".08\"/>"
                + "<path d=\"M260 -100 L1690 1180\" stroke=\"" + p.secondary + "\" stroke-width=\"28\" opacity=\".10\"/>"
                + "<path d=\"M1480 -80 L620 1160\" stroke=\"" + p.line + "\" stroke-width=\"12\" opacity=\".10\"/>";
    }

    private String balancedBlobs(Palette p) {
        return "<path d=\"M80 210 C210 40 510 70 620 250 C710 410 570 560 350 540 C140 520 -40 380 80 210 Z\" fill=\"" + p.accent + "\" opacity=\".11\"/>"
                + "<path d=\"M1320 520 C1490 390 1770 470 1870 650 C1970 830 1780 1030 1540 990 C1320 950 1140 690 1320 520 Z\" fill=\"" + p.secondary + "\" opacity=\".13\"/>"
                + "<path d=\"M820 140 C930 65 1100 100 1150 210 C1200 320 1080 410 950 380 C820 350 710 215 820 140 Z\" fill=\"" + p.line + "\" opacity=\".08\"/>";
    }

    private static Map<String, Palette> createPalettes() {
        Map<String, Palette> palettes = new LinkedHashMap<>();
        palettes.put("halo", new Palette("#F8FAFD", "#EEF3F8", "#C9DDF1", "#DCE8F3", "#AFC8DE"));
        palettes.put("havana", new Palette("#FBFAF6", "#F2EEE4", "#DED2BA", "#E3E9E5", "#C6D3CD"));
        palettes.put("helium", new Palette("#F7FBFC", "#EAF3F5", "#C9E0E5", "#D9E9ED", "#ABCBD3"));
        palettes.put("hover", new Palette("#F8F9FB", "#EEF1F4", "#D4DEE8", "#E1E7ED", "#BCCAD6"));
        palettes.put("hunttech-modern", new Palette("#FAFAFB", "#F0F2F5", "#FFE3B0", "#DDE5EC", "#C5D1DC"));
        palettes.put("hunttech-modern-light", new Palette("#FCFDFE", "#F1F5F8", "#FFE8BD", "#E2EBF2", "#CBD9E4"));
        palettes.put("hunttech-modern-dark", new Palette("#F1F4F7", "#E4E9EE", "#D5C29D", "#D2DCE4", "#B7C6D2"));
        return palettes;
    }

    private static final class Palette {
        private final String base;
        private final String surface;
        private final String accent;
        private final String secondary;
        private final String line;

        private Palette(String base, String surface, String accent, String secondary, String line) {
            this.base = base;
            this.surface = surface;
            this.accent = accent;
            this.secondary = secondary;
            this.line = line;
        }
    }
}
