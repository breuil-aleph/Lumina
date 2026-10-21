package map;

import state.PlayState;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public class WindArea extends MapArea {
    public static final int ISLAND_ID = 4;
    public static final int SIZE = PlayState.ISLAND_SIZE;

    public static final int MAP4_WIDTH = SIZE * PlayState.TILE_W;
    public static final int MAP4_HEIGHT = SIZE * PlayState.TILE_H;

    private static final double MAP4_SCALE = 1.1;

    public static final double ORIG_BG_W = 2912.0;
    public static final double ORIG_BG_H = 1440.0;

    private BufferedImage zephyrImg;

    private static final int RIM = 0, FOOT = 1, PLOT = 2, PATH = 3, PLAZA = 4;
    private static final int MID_A = 5, MID_B = 6;

    private static final int[][] PLOTS = {
            { 2, 4, 2, 4 }, { 2, 4, 7, 9 }, { 7, 9, 2, 4 }, { 7, 9, 7, 9 }
    };

    private static final Color PLOT_TINT = new Color(130, 240, 200, 35);

    private static final double[][] RAW_CRYSTALS = {
            { 1456, 1090 }, { 880, 825 }, { 1935, 825 }, { 490, 640 }, { 2340, 640 }
    };

    private static final int ORB_LIFT = 260;

    private static final int LEAF_COUNT = 200;
    private final double[] leafX = new double[LEAF_COUNT];
    private final double[] leafY = new double[LEAF_COUNT];
    private final double[] leafPhase = new double[LEAF_COUNT];
    private final double[] leafSpeed = new double[LEAF_COUNT];

    private BufferedImage islandBgImg;
    private boolean overlayHooked = false;

    public WindArea() {
        super(false);
        loadImages();
        zephyrImg = ImageHelper.loadImage("res/map4/Zephyr.png");
        initParticles();
    }

    @Override
    public void loadImages() {
        islandBgImg = ImageHelper.loadImage("res/map4/map4_island.png");
    }

    private void initParticles() {
        Random rnd = new Random(444);
        for (int i = 0; i < LEAF_COUNT; i++) {
            leafX[i] = rnd.nextDouble();
            leafY[i] = rnd.nextDouble();
            leafPhase[i] = rnd.nextDouble() * Math.PI * 2;
            leafSpeed[i] = 0.05 + rnd.nextDouble() * 0.1;
        }
    }

    public int kindOf(int r, int c) {
        if (isPlantable(r, c))
            return PLOT;
        int layer = Math.min(Math.min(r, c), Math.min(SIZE - 1 - r, SIZE - 1 - c));
        if (layer == 0)
            return RIM;
        boolean midR = (r == MID_A || r == MID_B), midC = (c == MID_A || c == MID_B);
        if (midR && midC)
            return PLAZA;
        if (midR || midC)
            return PATH;
        return FOOT;
    }

    @Override
    public boolean isPlantable(int r, int c) {
        for (int[] p : PLOTS) {
            if (r >= p[0] && r <= p[1] && c >= p[2] && c <= p[3])
                return true;
        }
        return false;
    }

    private double isoX(double r, double c) {
        return ImageHelper.getIslandCenterX(ISLAND_ID) + (c - r) * (PlayState.TILE_W / 2.0);
    }

    private double isoY(double r, double c) {
        return ImageHelper.getIslandCenterY(ISLAND_ID) + (c + r - (SIZE - 1)) * (PlayState.TILE_H / 2.0);
    }

    // Hàm phụ trợ
    private static double frac(double v) {
        return v - Math.floor(v);
    }

    private double toScreenX(double origX, int cx) {
        return cx + (origX - ORIG_BG_W / 2.0) * ((double) MAP4_WIDTH / ORIG_BG_W);
    }

    private double toScreenY(double origY, int cy) {
        return cy + (origY - ORIG_BG_H / 2.0) * ((double) MAP4_HEIGHT / ORIG_BG_H);
    }

    // =========================================================================
    // TẦNG ĐÁY (Vẽ nền, vành đai dưới, lưới, cổng)
    // =========================================================================
    @Override
    public void drawTiles(Graphics2D g2) {
        int cx = ImageHelper.getIslandCenterX(ISLAND_ID);
        int cy = ImageHelper.getIslandCenterY(ISLAND_ID);

        if (islandBgImg == null) {
            return;
        }

        Graphics2D mapG = (Graphics2D) g2.create();

        mapG.translate(cx, cy);
        mapG.scale(MAP4_SCALE, MAP4_SCALE);
        mapG.translate(-cx, -cy);

        ImageHelper.drawAreaBackground(
                mapG,
                islandBgImg,
                cx,
                cy,
                MAP4_WIDTH,
                MAP4_HEIGHT);

        mapG.dispose();
    }

    @Override
    public void drawObjects(Graphics2D g2) {
        double sec = System.currentTimeMillis() / 1000.0;
        int cx = ImageHelper.getIslandCenterX(ISLAND_ID);
        int cy = ImageHelper.getIslandCenterY(ISLAND_ID);

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Stroke oldStroke = g2.getStroke();
        Composite oldComp = g2.getComposite();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Mây phía sau đảo
        drawCloudCollar(g2, cx, cy, sec, true);
        drawCloudShadows(g2, cx, cy, sec);

        // Các hiệu ứng nền
        drawUpdrafts(g2, cx, cy, sec);

        // Vật thể trên đảo
        drawPlotBorders(g2, sec);
        drawCrystalGlints(g2, cx, cy, sec);

        // Mây phía trước đảo
        drawCloudCollar(g2, cx, cy, sec, false);

        if (!overlayHooked) {
            drawZephyrCore(g2, cx, cy, sec);
        }

        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);

        if (oldAA != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }
    }

    // =========================================================================
    // TẦNG TRÊN (Overlay đè lên ngọn cây)
    // =========================================================================
    public void drawOverlay(Graphics2D g2) {
        overlayHooked = true;
        double sec = System.currentTimeMillis() / 1000.0;
        int cx = ImageHelper.getIslandCenterX(ISLAND_ID);
        int cy = ImageHelper.getIslandCenterY(ISLAND_ID);

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Composite oldComp = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Gale Factor: Mỗi 10s gió giật mạnh 1.5s
        double galeFactor = (sec % 10.0 > 8.5) ? 3.5 : 1.0;

        drawWindRibbons(g2, cx, cy, sec, galeFactor);
        drawZephyrCore(g2, cx, cy, sec);

        // Hệ thống hiệu ứng gió mới
        drawWindWisps(g2, cx, cy, sec); // Tinh linh gió bay lượn

        drawAtmosphere(g2, cx, cy, sec, galeFactor); // Bão lụa ngang & lá bay (cũ)
        drawMotes(g2, cx, cy, sec); // Hạt sáng bay lên

        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);
        if (oldAA != null)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    // -------------------------------------------------------------------------
    // HỆ THỐNG HIỆU ỨNG GIÓ MỚI
    // -------------------------------------------------------------------------

    /**
     * Gió lướt: Những cú quét gió hình lưỡi liềm (nét cọ) bay ngang qua đảo.
     * Tỏa sáng ở đầu và tan dần ở đuôi, có hạt sáng bay kèm.
     */

    private void drawWindRibbons(
            Graphics2D g2, int cx, int cy, double sec, double galeFactor) {

        Graphics2D g = (Graphics2D) g2.create();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();

        // x, y, chiều dài, biên độ uốn, tốc độ, độ trễ
        double[][] ribbons = {
                { -900, -180, 520, 34, 0.75, 0.0 },
                { 620, -310, 440, 27, 0.55, 1.7 },
                { -760, 80, 390, 42, 0.90, 3.1 },
                { 580, 220, 560, 30, 0.65, 2.2 },
                { -380, 390, 460, 25, 0.48, 4.5 },
                { 150, -430, 350, 22, 0.82, 1.0 }
        };

        Color[] colors = {
                new Color(145, 245, 235),
                new Color(220, 255, 250),
                new Color(170, 255, 240),
                new Color(110, 220, 220)
        };

        double strength = Math.max(1.0, galeFactor);

        for (int i = 0; i < ribbons.length; i++) {
            double[] r = ribbons[i];

            double baseX = r[0];
            double baseY = r[1];
            double length = r[2];
            double amplitude = r[3];
            double speed = r[4];
            double phase = r[5];

            // Trôi ngang liên tục; khi đi khỏi màn hình thì xuất hiện lại.
            double travel = sec * speed * 100.0 * strength + phase * 230.0;
            double cycle = length + 220.0;
            double offsetX = travel % cycle;

            // Giữ chuyển động ngang, không quay quanh tâm đảo.
            double startX = cx + baseX + offsetX - cycle / 2.0;
            double y = cy + baseY;

            // Độ uốn thay đổi liên tục theo thời gian.
            double wavePhase = sec * (1.1 + speed * 0.3) + phase;

            Path2D.Double path = new Path2D.Double();
            path.moveTo(startX, y);

            int segments = 16;

            for (int j = 1; j <= segments; j++) {
                double p = j / (double) segments;
                double x = startX + length * p;

                double wave = Math.sin(p * Math.PI * 2.0 + wavePhase)
                        * amplitude;

                double secondaryWave = Math.sin(p * Math.PI * 4.0 - wavePhase * 0.7)
                        * amplitude * 0.28;

                double py = y + wave + secondaryWave;

                path.lineTo(x, py);
            }

            // Độ sáng nhấp nhô nhẹ, không nhấp nháy mạnh.
            float alpha = (float) (0.18 + 0.13
                    * (0.5 + 0.5 * Math.sin(sec * 1.2 + phase)));

            alpha = Math.min(0.55f, alpha);

            // Glow mềm phía sau.
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha * 0.30f));
            g.setColor(colors[i % colors.length]);
            g.setStroke(new BasicStroke(
                    (float) (13.0 * Math.min(strength, 1.8)),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.draw(path);

            // Thân ribbon.
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));
            g.setStroke(new BasicStroke(
                    (float) (4.0 * Math.min(strength, 1.8)),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.draw(path);

            // Lõi sáng mảnh.
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha * 0.65f));
            g.setColor(new Color(235, 255, 252));
            g.setStroke(new BasicStroke(
                    1.2f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.draw(path);
        }

        g.setComposite(oldComposite);
        g.setStroke(oldStroke);
        g.dispose();
    }

    /**
     * Tinh linh gió: Những đốm sáng nhỏ mang theo vệt đuôi dài bay lượn theo quỹ
     * đạo elip.
     */
    private void drawWindWisps(Graphics2D g2, int cx, int cy, double sec) {
        Graphics2D g = (Graphics2D) g2.create();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();

        int count = 9;

        Color[] windColors = {
                new Color(190, 255, 245),
                new Color(135, 240, 230),
                new Color(225, 255, 250),
                new Color(115, 215, 220)
        };

        for (int i = 0; i < count; i++) {
            double phase = i * 2.399 + Math.sin(i * 1.7) * 0.5;
            double speed = 0.22 + (i % 4) * 0.075;
            double progress = (sec * speed + i * 0.173) % 1.0;

            // Mỗi luồng gió có quỹ đạo và vị trí riêng.
            double baseAngle = phase + progress * Math.PI * 2.0;
            double radiusX = 850 + 220 * Math.sin(phase * 1.8);
            double radiusY = 390 + 120 * Math.cos(phase * 1.3);

            double angle = baseAngle
                    + 0.20 * Math.sin(sec * 0.7 + phase);

            double x = cx + Math.cos(angle) * radiusX;
            double y = cy - 80
                    + Math.sin(angle) * radiusY
                    + 45 * Math.sin(sec * 0.55 + phase);

            // Đường cong ngắn, hơi xoắn theo hướng chuyển động.
            double length = 25 + 25 * (0.5 + 0.5 * Math.sin(phase * 2.1));
            double direction = angle + Math.PI / 2.0
                    + 0.35 * Math.sin(sec * 0.8 + phase);

            double dx = Math.cos(direction);
            double dy = Math.sin(direction);

            double side = 25 * Math.sin(sec * 1.1 + phase);

            double x1 = x - dx * length * 0.5;
            double y1 = y - dy * length * 0.5;

            double x2 = x + dx * length * 0.5;
            double y2 = y + dy * length * 0.5;

            double c1x = x - dx * length * 0.12 - dy * side;
            double c1y = y - dy * length * 0.12 + dx * side;

            double c2x = x + dx * length * 0.12 + dy * side;
            double c2y = y + dy * length * 0.12 - dx * side;

            Path2D.Double path = new Path2D.Double();
            path.moveTo(x1, y1);
            path.curveTo(c1x, c1y, c2x, c2y, x2, y2);

            float alpha = (float) (0.20 + 0.35
                    * (0.5 + 0.5 * Math.sin(sec * 1.4 + phase)));

            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));

            // Lớp glow mờ phía sau.
            g.setStroke(new BasicStroke(
                    7.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(120, 245, 235));
            g.draw(path);

            // Lõi gió mảnh, sáng.
            g.setStroke(new BasicStroke(
                    2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(windColors[i % windColors.length]);
            g.draw(path);

            // Hạt sáng ở đầu luồng gió.
            float particleSize = 2.0f + (i % 3);

            g.fill(new Ellipse2D.Double(
                    x2 - particleSize / 2.0,
                    y2 - particleSize / 2.0,
                    particleSize,
                    particleSize));
        }

        g.setComposite(oldComposite);
        g.setStroke(oldStroke);
        g.dispose();
    }

    /**
     * Cột gió xoáy: Những cơn lốc nhỏ bốc lên từ mặt đất.
     */
    private void drawUpdrafts(Graphics2D g2, int cx, int cy, double sec) {
        double[][] spots = {
                { cx - 550, cy + 200 }, { cx + 550, cy + 200 },
                { cx - 450, cy - 150 }, { cx + 450, cy - 150 },
                { cx, cy + 350 }
        };

        for (int i = 0; i < spots.length; i++) {
            double sx = spots[i][0];
            double sy = spots[i][1];

            for (int j = 0; j < 5; j++) {
                double p = frac(sec * 0.5 + j * 0.2 + i * 0.13);
                double y = sy - p * 180;

                // Góc xoay
                double ang = sec * 10 + p * 20;
                double r = 20 + 50 * Math.sin(p * Math.PI);

                float a = (float) (100 * Math.sin(p * Math.PI));
                g2.setColor(new Color(200, 255, 240, (int) a));
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Vẽ cung xoáy
                double dx = Math.cos(ang) * r;
                double dy = Math.sin(ang) * r * 0.4;
                g2.draw(new Arc2D.Double(sx + dx - 10, y + dy - 10, 20, 20, Math.toDegrees(ang), 180, Arc2D.OPEN));
            }
        }
    }

    // -------------------------------------------------------------------------
    // CÁC HÀM CŨ (Giữ nguyên)
    // -------------------------------------------------------------------------

    private void drawPlotBorders(Graphics2D g2, double sec) {
        float pulse = (float) (0.5 + 0.5 * Math.sin(sec * 2.0));
        for (int[] p : PLOTS) {
            Path2D.Double poly = new Path2D.Double();
            poly.moveTo(isoX(p[0] - 0.5, p[2] - 0.5), isoY(p[0] - 0.5, p[2] - 0.5));
            poly.lineTo(isoX(p[0] - 0.5, p[3] + 0.5), isoY(p[0] - 0.5, p[3] + 0.5));
            poly.lineTo(isoX(p[1] + 0.5, p[3] + 0.5), isoY(p[1] + 0.5, p[3] + 0.5));
            poly.lineTo(isoX(p[1] + 0.5, p[2] - 0.5), isoY(p[1] + 0.5, p[2] - 0.5));
            poly.closePath();

            g2.setColor(PLOT_TINT);
            g2.fill(poly);
            g2.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(110, 255, 190, (int) (40 + 40 * pulse)));
            g2.draw(poly);
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(160, 255, 210, (int) (120 + 90 * pulse)));
            g2.draw(poly);
        }
    }

    // TODO: thay thành cái khác
    private void drawHalo(Graphics2D g2, int cx, int cy, double sec, boolean back) {
        int RING_BASE = 1950;
        int RING_DY = 120;
        double ccx = cx, ccy = cy + RING_DY;
        double start = back ? 0 : 180;

        double[][] bands = {
                { 1.000, 80, 150, 255, 220, 38 }, { 0.975, 36, 180, 255, 240, 90 },
                { 0.945, 12, 220, 255, 250, 160 }, { 0.915, 58, 120, 255, 200, 60 },
                { 0.885, 20, 190, 255, 230, 120 }, { 0.860, 8, 255, 255, 255, 140 }
        };

        for (double[] b : bands) {
            double a = RING_BASE * b[0], bb = a / 2;
            g2.setStroke(new BasicStroke((float) b[1], BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color((int) b[2], (int) b[3], (int) b[4], (int) b[5]));
            g2.draw(new Arc2D.Double(ccx - a, ccy - bb, a * 2, bb * 2, start, 180, Arc2D.OPEN));
        }

        for (int i = 0; i < 70; i++) {
            double ang = frac(i * 0.618 + sec * (0.006 + 0.01 * frac(i * 0.37))) * Math.PI * 2;
            boolean front = Math.sin(ang) > 0;
            if (front == back)
                continue;
            double a = RING_BASE * (0.86 + 0.14 * frac(i * 0.377));
            double x = ccx + Math.cos(ang) * a, y = ccy + Math.sin(ang) * a / 2;
            float tw = (float) Math.max(0, Math.min(1, 0.45 + 0.4 * Math.sin(sec * 3 + i * 2.1)));
            double sz = 5 + (i % 4) * 2.5;
            g2.setColor(new Color(130, 255, 200, (int) (tw * 120)));
            g2.fill(new Ellipse2D.Double(x - sz * 1.8, y - sz * 1.8, sz * 3.6, sz * 3.6));
            g2.setColor(new Color(255, 255, 255, (int) (tw * 255)));
            g2.fill(new Ellipse2D.Double(x - sz / 2, y - sz / 2, sz, sz));
        }
    }

    private void drawSkyClouds(Graphics2D g2, int cx, int cy, double sec) {
        double[][] clouds = {
                { 0.55, 900, 0.013, 1.0 },
                { 0.30, 1000, 0.011, 0.9 },
                { 0.80, 820, 0.015, 1.1 }
        };
        double range = 4200, xStart = cx - 2600;

        Color under = new Color(150, 220, 200);
        Color body = new Color(245, 255, 250);

        for (double[] cl : clouds) {
            double p = frac(cl[0] + sec * cl[2]);
            double x = xStart + p * range, y = cy + cl[1], s = cl[3];
            float a = (float) (0.85 * Math.min(1.0, Math.min(p, 1 - p) * 6));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));

            g2.setColor(under);
            g2.fill(new Ellipse2D.Double(x - 260 * s, y - 20 * s, 520 * s, 90 * s));
            g2.setColor(body);
            g2.fill(new Ellipse2D.Double(x - 200 * s, y - 70 * s, 260 * s, 110 * s));
            g2.fill(new Ellipse2D.Double(x - 60 * s, y - 110 * s, 300 * s, 140 * s));
            g2.fill(new Ellipse2D.Double(x + 90 * s, y - 60 * s, 250 * s, 100 * s));
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawCloudShadows(Graphics2D g2, int cx, int cy, double sec) {
        double[][] clouds = {
                { 0.55, 900, 0.013, 1.0 },
                { 0.30, 1000, 0.011, 0.9 },
                { 0.80, 820, 0.015, 1.1 }
        };
        double range = 4200, xStart = cx - 2600;

        g2.setColor(new Color(0, 0, 0, 15));
        for (double[] cl : clouds) {
            double p = frac(cl[0] + sec * cl[2]);
            double x = xStart + p * range - 150, y = cy + cl[1] + 350, s = cl[3];
            float a = (float) Math.min(1.0, Math.min(p, 1 - p) * 6);
            if (a > 0) {
                g2.fill(new Ellipse2D.Double(x - 200 * s, y - 40 * s, 400 * s, 120 * s));
            }
        }
    }

    /** Hạt sáng bay lên từ mặt đảo — đồng bộ style với EarthArea */
    /** Hạt sáng xanh mint bay lên từ mặt đảo — theo style EarthArea */
    private void drawMotes(Graphics2D g2, int cx, int cy, double sec) {
        Composite oldComp = g2.getComposite();

        for (int i = 0; i < 40; i++) {
            double sp = 0.03 + 0.04 * frac(i * 0.37);
            double p = frac(i * 0.618 + sec * sp);

            double x = cx
                    + (frac(i * 0.754) - 0.5) * 3000
                    + Math.sin(sec * 1.3 + i) * 40;

            double y = cy + 700 - p * 1900;

            double tw = 0.35 + 0.35 * Math.sin(sec * 5 + i * 2.3);

            float a = (float) Math.max(0, Math.min(1,
                    (tw + 0.2) * Math.min(1.0, Math.min(p, 1 - p) * 6)));

            g2.setComposite(
                    AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));

            double sz = 10 + (i % 3) * 5;

            // Quầng sáng xanh mint
            g2.setColor(new Color(150, 255, 230, 110));
            g2.fill(new Ellipse2D.Double(
                    x - sz, y - sz, sz * 2, sz * 2));

            // Lõi sáng
            g2.setColor(i % 4 == 0
                    ? new Color(255, 255, 255)
                    : new Color(205, 255, 240));

            g2.fill(new Ellipse2D.Double(
                    x - sz / 2, y - sz / 2, sz, sz));
        }

        g2.setComposite(oldComp);
    }

    private void drawZephyrCore(Graphics2D g2, int cx, int cy, double sec) {
        double bob = Math.sin(sec * 1.5) * 20;
        double ox = cx;
        double oy = cy - ORB_LIFT + bob;

        if (zephyrImg != null) {
            int drawW = 420;
            int drawH = (int) (drawW * (double) zephyrImg.getHeight()
                    / zephyrImg.getWidth());

            int x = (int) (ox - drawW / 2.0);
            int y = (int) (oy - drawH / 2.0);

            g2.drawImage(zephyrImg, x, y, drawW, drawH, null);
        }
    }

    private void drawCrystalGlints(Graphics2D g2, int cx, int cy, double sec) {
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < RAW_CRYSTALS.length; i++) {
            double px = toScreenX(RAW_CRYSTALS[i][0], cx);
            double py = toScreenY(RAW_CRYSTALS[i][1], cy);
            float a = (float) Math.max(0, Math.sin(sec * 2.2 + i * 1.4));
            if (a > 0.08f) {
                g2.setColor(new Color(1f, 1f, 1f, a * 0.85f));
                g2.draw(new Path2D.Double() {
                    {
                        moveTo(px - 14 * a, py);
                        lineTo(px + 14 * a, py);
                        moveTo(px, py - 14 * a);
                        lineTo(px, py + 14 * a);
                    }
                });
            }
        }
    }

    private void drawAtmosphere(Graphics2D g2, int cx, int cy,
            double sec, double galeFactor) {

        int w = MAP4_WIDTH + 600;
        int h = MAP4_HEIGHT + 600;

        double startX = cx - w / 2.0;
        double startY = cy - h / 2.0;

        double gust = 1.0 + 0.3 * (galeFactor - 1.0);

        // Tăng số lá ngọc bằng cách tăng LEAF_COUNT ở phần khai báo.
        for (int i = 0; i < LEAF_COUNT; i++) {
            double speed = leafSpeed[i] * gust;

            double px = startX
                    + frac(leafX[i] + sec * speed * 0.06) * w;

            double py = startY
                    + frac(leafY[i] - sec * speed * 0.025) * h
                    + Math.sin(sec * 1.2 + leafPhase[i]) * 18;

            double sway = Math.sin(sec * 1.5 + leafPhase[i]);
            double drift = 18 * sway;

            // Kích thước lá đa dạng.
            double size = 4.0 + 3.5 * frac(i * 0.371);

            // Lá xoay theo gió, mỗi lá có pha riêng.
            double angle = sec * (0.8 + leafSpeed[i] * 0.12)
                    * gust + leafPhase[i];

            // Độ trong suốt biến thiên nhẹ.
            int alpha = 105 + (int) (65 * (0.5 + 0.5
                    * Math.sin(sec * 1.3 + leafPhase[i])));

            Graphics2D leafG = (Graphics2D) g2.create();

            leafG.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            leafG.translate(px + drift, py);
            leafG.rotate(angle);

            // Một vài sắc độ xanh ngọc.
            switch (i % 4) {
                case 0:
                    leafG.setColor(new Color(115, 235, 185, alpha));
                    break;
                case 1:
                    leafG.setColor(new Color(155, 255, 215, alpha));
                    break;
                case 2:
                    leafG.setColor(new Color(85, 210, 175, alpha));
                    break;
                default:
                    leafG.setColor(new Color(195, 255, 225, alpha));
                    break;
            }

            // Hình lá thuôn nhọn.
            Path2D.Double leaf = new Path2D.Double();
            leaf.moveTo(-size, 0);
            leaf.quadTo(-size * 0.25, -size * 0.75,
                    size, 0);
            leaf.quadTo(-size * 0.25, size * 0.75,
                    -size, 0);
            leaf.closePath();

            leafG.fill(leaf);
            leafG.dispose();
        }
    }

    private void drawCloudCollar(
            Graphics2D g2, int cx, int cy, double sec, boolean back) {

        Graphics2D g = (Graphics2D) g2.create();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        Composite oldComposite = g.getComposite();

        /*
         * Các cụm mây phân bố không đều quanh đảo.
         * Tọa độ tương đối so với tâm đảo.
         */
        double[][] clusters = {
                { -1500, -280, 1.15 },
                { -1190, -620, 0.80 },
                { -760, -770, 0.65 },
                { -250, -880, 0.95 },
                { 350, -820, 0.72 },
                { 900, -650, 1.10 },
                { 1430, -300, 0.75 },

                { -1580, 180, 0.70 },
                { -1320, 480, 1.00 },
                { -930, 700, 0.68 },
                { -440, 830, 0.85 },
                { 180, 850, 0.65 },
                { 720, 760, 1.10 },
                { 1190, 520, 0.78 },
                { 1530, 160, 0.95 },

                { -1450, -100, 0.85 },
                { -1380, -400, 0.95 },
                { -1080, -720, 0.80 },
                { -620, -850, 0.90 },
                { -50, -900, 0.85 },
                { 520, -760, 0.95 },
                { 1100, -480, 0.85 },
                { 1460, -100, 0.90 },

                { -1480, 320, 0.95 },
                { -1120, 610, 0.85 },
                { -650, 780, 1.00 },
                { -120, 900, 0.80 },
                { 430, 820, 0.90 },
                { 950, 650, 0.95 },
                { 1320, 390, 0.85 },
                { 200, 500, 0.70 },
                { -500, 400, 0.75 }
        };

        for (int i = 0; i < clusters.length; i++) {
            double bx = clusters[i][0];
            double by = clusters[i][1];
            double scale = clusters[i][2];

            // Các cụm ở phía sau và phía trước đảo được vẽ riêng.
            boolean isBack = by < 0;
            if (isBack != back) {
                continue;
            }

            // Mây chuyển động chậm, mỗi cụm có pha riêng.
            double phase = i * 2.17;
            double driftX = Math.sin(sec * 0.18 + phase) * 28;
            double driftY = Math.cos(sec * 0.15 + phase) * 12;

            double x = cx + bx + driftX;
            double y = cy + by + driftY;

            // Một số cụm mây co giãn nhẹ để tránh cảm giác tĩnh.
            double pulse = 1.0
                    + 0.045 * Math.sin(sec * 0.42 + phase);

            double w = 350 * scale * pulse;
            double h = 145 * scale * pulse;

            float alpha = (float) (0.38 + 0.16
                    * (0.5 + 0.5 * Math.sin(sec * 0.3 + phase)));

            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));

            // Bóng mây phía dưới
            g.setColor(new Color(100, 195, 210));
            g.fill(new Ellipse2D.Double(
                    x - w * 0.48, y - h * 0.10,
                    w * 0.96, h * 0.80));

            // Thân mây: các khối chồng lấn dày hơn
            g.setColor(new Color(205, 250, 247));

            g.fill(new Ellipse2D.Double(
                    x - w * 0.50, y - h * 0.10,
                    w * 0.62, h * 0.78));

            g.fill(new Ellipse2D.Double(
                    x - w * 0.30, y - h * 0.42,
                    w * 0.58, h * 0.95));

            g.fill(new Ellipse2D.Double(
                    x - w * 0.05, y - h * 0.32,
                    w * 0.60, h * 0.88));

            g.fill(new Ellipse2D.Double(
                    x + w * 0.18, y - h * 0.08,
                    w * 0.48, h * 0.72));

            // Viền sáng rất nhẹ để hòa với hiệu ứng gió.
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha * 0.55f));

            g.setColor(new Color(235, 255, 253));
            g.setStroke(new BasicStroke(
                    2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g.draw(new Ellipse2D.Double(
                    x - w * 0.23,
                    y - h * 0.48,
                    w * 0.49,
                    h * 0.88));
        }

        g.setComposite(oldComposite);
        g.dispose();
    }

}