package state;

import core.GamePanel;
import data.SaveData;
import entity.Plant;
import map.EarthArea;
import map.FireArea;
import map.ImageHelper;
import map.WaterArea;
import map.WindArea;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class PlayState extends GameState {
    public static final int TILE_W = 240;
    public static final int TILE_H = 120;

    // KHOẢNG CÁCH VỰC SÂU CHIA TÁCH CÁC ĐẢO (PIXEL)
    public static final int ISLAND_GAP = 600; 

    // TOẠ ĐỘ TRỌNG TÂM CỦA ĐỀN THỜ TRUNG TÂM (ĐẢO 0)
    public static final int TEMPLE_CENTER_X = 5000;
    public static final int TEMPLE_CENTER_Y = 3500;
    public static final int TEMPLE_SIZE = 8;  // Đảo Đền 8x8 ô
    public static final int ISLAND_SIZE = 12; // 4 Đảo nguyên tố 12x12 ô

    // KHUNG BOUNDING BOX CHO CAMERA CLAMPING
    public static final double MAP_TOTAL_WIDTH  = 11000;
    public static final double MAP_TOTAL_HEIGHT = 8000;
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

    // Modal Gacha
    private boolean isGachaOpen = false;
    private Rectangle btnCloseGacha;
    private Rectangle[] btnGachaRoll = new Rectangle[5];
    private String gachaMessage = "Chọn loại Năng Lượng để Gacha (Tốn 1 NL/lần)";
    private Random random = new Random();

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

        btnCloseGacha = new Rectangle(580, 130, 40, 40);
        for (int i = 1; i <= 4; i++) {
            btnGachaRoll[i] = new Rectangle(200, 210 + (i - 1) * 65, 400, 50);
        }

        bgImage = ImageHelper.loadImage("res/general/background.png");

        fireArea = new FireArea();
        waterArea = new WaterArea();
        earthArea = new EarthArea();
        windArea = new WindArea();

        // Mức zoom ban đầu để nhìn thấy trọn vẹn 5 đảo tách rời
        gsm.getGamePanel().zoom = 0.14;
        gsm.getGamePanel().centerCameraOnMap();
        gsm.getGamePanel().clampCamera();
    }

    @Override
    public void update() {
        // Tích luỹ năng lượng theo từng frame
        for (int i = 1; i <= 4; i++) {
            for (int r = 0; r < ISLAND_SIZE; r++) {
                for (int c = 0; c < ISLAND_SIZE; c++) {
                    Plant p = plants[i][r][c];
                    if (p != null) {
                        double gainPerTick = (p.getBaseEnergyPerSecond() * difficultyMultiplier) / 60.0;
                        energies[p.getElementId()] += gainPerTick;
                    }
                }
            }
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        // 1. Nền vũ trụ
        if (bgImage != null) {
            g2.drawImage(bgImage, 0, 0, GamePanel.WIDTH, GamePanel.HEIGHT, null);
        } else {
            g2.setColor(new Color(10, 14, 22));
            g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);
        }

        // 2. Camera Isometric
        AffineTransform originalTransform = g2.getTransform();
        g2.scale(gsm.getGamePanel().zoom, gsm.getGamePanel().zoom);
        g2.translate(gsm.getGamePanel().cameraX, gsm.getGamePanel().cameraY);

        drawFiveFloatingIslands(g2);

        g2.setTransform(originalTransform);

        // 3. UI HUD
        drawStaticUI(g2);

        if (isGachaOpen) drawGachaModal(g2);
        if (isReadingBook) drawMagicBookModal(g2);
    }

    private void drawFiveFloatingIslands(Graphics2D g2) {
        // =========================================================
        // ĐẢO 0: ĐỀN THỜ TRUNG TÂM (8x8 ô) - Độc lập ở giữa
        // =========================================================
        for (int r = 0; r < TEMPLE_SIZE; r++) {
            for (int c = 0; c < TEMPLE_SIZE; c++) {
                ImageHelper.drawIslandTile(g2, null, 0, r, c, new Color(45, 25, 65));
            }
        }
        drawIslandLabel(g2, "ĐỀN THỜ TRUNG TÂM [NIÊM PHONG]", TEMPLE_CENTER_X, TEMPLE_CENTER_Y + 520, new Color(255, 215, 0));

        // =========================================================
        // 4 ĐẢO NGUYÊN TỐ TÁCH RỜI HOÀN TOÀN
        // =========================================================
        // 1. Đảo Lửa (Bắc)
        fireArea.drawTiles(g2);
        int fireLabelY = TEMPLE_CENTER_Y - (ISLAND_SIZE * TILE_H) - ISLAND_GAP + 200;
        drawIslandLabel(g2, "ĐẢO LỬA [MỞ]", TEMPLE_CENTER_X, fireLabelY, Color.WHITE);

        // 2. Đảo Nước (Đông)
        waterArea.drawTiles(g2);
        int waterLabelX = TEMPLE_CENTER_X + (ISLAND_SIZE * TILE_W / 2) + ISLAND_GAP;
        drawIslandLabel(g2, "ĐẢO NƯỚC [" + (mapUnlocked[2] ? "MỞ" : "KHÓA") + "]", waterLabelX, TEMPLE_CENTER_Y + 800, mapUnlocked[2] ? Color.CYAN : Color.GRAY);

        // 3. Đảo Đất (Tây)
        earthArea.drawTiles(g2);
        int earthLabelX = TEMPLE_CENTER_X - (ISLAND_SIZE * TILE_W / 2) - ISLAND_GAP;
        drawIslandLabel(g2, "ĐẢO ĐẤT [" + (mapUnlocked[3] ? "MỞ" : "KHÓA") + "]", earthLabelX, TEMPLE_CENTER_Y + 800, mapUnlocked[3] ? new Color(205, 133, 63) : Color.GRAY);

        // 4. Đảo Gió (Nam)
        windArea.drawTiles(g2);
        int windLabelY = TEMPLE_CENTER_Y + (ISLAND_SIZE * TILE_H) + ISLAND_GAP + (ISLAND_SIZE * TILE_H) + 100;
        drawIslandLabel(g2, "ĐẢO GIÓ [" + (mapUnlocked[4] ? "MỞ" : "KHÓA") + "]", TEMPLE_CENTER_X, windLabelY, mapUnlocked[4] ? Color.GREEN : Color.GRAY);

        // =========================================================
        // VIỀN SÁNG Ô ĐANG ĐƯỢC CHỌN TRÊN ĐẢO
        // =========================================================
        if (selectedIsland >= 0 && selectedRow >= 0 && selectedCol >= 0) {
            int isoX = ImageHelper.getIslandIsoX(selectedIsland, selectedRow, selectedCol);
            int isoY = ImageHelper.getIslandIsoY(selectedIsland, selectedRow, selectedCol);
            Polygon poly = getDiamondPolygon(isoX, isoY, TILE_W, TILE_H);
            g2.setColor(new Color(255, 255, 255, 120));
            g2.fillPolygon(poly);
            g2.setColor(Color.YELLOW);
            g2.drawPolygon(poly);
        }

        // =========================================================
        // VẬT THỂ & CÂY TRỒNG TRÊN 4 ĐẢO
        // =========================================================
        fireArea.drawObjects(g2);
        waterArea.drawObjects(g2);
        earthArea.drawObjects(g2);
        windArea.drawObjects(g2);

        for (int i = 1; i <= 4; i++) {
            for (int r = 0; r < ISLAND_SIZE; r++) {
                for (int c = 0; c < ISLAND_SIZE; c++) {
                    if (plants[i][r][c] != null) plants[i][r][c].draw(g2);
                }
            }
        }
        earthArea.drawOverlay(g2); 
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
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.setColor(Color.WHITE);
        g2.drawString("AVATAR", btnAvatar.x + 13, btnAvatar.y + 45);

        // Nút Gacha
        g2.setColor(new Color(80, 50, 20, 230));
        g2.fillRoundRect(btnGacha.x, btnGacha.y, btnGacha.width, btnGacha.height, 15, 15);
        g2.setColor(new Color(255, 215, 0));
        g2.drawRoundRect(btnGacha.x, btnGacha.y, btnGacha.width, btnGacha.height, 15, 15);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(Color.YELLOW);
        g2.drawString("GACHA", btnGacha.x + 18, btnGacha.y + 45);

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
                g2.setColor(new Color(40, 120, 60));
                g2.fillRoundRect(btnUpgrade.x, btnUpgrade.y, btnUpgrade.width, btnUpgrade.height, 10, 10);
                g2.setColor(Color.WHITE);
                g2.drawRoundRect(btnUpgrade.x, btnUpgrade.y, btnUpgrade.width, btnUpgrade.height, 10, 10);
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                g2.drawString("Nâng Lv." + (p.getLevel() + 1) + " (" + cost + " NL)", btnUpgrade.x + 15, btnUpgrade.y + 28);
            }
        }
    }

    private void drawGachaModal(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        int bx = 160, by = 110, bw = 480, bh = 400;
        g2.setColor(new Color(25, 30, 45));
        g2.fillRoundRect(bx, by, bw, bh, 20, 20);
        g2.setColor(new Color(255, 215, 0));
        g2.drawRoundRect(bx, by, bw, bh, 20, 20);

        g2.setColor(Color.RED);
        g2.fillRoundRect(btnCloseGacha.x, btnCloseGacha.y, btnCloseGacha.width, btnCloseGacha.height, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("X", btnCloseGacha.x + 14, btnCloseGacha.y + 26);

        g2.setColor(Color.YELLOW);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString("VÒNG QUAY NGUYÊN TỐ", bx + 110, by + 45);

        String[] titles = {
            "Gacha Bằng NL Lửa (50% Hụt | 49% Hạt 1 | 1% Hạt 2)",
            "Gacha Bằng NL Nước (70% Hạt 1 | 29% Hạt 2 | 1% Hạt 3)",
            "Gacha Bằng NL Đất (30% H1 | 40% H2 | 29% H3 | 1% H4)",
            "Gacha Bằng NL Gió (20% H1 | 20% H2 | 25% H3 | 34% H4 | 1% SÁCH)"
        };
        Color[] btnColors = { new Color(180, 50, 40), new Color(30, 100, 180), new Color(139, 85, 40), new Color(40, 140, 90) };

        for (int i = 1; i <= 4; i++) {
            Rectangle r = btnGachaRoll[i];
            g2.setColor(btnColors[i - 1]);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString(titles[i - 1], r.x + 15, r.y + 30);
        }

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.drawString(gachaMessage, bx + 25, by + 375);
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

    private void rollGacha(int elementId) {
        if (energies[elementId] < 1.0) {
            gachaMessage = "Không đủ Năng Lượng! Cần 1.0 NL loại này.";
            return;
        }

        energies[elementId] -= 1.0;
        int roll = random.nextInt(100);

        if (elementId == 1) {
            if (roll < 50) gachaMessage = "Không nhận được gì cả!";
            else if (roll < 99) { seeds[1]++; gachaMessage = "Nhận được: 1 Hạt Giống Lửa!"; }
            else { seeds[2]++; mapUnlocked[2] = true; gachaMessage = "XUẤT SẮC! MỞ KHÓA ĐẢO NƯỚC!"; }
        } else if (elementId == 2) {
            if (roll < 70) { seeds[1]++; gachaMessage = "Nhận được: 1 Hạt Giống Lửa."; }
            else if (roll < 99) { seeds[2]++; gachaMessage = "Nhận được: 1 Hạt Giống Nước."; }
            else { seeds[3]++; mapUnlocked[3] = true; gachaMessage = "TUYỆT VỜI! MỞ KHÓA ĐẢO ĐẤT!"; }
        } else if (elementId == 3) {
            if (roll < 30) { seeds[1]++; gachaMessage = "Nhận được: 1 Hạt Giống Lửa."; }
            else if (roll < 70) { seeds[2]++; gachaMessage = "Nhận được: 1 Hạt Giống Nước."; }
            else if (roll < 99) { seeds[3]++; gachaMessage = "Nhận được: 1 Hạt Giống Đất."; }
            else { seeds[4]++; mapUnlocked[4] = true; gachaMessage = "KỲ DIỆU! MỞ KHÓA ĐẢO GIÓ!"; }
        } else if (elementId == 4) {
            if (roll < 20) { seeds[1]++; gachaMessage = "Nhận được: 1 Hạt Lửa."; }
            else if (roll < 40) { seeds[2]++; gachaMessage = "Nhận được: 1 Hạt Nước."; }
            else if (roll < 65) { seeds[3]++; gachaMessage = "Nhận được: 1 Hạt Đất."; }
            else if (roll < 99) { seeds[4]++; gachaMessage = "Nhận được: 1 Hạt Gió."; }
            else { hasMagicBook = true; gachaMessage = "HUYỀN THOẠI! Nhận QUYỂN SÁCH PHÉP!"; }
        }
    }

    @Override
    public void mousePressed(int x, int y) {
        if (isReadingBook) { isReadingBook = false; return; }
        if (isGachaOpen) {
            if (btnCloseGacha.contains(x, y)) { isGachaOpen = false; return; }
            for (int i = 1; i <= 4; i++) {
                if (btnGachaRoll[i].contains(x, y)) { rollGacha(i); return; }
            }
            return;
        }

        if (btnSaveGame.contains(x, y)) { gsm.openSaveMenu(this); return; }
        if (btnGacha.contains(x, y)) { isGachaOpen = true; return; }
        if (hasMagicBook && btnReadBook.contains(x, y)) { isReadingBook = true; return; }

        // Nâng cấp cây trên đảo
        if (selectedIsland >= 1 && selectedIsland <= 4 && selectedRow >= 0 && selectedCol >= 0) {
            Plant p = plants[selectedIsland][selectedRow][selectedCol];
            if (btnUpgrade.contains(x, y) && p != null && p.getLevel() < 3) {
                int cost = p.getUpgradeCost();
                if (energies[p.getElementId()] >= cost) {
                    energies[p.getElementId()] -= cost;
                    p.upgrade();
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

                // Trồng cây trên 4 đảo nguyên tố
                if (isl >= 1 && isl <= 4 && mapUnlocked[isl]) {
                    if (plants[isl][clickedRow][clickedCol] == null && seeds[isl] > 0) {
                        plants[isl][clickedRow][clickedCol] = new Plant(clickedRow, clickedCol, isl);
                        seeds[isl]--;
                    }
                }
                break;
            }
        }
    }

    public SaveData exportSaveData() {
        SaveData d = new SaveData();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        d.saveTime = sdf.format(new Date());
        d.difficultyMultiplier = this.difficultyMultiplier;
        System.arraycopy(this.seeds, 0, d.seeds, 0, 5);
        System.arraycopy(this.energies, 0, d.energies, 0, 5);
        System.arraycopy(this.mapUnlocked, 0, d.mapUnlocked, 0, 5);
        d.hasMagicBook = this.hasMagicBook;
        return d;
    }

    public void importSaveData(SaveData d) {
        this.difficultyMultiplier = d.difficultyMultiplier;
        System.arraycopy(d.seeds, 0, this.seeds, 0, 5);
        System.arraycopy(d.energies, 0, this.energies, 0, 5);
        System.arraycopy(d.mapUnlocked, 0, this.mapUnlocked, 0, 5);
        this.hasMagicBook = d.hasMagicBook;
    }

    @Override public void mouseMoved(int x, int y) {}
}