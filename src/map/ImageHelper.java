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
    // CÔNG THỨC TOẠ ĐỘ TÂM ĐẢO TÁCH RỜI CHUẨN ISOMETRIC (KHÔNG BỊ CHÈN NHAU)
    // =========================================================================
    
    /**
     * Lấy toạ độ tâm X của từng đảo trong không gian World
     * islandId: 0: Đền, 1: Lửa (Bắc), 2: Nước (Đông), 3: Đất (Tây), 4: Gió (Nam)
     */
    public static int getIslandCenterX(int islandId) {
        int cx = PlayState.TEMPLE_CENTER_X;

        // Bán kính ngang Đền (8 ô) = 4 * TILE_W
        // Bán kính ngang Đảo Con (12 ô) = 6 * TILE_W
        // Khoảng cách tối thiểu để 2 đỉnh không chạm nhau = (4 + 6) * TILE_W
        // Cộng thêm khe vực thẳm PlayState.ISLAND_GAP
        int distIsoX = ((PlayState.TEMPLE_SIZE + PlayState.ISLAND_SIZE) / 2) * PlayState.TILE_W + PlayState.ISLAND_GAP;

        return switch (islandId) {
            case 1 -> cx;             // Đảo 1 (Lửa - Bắc): Thẳng hàng tâm X
            case 2 -> cx + distIsoX;  // Đảo 2 (Nước - Đông): Đẩy sang phải
            case 3 -> cx - distIsoX;  // Đảo 3 (Đất - Tây): Đẩy sang trái
            case 4 -> cx;             // Đảo 4 (Gió - Nam): Thẳng hàng tâm X
            default -> cx;            // Đảo 0 (Đền): Nằm chính giữa
        };
    }

    /**
     * Lấy toạ độ tâm Y của từng đảo trong không gian World
     */
    public static int getIslandCenterY(int islandId) {
        int cy = PlayState.TEMPLE_CENTER_Y;

        // Khoảng cách trục dọc theo tỉ lệ 2:1 của lưới Isometric
        int distIsoY = ((PlayState.TEMPLE_SIZE + PlayState.ISLAND_SIZE) / 2) * PlayState.TILE_H + PlayState.ISLAND_GAP;

        return switch (islandId) {
            case 1 -> cy - distIsoY;  // Đảo 1 (Lửa - Bắc): Đẩy lên trên
            case 2 -> cy;             // Đảo 2 (Nước - Đông): Ngang hàng tâm Đền
            case 3 -> cy;             // Đảo 3 (Đất - Tây): Ngang hàng tâm Đền
            case 4 -> cy + distIsoY;  // Đảo 4 (Gió - Nam): Đẩy xuống dưới
            default -> cy;            // Đảo 0 (Đền): Nằm chính giữa
        };
    }

    // =========================================================================
    // TÍNH TOẠ ĐỘ ISOMETRIC TỪNG Ô CỦA TỪNG ĐẢO (NEO THEO TRỌNG TÂM)
    // =========================================================================

    /**
     * Tính toạ độ X trên màn hình của ô (r, c) trên hòn đảo cụ thể
     */
    public static int getIslandIsoX(int islandId, int r, int c) {
        int islandCenterX = getIslandCenterX(islandId);
        int size = (islandId == 0) ? PlayState.TEMPLE_SIZE : PlayState.ISLAND_SIZE;

        // Khoảng lệch của ô (r, c) so với tâm của đảo đó
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