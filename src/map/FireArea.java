package map;

import state.PlayState;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.ImageIcon;

public class FireArea extends MapArea {
    public static final int ISLAND_ID = 1;
    public static final int SIZE = PlayState.ISLAND_SIZE;

    public static final int MAP1_WIDTH  = SIZE * PlayState.TILE_W;
    public static final int MAP1_HEIGHT = SIZE * PlayState.TILE_H;

    private BufferedImage islandBgImg;
    
    // --- KHAI BÁO BIẾN CHỨA ẢNH GIF VÒNG LỬA DƯỚI ĐÁY ĐẢO ---
    private Image fireRingGif;

    private double animTimer = 0;
    private final Random rand = new Random();

    // 1. Tàn tro lửa bay lên (Embers)
    private static class Ember {
        double x, y, vx, vy, size, phase;
        float life, maxLife;
        Color color;
    }
    private final List<Ember> embers = new ArrayList<>();

    // 2. Cột dung nham & Thiên thạch lửa rơi (Lava Meteors & Plumes)
    private static class FireMeteor {
        double startX, startY, targetX, targetY;
        double progress;
        double maxShockwaveRadius;
    }
    private final List<FireMeteor> meteors = new ArrayList<>();

    // 3. Khói núi lửa bốc lên
    private static class SmokeParticle {
        double x, y, vx, vy;
        double size, maxSize;
        float life, maxLife;
        double wavePhase;
    }
    private final List<SmokeParticle> smokePuffs = new ArrayList<>();

    // 4. Các điểm thoát nhiệt
    private static class HeatVent {
        double x, y;
        double phase;
    }
    private final List<HeatVent> heatVents = new ArrayList<>();

    public FireArea() {
        super(true);
        loadImages();
        initVisualEffects();
    }

    @Override
    public void loadImages() {
        // 1. Nạp ảnh nền map đảo
        islandBgImg = ImageHelper.loadImage("res/map1/map1_island.png");
        if (islandBgImg == null) {
            islandBgImg = ImageHelper.loadImage("res/map1_fire/map1_island.png");
        }

        // 2. Nạp ảnh GIF động (Dùng ImageIcon để tự động chuyển động từng khung hình)
        loadFireRingGif();
    }

    private void loadFireRingGif() {
        String[] paths = {
            "res/map1/fire_ring.gif",
            "res/map1/fire_aura.gif",
            "res/map1_fire/fire_ring.gif",
            "res/map1_fire/fire_aura.gif"
        };

        for (String p : paths) {
            File f = new File(p);
            if (f.exists()) {
                fireRingGif = new ImageIcon(p).getImage();
                return;
            }
        }

        // Tìm từ classpath/resources nếu được đóng gói trong file jar
        try {
            URL url = getClass().getResource("/map1/fire_ring.gif");
            if (url == null) url = getClass().getResource("/map1/fire_aura.gif");
            if (url != null) {
                fireRingGif = new ImageIcon(url).getImage();
            }
        } catch (Exception ignored) {}
    }

    private void initVisualEffects() {
        for (int i = 0; i < 95; i++) {
            embers.add(spawnEmber(true));
        }

        for (int i = 0; i < 40; i++) {
            smokePuffs.add(spawnSmoke(true));
        }

        for (int i = 0; i < 8; i++) {
            HeatVent v = new HeatVent();
            int r = 2 + rand.nextInt(SIZE - 4);
            int c = 2 + rand.nextInt(SIZE - 4);
            v.x = ImageHelper.getIslandIsoX(ISLAND_ID, r, c);
            v.y = ImageHelper.getIslandIsoY(ISLAND_ID, r, c);
            v.phase = rand.nextDouble() * Math.PI * 2;
            heatVents.add(v);
        }
    }

    private Ember spawnEmber(boolean randomProgress) {
        Ember e = new Ember();
        double r = 0.5 + rand.nextDouble() * (SIZE - 1.0);
        double c = 0.5 + rand.nextDouble() * (SIZE - 1.0);
        e.x = ImageHelper.getIslandIsoX(ISLAND_ID, (int) r, (int) c) + (rand.nextDouble() - 0.5) * 120;
        e.y = ImageHelper.getIslandIsoY(ISLAND_ID, (int) r, (int) c) + (rand.nextDouble() - 0.5) * 60;

        e.vx = (rand.nextDouble() - 0.5) * 1.5;
        e.vy = -(1.8 + rand.nextDouble() * 3.0);
        e.size = 6 + rand.nextDouble() * 10;
        e.phase = rand.nextDouble() * Math.PI * 2;
        e.maxLife = 65 + rand.nextInt(75);
        e.life = randomProgress ? rand.nextInt((int) e.maxLife) : 0;

        if (randomProgress) {
            e.y += e.vy * e.life;
        }

        int type = rand.nextInt(3);
        e.color = switch (type) {
            case 0 -> new Color(255, 250, 140);
            case 1 -> new Color(255, 150, 20);
            default -> new Color(255, 50, 20);
        };
        return e;
    }

    private SmokeParticle spawnSmoke(boolean randomProgress) {
        SmokeParticle s = new SmokeParticle();
        if (!heatVents.isEmpty() && rand.nextBoolean()) {
            HeatVent v = heatVents.get(rand.nextInt(heatVents.size()));
            s.x = v.x + (rand.nextDouble() - 0.5) * 20;
            s.y = v.y + (rand.nextDouble() - 0.5) * 10;
        } else {
            double r = 1.0 + rand.nextDouble() * (SIZE - 2.0);
            double c = 1.0 + rand.nextDouble() * (SIZE - 2.0);
            s.x = ImageHelper.getIslandIsoX(ISLAND_ID, (int) r, (int) c) + (rand.nextDouble() - 0.5) * 60;
            s.y = ImageHelper.getIslandIsoY(ISLAND_ID, (int) r, (int) c) + (rand.nextDouble() - 0.5) * 30;
        }

        s.vx = (rand.nextDouble() - 0.5) * 0.4;
        s.vy = -(1.0 + rand.nextDouble() * 1.5);
        s.size = 18 + rand.nextDouble() * 12;
        s.maxSize = s.size * (2.5 + rand.nextDouble());
        s.wavePhase = rand.nextDouble() * Math.PI * 2;
        s.maxLife = 85 + rand.nextInt(55);
        s.life = randomProgress ? rand.nextInt((int) s.maxLife) : 0;

        if (randomProgress) {
            s.y += s.vy * s.life;
        }
        return s;
    }

    @Override
    public void drawTiles(Graphics2D g2) {
        animTimer += 0.045;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = ImageHelper.getIslandCenterX(ISLAND_ID);
        int centerY = ImageHelper.getIslandCenterY(ISLAND_ID);

        // =====================================================================
        // BƯỚC 1: VẼ ẢNH GIF VÒNG LỬA DƯỚI ĐÁY ĐẢO (LÀM NỀN LÓT)
        // =====================================================================
        if (fireRingGif != null) {
            double ringScale = 1.35;
            int ringW = (int) (MAP1_WIDTH * ringScale);
            int ringH = (int) (MAP1_HEIGHT * ringScale);

            int drawRingX = centerX - (ringW / 2);
            int drawRingY = centerY - (ringH / 2) + 60; // Đẩy nhẹ theo độ dời của chân đảo

            g2.drawImage(fireRingGif, drawRingX, drawRingY, ringW, ringH, null);
        }

        // =====================================================================
        // BƯỚC 2: PHÓNG TO VÀ CÂN CHỈNH ẢNH NỀN ĐẢO ĐỂ BỀ MẶT KHỚP LƯỚI Ô ĐẤT
        // =====================================================================
        // =====================================================================
        // BƯỚC 2: PHÓNG TO VÀ KÉO DÀI RIÊNG CHIỀU DỌC ĐỂ KHỚP BỀ MẶT ĐẢO
        // =====================================================================
        if (islandBgImg != null) {
            // Hệ số co giãn chiều ngang (giữ nguyên hoặc tăng nhẹ)
            double scaleX = 1.05; 

            // Hệ số co giãn chiều dọc (tăng cao hơn scaleX để kéo dài chiều dọc ảnh)
            double scaleY = 1.7; 

            int drawW = (int) (MAP1_WIDTH * scaleX);
            int drawH = (int) (MAP1_HEIGHT * scaleY);

            // Căn giữa theo trục X
            int drawX = centerX - (drawW / 2);

            // Đẩy độ dời theo trục Y để đưa bề mặt phẳng vào đúng lưới tọa độ
            int offsetY = 120; // Tăng/giảm con số này để đưa bề mặt lên hoặc xuống
            int drawY = centerY - (drawH / 2) + offsetY;

            g2.drawImage(islandBgImg, drawX, drawY, drawW, drawH, null);
        } else {
            drawProceduralVolcanicTiles(g2);
        }

        // Vẽ nhẹ các miệng nung đỏ trên mặt đất
        drawHeatVentGlow(g2);
    }

    private void drawProceduralVolcanicTiles(Graphics2D g2) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                double localPulse = (Math.sin(animTimer * 2.0 + (r + c) * 0.5) + 1.0) / 2.0;
                boolean isRim = (r == 0 || c == 0 || r == SIZE - 1 || c == SIZE - 1);
                boolean isCenterCourt = (r >= 3 && r <= 8 && c >= 3 && c <= 8);

                Color tileColor;
                if (isRim) {
                    tileColor = new Color(165 + (int) (45 * localPulse), 50 + (int) (30 * localPulse), 25);
                } else if (isCenterCourt) {
                    tileColor = new Color(55, 22, 22);
                } else {
                    tileColor = new Color(95 + (int) (25 * localPulse), 30, 25);
                }
                ImageHelper.drawIslandTile(g2, null, ISLAND_ID, r, c, tileColor);
            }
        }
    }

    // Thêm hàm này vào FireArea.java
    @Override
    public boolean isPlantable(int r, int c) {
        // Né 2 vòng đá ngoài cùng (núi lửa bao quanh)
        if (r < 2 || r > SIZE - 3 || c < 2 || c > SIZE - 3) return false;
        
        // Né 4 ô lõi dung nham trung tâm
        if ((r == 5 || r == 6) && (c == 5 || c == 6)) return false;

        return true;
    }
    private void drawHeatVentGlow(Graphics2D g2) {
        Composite oldComp = g2.getComposite();
        for (HeatVent v : heatVents) {
            float pulse = (float) ((Math.sin(animTimer * 2.2 + v.phase) + 1.0) / 2.0);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f + 0.25f * pulse));
            g2.setColor(new Color(255, 100, 20));
            g2.fillOval((int) v.x - 24, (int) v.y - 12, 48, 24);
        }
        g2.setComposite(oldComp);
    }

    @Override
    public void drawObjects(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Khói núi lửa
        updateAndDrawSmoke(g2);

        // Ảo ảnh nhiệt
        drawHeatShimmer(g2);

        // Thiên thạch nổ
        updateAndDrawFireMeteors(g2);

        // Tàn tro bay
        updateAndDrawEmbers(g2);
    }

    private void drawHeatShimmer(Graphics2D g2) {
        Composite oldComp = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(1.8f));

        for (HeatVent v : heatVents) {
            for (int line = 0; line < 3; line++) {
                double offsetLine = line * 18;
                double waveY = v.y - 12 - offsetLine - ((animTimer * 28.0) % 36.0);
                double waveX = v.x + Math.sin(animTimer * 4.5 + v.phase + line) * 7.0;

                float alpha = (float) Math.max(0.0, 0.25 - (v.y - waveY) * 0.003);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(new Color(255, 215, 140));
                g2.drawArc((int) waveX - 18, (int) waveY - 5, 36, 10, 0, 180);
            }
        }

        g2.setStroke(oldStroke);
        g2.setComposite(oldComp);
    }

    private void updateAndDrawSmoke(Graphics2D g2) {
        Composite oldComp = g2.getComposite();

        for (int i = 0; i < smokePuffs.size(); i++) {
            SmokeParticle s = smokePuffs.get(i);
            s.life++;
            s.x += s.vx + Math.sin(animTimer * 1.8 + s.wavePhase) * 0.7;
            s.y += s.vy;

            if (s.life >= s.maxLife) {
                smokePuffs.set(i, spawnSmoke(false));
                continue;
            }

            float progress = s.life / s.maxLife;
            double curSize = s.size + (s.maxSize - s.size) * progress;
            float alpha = (float) Math.sin(progress * Math.PI) * 0.35f;

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));

            g2.setColor(new Color(85, 70, 70));
            int drawW = (int) curSize;
            int drawH = (int) (curSize * 0.65);
            g2.fillOval((int) (s.x - drawW / 2), (int) (s.y - drawH / 2), drawW, drawH);
        }

        g2.setComposite(oldComp);
    }

    private void updateAndDrawFireMeteors(Graphics2D g2) {
        if (meteors.size() < 3 && rand.nextInt(100) < 5) {
            FireMeteor m = new FireMeteor();
            int r = 1 + rand.nextInt(SIZE - 2);
            int c = 1 + rand.nextInt(SIZE - 2);
            m.targetX = ImageHelper.getIslandIsoX(ISLAND_ID, r, c);
            m.targetY = ImageHelper.getIslandIsoY(ISLAND_ID, r, c);
            m.startX = m.targetX - 220 - rand.nextInt(120);
            m.startY = m.targetY - 550 - rand.nextInt(180);
            m.progress = 0.0;
            m.maxShockwaveRadius = 65 + rand.nextInt(45);
            meteors.add(m);
        }

        Stroke oldStroke = g2.getStroke();
        Iterator<FireMeteor> it = meteors.iterator();
        while (it.hasNext()) {
            FireMeteor m = it.next();
            m.progress += 0.038;

            if (m.progress < 1.0) {
                double curX = m.startX + (m.targetX - m.startX) * m.progress;
                double curY = m.startY + (m.targetY - m.startY) * m.progress;

                int warnW = (int) (55 * m.progress);
                int warnH = warnW / 2;
                g2.setColor(new Color(255, 50, 0, (int) (130 * m.progress)));
                g2.fillOval((int) m.targetX - warnW / 2, (int) m.targetY - warnH / 2, warnW, warnH);

                g2.setStroke(new BasicStroke(11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(255, 90, 0, 160));
                g2.drawLine((int) curX, (int) curY, (int) (curX - 40), (int) (curY - 85));

                g2.setColor(new Color(255, 250, 180));
                g2.fillOval((int) curX - 12, (int) curY - 12, 24, 24);
            } else if (m.progress <= 2.0) {
                double boomProg = m.progress - 1.0;
                int alpha = (int) ((1.0 - boomProg) * 220);

                int waveW = (int) (m.maxShockwaveRadius * 2.2 * boomProg);
                int waveH = waveW / 2;

                g2.setStroke(new BasicStroke(4.5f));
                g2.setColor(new Color(255, 200, 50, Math.max(0, alpha)));
                g2.drawOval((int) m.targetX - waveW / 2, (int) m.targetY - waveH / 2, waveW, waveH);

                int pillarH = (int) (Math.sin(boomProg * Math.PI) * 140);
                g2.setColor(new Color(255, 100, 10, Math.max(0, alpha)));
                g2.fillOval((int) m.targetX - 16, (int) m.targetY - pillarH, 32, pillarH);

                g2.setColor(new Color(255, 255, 190, Math.max(0, alpha)));
                g2.fillOval((int) m.targetX - 8, (int) m.targetY - (int) (pillarH * 0.8), 16, (int) (pillarH * 0.8));
            } else {
                it.remove();
            }
        }
        g2.setStroke(oldStroke);
    }

    private void updateAndDrawEmbers(Graphics2D g2) {
        Composite oldComp = g2.getComposite();

        for (int i = 0; i < embers.size(); i++) {
            Ember e = embers.get(i);
            e.life++;
            e.x += e.vx + Math.sin(animTimer * 1.5 + e.phase) * 1.0;
            e.y += e.vy;

            if (e.life >= e.maxLife) {
                embers.set(i, spawnEmber(false));
                continue;
            }

            float ratio = e.life / e.maxLife;
            float alpha = (float) Math.sin(ratio * Math.PI);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));

            int glowSize = (int) (e.size * 2.2);
            g2.setColor(new Color(255, 70, 0, 95));
            g2.fillOval((int) e.x - glowSize / 2, (int) e.y - glowSize / 2, glowSize, glowSize);

            g2.setColor(e.color);
            g2.fillOval((int) e.x - (int) e.size / 2, (int) e.y - (int) e.size / 2, (int) e.size, (int) e.size);
        }

        g2.setComposite(oldComp);
    }
}