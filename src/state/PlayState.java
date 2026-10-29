package state;

import core.GamePanel;
import data.PlayerStats;
import data.SaveData;
import entity.Plant;
import map.EarthArea;
import map.FireArea;
import map.ImageHelper;
import map.MapArea;
import map.TempleArea;
import map.WaterArea;
import map.WindArea;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

public class PlayState extends GameState {
    public static final int TILE_W = 240;
    public static final int TILE_H = 120;

    // KHOẢNG CÁCH VỰC SÂU CHIA TÁCH CÁC ĐẢO (PIXEL)
    public static final int ISLAND_GAP = 600;

    private static final double WIND_ISLAND_SCALE = 1.1; // Windmap

    // TOẠ ĐỘ TRỌNG TÂM CỦA ĐỀN THỜ TRUNG TÂM (ĐẢO 0)
    public static final int TEMPLE_CENTER_X = 5000;
    public static final int TEMPLE_CENTER_Y = 2000;
    public static final int TEMPLE_SIZE = 8;  // Đảo Đền 8x8 ô
    public static final int ISLAND_SIZE = 12; // 4 Đảo nguyên tố 12x12 ô

    // KHUNG BOUNDING BOX CHO CAMERA CLAMPING
    public static final double MAP_TOTAL_WIDTH  = 13000;
    public static final double MAP_TOTAL_HEIGHT = 10000;
    public static final double MAP_BOUND_LEFT   = TEMPLE_CENTER_X - (MAP_TOTAL_WIDTH / 2.0);
    public static final double MAP_BOUND_RIGHT  = TEMPLE_CENTER_X + (MAP_TOTAL_WIDTH / 2.0);
    public static final double MAP_BOUND_TOP    = TEMPLE_CENTER_Y - (MAP_TOTAL_HEIGHT / 2.0);
    public static final double MAP_BOUND_BOTTOM = TEMPLE_CENTER_Y + (MAP_TOTAL_HEIGHT / 2.0);
    public static final double MAP_CENTER_X     = TEMPLE_CENTER_X;
    public static final double MAP_CENTER_Y     = TEMPLE_CENTER_Y;

    private BufferedImage bgImage;
    private FireArea fireArea;
    private WaterArea waterArea;
    private EarthArea earthArea;
    private WindArea windArea;
    private TempleArea templeArea;
    private MapArea[] islandAreas = new MapArea[5];

    // Quản lý cây trên 4 đảo: [islandId 1..4][r 0..11][c 0..11]
    private Plant[][][] plants = new Plant[5][ISLAND_SIZE][ISLAND_SIZE];

    private double difficultyMultiplier = 1.0;
    private int[] seeds = new int[5];
    private double[] energies = new double[5];
    private boolean[] mapUnlocked = new boolean[5];

    private boolean hasMagicBook = false;
    private boolean isReadingBook = false;

    // Vị trí đang click: đảo nào, hàng nào, cột nào
    private int selectedIsland = -1;
    private int selectedRow = -1;
    private int selectedCol = -1;

    // HUD Buttons
    private Rectangle btnAvatar, btnGacha, btnReadBook;
    private Rectangle btnUpgrade;
    private Rectangle btnSaveGame;

    // Dữ liệu thống kê người chơi
    private PlayerStats playerStats = new PlayerStats();

    // Đồng hồ đếm theo giây
    private long lastSecondMillis;

    // Quản lý Avatar trong phiên chơi
    private String activeAvatarId = "lumi";
    private BufferedImage[] avatarChibiImages = new BufferedImage[6];
    private java.util.List<String> unlockedAvatarIds = new java.util.ArrayList<>();

    private static final String[] AVATAR_IDS = {
            "lumi", "fire_spirit", "aqua_spirit", "terra_guard", "zephyr", "sage_ancient"
    };

    public PlayState(GameStateManager gsm) {
        this(gsm, 1.0);
    }

    public PlayState(GameStateManager gsm, double difficultyMultiplier) {
        super(gsm);
        this.difficultyMultiplier = difficultyMultiplier;
        init();
    }

    @Override
    public void init() {
        btnAvatar = new Rectangle(20, GamePanel.HEIGHT - 100, 80, 80);
        btnGacha = new Rectangle(GamePanel.WIDTH - 120, GamePanel.HEIGHT - 100, 100, 80);
        btnReadBook = new Rectangle(GamePanel.WIDTH - 240, GamePanel.HEIGHT - 90, 110, 70);
        btnUpgrade = new Rectangle(GamePanel.WIDTH - 220, 20, 200, 45);
        btnSaveGame = new Rectangle(GamePanel.WIDTH - 330, 20, 100, 45);

        seeds[1] = 5;
        mapUnlocked[1] = true;
        mapUnlocked[2] = false;
        mapUnlocked[3] = false;
        mapUnlocked[4] = false;

        // Đảm bảo ban đầu luôn có Lumi
        if (!unlockedAvatarIds.contains("lumi")) {
            unlockedAvatarIds.add("lumi");
        }

        bgImage = ImageHelper.loadImage("res/general/background.png");

        fireArea = new FireArea();
        waterArea = new WaterArea();
        earthArea = new EarthArea();
        windArea = new WindArea();
        templeArea = new TempleArea();

        islandAreas[0] = templeArea;
        islandAreas[1] = fireArea;
        islandAreas[2] = waterArea;
        islandAreas[3] = earthArea;
        islandAreas[4] = windArea;

        // Mức zoom ban đầu để nhìn thấy trọn vẹn 5 đảo tách rời
        gsm.getGamePanel().zoom = 0.07;
        gsm.getGamePanel().centerCameraOnMap();
        gsm.getGamePanel().clampCamera();

        // Khởi động đồng hồ tính giờ
        resetStatsTimer();

        // Nạp hình ảnh avatar chibi
        loadChibiAvatarAssets();
    }

    public void resetStatsTimer() {
        this.lastSecondMillis = System.currentTimeMillis();
    }

    @Override
    public void update() {
        // 1. TÍNH THỜI GIAN CHƠI THEO GIÂY
        long now = System.currentTimeMillis();
        if (now - lastSecondMillis >= 1000) {
            long passedSeconds = (now - lastSecondMillis) / 1000;
            playerStats.addPlayTimeSeconds(passedSeconds);
            lastSecondMillis += passedSeconds * 1000;
        }

        // 2. TÍCH LUỸ NĂNG LƯỢNG THEO TỪNG FRAME
        for (int i = 1; i <= 4; i++) {
            for (int r = 0; r < ISLAND_SIZE; r++) {
                for (int c = 0; c < ISLAND_SIZE; c++) {
                    Plant p = plants[i][r][c];
                    if (p != null) {
                        double gainPerTick = (p.getBaseEnergyPerSecond() * difficultyMultiplier) / 60.0;
                        int elemId = p.getElementId();
                        energies[elemId] += gainPerTick;
                        playerStats.addEnergyCollected(elemId, gainPerTick);
                    }
                }
            }
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        // 2. Camera Isometric
        AffineTransform originalTransform = g2.getTransform();
        g2.scale(gsm.getGamePanel().zoom, gsm.getGamePanel().zoom);
        g2.translate(gsm.getGamePanel().cameraX, gsm.getGamePanel().cameraY);

        // 1. Nền vũ trụ
        if (bgImage != null) {
            g2.drawImage(bgImage, (int) MAP_BOUND_LEFT, (int) MAP_BOUND_TOP,
                    (int) MAP_TOTAL_WIDTH, (int) MAP_TOTAL_HEIGHT, null);
        }

        // Vẽ 5 đảo
        drawFiveFloatingIslands(g2);

        g2.setTransform(originalTransform);

        // 3. UI HUD
        drawStaticUI(g2);

        if (isReadingBook) {
            drawMagicBookModal(g2);
        }
    }

    private static boolean isKnownAvatarId(String id) {
        if (id == null) return false;
        for (String knownId : AVATAR_IDS) {
            if (knownId.equals(id)) return true;
        }
        return false;
    }

    private void normalizeAvatarData() {
        if (unlockedAvatarIds == null) unlockedAvatarIds = new java.util.ArrayList<>();
        unlockedAvatarIds.removeIf(id -> !isKnownAvatarId(id));
        if (!unlockedAvatarIds.contains("lumi")) unlockedAvatarIds.add("lumi");
        if (!isKnownAvatarId(activeAvatarId) || !unlockedAvatarIds.contains(activeAvatarId)) {
            activeAvatarId = "lumi";
        }
    }

    private void loadChibiAvatarAssets() {
        for (int i = 0; i < AVATAR_IDS.length; i++) {
            String path = "res/avt/" + AVATAR_IDS[i] + "_chibi.png";
            try {
                BufferedImage img = ImageIO.read(new File(path));
                avatarChibiImages[i] = img;
                if (img == null) System.err.println("Missing/invalid avatar image: " + path + " (fallback enabled)");
            } catch (IOException | SecurityException e) {
                avatarChibiImages[i] = null;
                System.err.println("Cannot load avatar image: " + path + " (fallback enabled): " + e.getMessage());
            }
        }
    }

    private void drawImageFit(Graphics2D g2, BufferedImage image, int x, int y, int width, int height) {
        double scale = Math.min((double) width / image.getWidth(), (double) height / image.getHeight());
        int drawWidth = (int) Math.round(image.getWidth() * scale);
        int drawHeight = (int) Math.round(image.getHeight() * scale);
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;
        g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
    }

    private int getActiveAvatarIndex() {
        normalizeAvatarData();
        for (int i = 0; i < AVATAR_IDS.length; i++) {
            if (AVATAR_IDS[i].equals(activeAvatarId)) return i;
        }
        return 0;
    }

    public String getActiveAvatarId() {
        normalizeAvatarData();
        return activeAvatarId;
    }

    public boolean isAvatarUnlocked(String id) {
        normalizeAvatarData();
        return isKnownAvatarId(id) && unlockedAvatarIds.contains(id);
    }

    public boolean unlockAvatar(String id) {
        if (!isKnownAvatarId(id)) return false;
        normalizeAvatarData();
        if (!unlockedAvatarIds.contains(id)) unlockedAvatarIds.add(id);
        return true;
    }

    public boolean equipAvatar(String id) {
        normalizeAvatarData();
        if (!isKnownAvatarId(id) || !unlockedAvatarIds.contains(id)) return false;
        activeAvatarId = id;
        return true;
    }

    private void drawFiveFloatingIslands(Graphics2D g2) {
        // =========================================================
        // ĐẢO 0: ĐỀN THỜ TRUNG TÂM (8x8 ô) - Độc lập ở giữa ngũ giác
        // =========================================================
        templeArea.drawTiles(g2); // Tự động vẽ ảnh nền temple_island.png
        drawIslandLabel(g2, "ĐỀN THỜ TRUNG TÂM [NIÊM PHONG]", TEMPLE_CENTER_X, TEMPLE_CENTER_Y + 520, new Color(255, 215, 0));

        // =========================================================
        // 4 ĐẢO NGUYÊN TỐ (XẾP THEO 4 CÁNH DƯỚI CỦA NGŨ GIÁC)
        // =========================================================
        // 1. Đảo Lửa (Tây Bắc)
        fireArea.drawTiles(g2);
        drawIslandLabel(g2, "ĐẢO LỬA [MỞ]",
                ImageHelper.getIslandCenterX(1), ImageHelper.getIslandCenterY(1) + 800, Color.WHITE);

        // 2. Đảo Nước (Đông Bắc)
        waterArea.drawTiles(g2);
        drawIslandLabel(g2, "ĐẢO NƯỚC [" + (mapUnlocked[2] ? "MỞ" : "KHÓA") + "]",
                ImageHelper.getIslandCenterX(2), ImageHelper.getIslandCenterY(2) + 800, mapUnlocked[2] ? Color.CYAN : Color.GRAY);

        // 3. Đảo Đất/Sét (Tây Nam)
        earthArea.drawTiles(g2);
        drawIslandLabel(g2, "ĐẢO ĐẤT [" + (mapUnlocked[3] ? "MỞ" : "KHÓA") + "]",
                ImageHelper.getIslandCenterX(3), ImageHelper.getIslandCenterY(3) + 800, mapUnlocked[3] ? new Color(205, 133, 63) : Color.GRAY);

        // 4. Đảo Gió (Đông Nam) - có phóng to nhẹ
        int windCx = ImageHelper.getIslandCenterX(4);
        int windCy = ImageHelper.getIslandCenterY(4);

        AffineTransform oldTransform = g2.getTransform();
        g2.translate(windCx, windCy);
        g2.scale(WIND_ISLAND_SCALE, WIND_ISLAND_SCALE);
        g2.translate(-windCx, -windCy);

        windArea.drawTiles(g2);
        g2.setTransform(oldTransform);

        drawIslandLabel(g2, "ĐẢO GIÓ [" + (mapUnlocked[4] ? "MỞ" : "KHÓA") + "]",
                ImageHelper.getIslandCenterX(4), ImageHelper.getIslandCenterY(4) + 800, mapUnlocked[4] ? Color.GREEN : Color.GRAY);

        // =========================================================
        // VIỀN SÁNG Ô ĐANG ĐƯỢC CHỌN (BÁO HIỆU CÓ ĐƯỢC TRỒNG HAY KHÔNG)
        // =========================================================
        if (selectedIsland >= 0 && selectedRow >= 0 && selectedCol >= 0) {
            int isoX = ImageHelper.getIslandIsoX(selectedIsland, selectedRow, selectedCol);
            int isoY = ImageHelper.getIslandIsoY(selectedIsland, selectedRow, selectedCol);
            Polygon poly = getDiamondPolygon(isoX, isoY, TILE_W, TILE_H);

            boolean plantable = isTilePlantable(selectedIsland, selectedRow, selectedCol);
            if (plantable) {
                // Ô hợp lệ để trồng -> Viền Vàng rực rỡ
                g2.setColor(new Color(255, 255, 255, 120));
                g2.fillPolygon(poly);
                g2.setColor(Color.YELLOW);
                g2.drawPolygon(poly);
            } else {
                // Ô cấm trồng (Núi đá, nước, đường đi...) -> Viền Đỏ cảnh báo
                g2.setColor(new Color(255, 50, 50, 60));
                g2.fillPolygon(poly);
                g2.setColor(new Color(255, 60, 60, 200));
                g2.drawPolygon(poly);
            }
        }

        // =========================================================
        // VẬT THỂ & CÂY TRỒNG TRÊN 4 ĐẢO
        // =========================================================
        fireArea.drawObjects(g2);
        waterArea.drawObjects(g2);
        earthArea.drawObjects(g2);
        windArea.drawObjects(g2);
        if (templeArea != null) {
            templeArea.drawObjects(g2);
        }

        for (int i = 1; i <= 4; i++) {
            for (int r = 0; r < ISLAND_SIZE; r++) {
                for (int c = 0; c < ISLAND_SIZE; c++) {
                    if (plants[i][r][c] != null) plants[i][r][c].draw(g2);
                }
            }
        }

        // Lớp hiệu ứng Overlay phủ lên trên cây của Earth và Wind
        earthArea.drawOverlay(g2);
        windArea.drawOverlay(g2);
    }

    /**
     * Kiểm tra xem ô (r, c) của hòn đảo có cho phép trồng cây hay không
     */
    private boolean isTilePlantable(int islandId, int r, int c) {
        if (islandId == 0) return false; // Đền thờ không cho trồng
        if (islandId < 1 || islandId > 4) return false;
        if (!mapUnlocked[islandId]) return false; // Chưa mở khoá đảo

        MapArea area = islandAreas[islandId];
        return area != null && area.isPlantable(r, c);
    }

    private Polygon getDiamondPolygon(int centerX, int centerY, int w, int h) {
        return new Polygon(
                new int[]{centerX, centerX + w / 2, centerX, centerX - w / 2},
                new int[]{centerY - h / 2, centerY, centerY + h / 2, centerY}, 4
        );
    }

    private void drawIslandLabel(Graphics2D g2, String text, int x, int y, Color c) {
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.setColor(Color.BLACK);
        g2.drawString(text, x - 180 + 2, y + 2);
        g2.setColor(c);
        g2.drawString(text, x - 180, y);
    }

    private void drawStaticUI(Graphics2D g2) {
        // Nút Avatar
        g2.setColor(new Color(30, 40, 60, 230));
        g2.fillRoundRect(btnAvatar.x, btnAvatar.y, btnAvatar.width, btnAvatar.height, 15, 15);
        g2.setColor(Color.CYAN);
        g2.drawRoundRect(btnAvatar.x, btnAvatar.y, btnAvatar.width, btnAvatar.height, 15, 15);

        // Lấy ảnh chibi của avatar đang trang bị
        int activeIndex = getActiveAvatarIndex();
        BufferedImage chibiImg = avatarChibiImages[activeIndex];

        if (chibiImg != null) {
            drawImageFit(g2, chibiImg, btnAvatar.x + 5, btnAvatar.y + 5,
                    btnAvatar.width - 10, btnAvatar.height - 10);
        } else {
            g2.setColor(new Color(45, 55, 75));
            g2.fillOval(btnAvatar.x + 8, btnAvatar.y + 8, btnAvatar.width - 16, btnAvatar.height - 16);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 30));
            String initial = activeAvatarId.isEmpty() ? "L" : activeAvatarId.substring(0, 1).toUpperCase();
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initial, btnAvatar.x + (btnAvatar.width - fm.stringWidth(initial)) / 2,
                    btnAvatar.y + (btnAvatar.height + fm.getAscent()) / 2 - 5);
        }

        // Nút Gacha (Mở GachaState)
        BufferedImage gachaBtnImg = ImageHelper.getOrLoadImage("res/gachasystem/button.png");
        if (gachaBtnImg != null) {
            g2.drawImage(gachaBtnImg, btnGacha.x, btnGacha.y, btnGacha.width, btnGacha.height, null);
        } else {
            g2.setColor(new Color(80, 50, 20, 230));
            g2.fillRoundRect(btnGacha.x, btnGacha.y, btnGacha.width, btnGacha.height, 15, 15);
            g2.setColor(new Color(255, 215, 0));
            g2.drawRoundRect(btnGacha.x, btnGacha.y, btnGacha.width, btnGacha.height, 15, 15);
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.setColor(Color.YELLOW);
            g2.drawString("GACHA", btnGacha.x + 18, btnGacha.y + 45);
        }

        // Nút Sách Phép
        if (hasMagicBook) {
            g2.setColor(new Color(60, 20, 80, 240));
            g2.fillRoundRect(btnReadBook.x, btnReadBook.y, btnReadBook.width, btnReadBook.height, 15, 15);
            g2.setColor(Color.MAGENTA);
            g2.drawRoundRect(btnReadBook.x, btnReadBook.y, btnReadBook.width, btnReadBook.height, 15, 15);
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(Color.WHITE);
            g2.drawString("SÁCH PHÉP", btnReadBook.x + 13, btnReadBook.y + 40);
        }

        // Bảng tài nguyên góc trên bên trái
        g2.setColor(new Color(20, 25, 35, 230));
        g2.fillRoundRect(15, 15, 420, 85, 12, 12);
        g2.setColor(new Color(255, 215, 0));
        g2.drawRoundRect(15, 15, 420, 85, 12, 12);

        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.setColor(new Color(255, 120, 80));
        g2.drawString(String.format("Lửa: %.1f NL | %d Hạt", energies[1], seeds[1]), 25, 38);
        g2.setColor(new Color(80, 180, 255));
        g2.drawString(String.format("Nước: %.1f NL | %d Hạt", energies[2], seeds[2]), 225, 38);
        g2.setColor(new Color(210, 140, 80));
        g2.drawString(String.format("Đất: %.1f NL | %d Hạt", energies[3], seeds[3]), 25, 65);
        g2.setColor(new Color(100, 255, 150));
        g2.drawString(String.format("Gió: %.1f NL | %d Hạt", energies[4], seeds[4]), 225, 65);

        // Nút Lưu Game
        g2.setColor(new Color(40, 90, 160));
        g2.fillRoundRect(btnSaveGame.x, btnSaveGame.y, btnSaveGame.width, btnSaveGame.height, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(btnSaveGame.x, btnSaveGame.y, btnSaveGame.width, btnSaveGame.height, 10, 10);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString("LƯU GAME", btnSaveGame.x + 12, btnSaveGame.y + 28);

        // Nút Nâng Cấp cây
        if (selectedIsland >= 1 && selectedIsland <= 4 && selectedRow >= 0 && selectedCol >= 0) {
            Plant p = plants[selectedIsland][selectedRow][selectedCol];
            if (p != null && p.getLevel() < 3) {
                int cost = p.getUpgradeCost();
                if (energies[p.getElementId()] >= cost) {
                    g2.setColor(new Color(40, 120, 60));
                    g2.fillRoundRect(btnUpgrade.x, btnUpgrade.y, btnUpgrade.width, btnUpgrade.height, 10, 10);
                    g2.setColor(Color.WHITE);
                    g2.drawRoundRect(btnUpgrade.x, btnUpgrade.y, btnUpgrade.width, btnUpgrade.height, 10, 10);
                    g2.setFont(new Font("Arial", Font.BOLD, 14));
                    g2.drawString("Nâng Lv." + (p.getLevel() + 1) + " (" + cost + " NL)", btnUpgrade.x + 15, btnUpgrade.y + 28);
                }
            }
        }
    }

    private void drawMagicBookModal(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        int bx = 120, by = 80, bw = 560, bh = 440;
        g2.setColor(new Color(40, 20, 55));
        g2.fillRoundRect(bx, by, bw, bh, 20, 20);
        g2.setColor(new Color(255, 215, 0));
        g2.drawRoundRect(bx, by, bw, bh, 20, 20);

        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString("BÍ TRUYỀN THUẬT PHÁP CỔ", bx + 130, by + 50);

        g2.setFont(new Font("Arial", Font.PLAIN, 15));
        g2.setColor(Color.WHITE);
        g2.drawString("- Đây là cổ thư ghi chép bí thuật cổ xưa của khu vườn.", bx + 40, by + 110);
        g2.drawString("- 4 đảo nguyên tố Lửa, Nước, Đất, Gió đều đã được liên kết.", bx + 40, by + 145);
        g2.drawString("- Phong ấn Đảo Đền Trung Tâm đang dần suy yếu...", bx + 40, by + 180);

        g2.setFont(new Font("Arial", Font.ITALIC, 13));
        g2.setColor(Color.YELLOW);
        g2.drawString("[Click bất kỳ đâu để đóng sách]", bx + 180, by + 390);
    }

    @Override
    public void mousePressed(int x, int y) {
        if (isReadingBook) {
            isReadingBook = false;
            return;
        }

        // Click Avatar Button -> Mở AvatarState
        if (btnAvatar.contains(x, y)) {
            gsm.openAvatarMenu(this);
            return;
        }

        // Click Gacha Button -> Mở GachaState riêng biệt
        if (btnGacha.contains(x, y)) {
            gsm.openGachaMenu(this);
            return;
        }

        // Click Lưu Game
        if (btnSaveGame.contains(x, y)) {
            gsm.openSaveMenu(this);
            return;
        }

        // Click Sách phép
        if (hasMagicBook && btnReadBook.contains(x, y)) {
            isReadingBook = true;
            return;
        }

        // Nâng cấp cây trên đảo
        if (selectedIsland >= 1 && selectedIsland <= 4 && selectedRow >= 0 && selectedCol >= 0) {
            Plant p = plants[selectedIsland][selectedRow][selectedCol];
            if (btnUpgrade.contains(x, y) && p != null && p.getLevel() < 3) {
                int cost = p.getUpgradeCost();
                if (energies[p.getElementId()] >= cost) {
                    energies[p.getElementId()] -= cost;
                    p.upgrade();
                    playerStats.incrementPlantsUpgraded();
                }
                return;
            }
        }

        // TÍNH TOÁN CLICK CHUỘT TRÊN 5 ĐẢO RIÊNG BIỆT
        double worldX = (x / gsm.getGamePanel().zoom) - gsm.getGamePanel().cameraX;
        double worldY = (y / gsm.getGamePanel().zoom) - gsm.getGamePanel().cameraY;

        selectedIsland = -1;
        selectedRow = -1;
        selectedCol = -1;

        // Quét click trên 5 đảo (0: Đền, 1: Lửa, 2: Nước, 3: Đất, 4: Gió)
        for (int isl = 0; isl <= 4; isl++) {
            int size = (isl == 0) ? TEMPLE_SIZE : ISLAND_SIZE;

            int islandCenterX = ImageHelper.getIslandCenterX(isl);
            int islandCenterY = ImageHelper.getIslandCenterY(isl);

            double dx = worldX - islandCenterX;
            double dy = worldY - islandCenterY;

            // Đổi ngược từ toạ độ World về toạ độ (row, col) neo từ tâm đảo
            double clickColDec = (dy / TILE_H) + (dx / TILE_W) + ((size - 1) / 2.0);
            double clickRowDec = (dy / TILE_H) - (dx / TILE_W) + ((size - 1) / 2.0);

            int clickedCol = (int) Math.round(clickColDec);
            int clickedRow = (int) Math.round(clickRowDec);

            if (clickedRow >= 0 && clickedRow < size && clickedCol >= 0 && clickedCol < size) {
                selectedIsland = isl;
                selectedRow = clickedRow;
                selectedCol = clickedCol;

                // CHỈ CHO PHÉP TRỒNG NẾU Ô ĐÓ HỢP LỆ THEO QUY HOẠCH CỦA TỪNG ĐẢO
                if (isTilePlantable(isl, clickedRow, clickedCol)) {
                    if (plants[isl][clickedRow][clickedCol] == null && seeds[isl] > 0) {
                        plants[isl][clickedRow][clickedCol] = new Plant(clickedRow, clickedCol, isl);
                        seeds[isl]--;
                        playerStats.incrementPlantsPlanted();
                    }
                }
                break;
            }
        }
    }

    /**
     * Xuất dữ liệu hiện tại để lưu ra file
     */
    public SaveData exportSaveData() {
        SaveData d = new SaveData();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        d.saveTime = sdf.format(new Date());
        d.difficultyMultiplier = this.difficultyMultiplier;

        System.arraycopy(this.seeds, 0, d.seeds, 0, 5);
        System.arraycopy(this.energies, 0, d.energies, 0, 5);
        System.arraycopy(this.mapUnlocked, 0, d.mapUnlocked, 0, 5);
        d.hasMagicBook = this.hasMagicBook;

        // Lưu trạng thái toàn bộ cây trên 4 hòn đảo
        for (int isl = 1; isl <= 4; isl++) {
            for (int r = 0; r < ISLAND_SIZE; r++) {
                for (int c = 0; c < ISLAND_SIZE; c++) {
                    Plant p = plants[isl][r][c];
                    if (p != null) {
                        d.plantElement[isl][r][c] = p.getElementId();
                        d.plantLevel[isl][r][c] = p.getLevel();
                    } else {
                        d.plantElement[isl][r][c] = 0;
                        d.plantLevel[isl][r][c] = 0;
                    }
                }
            }
        }

        // Lưu thông tin thống kê người chơi
        d.stats = this.playerStats;

        // Lưu trạng thái avatar
        normalizeAvatarData();
        d.activeAvatarId = this.activeAvatarId;
        d.unlockedAvatarIds = new java.util.ArrayList<>(this.unlockedAvatarIds);

        return d;
    }

    /**
     * Nạp dữ liệu từ file save vào trò chơi
     */
    public void importSaveData(SaveData d) {
        if (d == null) return;

        this.difficultyMultiplier = d.difficultyMultiplier;
        if (d.seeds != null && d.seeds.length >= 5) System.arraycopy(d.seeds, 0, this.seeds, 0, 5);
        if (d.energies != null && d.energies.length >= 5) System.arraycopy(d.energies, 0, this.energies, 0, 5);
        if (d.mapUnlocked != null && d.mapUnlocked.length >= 5) System.arraycopy(d.mapUnlocked, 0, this.mapUnlocked, 0, 5);
        this.hasMagicBook = d.hasMagicBook;

        // Khôi phục avatar
        this.activeAvatarId = d.activeAvatarId;
        this.unlockedAvatarIds = (d.unlockedAvatarIds == null)
                ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(d.unlockedAvatarIds);
        normalizeAvatarData();

        // Khôi phục thống kê
        this.playerStats = (d.stats != null) ? d.stats : new PlayerStats();
        resetStatsTimer();

        // Xóa sạch cây cũ và khôi phục lại cây từ file lưu
        if (d.plantElement != null && d.plantLevel != null) {
            for (int isl = 1; isl <= 4; isl++) {
                for (int r = 0; r < ISLAND_SIZE; r++) {
                    for (int c = 0; c < ISLAND_SIZE; c++) {
                        plants[isl][r][c] = null; // Reset ô trồng

                        int elem = d.plantElement[isl][r][c];
                        int level = d.plantLevel[isl][r][c];

                        if (elem > 0) {
                            Plant p = new Plant(r, c, elem);
                            while (p.getLevel() < level && p.getLevel() < 3) {
                                p.upgrade();
                            }
                            plants[isl][r][c] = p;
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // GETTERS & UTILITIES DÀNH CHO AVATAR & GACHA
    // ==========================================
    public int[] getSeedsArray() {
        return seeds;
    }

    public double[] getEnergiesArray() {
        return energies;
    }

    public boolean[] getMapUnlockedArray() {
        return mapUnlocked;
    }

    public void unlockMagicBook() {
        this.hasMagicBook = true;
    }

    public PlayerStats getPlayerStats() {
        return playerStats;
    }

    public double getEnergy(int elementId) {
        if (elementId < 1 || elementId > 4) return 0.0;
        return energies[elementId];
    }

    public int getSeeds(int elementId) {
        if (elementId < 1 || elementId > 4) return 0;
        return seeds[elementId];
    }

    public boolean consumeEnergy(int elementId, double amount) {
        if (elementId >= 1 && elementId <= 4 && amount > 0 && !Double.isInfinite(amount)
                && energies[elementId] >= amount) {
            energies[elementId] -= amount;
            return true;
        }
        return false;
    }

    public boolean consumeSeeds(int elementId, int amount) {
        if (elementId >= 1 && elementId <= 4 && amount > 0 && seeds[elementId] >= amount) {
            seeds[elementId] -= amount;
            return true;
        }
        return false;
    }

    @Override
    public void mouseMoved(int x, int y) {}
}
