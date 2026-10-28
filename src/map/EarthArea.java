package map;

import state.PlayState;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ĐẢO 3 - THẾ GIỚI SẤM SÉT (màu chủ đạo: tím)
 *
 * Bố cục: vành đảo -> chân đồi -> 4 ô trồng cây (mỗi ô 3x3) ngăn bởi đường chữ thập
 * -> quảng trường trung tâm có quả cầu lơ lửng. Cổng sấm sét đứng ở góc sau của đảo.
 * Trên trời: mây bão tím, sét đánh (có nháy sáng), tia điện nhỏ bay lên.
 *
 * ===== CHỖ ĐỂ CHÈN ẢNH (đặt trong thư mục res/map3/, chưa có ảnh thì tự vẽ hình giữ chỗ) =====
 *   gate.png        - cổng / vòng tròn sấm sét, đứng ở góc sau. Chỉnh GATE_H, GATE_R, GATE_C bên dưới.
 *   center_orb.png  - quả cầu/bong bóng lơ lửng giữa đảo. Chỉnh ORB_W, ORB_LIFT bên dưới.
 *   lake.png        - hồ nước dưới quả cầu (ảnh phẳng nằm trên mặt đất, dạng hình thoi/elip 2:1). Chỉnh LAKE_W.
 *   ring_back.png, ring_front.png - (tuỳ chọn) vành hào quang kiểu sao Thổ, cùng kích thước & căn giữa:
 *                     ring_back là nửa nằm sau đảo, ring_front là nửa nằm trước đảo. Có ảnh thì bỏ vành tự vẽ.
 *   map3_island.png - (tuỳ chọn) ảnh nền cả đảo 2880x1440; nếu có thì thay luôn nền ô tự vẽ.
 *   plant_lv1/2/3.png - cây người chơi trồng (Plant.java nạp), nên vẽ tông tím.
 */
public class EarthArea extends MapArea {
    public static final int ISLAND_ID = 3;
    public static final int SIZE = PlayState.ISLAND_SIZE;

    public static final int MAP3_WIDTH  = SIZE * PlayState.TILE_W;
    public static final int MAP3_HEIGHT = SIZE * PlayState.TILE_H;

    // ====================== THAM SỐ DỄ CHỈNH ======================
    private static final int GATE_R = 0, GATE_C = 0;   // ô đặt cổng (0,0 = góc sau cùng của đảo)
    private static final int GATE_H = 720;             // chiều cao cổng khi dùng ảnh (px world)
    private static final int GATE_LIFT = 20;           // nâng cổng lên (+) hay hạ xuống (-)
    private static final int ORB_W = 340;              // chiều rộng quả cầu khi dùng ảnh
    private static final int ORB_LIFT = 260;           // độ cao quả cầu so với mặt đất
    private static final int LAKE_W = 440;             // chiều rộng hồ khi dùng ảnh

    // ====== HIỆU ỨNG ĐỘNG phủ lên ảnh (gate.png / lake.png / center_orb.png) - tắt bằng false ======
    private static final boolean GATE_FX = true, LAKE_FX = true, ORB_FX = true;
    private static final boolean FX_DEBUG = false;         // true = vẽ khung đỏ vùng hiệu ứng để canh cho khớp ảnh
    private static final double GATE_PORTAL_Y = 0.5;      // tâm mặt cổng nằm cách đỉnh ảnh gate bao nhiêu (0.0 = đỉnh, 1.0 = đáy)
    private static final double GATE_PORTAL_DX = 0;        // lệch tâm mặt cổng sang phải (+) / trái (-) (px world)
    private static final double GATE_PORTAL_R = 200;       // bán kính vùng xoáy trong cổng (px world)
    private static final boolean GATE_RUNES = true;        // hạt rune nhấp nháy quanh vòng cổng
    private static final double LAKE_FX_RX = 170;          // nửa chiều rộng vùng nước có gợn sóng (chiều cao = 1/2)
    private static final double ORB_FX_R = 130;            // bán kính vùng tia điện bên trong quả cầu

    // Bật/tắt các lớp trang trí bên ngoài đảo
    private static final boolean SHOW_CLOUD_COLLAR = true;   // mây bao quanh đảo (phía ngoài)
    private static final boolean SHOW_HALO_RING = true;      // vành hào quang kiểu sao Thổ
    private static final int RING_BASE = 1950;               // bán kính ngoài của vành (px world)
    private static final int RING_DY = 120;                  // dịch tâm vành xuống một chút so với tâm đảo
    private static final int RING_IMG_W = 4000;              // chiều rộng ảnh vành khi dùng ảnh

    // Màu phủ lên 4 ô trồng cây (R, G, B, độ đậm 0-255). Muốn nhạt hơn thì giảm số cuối, đậm hơn thì tăng.
    private static final Color PLOT_TINT = new Color(205, 175, 255, 70);

    // Loại ô
    private static final int RIM = 0, FOOT = 1, PLOT = 2, PATH = 3, PLAZA = 4;
    private static final int MID_A = 5, MID_B = 6;     // 2 hàng/cột giữa là đường chữ thập
    // 4 ô trồng cây: {hàng đầu, hàng cuối, cột đầu, cột cuối}
    private static final int[][] PLOTS = {{2, 4, 2, 4}, {2, 4, 7, 9}, {7, 9, 2, 4}, {7, 9, 7, 9}};

    // Sét: 3 "kênh" đánh lệch nhịp nhau
    private static final double[] BOLT_PERIOD = {3.3, 4.7, 6.1};
    private static final double[] BOLT_OFFSET = {0.0, 1.7, 3.1};

    private BufferedImage islandBgImg, gateImg, orbImg, lakeImg, ringBackImg, ringFrontImg;
    private final long[] boltSlot = {-1, -1, -1};
    private final Path2D[] boltMain = new Path2D[3];
    private final Path2D[] boltBranch = new Path2D[3];
    private final double[][] boltTarget = new double[3][3];  // x, y, loại (1 quả cầu, 2 trời)

    public EarthArea() {
        super(false);
        loadImages();
    }

    @Override
    public void loadImages() {
        islandBgImg = ImageHelper.loadImage("res/map3/map3_island.png");
        if (islandBgImg == null) islandBgImg = ImageHelper.loadImage("res/map3_earth/map3_island.png");
        gateImg = ImageHelper.loadImage("res/map3/gate.png");
        orbImg = ImageHelper.loadImage("res/map3/center_orb.png");
        lakeImg = ImageHelper.loadImage("res/map3/lake.png");
        ringBackImg = ImageHelper.loadImage("res/map3/ring_back.png");
        ringFrontImg = ImageHelper.loadImage("res/map3/ring_front.png");
    }

    // =========================================================================
    // PHÂN LOẠI Ô
    // =========================================================================
    private int kindOf(int r, int c) {
        int layer = Math.min(Math.min(r, c), Math.min(SIZE - 1 - r, SIZE - 1 - c));
        if (layer == 0) return RIM;
        boolean midR = (r == MID_A || r == MID_B), midC = (c == MID_A || c == MID_B);
        if (midR && midC) return PLAZA;
        if (midR || midC) return PATH;
        if (layer == 1) return FOOT;
        return PLOT;
    }

    // Toạ độ world của điểm (r, c) có thể là số lẻ - cùng công thức với ImageHelper
    private double isoX(double r, double c) {
        return ImageHelper.getIslandCenterX(ISLAND_ID) + (c - r) * (PlayState.TILE_W / 2.0);
    }
    private double isoY(double r, double c) {
        return ImageHelper.getIslandCenterY(ISLAND_ID) + (c + r - (SIZE - 1)) * (PlayState.TILE_H / 2.0);
    }

    private static double frac(double v) { return v - Math.floor(v); }

    private static Color lerp(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                         (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                         (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static Color scale(Color c, double f) {
        return new Color(Math.min(255, (int) (c.getRed() * f)),
                         Math.min(255, (int) (c.getGreen() * f)),
                         Math.min(255, (int) (c.getBlue() * f)));
    }

    // =========================================================================
    // NỀN ĐẢO
    // =========================================================================
    @Override
    public void drawTiles(Graphics2D g2) {
        int cx = ImageHelper.getIslandCenterX(ISLAND_ID);
        int cy = ImageHelper.getIslandCenterY(ISLAND_ID);

        if (islandBgImg != null) {
            ImageHelper.drawAreaBackground(g2, islandBgImg, cx, cy, MAP3_WIDTH, MAP3_HEIGHT);
            return;
        }

        drawUnderside(g2, cx, cy);

        Stroke oldStroke = g2.getStroke();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int kind = kindOf(r, c);
                ImageHelper.drawIslandTile(g2, null, ISLAND_ID, r, c, groundColor(kind, r, c));
                if (kind == PLOT) {
                    // luống đất
                    int x = ImageHelper.getIslandIsoX(ISLAND_ID, r, c);
                    int y = ImageHelper.getIslandIsoY(ISLAND_ID, r, c);
                    g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(new Color(40, 24, 66, 170));
                    g2.drawLine(x - 40, y - 20 - 10, x + 40, y + 20 - 10);
                    g2.drawLine(x - 40, y - 20 + 10, x + 40, y + 20 + 10);
                }
            }
        }
        g2.setStroke(oldStroke);
    }

    private Color groundColor(int kind, int r, int c) {
        boolean alt = ((r + c) & 1) == 0;
        switch (kind) {
            case RIM:  return alt ? new Color(58, 44, 86) : new Color(52, 40, 80);
            case FOOT: return alt ? new Color(92, 74, 130) : new Color(86, 68, 122);
            case PLOT: return alt ? new Color(72, 46, 104) : new Color(66, 42, 96);
            case PATH: return alt ? new Color(150, 134, 190) : new Color(142, 126, 182);
            default:   return alt ? new Color(182, 166, 224) : new Color(174, 158, 216);
        }
    }

    /** Phần đá dưới đáy đảo cho có cảm giác đảo lơ lửng */
    private void drawUnderside(Graphics2D g2, int cx, int cy) {
        int D = 230;
        int[] xs = {cx - 1440, cx, cx + 1440, cx + 1050, cx + 520, cx, cx - 480, cx - 1050};
        int[] ys = {cy, cy + 720, cy, cy + 360 + D, cy + 600 + D, cy + 720 + D + 170, cy + 610 + D, cy + 360 + D};
        g2.setColor(new Color(50, 38, 82));
        g2.fillPolygon(xs, ys, xs.length);
        g2.setColor(new Color(74, 58, 114));
        g2.fillPolygon(new int[]{cx, cx + 1440, cx + 1050, cx + 520, cx},
                       new int[]{cy + 720, cy, cy + 360 + D, cy + 600 + D, cy + 720 + D + 170}, 5);
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(28, 18, 52));
        g2.drawPolygon(xs, ys, xs.length);
        g2.setColor(new Color(28, 18, 52, 160));
        g2.drawLine(cx - 700, cy + 360, cx - 520, cy + 360 + D + 120);
        g2.drawLine(cx + 600, cy + 330, cx + 420, cy + 360 + D + 160);
        g2.drawLine(cx - 200, cy + 700, cx - 120, cy + 720 + D + 100);
    }

    // =========================================================================
    // VẬT THỂ + HIỆU ỨNG BÃO
    // =========================================================================
    // Trạng thái sét của khung hình hiện tại (tính trong drawObjects, dùng lại ở drawOverlay)
    private final double[] stormInten = new double[3];
    private double stormFlash = 0, stormCharge = 0, stormSec = 0;
    // Chỉ thành true khi PlayState có gọi drawOverlay(). Chưa gọi thì quả cầu tự vẽ ngay trong drawObjects (nằm dưới cây)
    private boolean overlayHooked = false;

    /** LƯỢT DƯỚI - vẽ TRƯỚC cây: mọi thứ của đảo, trừ quả cầu (quả cầu vẽ ở drawOverlay để nằm trên cây) */
    @Override
    public void drawObjects(Graphics2D g2) {
        double sec = System.currentTimeMillis() / 1000.0;
        int cx = ImageHelper.getIslandCenterX(ISLAND_ID);
        int cy = ImageHelper.getIslandCenterY(ISLAND_ID);

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldInterp = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Composite oldComp = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Trạng thái sét ở thời điểm hiện tại
        double flash = 0, orbCharge = 0;
        for (int i = 0; i < 3; i++) {
            double u = sec + BOLT_OFFSET[i];
            long slot = (long) Math.floor(u / BOLT_PERIOD[i]);
            double t = u - slot * BOLT_PERIOD[i];
            ensureBolt(i, slot, cx, cy);
            stormInten[i] = flashIntensity(t);
            flash = Math.max(flash, stormInten[i]);
            if (boltTarget[i][2] == 1) orbCharge = Math.max(orbCharge, stormInten[i]);
        }
        stormFlash = flash; stormCharge = orbCharge; stormSec = sec;

        drawStormClouds(g2, cx, cy, sec, flash);
        drawHalo(g2, cx, cy, sec, flash, true);           // nửa sau của vành hào quang
        drawCloudCollar(g2, cx, cy, sec, flash, true);    // mây phía sau đảo

        drawPlotBorders(g2, sec);
        drawRuneRing(g2, cx, cy, sec, orbCharge);
        drawGate(g2, sec, flash);
        if (!overlayHooked) drawOrb(g2, cx, cy, sec, orbCharge);   // dự phòng khi PlayState chưa gọi drawOverlay()
        drawHalo(g2, cx, cy, sec, flash, false);          // nửa trước của vành hào quang
        drawCloudCollar(g2, cx, cy, sec, flash, false);   // mây phía trước đảo
        drawBolts(g2, stormInten);
        drawMotes(g2, cx, cy, sec);
        drawFlash(g2, cx, cy, flash);

        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        if (oldInterp != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);
        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);
    }

    /** LƯỢT TRÊN - PlayState gọi SAU KHI vẽ cây, nên quả cầu lơ lửng nằm trên cây */
    public void drawOverlay(Graphics2D g2) {
        overlayHooked = true;
        int cx = ImageHelper.getIslandCenterX(ISLAND_ID);
        int cy = ImageHelper.getIslandCenterY(ISLAND_ID);

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldInterp = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Composite oldComp = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        drawOrb(g2, cx, cy, stormSec, stormCharge);

        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        if (oldInterp != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);
        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);
    }

    // ------------------------------ Ô trồng + trung tâm ------------------------------
    private void drawPlotBorders(Graphics2D g2, double sec) {
        float pulse = (float) (0.5 + 0.5 * Math.sin(sec * 2.0));
        for (int[] p : PLOTS) {
            Path2D.Double poly = new Path2D.Double();
            poly.moveTo(isoX(p[0] - 0.5, p[2] - 0.5), isoY(p[0] - 0.5, p[2] - 0.5));
            poly.lineTo(isoX(p[0] - 0.5, p[3] + 0.5), isoY(p[0] - 0.5, p[3] + 0.5));
            poly.lineTo(isoX(p[1] + 0.5, p[3] + 0.5), isoY(p[1] + 0.5, p[3] + 0.5));
            poly.lineTo(isoX(p[1] + 0.5, p[2] - 0.5), isoY(p[1] + 0.5, p[2] - 0.5));
            poly.closePath();
            g2.setColor(PLOT_TINT);                      // phủ tím nhạt lên cả ô trồng
            g2.fill(poly);
            g2.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(150, 90, 255, (int) (40 + 40 * pulse)));
            g2.draw(poly);
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(205, 165, 255, (int) (120 + 90 * pulse)));
            g2.draw(poly);
        }
    }

    private void drawRuneRing(Graphics2D g2, int cx, int cy, double sec, double charge) {
        g2.setColor(new Color(170, 120, 255, 60));
        g2.fill(new Ellipse2D.Double(cx - 330, cy - 165, 660, 330));

        drawLake(g2, cx, cy, sec, charge);

        float dp = (float) ((sec * 40) % 70);
        g2.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                                     new float[]{36f, 34f}, dp));
        g2.setColor(new Color(215, 175, 255, 190));
        g2.draw(new Ellipse2D.Double(cx - 300, cy - 150, 600, 300));

        float dp2 = (float) (40 - (sec * 30) % 40);
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                                     new float[]{14f, 26f}, dp2));
        g2.setColor(new Color(235, 210, 255, 170));
        g2.draw(new Ellipse2D.Double(cx - 210, cy - 105, 420, 210));
    }

    // ------------------------------ Hồ nước dưới quả cầu ------------------------------
    private void drawLake(Graphics2D g2, int cx, int cy, double sec, double charge) {
        if (lakeImg != null) {
            int w = LAKE_W;
            int h = (int) Math.round(lakeImg.getHeight() * (double) w / lakeImg.getWidth());
            g2.drawImage(lakeImg, cx - w / 2, cy - h / 2, w, h, null);
            if (LAKE_FX) drawLakeFx(g2, cx, cy, sec, charge);
            return;
        }

        // ---- Hình giữ chỗ: hồ tím có gợn sóng, phản chiếu quả cầu (thay bằng lake.png khi có ảnh) ----
        double rx = 190, ry = 95;   // vừa lọt lòng quảng trường 2x2 ô
        AffineTransform old = g2.getTransform();

        g2.setColor(new Color(112, 98, 160));      // bờ đá
        g2.fill(new Ellipse2D.Double(cx - rx - 16, cy - ry - 8, (rx + 16) * 2, (ry + 8) * 2));

        // vẽ trong không gian bị ép dẹt 2:1 để gradient thành hình elip
        g2.translate(cx, cy);
        g2.scale(1.0, ry / rx);
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(0, 0), (float) rx, new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(30, 22, 110), new Color(70, 52, 170), new Color(120, 100, 230)}));
        g2.fill(new Ellipse2D.Double(-rx, -rx, rx * 2, rx * 2));

        // phản chiếu quả cầu, sáng hẳn lên khi sét đánh trúng
        float refl = (float) Math.min(1.0, 0.45 + 0.1 * Math.sin(sec * 2.4) + 0.5 * charge);
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(0, 0), 130f, new float[]{0f, 1f},
                new Color[]{new Color(0.85f, 0.7f, 1f, refl), new Color(0.6f, 0.4f, 1f, 0f)}));
        g2.fill(new Ellipse2D.Double(-130, -130, 260, 260));

        // gợn sóng loang ra từ giữa hồ
        for (int k = 0; k < 3; k++) {
            double pr = frac(sec * 0.22 + k / 3.0);
            g2.setStroke(new BasicStroke(5f));
            g2.setColor(new Color(205, 185, 255, (int) (150 * (1 - pr))));
            g2.draw(new Ellipse2D.Double(-rx * pr, -rx * pr, rx * 2 * pr, rx * 2 * pr));
        }
        if (charge > 0.05) {
            g2.setStroke(new BasicStroke(9f));
            g2.setColor(new Color(255, 245, 255, (int) (230 * Math.min(1, charge))));
            g2.draw(new Ellipse2D.Double(-rx * 0.85, -rx * 0.85, rx * 1.7, rx * 1.7));
        }
        g2.setTransform(old);

        // vệt sáng lấp lánh trên mặt nước
        for (int k = 0; k < 7; k++) {
            double x = cx + (frac(k * 0.37 + sec * 0.04) - 0.5) * rx * 1.5;
            double y = cy + (frac(k * 0.61) - 0.5) * ry * 1.3;
            float a = (float) Math.max(0, 0.5 + 0.5 * Math.sin(sec * 3 + k * 1.9));
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(1f, 1f, 1f, a * 0.7f));
            g2.draw(new Line2D.Double(x - 22, y, x + 22, y));
        }

        g2.setStroke(new BasicStroke(5f));
        g2.setColor(new Color(190, 170, 245, 170));
        g2.draw(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
    }

    // ------------------------------ Vành hào quang kiểu sao Thổ ------------------------------
    private void drawHalo(Graphics2D g2, int cx, int cy, double sec, double flash, boolean back) {
        if (!SHOW_HALO_RING) return;
        double ccx = cx, ccy = cy + RING_DY;

        if (ringBackImg != null || ringFrontImg != null) {          // có ảnh thì dùng ảnh
            BufferedImage img = back ? ringBackImg : ringFrontImg;
            if (img != null) {
                int w = RING_IMG_W;
                int h = (int) Math.round(img.getHeight() * (double) w / img.getWidth());
                g2.drawImage(img, cx - w / 2, (int) ccy - h / 2, w, h, null);
            }
            return;
        }

        // Các dải xếp đồng tâm: {tỉ lệ bán kính, độ dày, r, g, b, alpha}; vành dẹt 2:1 như nhìn xiên
        double[][] bands = {
            {1.000, 80, 150, 100, 255, 38}, {0.975, 36, 190, 140, 255, 90}, {0.945, 12, 245, 230, 255, 160},
            {0.915, 58, 160, 110, 250, 60}, {0.885, 20, 215, 175, 255, 120}, {0.860, 8, 250, 240, 255, 140}
        };
        double boost = 1 + 0.9 * flash;                        // sét đánh thì vành sáng lên
        double start = back ? 0 : 180;                          // 0..180 = nửa trên (sau đảo), 180..360 = nửa dưới (trước đảo)
        for (double[] b : bands) {
            double a = RING_BASE * b[0], bb = a / 2;
            g2.setStroke(new BasicStroke((float) b[1], BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color((int) b[2], (int) b[3], (int) b[4], (int) Math.min(255, b[5] * boost)));
            g2.draw(new Arc2D.Double(ccx - a, ccy - bb, a * 2, bb * 2, start, 180, Arc2D.OPEN));
        }

        // bụi sáng trôi chậm dọc theo vành
        for (int i = 0; i < 70; i++) {
            double ang = frac(i * 0.618 + sec * (0.006 + 0.01 * frac(i * 0.37))) * Math.PI * 2;
            boolean front = Math.sin(ang) > 0;
            if (front == back) continue;
            double a = RING_BASE * (0.86 + 0.14 * frac(i * 0.377));
            double x = ccx + Math.cos(ang) * a, y = ccy + Math.sin(ang) * a / 2;
            float tw = (float) Math.max(0, Math.min(1, 0.45 + 0.4 * Math.sin(sec * 3 + i * 2.1)));
            double sz = 5 + (i % 4) * 2.5;
            g2.setColor(new Color(0.8f, 0.65f, 1f, tw * 0.5f));
            g2.fill(new Ellipse2D.Double(x - sz * 1.8, y - sz * 1.8, sz * 3.6, sz * 3.6));
            g2.setColor(new Color(1f, 0.95f, 1f, tw));
            g2.fill(new Ellipse2D.Double(x - sz / 2, y - sz / 2, sz, sz));
        }
    }

    // ------------------------------ Mây bao quanh đảo (phía ngoài) ------------------------------
    private void drawCloudCollar(Graphics2D g2, int cx, int cy, double sec, double flash, boolean back) {
        if (!SHOW_CLOUD_COLLAR) return;
        double W = 1440 + 260, H = 720 + 140;                   // đường viền hình thoi nới rộng ra ngoài đảo
        double[][] corners = {{0, -H}, {W, 0}, {0, H}, {-W, 0}};
        Color body = lerp(new Color(58, 42, 104), new Color(190, 150, 250), flash * 0.8);
        Color under = lerp(new Color(34, 22, 66), new Color(150, 110, 220), flash * 0.5);
        int n = 26;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
        for (int i = 0; i < n; i++) {
            double u = frac(i / (double) n + sec * 0.004) * 4;   // mây trôi rất chậm vòng quanh đảo
            int e = (int) u;
            double s = u - e;
            double[] a = corners[e % 4], b = corners[(e + 1) % 4];
            double px = cx + a[0] + (b[0] - a[0]) * s + Math.sin(sec * 0.6 + i * 1.7) * 30;
            double py = cy + a[1] + (b[1] - a[1]) * s + Math.cos(sec * 0.5 + i * 2.1) * 14;
            if ((py < cy + 40) != back) continue;
            double sc = 0.9 + 0.5 * frac(i * 0.618);
            g2.setColor(under);
            g2.fill(new Ellipse2D.Double(px - 210 * sc, py - 20 * sc, 420 * sc, 70 * sc));
            g2.setColor(body);
            g2.fill(new Ellipse2D.Double(px - 170 * sc, py - 70 * sc, 220 * sc, 100 * sc));
            g2.fill(new Ellipse2D.Double(px - 50 * sc, py - 100 * sc, 260 * sc, 120 * sc));
            g2.fill(new Ellipse2D.Double(px + 70 * sc, py - 55 * sc, 200 * sc, 90 * sc));
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ------------------------------ Cổng sấm sét (góc sau) ------------------------------
    private void drawGate(Graphics2D g2, double sec, double flash) {
        int x = ImageHelper.getIslandIsoX(ISLAND_ID, GATE_R, GATE_C);
        int y = ImageHelper.getIslandIsoY(ISLAND_ID, GATE_R, GATE_C) + PlayState.TILE_H / 4 - GATE_LIFT;

        if (gateImg != null) {
            int h = GATE_H;
            int w = (int) Math.round(gateImg.getWidth() * (double) h / gateImg.getHeight());
            double pcx = x + GATE_PORTAL_DX, pcy = y - h + h * GATE_PORTAL_Y;
            if (GATE_FX) drawGateGlow(g2, pcx, pcy, GATE_PORTAL_R, sec, flash);   // ánh sáng toả phía sau
            g2.drawImage(gateImg, x - w / 2, y - h, w, h, null);
            if (GATE_FX) drawGateFx(g2, pcx, pcy, GATE_PORTAL_R, sec, flash);     // xoáy + tia điện phủ lên mặt cổng
            return;
        }

        // ---- Hình giữ chỗ: vòng đá + cổng xoáy tím có tia điện (thay bằng gate.png khi có ảnh) ----
        double R = 210, ccx = x, ccy = y - 90 - R - 40;

        g2.setPaint(new RadialGradientPaint(new Point2D.Double(ccx, ccy), (float) (R * 1.9),
                new float[]{0f, 1f},
                new Color[]{new Color(170, 110, 255, Math.min(255, (int) (90 + 90 * flash))), new Color(170, 110, 255, 0)}));
        g2.fill(new Ellipse2D.Double(ccx - R * 1.9, ccy - R * 1.9, R * 3.8, R * 3.8));

        // chân đế
        g2.setColor(new Color(54, 42, 82));
        g2.fillPolygon(new Polygon(new int[]{x - 190, x + 190, x + 150, x - 150},
                                   new int[]{y, y, y - 60, y - 60}, 4));
        g2.setColor(new Color(78, 64, 112));
        g2.fillPolygon(new Polygon(new int[]{x, x + 190, x + 150, x},
                                   new int[]{y, y, y - 60, y - 60}, 4));
        g2.setColor(new Color(60, 48, 92));
        g2.fillRoundRect(x - 45, (int) (ccy + R) - 6, 90, (int) (y - 56 - (ccy + R)) + 6, 16, 16);

        // mặt cổng
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(ccx, ccy), (float) R,
                new float[]{0f, 0.45f, 1f},
                new Color[]{new Color(245, 225, 255), new Color(150, 80, 235), new Color(52, 20, 110)}));
        g2.fill(new Ellipse2D.Double(ccx - R, ccy - R, R * 2, R * 2));

        Shape oldClip = g2.getClip();
        g2.setClip(new Ellipse2D.Double(ccx - R, ccy - R, R * 2, R * 2));
        for (int k = 0; k < 4; k++) {
            double rad = R * (0.28 + 0.17 * k);
            double start = Math.toDegrees(sec * (2.0 - 0.3 * k)) + k * 50;
            g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(235, 215, 255, 150));
            g2.draw(new Arc2D.Double(ccx - rad, ccy - rad, rad * 2, rad * 2, start, 200, Arc2D.OPEN));
        }
        long tick = (long) (sec * 9);
        Random rnd = new Random(tick * 13 + 5);
        for (int k = 0; k < 3; k++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            Path2D z = zigzag(ccx, ccy, ccx + Math.cos(ang) * R * 0.95, ccy + Math.sin(ang) * R * 0.95, rnd, 7, 28);
            g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(170, 110, 255, 100));
            g2.draw(z);
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 245, 255));
            g2.draw(z);
        }
        g2.setClip(oldClip);

        // khung đá + hạt rune
        g2.setStroke(new BasicStroke(46f));
        g2.setColor(new Color(66, 54, 98));
        g2.draw(new Ellipse2D.Double(ccx - R - 10, ccy - R - 10, (R + 10) * 2, (R + 10) * 2));
        g2.setStroke(new BasicStroke(6f));
        g2.setColor(new Color(200, 150, 255, (int) (150 + 80 * Math.sin(sec * 3))));
        g2.draw(new Ellipse2D.Double(ccx - R + 2, ccy - R + 2, (R - 2) * 2, (R - 2) * 2));
        for (int k = 0; k < 16; k++) {
            double ang = k * Math.PI / 8;
            float a = (float) (0.4 + 0.4 * Math.sin(sec * 2.5 + k));
            g2.setColor(new Color(0.85f, 0.7f, 1f, Math.max(0f, Math.min(1f, a))));
            double rx = ccx + Math.cos(ang) * (R + 10), ry = ccy + Math.sin(ang) * (R + 10);
            g2.fill(new Ellipse2D.Double(rx - 7, ry - 7, 14, 14));
        }
    }

    // ------------------------------ Quả cầu lơ lửng ở trung tâm ------------------------------
    private void drawOrb(Graphics2D g2, int cx, int cy, double sec, double charge) {
        double bob = Math.sin(sec * 1.2) * 22;
        double ox = cx, oy = cy - ORB_LIFT + bob, R = 120;

        g2.setPaint(new RadialGradientPaint(new Point2D.Double(ox, oy), (float) (R * 2.4),
                new float[]{0f, 1f},
                new Color[]{new Color(190, 120, 255, Math.min(255, (int) (110 + 120 * charge))), new Color(190, 120, 255, 0)}));
        g2.fill(new Ellipse2D.Double(ox - R * 2.4, oy - R * 2.4, R * 4.8, R * 4.8));

        if (orbImg != null) {
            int w = ORB_W;
            int h = (int) Math.round(orbImg.getHeight() * (double) w / orbImg.getWidth());
            g2.drawImage(orbImg, (int) (ox - w / 2.0), (int) (oy - h / 2.0), w, h, null);
            if (ORB_FX) drawOrbFx(g2, ox, oy, ORB_FX_R, sec, charge);
            return;
        }

        // ---- Hình giữ chỗ: bong bóng tím có tia điện bên trong (thay bằng center_orb.png khi có ảnh) ----
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(ox, oy), (float) R,
                new Point2D.Double(ox - R * 0.35, oy - R * 0.4),
                new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(238, 222, 255, 215), new Color(150, 95, 235, 170), new Color(66, 28, 140, 215)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2.fill(new Ellipse2D.Double(ox - R, oy - R, R * 2, R * 2));

        Shape oldClip = g2.getClip();
        g2.setClip(new Ellipse2D.Double(ox - R, oy - R, R * 2, R * 2));
        Random rnd = new Random((long) (sec * 8) * 31 + 3);
        for (int k = 0; k < 3; k++) {
            double a1 = rnd.nextDouble() * Math.PI * 2, a2 = a1 + Math.PI * (0.7 + rnd.nextDouble() * 0.6);
            Path2D z = zigzag(ox + Math.cos(a1) * R * 0.9, oy + Math.sin(a1) * R * 0.9,
                              ox + Math.cos(a2) * R * 0.9, oy + Math.sin(a2) * R * 0.9, rnd, 6, 20);
            g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(190, 140, 255, 110));
            g2.draw(z);
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 245, 255, 230));
            g2.draw(z);
        }
        g2.setClip(oldClip);

        g2.setStroke(new BasicStroke(6f));
        g2.setColor(new Color(225, 200, 255, 200));
        g2.draw(new Ellipse2D.Double(ox - R, oy - R, R * 2, R * 2));
        g2.setColor(new Color(255, 255, 255, 175));
        g2.fill(new Ellipse2D.Double(ox - R * 0.62, oy - R * 0.72, R * 0.5, R * 0.28));

        for (int k = 0; k < 3; k++) {
            double ang = sec * 1.6 + k * 2.094;
            double ex = ox + Math.cos(ang) * R * 1.45, ey = oy + Math.sin(ang) * R * 0.55;
            g2.setColor(new Color(200, 150, 255, 90));
            g2.fill(new Ellipse2D.Double(ex - 16, ey - 16, 32, 32));
            g2.setColor(new Color(250, 235, 255));
            g2.fill(new Ellipse2D.Double(ex - 7, ey - 7, 14, 14));
        }
    }

    // =========================================================================
    // HIỆU ỨNG ĐỘNG PHỦ LÊN ẢNH (dùng khi đã có gate.png / lake.png / center_orb.png)
    // =========================================================================
    private void drawDebugCircle(Graphics2D g2, double cx, double cy, double rx, double ry) {
        if (!FX_DEBUG) return;
        g2.setStroke(new BasicStroke(5f));
        g2.setColor(new Color(255, 40, 40, 220));
        g2.draw(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
        g2.draw(new Line2D.Double(cx - 20, cy, cx + 20, cy));
        g2.draw(new Line2D.Double(cx, cy - 20, cx, cy + 20));
    }

    /** Quầng sáng tím phía sau cổng, nhấp nhô nhẹ và sáng lên khi sét đánh */
    private void drawGateGlow(Graphics2D g2, double ccx, double ccy, double R, double sec, double flash) {
        int a = (int) Math.max(0, Math.min(255, 90 + 90 * flash + 25 * Math.sin(sec * 2.0)));
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(ccx, ccy), (float) (R * 1.9), new float[]{0f, 1f},
                new Color[]{new Color(170, 110, 255, a), new Color(170, 110, 255, 0)}));
        g2.fill(new Ellipse2D.Double(ccx - R * 1.9, ccy - R * 1.9, R * 3.8, R * 3.8));
    }

    /** Vòng xoáy + tia điện chạy trong mặt cổng, hạt rune nhấp nháy quanh viền */
    private void drawGateFx(Graphics2D g2, double ccx, double ccy, double R, double sec, double flash) {
        Shape oldClip = g2.getClip();
        g2.setClip(new Ellipse2D.Double(ccx - R, ccy - R, R * 2, R * 2));

        if (flash > 0.02) {   // sét đánh thì mặt cổng loé sáng
            g2.setPaint(new RadialGradientPaint(new Point2D.Double(ccx, ccy), (float) R, new float[]{0f, 1f},
                    new Color[]{new Color(245, 225, 255, (int) Math.min(255, 150 * flash)), new Color(170, 110, 255, 0)}));
            g2.fill(new Ellipse2D.Double(ccx - R, ccy - R, R * 2, R * 2));
        }
        for (int k = 0; k < 4; k++) {
            double rad = R * (0.28 + 0.17 * k);
            double start = Math.toDegrees(sec * (2.0 - 0.3 * k)) + k * 50;
            g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(235, 215, 255, 110));
            g2.draw(new Arc2D.Double(ccx - rad, ccy - rad, rad * 2, rad * 2, start, 200, Arc2D.OPEN));
        }
        long tick = (long) (sec * 9);
        Random rnd = new Random(tick * 13 + 5);
        for (int k = 0; k < 3; k++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            Path2D z = zigzag(ccx, ccy, ccx + Math.cos(ang) * R * 0.95, ccy + Math.sin(ang) * R * 0.95, rnd, 7, 28);
            g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(170, 110, 255, 100));
            g2.draw(z);
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 245, 255));
            g2.draw(z);
        }
        g2.setClip(oldClip);

        g2.setStroke(new BasicStroke(6f));
        g2.setColor(new Color(200, 150, 255, (int) (150 + 80 * Math.sin(sec * 3))));
        g2.draw(new Ellipse2D.Double(ccx - R, ccy - R, R * 2, R * 2));
        if (GATE_RUNES) {
            for (int k = 0; k < 16; k++) {
                double ang = k * Math.PI / 8;
                float a = (float) (0.4 + 0.4 * Math.sin(sec * 2.5 + k));
                g2.setColor(new Color(0.85f, 0.7f, 1f, Math.max(0f, Math.min(1f, a))));
                double rx = ccx + Math.cos(ang) * (R + 10), ry = ccy + Math.sin(ang) * (R + 10);
                g2.fill(new Ellipse2D.Double(rx - 7, ry - 7, 14, 14));
            }
        }
        drawDebugCircle(g2, ccx, ccy, R, R);
    }

    /** Gợn sóng, ánh nước lấp lánh, phản chiếu quả cầu (sáng lên khi sét đánh vào quả cầu) trên mặt hồ */
    private void drawLakeFx(Graphics2D g2, int cx, int cy, double sec, double charge) {
        double rx = LAKE_FX_RX, ry = rx / 2;
        Shape oldClip = g2.getClip();
        Stroke oldStroke = g2.getStroke();
        g2.setClip(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));

        float refl = (float) Math.min(1.0, 0.25 + 0.08 * Math.sin(sec * 2.4) + 0.6 * charge);
        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.scale(1.0, 0.5);
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(0, 0), (float) (rx * 0.75), new float[]{0f, 1f},
                new Color[]{new Color(0.85f, 0.7f, 1f, refl), new Color(0.6f, 0.4f, 1f, 0f)}));
        g2.fill(new Ellipse2D.Double(-rx, -rx, rx * 2, rx * 2));
        for (int k = 0; k < 3; k++) {     // gợn sóng loang ra từ giữa hồ
            double pr = frac(sec * 0.22 + k / 3.0);
            g2.setStroke(new BasicStroke(5f));
            g2.setColor(new Color(205, 185, 255, (int) (130 * (1 - pr))));
            g2.draw(new Ellipse2D.Double(-rx * pr, -rx * pr, rx * 2 * pr, rx * 2 * pr));
        }
        if (charge > 0.05) {
            g2.setStroke(new BasicStroke(9f));
            g2.setColor(new Color(255, 245, 255, (int) (230 * Math.min(1, charge))));
            g2.draw(new Ellipse2D.Double(-rx * 0.85, -rx * 0.85, rx * 1.7, rx * 1.7));
        }
        g2.setTransform(old);

        for (int k = 0; k < 7; k++) {     // vệt sáng lấp lánh
            double x = cx + (frac(k * 0.37 + sec * 0.04) - 0.5) * rx * 1.5;
            double y = cy + (frac(k * 0.61) - 0.5) * ry * 1.3;
            float a = (float) Math.max(0, 0.5 + 0.5 * Math.sin(sec * 3 + k * 1.9));
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(1f, 1f, 1f, a * 0.7f));
            g2.draw(new Line2D.Double(x - 22, y, x + 22, y));
        }
        g2.setClip(oldClip);
        g2.setStroke(oldStroke);
        drawDebugCircle(g2, cx, cy, rx, ry);
    }

    /** Tia điện chạy trong quả cầu, nhịp phát sáng, vệt tia quay quanh; chớp trắng khi sét đánh vào quả cầu */
    private void drawOrbFx(Graphics2D g2, double ox, double oy, double R, double sec, double charge) {
        Shape oldClip = g2.getClip();
        g2.setClip(new Ellipse2D.Double(ox - R, oy - R, R * 2, R * 2));

        float pulse = (float) (0.5 + 0.5 * Math.sin(sec * 2.2));
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(ox, oy), (float) R, new float[]{0f, 1f},
                new Color[]{new Color(1f, 0.92f, 1f, Math.min(1f, 0.10f + 0.12f * pulse + 0.6f * (float) charge)),
                            new Color(0.7f, 0.45f, 1f, 0f)}));
        g2.fill(new Ellipse2D.Double(ox - R, oy - R, R * 2, R * 2));

        Random rnd = new Random((long) (sec * 8) * 31 + 3);
        for (int k = 0; k < 3; k++) {
            double a1 = rnd.nextDouble() * Math.PI * 2, a2 = a1 + Math.PI * (0.7 + rnd.nextDouble() * 0.6);
            Path2D z = zigzag(ox + Math.cos(a1) * R * 0.9, oy + Math.sin(a1) * R * 0.9,
                              ox + Math.cos(a2) * R * 0.9, oy + Math.sin(a2) * R * 0.9, rnd, 6, 20);
            g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(190, 140, 255, 110));
            g2.draw(z);
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 245, 255, 230));
            g2.draw(z);
        }
        g2.setClip(oldClip);

        g2.setStroke(new BasicStroke(6f));
        g2.setColor(new Color(225, 200, 255, (int) (70 + 60 * pulse + 110 * Math.min(1, charge))));
        g2.draw(new Ellipse2D.Double(ox - R, oy - R, R * 2, R * 2));

        for (int k = 0; k < 3; k++) {     // 3 hạt sáng bay vòng quanh
            double ang = sec * 1.6 + k * 2.094;
            double ex = ox + Math.cos(ang) * R * 1.45, ey = oy + Math.sin(ang) * R * 0.55;
            g2.setColor(new Color(200, 150, 255, 90));
            g2.fill(new Ellipse2D.Double(ex - 16, ey - 16, 32, 32));
            g2.setColor(new Color(250, 235, 255));
            g2.fill(new Ellipse2D.Double(ex - 7, ey - 7, 14, 14));
        }
        drawDebugCircle(g2, ox, oy, R, R);
    }

    // ------------------------------ Mây bão ------------------------------
    private void drawStormClouds(Graphics2D g2, int cx, int cy, double sec, double flash) {
        // {pha, độ lệch dọc so với tâm đảo, tốc độ, kích cỡ, độ đậm}
        double[][] clouds = {
            {0.00, -1850, 0.012, 1.3, 0.85}, {0.22, -1700, 0.009, 1.6, 0.85},
            {0.45, -1950, 0.014, 1.2, 0.85}, {0.68, -1780, 0.010, 1.5, 0.85},
            {0.85, -1900, 0.011, 1.1, 0.85}, {0.10, -1250, 0.016, 1.0, 0.55},
            {0.55,   900, 0.013, 1.0, 0.35}, {0.30,  1000, 0.011, 0.9, 0.35},
            {0.80,   820, 0.015, 1.1, 0.35}
        };
        double range = 4200, xStart = cx - 2600;
        Color dark = new Color(48, 32, 86), lit = new Color(176, 136, 244);
        Color body = lerp(dark, lit, flash * 0.8);
        Color under = lerp(new Color(30, 18, 58), lit, flash * 0.5);
        for (double[] cl : clouds) {
            double p = frac(cl[0] + sec * cl[2]);
            double x = xStart + p * range, y = cy + cl[1], s = cl[3];
            float a = (float) (cl[4] * Math.min(1.0, Math.min(p, 1 - p) * 6));
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

    // ------------------------------ Sét ------------------------------
    private static double flashIntensity(double t) {
        if (t < 0.07) return 1.0;
        if (t < 0.13) return 0.2;
        if (t < 0.25) return 0.9;
        if (t < 0.55) return 0.55 * (1 - (t - 0.25) / 0.30);
        return 0;
    }

    private List<double[]> jaggedPoints(double x0, double y0, double x1, double y1, Random rnd, int depth, double disp) {
        List<double[]> pts = new ArrayList<>();
        pts.add(new double[]{x0, y0});
        pts.add(new double[]{x1, y1});
        for (int d = 0; d < depth; d++) {
            List<double[]> next = new ArrayList<>();
            for (int i = 0; i < pts.size() - 1; i++) {
                double[] a = pts.get(i), b = pts.get(i + 1);
                double dx = b[0] - a[0], dy = b[1] - a[1], len = Math.max(1, Math.hypot(dx, dy));
                double off = (rnd.nextDouble() - 0.5) * len * disp;
                next.add(a);
                next.add(new double[]{(a[0] + b[0]) / 2 + (-dy / len) * off, (a[1] + b[1]) / 2 + (dx / len) * off});
            }
            next.add(pts.get(pts.size() - 1));
            pts = next;
        }
        return pts;
    }

    private Path2D toPath(List<double[]> pts) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(pts.get(0)[0], pts.get(0)[1]);
        for (int i = 1; i < pts.size(); i++) p.lineTo(pts.get(i)[0], pts.get(i)[1]);
        return p;
    }

    private Path2D zigzag(double x0, double y0, double x1, double y1, Random rnd, int segs, double amp) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x0, y0);
        double dx = x1 - x0, dy = y1 - y0, len = Math.max(1, Math.hypot(dx, dy));
        double nx = -dy / len, ny = dx / len;
        for (int k = 1; k < segs; k++) {
            double t = (double) k / segs, off = (rnd.nextDouble() - 0.5) * 2 * amp;
            p.lineTo(x0 + dx * t + nx * off, y0 + dy * t + ny * off);
        }
        p.lineTo(x1, y1);
        return p;
    }

    /** Mỗi "kênh" sét tạo lại tia mới khi sang nhịp mới (dựa vào hạt giống nên ổn định) */
    private void ensureBolt(int i, long slot, int cx, int cy) {
        if (boltSlot[i] == slot) return;
        boltSlot[i] = slot;
        Random rnd = new Random(slot * 7919L + i * 104729L + 17);

        double tx, ty, type;
        double roll = rnd.nextDouble();
        if (roll < 0.35) {
            tx = cx; ty = cy - ORB_LIFT; type = 1;
        } else {
            tx = cx + (rnd.nextDouble() - 0.5) * 3600; ty = cy + (rnd.nextDouble() - 0.3) * 1000; type = 2;
        }
        boltTarget[i][0] = tx; boltTarget[i][1] = ty; boltTarget[i][2] = type;

        double sx = tx + (rnd.nextDouble() - 0.5) * 700, sy = cy - 1750 + rnd.nextDouble() * 100;
        List<double[]> pts = jaggedPoints(sx, sy, tx, ty, rnd, 6, 0.45);
        boltMain[i] = toPath(pts);

        Path2D.Double br = new Path2D.Double();
        for (int k = 0; k < 2; k++) {
            int idx = (int) (pts.size() * (0.25 + 0.45 * rnd.nextDouble()));
            double[] s0 = pts.get(idx);
            double ex = s0[0] + (rnd.nextDouble() - 0.5) * 800, ey = s0[1] + 300 + rnd.nextDouble() * 350;
            br.append(toPath(jaggedPoints(s0[0], s0[1], ex, ey, rnd, 4, 0.5)), false);
        }
        boltBranch[i] = br;
    }

    private void drawBolts(Graphics2D g2, double[] inten) {
        for (int i = 0; i < 3; i++) {
            double a = inten[i];
            if (a <= 0.01 || boltMain[i] == null) continue;

            g2.setStroke(new BasicStroke(46f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(150, 90, 255, (int) (60 * a)));
            g2.draw(boltMain[i]);
            g2.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(190, 140, 255, (int) (150 * a)));
            g2.draw(boltMain[i]);
            g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(boltBranch[i]);
            g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(245, 235, 255, (int) (255 * a)));
            g2.draw(boltMain[i]);
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(boltBranch[i]);

            double tx = boltTarget[i][0], ty = boltTarget[i][1];
            g2.setPaint(new RadialGradientPaint(new Point2D.Double(tx, ty), 220f, new float[]{0f, 1f},
                    new Color[]{new Color(235, 210, 255, (int) (190 * a)), new Color(150, 90, 255, 0)}));
            g2.fill(new Ellipse2D.Double(tx - 220, ty - 220, 440, 440));
        }
    }

    /** Ánh chớp sáng lên cả vùng đảo mỗi khi có sét */
    private void drawFlash(Graphics2D g2, int cx, int cy, double flash) {
        if (flash <= 0.02) return;
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy - 300), 2600f, new float[]{0f, 1f},
                new Color[]{new Color(215, 180, 255, (int) (70 * flash)), new Color(120, 60, 220, 0)}));
        g2.fill(new Ellipse2D.Double(cx - 2600, cy - 2900, 5200, 5200));
    }

    /** Hạt điện lấp lánh bay lên từ mặt đảo */
    private void drawMotes(Graphics2D g2, int cx, int cy, double sec) {
        for (int i = 0; i < 40; i++) {
            double sp = 0.03 + 0.04 * frac(i * 0.37);
            double p = frac(i * 0.618 + sec * sp);
            double x = cx + (frac(i * 0.754) - 0.5) * 3000 + Math.sin(sec * 1.3 + i) * 40;
            double y = cy + 700 - p * 1900;
            double tw = 0.35 + 0.35 * Math.sin(sec * 5 + i * 2.3);
            float a = (float) Math.max(0, Math.min(1, (tw + 0.2) * Math.min(1.0, Math.min(p, 1 - p) * 6)));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            double sz = 10 + (i % 3) * 5;
            g2.setColor(new Color(170, 110, 255, 110));
            g2.fill(new Ellipse2D.Double(x - sz, y - sz, sz * 2, sz * 2));
            g2.setColor(i % 4 == 0 ? new Color(250, 240, 255) : new Color(205, 165, 255));
            g2.fill(new Ellipse2D.Double(x - sz / 2, y - sz / 2, sz, sz));
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
}