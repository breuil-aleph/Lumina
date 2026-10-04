package map;

import state.PlayState;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ĐẢO 3 - THÁNH ĐỊA CỦA GIÓ
 * Đồng cỏ tươi mát, rừng dày ở vành đảo, suối chảy ra mép đảo, Tháp Gió (vẽ bằng code), cối xay gió,
 * vòng đá thiêng, lối mòn nét đứt, cột đá lơ lửng, mây trôi và lá bay.
 *
 * Mọi thứ nằm trong file này, không cần ảnh (tự vẽ bằng Graphics2D).
 * Ảnh tuỳ chọn trong res/map3/:
 *   map3_island.png   - ảnh nền cả đảo (có ảnh thì dùng thay cho cỏ tự vẽ)
 *   deco_tree.png, deco_windmill.png - thay cây / cối xay tự vẽ
 *   tancy.png         - ảnh tán cây nhỏ dùng cho rừng vành ngoài
 *   plant_lv1/2/3.png - cây người chơi trồng (Plant.java nạp), nên vẽ màu xanh
 */
public class EarthArea extends MapArea {
    public static final int ISLAND_ID = 3;
    public static final int SIZE = PlayState.ISLAND_SIZE;

    public static final int MAP3_WIDTH  = SIZE * PlayState.TILE_W;
    public static final int MAP3_HEIGHT = SIZE * PlayState.TILE_H;

    private static final int TREE = 0, WINDMILL = 1, PILLAR = 2, SPIRAL = 3, TOWER = 4, CIRCLE = 5;

    // Lề của ảnh đệm rừng (tán cây vẽ lòi ra ngoài mép hình thoi một chút)
    private static final int PAD_X = 60, PAD_TOP = 220, PAD_BOTTOM = 80;

    private BufferedImage islandBgImg, treeImg, windmillImg, tancyImg;
    private BufferedImage forestLayer;   // rừng vẽ sẵn 1 lần
    private Path2D streamPath, trailPath;

    // {loại, hàng, cột}, sắp theo r + c để vẽ đúng thứ tự trước/sau
    private final List<int[]> decor = new ArrayList<>();
    private final int[][][] tufts = new int[SIZE][SIZE][6];

    public EarthArea() {
        super(false);
        loadImages();
        buildDecorLayout();
        buildTufts();
        buildPaths();
        buildForestLayer();
    }

    @Override
    public void loadImages() {
        islandBgImg = ImageHelper.loadImage("res/map3/map3_island.png");
        if (islandBgImg == null) islandBgImg = ImageHelper.loadImage("res/map3_earth/map3_island.png");
        
        treeImg = ImageHelper.loadImage("res/map3/deco_tree.png");
        windmillImg = ImageHelper.loadImage("res/map3/deco_windmill.png");
        
        // Nạp ảnh tán cây (tancy.png) cho vành rừng ngoài
        tancyImg = ImageHelper.loadImage("res/map3/tancy.png");
        if (tancyImg == null) {
            tancyImg = ImageHelper.loadImage("res/map3_earth/tancy.png");
        }
    }

    // =========================================================================
    // BỐ CỤC (muốn dời vật nào chỉ cần sửa toạ độ hàng, cột ở đây)
    // =========================================================================
    private void buildDecorLayout() {
        decor.add(new int[]{WINDMILL, 0, 0});          // góc sau
        decor.add(new int[]{TOWER, 1, 10});            // Tháp Gió (vẽ bằng code)
        decor.add(new int[]{PILLAR, 0, 11});           // cột đá lơ lửng
        decor.add(new int[]{PILLAR, 11, 0});
        decor.add(new int[]{SPIRAL, 11, 11});          // xoáy gió ở góc trước
        decor.add(new int[]{CIRCLE, 8, 3});            // vòng đá thiêng

        int[][] trees = {{1, 6}, {3, 1}, {9, 1}, {9, 7}};   // vài cây lẻ đung đưa
        for (int[] t : trees) decor.add(new int[]{TREE, t[0], t[1]});

        decor.sort((a, b) -> (a[1] + a[2]) - (b[1] + b[2]));
    }

    /** Ô nào là rừng: vành ngoài cùng (trừ các góc và chỗ suối chảy ra) + vài mảng bên trong. */
    private boolean isForestTile(int r, int c) {
        int[][] patches = {{1, 3}, {1, 4}, {2, 2}, {5, 1}, {6, 1}, {10, 7}, {10, 8}};
        for (int[] p : patches) if (p[0] == r && p[1] == c) return true;

        boolean rim = r == 0 || c == 0 || r == SIZE - 1 || c == SIZE - 1;
        if (!rim) return false;
        int[][] corners = {{0, 0}, {0, SIZE - 1}, {SIZE - 1, 0}, {SIZE - 1, SIZE - 1}};
        for (int[] k : corners) if (Math.abs(r - k[0]) <= 1 && Math.abs(c - k[1]) <= 1) return false;
        if (c == SIZE - 1 && r >= 8 && r <= 10) return false;   // chừa chỗ suối chảy ra
        return true;
    }

    private void buildTufts() {
        Random rnd = new Random(33);
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                for (int k = 0; k < 3; k++) {
                    tufts[r][c][k * 2]     = rnd.nextInt(121) - 60;
                    tufts[r][c][k * 2 + 1] = rnd.nextInt(31) - 15;
                }
    }

    private double isoX(double r, double c) {
        return ImageHelper.getIslandCenterX(ISLAND_ID) + (c - r) * (PlayState.TILE_W / 2.0);
    }
    private double isoY(double r, double c) {
        return ImageHelper.getIslandCenterY(ISLAND_ID) + (c + r - (SIZE - 1)) * (PlayState.TILE_H / 2.0);
    }

    private Path2D smoothPath(double[][] rc) {
        int n = rc.length;
        double[] px = new double[n], py = new double[n];
        for (int i = 0; i < n; i++) { px[i] = isoX(rc[i][0], rc[i][1]); py[i] = isoY(rc[i][0], rc[i][1]); }
        Path2D.Double p = new Path2D.Double();
        p.moveTo(px[0], py[0]);
        for (int i = 1; i < n - 1; i++)
            p.quadTo(px[i], py[i], (px[i] + px[i + 1]) / 2, (py[i] + py[i + 1]) / 2);
        p.lineTo(px[n - 1], py[n - 1]);
        return p;
    }

    private void buildPaths() {
        streamPath = smoothPath(new double[][]{
            {1.2, 10.0}, {2.0, 8.6}, {3.4, 7.2}, {4.6, 7.8}, {5.2, 9.2}, {6.8, 9.8}, {8.2, 10.6}, {9.4, 11.45}
        });
        trailPath = smoothPath(new double[][]{
            {1.8, 1.8}, {3.2, 3.0}, {4.6, 3.6}, {6.0, 3.2}, {7.0, 3.0}
        });
    }

    private void buildForestLayer() {
        int w = MAP3_WIDTH + PAD_X * 2;
        int h = MAP3_HEIGHT + PAD_TOP + PAD_BOTTOM;
        forestLayer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = forestLayer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double ox = -w / 2.0, oy = -MAP3_HEIGHT / 2.0 - PAD_TOP;
        Random rnd = new Random(7);
        List<double[]> crowns = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!isForestTile(r, c)) continue;
                double tx = (c - r) * (PlayState.TILE_W / 2.0);
                double ty = (c + r - (SIZE - 1)) * (PlayState.TILE_H / 2.0);
                for (int k = 0; k < 16; k++) {
                    double a = rnd.nextDouble() - 0.5, b = rnd.nextDouble() - 0.5;
                    double x = tx + (b - a) * (PlayState.TILE_W / 2.0);
                    double y = ty + (a + b) * (PlayState.TILE_H / 2.0);
                    crowns.add(new double[]{x, y, 30 + rnd.nextDouble() * 16, rnd.nextDouble()});
                }
            }
        }
        crowns.sort((p, q) -> Double.compare(p[1], q[1]));

        for (double[] cr : crowns) {
            double x = cr[0] - ox, y = cr[1] - oy - 24, rad = cr[2], t = cr[3];
            g.setColor(new Color(0, 0, 0, 55));
            g.fill(new Ellipse2D.Double(x - rad, y + rad * 0.55, rad * 2, rad * 0.8));

            if (tancyImg != null) {
                int drawWidth = (int) (rad * 2.2);
                int drawHeight = (int) (rad * 2.2);
                g.drawImage(tancyImg, (int) x - drawWidth / 2, (int) y - drawHeight / 2, drawWidth, drawHeight, null);
            } else {
                int gr = (int) (105 + t * 45);
                g.setColor(new Color(28, (int) (gr * 0.78), 55));
                g.fill(new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2));
                g.setColor(new Color(40, gr, 62));
                g.fill(new Ellipse2D.Double(x - rad * 0.9, y - rad * 1.0, rad * 1.7, rad * 1.7));
                g.setColor(new Color(95, Math.min(235, gr + 70), 90));
                g.fill(new Ellipse2D.Double(x - rad * 0.65, y - rad * 0.85, rad * 0.9, rad * 0.8));
            }
        }
        g.dispose();
    }

    @Override
    public void drawTiles(Graphics2D g2) {
        int centerX = ImageHelper.getIslandCenterX(ISLAND_ID);
        int centerY = ImageHelper.getIslandCenterY(ISLAND_ID);

        if (islandBgImg != null) {
            ImageHelper.drawAreaBackground(g2, islandBgImg, centerX, centerY, MAP3_WIDTH, MAP3_HEIGHT);
            return;
        }

        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Color base = ((r + c) % 2 == 0) ? new Color(104, 198, 112) : new Color(92, 186, 104);
                ImageHelper.drawIslandTile(g2, null, ISLAND_ID, r, c, base);

                int x = ImageHelper.getIslandIsoX(ISLAND_ID, r, c);
                int y = ImageHelper.getIslandIsoY(ISLAND_ID, r, c);
                g2.setColor(new Color(56, 150, 84));
                for (int k = 0; k < 3; k++) {
                    int gx = x + tufts[r][c][k * 2];
                    int gy = y + tufts[r][c][k * 2 + 1];
                    g2.drawLine(gx, gy, gx - 7, gy - 17);
                    g2.drawLine(gx, gy, gx, gy - 22);
                    g2.drawLine(gx, gy, gx + 7, gy - 17);
                }
            }
        }
        g2.setStroke(oldStroke);
    }

    @Override
    public void drawObjects(Graphics2D g2) {
        double sec = System.currentTimeMillis() / 1000.0;
        int cx = ImageHelper.getIslandCenterX(ISLAND_ID);
        int cy = ImageHelper.getIslandCenterY(ISLAND_ID);

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Composite oldComp = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawClouds(g2, cx, cy, sec);

        g2.drawImage(forestLayer, cx - forestLayer.getWidth() / 2,
                     cy - MAP3_HEIGHT / 2 - PAD_TOP, null);

        drawStream(g2, sec);
        drawWaterfall(g2, sec);
        drawTrail(g2);

        for (int[] d : decor) {
            int r = d[1], c = d[2];
            int x = ImageHelper.getIslandIsoX(ISLAND_ID, r, c);
            int y = ImageHelper.getIslandIsoY(ISLAND_ID, r, c);
            switch (d[0]) {
                case TREE -> {
                    if (treeImg != null) ImageHelper.drawIslandObject(g2, treeImg, ISLAND_ID, r, c, 300, 400);
                    else drawTree(g2, x, y, sec, r, c);
                }
                case WINDMILL -> {
                    if (windmillImg != null) ImageHelper.drawIslandObject(g2, windmillImg, ISLAND_ID, r, c, 260, 420);
                    else drawWindmill(g2, x, y, sec);
                }
                case TOWER -> drawTower(g2, x, y, sec);
                case PILLAR -> drawPillar(g2, x, y, sec, r * 7 + c);
                case SPIRAL -> drawSpiral(g2, x, y, sec);
                case CIRCLE -> drawStoneCircle(g2, x, y, sec);
                default -> { }
            }
        }

        drawLeaves(g2, cx, cy, sec);

        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    private void blob(Graphics2D g2, double x, double y, double rad, Color col) {
        g2.setColor(col);
        g2.fill(new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2));
    }

    private void drawStream(Graphics2D g2, double sec) {
        g2.setStroke(new BasicStroke(78f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(66, 140, 74));
        g2.draw(streamPath);
        g2.setStroke(new BasicStroke(58f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(70, 150, 205));
        g2.draw(streamPath);
        g2.setStroke(new BasicStroke(31f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(120, 200, 238));
        g2.draw(streamPath);

        float phase = (float) (110 - (sec * 70) % 110);
        g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f,
                                     new float[]{30f, 80f}, phase));
        g2.setColor(new Color(255, 255, 255, 170));
        g2.draw(streamPath);
    }

    private void drawWaterfall(Graphics2D g2, double sec) {
        double ex = isoX(9.4, 11.45), ey = isoY(9.4, 11.45);
        g2.setPaint(new java.awt.GradientPaint((float) ex, (float) ey, new Color(120, 200, 238, 210),
                                               (float) ex, (float) (ey + 190), new Color(200, 235, 255, 0)));
        g2.fillRoundRect((int) ex - 30, (int) ey - 6, 60, 196, 24, 24);

        float phase = (float) (60 - (sec * 90) % 60);
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f,
                                     new float[]{26f, 34f}, phase));
        g2.setColor(new Color(255, 255, 255, 170));
        for (int i = 0; i < 3; i++) {
            double lx = ex - 18 + i * 18;
            g2.draw(new Line2D.Double(lx, ey, lx, ey + 180));
        }
    }

    private void drawTrail(Graphics2D g2) {
        g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f,
                                     new float[]{22f, 24f}, 0f));
        g2.setColor(new Color(248, 243, 218, 215));
        g2.draw(trailPath);
    }

    private void drawTree(Graphics2D g2, int x, int y, double sec, int r, int c) {
        double sway = Math.sin(sec * 1.5 + r * 0.7 + c * 0.5) * 14;

        g2.setColor(new Color(0, 0, 0, 50));
        g2.fill(new Ellipse2D.Double(x - 70, y - 12, 140, 40));
        g2.setStroke(new BasicStroke(22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(110, 80, 50));
        g2.draw(new Line2D.Double(x, y, x + sway * 0.4, y - 110));

        double tx = x + sway, ty = y - 150;
        blob(g2, tx - 55, ty + 25, 62, new Color(46, 150, 80));
        blob(g2, tx + 55, ty + 25, 62, new Color(46, 150, 80));
        blob(g2, tx, ty, 82, new Color(70, 185, 100));
        blob(g2, tx - 20, ty - 25, 40, new Color(130, 220, 130));
    }

    private void drawWindmill(Graphics2D g2, int x, int y, double sec) {
        int h = 300;
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fill(new Ellipse2D.Double(x - 90, y - 15, 180, 50));

        Polygon tower = new Polygon(new int[]{x - 62, x + 62, x + 38, x - 38},
                                    new int[]{y, y, y - h, y - h}, 4);
        g2.setColor(new Color(236, 232, 214));
        g2.fillPolygon(tower);
        g2.setColor(new Color(200, 196, 176));
        g2.fillPolygon(new Polygon(new int[]{x, x + 62, x + 38, x},
                                   new int[]{y, y, y - h, y - h}, 4));
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(120, 110, 90));
        g2.drawPolygon(tower);

        g2.setColor(new Color(90, 140, 100));
        g2.fillRoundRect(x - 16, y - 70, 32, 70, 16, 16);

        Polygon roof = new Polygon(new int[]{x - 52, x + 52, x},
                                   new int[]{y - h + 4, y - h + 4, y - h - 70}, 3);
        g2.setColor(new Color(60, 160, 110));
        g2.fillPolygon(roof);

        int hx = x, hy = y - h + 10;
        AffineTransform old = g2.getTransform();
        g2.rotate(sec * 1.1, hx, hy);
        for (int k = 0; k < 4; k++) {
            g2.rotate(Math.PI / 2, hx, hy);
            g2.setColor(new Color(150, 110, 70));
            g2.fillRect(hx - 6, hy - 190, 12, 190);
            g2.setColor(new Color(245, 245, 235, 230));
            g2.fillRect(hx + 6, hy - 185, 44, 110);
            g2.setColor(new Color(120, 90, 60));
            g2.drawRect(hx + 6, hy - 185, 44, 110);
        }
        g2.setTransform(old);
        blob(g2, hx, hy, 15, new Color(110, 80, 50));
    }

    /** Tháp Gió: Vẽ hoàn toàn bằng Graphics2D (có cờ phấp phới và vòng gió) */
    private void drawTower(Graphics2D g2, int x, int y, double sec) {
        int h = 350;
        // 1. Bóng đổ dưới chân tháp
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fill(new Ellipse2D.Double(x - 80, y - 15, 160, 45));

        // 2. Thân tháp (hình thang cân)
        Polygon tower = new Polygon(
            new int[]{x - 55, x + 55, x + 35, x - 35},
            new int[]{y, y, y - h, y - h},
            4
        );
        g2.setColor(new Color(225, 220, 200));
        g2.fillPolygon(tower);
        g2.setColor(new Color(190, 185, 165));
        g2.fillPolygon(new Polygon(
            new int[]{x, x + 55, x + 35, x},
            new int[]{y, y, y - h, y - h},
            4
        ));
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(110, 100, 80));
        g2.drawPolygon(tower);

        // Cửa vòm ở gốc tháp
        g2.setColor(new Color(80, 120, 90));
        g2.fillRoundRect(x - 14, y - 60, 28, 60, 14, 14);

        // Mái vòm / đỉnh tháp
        Polygon roof = new Polygon(
            new int[]{x - 45, x + 45, x},
            new int[]{y - h + 2, y - h + 2, y - h - 60},
            3
        );
        g2.setColor(new Color(70, 170, 120));
        g2.fillPolygon(roof);
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(new Color(50, 130, 90));
        g2.drawPolygon(roof);

        // 3. Cán cờ và cờ phấp phới ở đỉnh
        int flagX = x;
        int flagY = y - h - 60;
        g2.setStroke(new BasicStroke(4f));
        g2.setColor(new Color(110, 80, 50));
        g2.drawLine(flagX, flagY, flagX, flagY - 50);

        double wave = Math.sin(sec * 6) * 8;
        Polygon flag = new Polygon(
            new int[]{flagX, flagX + 45, (int)(flagX + 45 + wave), flagX},
            new int[]{flagY - 50, flagY - 45, (int)(flagY - 25 + wave * 0.3), flagY - 20},
            4
        );
        g2.setColor(new Color(130, 225, 170));
        g2.fillPolygon(flag);

        // 4. Hiệu ứng vòng gió xoáy quanh tháp
        drawSpiral(g2, x, y - 160, sec);
    }

    private void drawStoneCircle(Graphics2D g2, int x0, int y0, double sec) {
        double rx = 175, ry = 88;
        g2.setColor(new Color(205, 228, 160, 150));
        g2.fill(new Ellipse2D.Double(x0 - rx, y0 - ry, rx * 2, ry * 2));
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(150, 190, 110, 170));
        g2.draw(new Arc2D.Double(x0 - rx * 0.6, y0 - ry * 0.6, rx * 1.2, ry * 1.2, 20, 250, Arc2D.OPEN));
        g2.draw(new Arc2D.Double(x0 - rx * 0.3, y0 - ry * 0.3, rx * 0.6, ry * 0.6, 200, 250, Arc2D.OPEN));

        int n = 10;
        List<double[]> stones = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            double a = 2 * Math.PI * k / n;
            stones.add(new double[]{x0 + rx * Math.cos(a), y0 + ry * Math.sin(a), k});
        }
        stones.sort((p, q) -> Double.compare(p[1], q[1]));

        for (double[] s : stones) {
            int px = (int) s[0], py = (int) s[1];
            g2.setColor(new Color(0, 0, 0, 55));
            g2.fill(new Ellipse2D.Double(px - 26, py - 8, 52, 20));
            g2.setColor(new Color(205, 205, 185));
            g2.fillRoundRect(px - 17, py - 64, 34, 64, 12, 12);
            g2.setColor(new Color(228, 228, 208));
            g2.fillRoundRect(px - 17, py - 64, 12, 64, 12, 12);
            g2.setColor(new Color(105, 175, 95));
            g2.fillRoundRect(px - 17, py - 64, 34, 14, 12, 12);
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(new Color(120, 120, 105));
            g2.drawRoundRect(px - 17, py - 64, 34, 64, 12, 12);
            float glow = (float) (0.35 + 0.3 * Math.sin(sec * 2.0 + s[2]));
            g2.setColor(new Color(0.7f, 1f, 0.8f, glow));
            g2.fill(new Ellipse2D.Double(px - 8, py - 86, 16, 16));
        }
    }

    private void drawPillar(Graphics2D g2, int x, int y, double sec, int idx) {
        double bob = Math.sin(sec * 1.3 + idx * 2.0) * 14;
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fill(new Ellipse2D.Double(x - 55, y - 14, 110, 40));

        int top = (int) (y - 190 - bob);
        g2.setColor(new Color(150, 160, 150));
        g2.fillRoundRect(x - 34, top, 68, 150, 24, 24);
        g2.setColor(new Color(190, 205, 190));
        g2.fillRoundRect(x - 34, top, 24, 150, 24, 24);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(90, 100, 92));
        g2.drawRoundRect(x - 34, top, 68, 150, 24, 24);

        float pulse = (float) (0.55 + 0.35 * Math.sin(sec * 2.2 + idx));
        g2.setColor(new Color(0.55f, 1f, 0.7f, pulse * 0.5f));
        g2.fill(new Ellipse2D.Double(x - 30, top - 50, 60, 60));
        g2.setColor(new Color(0.8f, 1f, 0.85f, Math.min(1f, pulse + 0.2f)));
        g2.fill(new Ellipse2D.Double(x - 14, top - 34, 28, 28));
    }

    private void drawSpiral(Graphics2D g2, int x, int y, double sec) {
        g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int k = 0; k < 3; k++) {
            double rad = 45 + k * 32;
            double start = Math.toDegrees(sec * (1.6 - k * 0.3)) + k * 70;
            g2.setColor(new Color(235, 255, 245, 190 - k * 40));
            g2.draw(new Arc2D.Double(x - rad, y - rad / 2 - 40 - k * 18, rad * 2, rad, start, 240, Arc2D.OPEN));
        }
    }

    private void drawClouds(Graphics2D g2, int cx, int cy, double sec) {
        double[][] clouds = {
            {0.00, -0.62, 0.020}, {0.35, -0.40, 0.014}, {0.70, 0.55, 0.017},
            {0.15, 0.62, 0.012}, {0.55, -0.58, 0.016}, {0.85, 0.40, 0.010}
        };
        double range = MAP3_WIDTH * 1.4;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f));
        g2.setColor(Color.WHITE);
        for (double[] cl : clouds) {
            double x = cx - range / 2 + frac(cl[0] + sec * cl[2]) * range;
            double y = cy + cl[1] * MAP3_HEIGHT;
            g2.fill(new Ellipse2D.Double(x - 130, y - 30, 260, 70));
            g2.fill(new Ellipse2D.Double(x - 70, y - 60, 150, 80));
            g2.fill(new Ellipse2D.Double(x + 30, y - 40, 170, 70));
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawLeaves(Graphics2D g2, int cx, int cy, double sec) {
        AffineTransform old = g2.getTransform();
        double range = MAP3_WIDTH * 1.1;
        for (int i = 0; i < 30; i++) {
            double speed = 0.035 + 0.03 * frac(i * 0.37);
            double p = frac(i * 0.618 + sec * speed);
            double x = cx - range / 2 + p * range;
            double y = cy + (frac(i * 0.754) - 0.5) * MAP3_HEIGHT * 0.9
                     + Math.sin(sec * 1.6 + i) * 40 + p * 60 - 70;
            float a = (float) Math.min(1.0, Math.min(p, 1 - p) * 8);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.85f));
            g2.setColor(i % 3 == 0 ? new Color(150, 230, 120)
                      : i % 3 == 1 ? new Color(90, 200, 110) : new Color(60, 170, 120));
            g2.rotate(sec * 2.0 + i, x, y);
            g2.fill(new Ellipse2D.Double(x - 22, y - 9, 44, 18));
            g2.setTransform(old);
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
}