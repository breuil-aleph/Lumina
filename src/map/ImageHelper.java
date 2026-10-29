package map;

import state.PlayState;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ImageHelper {
    // Bộ nhớ đệm tránh việc nạp lại ảnh nhiều lần từ ổ cứng
    private static final Map<String, BufferedImage> imageCache = new HashMap<>();

    /**
     * Nạp ảnh từ file (trả về null nếu không tìm thấy, không làm sập game)
     */
    public static BufferedImage loadImage(String path) {
        try {
            File file = new File(path);
            if (file.exists()) return ImageIO.read(file);
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Nạp ảnh tự động có sử dụng Cache
     */
    public static BufferedImage getOrLoadImage(String path) {
        if (!imageCache.containsKey(path)) {
            BufferedImage img = loadImage(path);
            imageCache.put(path, img);
        }
        return imageCache.get(path);
    }

    // =========================================================================
    // CÔNG THỨC TOẠ ĐỘ TÂM ĐẢO THEO HÌNH NGŨ GIÁC (4 CÁNH DƯỚI, TRÊN TRỐNG)
    // TƯƠNG ỨNG VỚI 4 VÙNG MÀU TRONG ẢNH VŨ TRỤ:
    // - Tâm (0): Đền thờ trung tâm
    // - Đỉnh trên (90 độ): Trống (vùng ánh sáng vàng)
    // - Góc Tây Bắc (~162 độ): Đỏ/Cam -> Đảo Lửa (Island 1)
    // - Góc Đông Bắc (~18 độ): Xanh Dương -> Đảo Nước (Island 2)
    // - Góc Tây Nam (~234 độ): Sấm Tím -> Đảo Đất/Sét (Island 3)
    // - Góc Đông Nam (~306 độ): Xanh Lá -> Đảo Gió (Island 4)
    // =========================================================================

    // Khoảng cách bán kính từ tâm đến các đảo vệ tinh (tính theo pixel)
    // Có thể tăng/giảm số này để các đảo xa nhau hơn hoặc gần tâm hơn
    private static final double PENTAGON_RADIUS = 4200.0;

    /**
     * Lấy toạ độ tâm X của từng đảo trong không gian World
     */
    public static int getIslandCenterX(int islandId) {
        int cx = PlayState.TEMPLE_CENTER_X;

        // Góc tính theo độ toán học (0 = Phải, 90 = Trên, 180 = Trái, 270 = Dưới)
        // Hệ toạ độ màn hình máy tính: Y hướng xuống dưới nên toạ độ Y sẽ lấy -sin(angle)
        return switch (islandId) {
            // Đảo 1 (Lửa - Đỏ): Góc trên bên trái (Tây Bắc ~ 162 độ)
            case 1 -> cx + (int) Math.round(PENTAGON_RADIUS * Math.cos(Math.toRadians(162)));

            // Đảo 2 (Nước - Xanh dương): Góc trên bên phải (Đông Bắc ~ 18 độ)
            case 2 -> cx + (int) Math.round(PENTAGON_RADIUS * Math.cos(Math.toRadians(18)));

            // Đảo 3 (Đất/Sét - Tím): Góc dưới bên trái (Tây Nam ~ 234 độ)
            case 3 -> cx + (int) Math.round(PENTAGON_RADIUS * Math.cos(Math.toRadians(234)));

            // Đảo 4 (Gió - Xanh lá): Góc dưới bên phải (Đông Nam ~ 306 độ)
            case 4 -> cx + (int) Math.round(PENTAGON_RADIUS * Math.cos(Math.toRadians(306)));

            // Đảo 0 (Đền thờ): Nằm chính giữa tâm
            default -> cx;
        };
    }

    /**
     * Lấy toạ độ tâm Y của từng đảo trong không gian World
     * Lưu ý: Nhân hệ số 0.5 theo tỉ lệ phối cảnh Isometric 2:1 (TILE_H / TILE_W)
     */
    // Trong ImageHelper.java

// Độ dời hạ thấp các đảo xuống (muốn đảo xuống thấp bao nhiêu px thì tăng số này lên)
    public static final int GLOBAL_OFFSET_Y = 1800; 

    public static int getIslandCenterY(int islandId) {
    // Thêm GLOBAL_OFFSET_Y vào tâm gốc
    int cy = PlayState.TEMPLE_CENTER_Y + GLOBAL_OFFSET_Y; 
    double isoRatioY = 0.5;

    return switch (islandId) {
        case 1 -> cy - (int) Math.round(PENTAGON_RADIUS * isoRatioY * Math.sin(Math.toRadians(162)));
        case 2 -> cy - (int) Math.round(PENTAGON_RADIUS * isoRatioY * Math.sin(Math.toRadians(18)));
        case 3 -> cy - (int) Math.round(PENTAGON_RADIUS * isoRatioY * Math.sin(Math.toRadians(234)));
        case 4 -> cy - (int) Math.round(PENTAGON_RADIUS * isoRatioY * Math.sin(Math.toRadians(306)));
        default -> cy;
    };
    }

    // =========================================================================
    // TÍNH TOẠ ĐỘ ISOMETRIC TỪNG Ô CỦA TỪNG ĐẢO (NEO THEO TRỌNG TÂM ĐẢO)
    // =========================================================================

    /**
     * Tính toạ độ X trên màn hình của ô (r, c) trên hòn đảo cụ thể
     */
    public static int getIslandIsoX(int islandId, int r, int c) {
        int islandCenterX = getIslandCenterX(islandId);
        int size = (islandId == 0) ? PlayState.TEMPLE_SIZE : PlayState.ISLAND_SIZE;

        double offsetC = c - (size - 1) / 2.0;
        double offsetR = r - (size - 1) / 2.0;

        return (int) Math.round(islandCenterX + (offsetC - offsetR) * (PlayState.TILE_W / 2.0));
    }

    /**
     * Tính toạ độ Y trên màn hình của ô (r, c) trên hòn đảo cụ thể
     */
    public static int getIslandIsoY(int islandId, int r, int c) {
        int islandCenterY = getIslandCenterY(islandId);
        int size = (islandId == 0) ? PlayState.TEMPLE_SIZE : PlayState.ISLAND_SIZE;

        double offsetC = c - (size - 1) / 2.0;
        double offsetR = r - (size - 1) / 2.0;

        return (int) Math.round(islandCenterY + (offsetC + offsetR) * (PlayState.TILE_H / 2.0));
    }

    // =========================================================================
    // CÁC HÀM VẼ ĐỒ HOẠ
    // =========================================================================

    /**
     * Vẽ một ô đất hình thoi thuộc một đảo
     */
    public static void drawIslandTile(Graphics2D g2, BufferedImage img, int islandId, int r, int c, Color fallback) {
        int isoX = getIslandIsoX(islandId, r, c);
        int isoY = getIslandIsoY(islandId, r, c);

        if (img != null) {
            g2.drawImage(img, isoX - (PlayState.TILE_W / 2), isoY - (PlayState.TILE_H / 2), PlayState.TILE_W, PlayState.TILE_H, null);
        } else {
            g2.setColor(fallback);
            int[] xp = { isoX, isoX + PlayState.TILE_W / 2, isoX, isoX - PlayState.TILE_W / 2 };
            int[] yp = { isoY - PlayState.TILE_H / 2, isoY, isoY + PlayState.TILE_H / 2, isoY };
            g2.fillPolygon(xp, yp, 4);
            g2.setColor(new Color(255, 255, 255, 45));
            g2.drawPolygon(xp, yp, 4);
        }
    }

    /**
     * Vẽ vật thể/trang trí cắm gốc vào tâm ô của một đảo
     */
    public static void drawIslandObject(Graphics2D g2, BufferedImage img, int islandId, int r, int c, int w, int h) {
        if (img == null) return;
        int isoX = getIslandIsoX(islandId, r, c);
        int isoY = getIslandIsoY(islandId, r, c);

        int drawX = isoX - (w / 2);
        int drawY = isoY - h + (PlayState.TILE_H / 2);
        g2.drawImage(img, drawX, drawY, w, h, null);
    }

    /**
     * Vẽ ảnh nền to bao phủ trọn vẹn toàn bộ một hòn đảo dựa theo tâm
     */
    public static void drawAreaBackground(Graphics2D g2, BufferedImage img, int centerX, int centerY, int totalW, int totalH) {
        if (img == null) return;
        int drawX = centerX - (totalW / 2);
        int drawY = centerY - (totalH / 2);
        g2.drawImage(img, drawX, drawY, totalW, totalH, null);
    }

    // Các hàm tương thích ngược (nếu các file cũ còn gọi)
    public static int getIsoX(int r, int c) {
        return getIslandIsoX(0, r, c);
    }

    public static int getIsoY(int r, int c) {
        return getIslandIsoY(0, r, c);
    }

    public static void drawTile(Graphics2D g2, BufferedImage img, int r, int c, Color fallback) {
        drawIslandTile(g2, img, 0, r, c, fallback);
    }

    public static void drawObject(Graphics2D g2, BufferedImage img, int r, int c, int w, int h) {
        drawIslandObject(g2, img, 0, r, c, w, h);
    }
}