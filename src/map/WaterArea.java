package map;

import state.PlayState;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class WaterArea extends MapArea {
    public static final int ISLAND_ID = 2;
    public static final int SIZE = PlayState.ISLAND_SIZE;

    public static final int MAP2_WIDTH  = SIZE * PlayState.TILE_W;
    public static final int MAP2_HEIGHT = SIZE * PlayState.TILE_H;

    // --- CÁC THÔNG SỐ CO GIÃN VÀ ĐỘ DỜI BỀ MẶT ĐẢO ---
    public static double scaleX = 1.05; // Tỉ lệ co giãn chiều ngang
    public static double scaleY = 1.32; // Kéo dài riêng chiều dọc
    public static int offsetY = 60;     // Dịch chuyển lên (-) hoặc xuống (+) để khớp lưới ô đất

    private BufferedImage islandBgImg;

    // --- 1. KHAI BÁO BIẾN LƯU ẢNH CÂY VÀ BIẾN ĐẾM THỜI GIAN ĐUNG ĐƯA ---
    private BufferedImage plantLv1Img;
    private BufferedImage plantLv2Img;
    private BufferedImage plantLv3Img;

    // Biến đếm thời gian để tính toán góc nghiêng đung đưa
    private double swayTimer = 0;

    public WaterArea() {
        super(false);
        loadImages();
    }

    @Override
    public void loadImages() {
        // Load ảnh nền của Map 2
        islandBgImg = ImageHelper.loadImage("res/map2/map2_island.png");
        if (islandBgImg == null) islandBgImg = ImageHelper.loadImage("res/map2_water/map2_island.png");

        // --- 2. LOAD ẢNH 3 CÂY CỦA MAP 2 ---
        plantLv1Img = ImageHelper.loadImage("res/map2/plant_lv1.png");
        plantLv2Img = ImageHelper.loadImage("res/map2/plant_lv2.png");
        plantLv3Img = ImageHelper.loadImage("res/map2/plant_lv3.png");
    }

    // Thêm hàm này vào WaterArea.java
    @Override
    public boolean isPlantable(int r, int c) {
        // Né vành biển ngoài cùng
        if (r < 1 || r > SIZE - 2 || c < 1 || c > SIZE - 2) return false;

        // Né lõi hồ nước sâu ở giữa (4x4 ô trung tâm)
        if (r >= 4 && r <= 7 && c >= 4 && c <= 7) return false;

        // Trồng trên các bờ đất bãi bồi
        return true;
    }

    @Override
    public void drawTiles(Graphics2D g2) {
        int centerX = ImageHelper.getIslandCenterX(ISLAND_ID);
        int centerY = ImageHelper.getIslandCenterY(ISLAND_ID);

        if (islandBgImg != null) {
            // Tính toán kích thước theo scaleX và scaleY riêng biệt
            int drawW = (int) (MAP2_WIDTH * scaleX);
            int drawH = (int) (MAP2_HEIGHT * scaleY);

            // Căn giữa tâm X và bù trừ độ dời offsetY theo chiều dọc
            int drawX = centerX - (drawW / 2);
            int drawY = centerY - (drawH / 2) + offsetY;

            g2.drawImage(islandBgImg, drawX, drawY, drawW, drawH, null);
        } else {
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    ImageHelper.drawIslandTile(g2, null, ISLAND_ID, r, c, new Color(30, 90, 150));
                }
            }
        }
    }

    @Override
    public void drawObjects(Graphics2D g2) {
        // --- 3. CẬP NHẬT NHỊP ĐUNG ĐƯA THEO MỖI FRAME ---
        swayTimer += 0.04;
    }

    /**
     * --- 4. HÀM VẼ CÂY ĐUNG ĐƯA BẰNG GRAPHICS2D ---
     */
    public void drawSwayingPlant(Graphics2D g2, BufferedImage plantImg, int x, int y) {
        if (plantImg == null) return;

        double angleInDegrees = Math.sin(swayTimer) * 4.0;

        double pivotX = x + plantImg.getWidth() / 2.0;
        double pivotY = y + plantImg.getHeight();

        AffineTransform oldTransform = g2.getTransform();
        g2.rotate(Math.toRadians(angleInDegrees), pivotX, pivotY);
        g2.drawImage(plantImg, x, y, null);
        g2.setTransform(oldTransform);
    }

    public BufferedImage getPlantLv1Img() { return plantLv1Img; }
    public BufferedImage getPlantLv2Img() { return plantLv2Img; }
    public BufferedImage getPlantLv3Img() { return plantLv3Img; }
}