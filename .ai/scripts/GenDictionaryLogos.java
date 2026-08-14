import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Генератор тематических логотипов Dictionary Edit-форм HRM HuntTech.
 *
 * Рисует 200x200 PNG в палитре HuntTech (фон #172638, символ #f8fafc,
 * акцент #e74c3c) примитивами Java2D и раскладывает во все 7 тем:
 * modules/web/themes/<theme>/icons/dictionaries/<name>.png
 *
 * Запуск (Java 11):
 *   javac GenDictionaryLogos.java && java GenDictionaryLogos
 */
public class GenDictionaryLogos {

    static final int SIZE = 200;
    static final int C = SIZE / 2;

    static final Color BG = new Color(23, 38, 56, 255);
    static final Color FG = new Color(248, 250, 252, 255);
    static final Color ACC = new Color(231, 76, 60, 255);
    static final Color MUT = new Color(148, 163, 184, 255);

    static final String[] THEMES = {
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-dark", "hunttech-modern-light"
    };

    interface Painter {
        void paint(Graphics2D g);
    }

    static BufferedImage canvas() {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(BG);
        g.fillRect(0, 0, SIZE, SIZE);
        return img;
    }

    static void ring(Graphics2D g, double cx, double cy, double r, Color c, double w) {
        g.setColor(c);
        g.setStroke(new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
    }

    static void line(Graphics2D g, double x1, double y1, double x2, double y2, Color c, double w) {
        g.setColor(c);
        g.setStroke(new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    static void rect(Graphics2D g, double x, double y, double w, double h, Color c) {
        g.setColor(c);
        g.fill(new Rectangle2D.Double(x, y, w, h));
    }

    static void rounded(Graphics2D g, double x, double y, double w, double h, double r, Color c, boolean fill) {
        g.setColor(c);
        RoundRectangle2D rr = new RoundRectangle2D.Double(x, y, w, h, r, r);
        if (fill) {
            g.fill(rr);
        } else {
            g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(rr);
        }
    }

    static void ellipse(Graphics2D g, double x, double y, double w, double h, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Double(x, y, w, h));
    }

    static void triangle(Graphics2D g, double x1, double y1, double x2, double y2, double x3, double y3, Color c) {
        g.setColor(c);
        Path2D p = new Path2D.Double();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        p.closePath();
        g.fill(p);
    }

    // ---- iteraction: две круговые стрелки цикла ----
    static void iteraction(Graphics2D g) {
        ring(g, C, C, 62, FG, 10);
        for (double ang : new double[]{Math.toRadians(45), Math.toRadians(225)}) {
            double x1 = C + 62 * Math.cos(ang);
            double y1 = C + 62 * Math.sin(ang);
            double bx1 = C + 52 * Math.cos(ang + 0.18);
            double by1 = C + 52 * Math.sin(ang + 0.18);
            double bx2 = C + 52 * Math.cos(ang - 0.18);
            double by2 = C + 52 * Math.sin(ang - 0.18);
            triangle(g, x1, y1, bx1, by1, bx2, by2, FG);
        }
        ellipse(g, C - 16, C - 16, 32, 32, ACC);
    }

    // ---- specialisation: мишень ----
    static void specialisation(Graphics2D g) {
        ring(g, C, C, 70, FG, 8);
        ring(g, C, C, 46, FG, 8);
        ring(g, C, C, 22, FG, 8);
        ellipse(g, C - 8, C - 8, 16, 16, ACC);
    }

    // ---- country: глобус с меридианами и пин ----
    static void country(Graphics2D g) {
        ring(g, C, C, 62, FG, 8);
        line(g, C, C - 62, C, C + 62, FG, 6);
        line(g, C - 62, C, C + 62, C, FG, 6);
        g.setColor(MUT);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(C - 62, C - 34, 124, 68, 0, 180, Arc2D.OPEN));
        g.draw(new Arc2D.Double(C - 62, C - 34, 124, 68, 180, 180, Arc2D.OPEN));
        g.draw(new Arc2D.Double(C - 34, C - 62, 68, 124, 90, 180, Arc2D.OPEN));
        g.draw(new Arc2D.Double(C - 34, C - 62, 68, 124, -90, 180, Arc2D.OPEN));
        triangle(g, C - 14, C - 88, C + 14, C - 88, C, C - 58, ACC);
        ellipse(g, C - 9, C - 97, 18, 18, ACC);
    }

    // ---- city: силуэты зданий ----
    static void city(Graphics2D g) {
        rect(g, C - 84, C - 46, 54, 108, FG);
        rect(g, C - 74, C - 78, 34, 32, FG);
        for (int dx : new int[]{-68, -46}) {
            for (int dy : new int[]{-30, 0, 30}) {
                rect(g, dx, C + dy - 12, 10, 24, BG);
            }
        }
        rect(g, C - 24, C - 70, 50, 132, MUT);
        for (int dx : new int[]{-14, 6}) {
            for (int dy : new int[]{-52, -24, 4, 32}) {
                rect(g, dx, C + dy - 10, 8, 20, BG);
            }
        }
        rect(g, C + 34, C - 28, 52, 90, FG);
        for (int dx : new int[]{44, 66}) {
            for (int dy : new int[]{-14, 12}) {
                rect(g, dx, C + dy - 10, 10, 20, BG);
            }
        }
    }

    // ---- region: карта с дорогами и пин ----
    static void region(Graphics2D g) {
        g.setColor(FG);
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D map = new Path2D.Double();
        map.moveTo(C - 78, C - 62);
        map.lineTo(C - 12, C - 74);
        map.lineTo(C + 66, C - 46);
        map.lineTo(C + 80, C + 30);
        map.lineTo(C + 20, C + 66);
        map.lineTo(C - 60, C + 52);
        map.lineTo(C - 82, C + 10);
        map.closePath();
        g.draw(map);
        line(g, C - 40, C - 50, C + 30, C + 40, MUT, 7);
        line(g, C - 60, C + 20, C + 50, C - 20, MUT, 7);
        ellipse(g, C - 10, C - 14, 20, 20, ACC);
    }

    // ---- ownershup: документ с ключом ----
    static void ownershup(Graphics2D g) {
        rounded(g, C - 58, C - 78, 116, 148, 10, FG, false);
        line(g, C - 34, C - 40, C + 34, C - 40, FG, 7);
        line(g, C - 34, C - 10, C + 34, C - 10, FG, 7);
        line(g, C - 34, C + 20, C + 10, C + 20, FG, 7);
        ring(g, C - 10, C + 46, 16, ACC, 8);
        line(g, C + 16, C + 40, C + 44, C + 12, ACC, 8);
        line(g, C + 44, C + 12, C + 56, C + 12, ACC, 8);
        line(g, C + 40, C + 16, C + 52, C + 28, ACC, 8);
    }

    // ---- position: бейдж с человечком ----
    static void position(Graphics2D g) {
        rounded(g, C - 66, C - 80, 132, 150, 16, FG, false);
        ellipse(g, C - 20, C - 56, 40, 40, FG);
        g.setColor(FG);
        g.fill(new Arc2D.Double(C - 44, C + 8, 88, 88, 180, 180, Arc2D.PIE));
        ellipse(g, C - 16, C + 44, 32, 16, ACC);
    }

    // ---- file-type: файл с загнутым уголком ----
    static void fileType(Graphics2D g) {
        rounded(g, C - 62, C - 78, 124, 148, 10, FG, false);
        g.setColor(BG);
        Path2D corner = new Path2D.Double();
        corner.moveTo(C + 22, C - 78);
        corner.lineTo(C + 62, C - 78);
        corner.lineTo(C + 62, C - 38);
        corner.lineTo(C + 22, C - 38);
        corner.closePath();
        g.fill(corner);
        line(g, C + 22, C - 78, C + 62, C - 38, FG, 8);
        line(g, C + 22, C - 78, C + 22, C - 38, FG, 8);
        for (int dy : new int[]{-16, 8, 32}) {
            line(g, C - 34, C + dy, C + 30, C + dy, MUT, 7);
        }
        line(g, C - 34, C + 48, C + 10, C + 48, ACC, 7);
    }

    // ---- grade: лестница уровней ----
    static void grade(Graphics2D g) {
        for (int i = 0; i < 4; i++) {
            int y = C + 66 - i * 30;
            int x0 = C - 78 + i * 26;
            Color c = i % 2 == 0 ? FG : MUT;
            line(g, x0, y, C + 70, y, c, 9);
            line(g, x0, y, x0, y - 30, c, 9);
        }
        line(g, C + 70, C - 24, C + 70, C + 66, FG, 9);
        ellipse(g, C + 60, C - 40, 20, 20, ACC);
    }

    // ---- currency: монета с символом ----
    static void currency(Graphics2D g) {
        ring(g, C, C, 70, FG, 10);
        line(g, C + 6, C - 46, C + 6, C + 46, FG, 10);
        line(g, C - 14, C - 24, C + 26, C - 24, FG, 9);
        line(g, C - 14, C + 24, C + 26, C + 24, FG, 9);
        line(g, C + 6, C - 58, C + 6, C - 40, ACC, 9);
        line(g, C + 6, C + 40, C + 6, C + 58, ACC, 9);
    }

    // ---- outstaffing-rates: шкала со стрелкой ----
    static void outstaffingRates(Graphics2D g) {
        rounded(g, C - 76, C - 10, 152, 20, 10, FG, false);
        rounded(g, C - 76, C - 10, 82, 20, 10, FG, true);
        triangle(g, C - 6, C - 52, C + 26, C - 52, C + 10, C - 84, FG);
        line(g, C - 6, C - 52, C + 26, C - 52, FG, 8);
        line(g, C + 10, C - 84, C + 10, C - 52, FG, 8);
        line(g, C - 6, C - 52, C + 10, C - 84, FG, 8);
        line(g, C + 10, C - 84, C + 26, C - 52, FG, 8);
        ellipse(g, C + 2, C - 22, 20, 20, ACC);
    }

    // ---- employee-work-status: часы ----
    static void employeeWorkStatus(Graphics2D g) {
        ring(g, C, C, 70, FG, 10);
        line(g, C, C - 46, C, C, FG, 9);
        line(g, C, C, C + 34, C + 16, FG, 9);
        ellipse(g, C - 9, C - 9, 18, 18, FG);
        ellipse(g, C - 14, C + 46, 28, 28, ACC);
    }

    // ---- sign-icons: звезда ----
    static void signIcons(Graphics2D g) {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? 74 : 30;
            double a = Math.PI / 2 + i * Math.PI / 5;
            double x = C + r * Math.cos(a);
            double y = C - 4 + r * Math.sin(a);
            if (i == 0) {
                star.moveTo(x, y);
            } else {
                star.lineTo(x, y);
            }
        }
        star.closePath();
        g.setColor(FG);
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(star);
        ellipse(g, C - 12, C + 44, 24, 24, ACC);
    }

    public static void main(String[] args) throws Exception {
        java.util.Map<String, Painter> gens = new java.util.LinkedHashMap<>();
        gens.put("iteraction", GenDictionaryLogos::iteraction);
        gens.put("specialisation", GenDictionaryLogos::specialisation);
        gens.put("country", GenDictionaryLogos::country);
        gens.put("city", GenDictionaryLogos::city);
        gens.put("region", GenDictionaryLogos::region);
        gens.put("ownershup", GenDictionaryLogos::ownershup);
        gens.put("position", GenDictionaryLogos::position);
        gens.put("file-type", GenDictionaryLogos::fileType);
        gens.put("grade", GenDictionaryLogos::grade);
        gens.put("currency", GenDictionaryLogos::currency);
        gens.put("outstaffing-rates", GenDictionaryLogos::outstaffingRates);
        gens.put("employee-work-status", GenDictionaryLogos::employeeWorkStatus);
        gens.put("sign-icons", GenDictionaryLogos::signIcons);

        Path base = Paths.get("modules", "web", "themes");
        for (java.util.Map.Entry<String, Painter> e : gens.entrySet()) {
            BufferedImage img = canvas();
            Graphics2D g = img.createGraphics();
            e.getValue().paint(g);
            g.dispose();
            for (String theme : THEMES) {
                Path dir = base.resolve(theme).resolve("icons").resolve("dictionaries");
                Files.createDirectories(dir);
                ImageIO.write(img, "png", dir.resolve(e.getKey() + ".png").toFile());
            }
            System.out.println("OK " + e.getKey() + " " + img.getWidth() + "x" + img.getHeight());
        }
    }
}
