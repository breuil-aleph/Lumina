package map;

import state.PlayState;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class TempleArea extends MapArea {
    public static final int ISLAND_ID = 0;
    public static final int SIZE = PlayState.TEMPLE_SIZE; // 8x8 ô[cite: 3]

    public static final int MAP0_WIDTH  = SIZE * PlayState.TILE_W;
    public static final int MAP0_HEIGHT = SIZE * PlayState.TILE_H;

    public static double scaleX = 7.5; 
    public static double scaleY = 10; 
    public static int offsetY = -2500; 

    // =========================================================================
    // THÔNG SỐ ĐƯA TOÀN BỘ GỐC CÂY LÊN CAO
    // =========================================================================
    // Kích thước tán cây (rộng để vươn sang các đảo)
    public static int TREE_WIDTH = 4200;

    /**
     * KHOẢNG CÁCH GỐC CÂY CÁCH MẶT ĐẤT (px world):
     * - Giá trị càng lớn thì toàn bộ gốc và rễ cây càng bay lên cao tách khỏi mặt đảo.
     * - Bạn thử các mốc: 600, 1000, 1500 cho đến khi vừa mắt.
     */
    public static int ROOT_HEIGHT_ABOVE_GROUND = 600;

    private BufferedImage islandBgImg;
    private BufferedImage worldTreeImg;

    public TempleArea() {
        super(false);
        loadImages();
    }

    @Override
    public void loadImages() {
        islandBgImg = ImageHelper.loadImage("res/map0/temple_island.png");
        if (islandBgImg == null) islandBgImg = ImageHelper.loadImage("res/temple/temple_island.png");

        worldTreeImg = ImageHelper.loadImage("res/map0/world_tree.png");
        if (worldTreeImg == null) worldTreeImg = ImageHelper.loadImage("res/temple/world_tree.png");
    }

    @Override
    public void drawTiles(Graphics2D g2) {
        int centerX = ImageHelper.getIslandCenterX(ISLAND_ID);
        int centerY = ImageHelper.getIslandCenterY(ISLAND_ID);

        // Vẽ bệ đá đền thờ ở mặt đất
        if (islandBgImg != null) {
            int drawW = (int) (MAP0_WIDTH * scaleX);
            int drawH = (int) (MAP0_HEIGHT * scaleY);
            int drawX = centerX - (drawW / 2);
            int drawY = centerY - (drawH / 2) + offsetY;

            g2.drawImage(islandBgImg, drawX, drawY, drawW, drawH, null);
        } else {
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    ImageHelper.drawIslandTile(g2, null, ISLAND_ID, r, c, new Color(45, 25, 65));
                }
            }
        }
    }

    @Override
    public void drawObjects(Graphics2D g2) {
        if (worldTreeImg == null) return;

        int centerX = ImageHelper.getIslandCenterX(ISLAND_ID);
        int centerY = ImageHelper.getIslandCenterY(ISLAND_ID);

        int treeW = TREE_WIDTH;
        int treeH = (int) Math.round(worldTreeImg.getHeight() * ((double) treeW / worldTreeImg.getWidth()));

        // Căn giữa trục X theo tâm đảo
        int treeX = centerX - (treeW / 2);

        // =====================================================================
        // ĐƯA CẢ GỐC CÂY LÊN CAO:
        // Đáy của ảnh (gốc rễ cây) nằm tại: (centerY + offsetY) - ROOT_HEIGHT_ABOVE_GROUND
        // Do đó toạ độ đỉnh trên treeY = Đáy - treeH
        // =====================================================================
        int rootBaseY = (centerY + offsetY) - ROOT_HEIGHT_ABOVE_GROUND;
        int treeY = rootBaseY - treeH;

        g2.drawImage(worldTreeImg, treeX, treeY, treeW, treeH, null);
    }

    @Override
    public boolean isPlantable(int r, int c) {
        return false;
    }
}