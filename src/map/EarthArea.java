package map;

import state.PlayState;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class EarthArea extends MapArea {
    public static final int ISLAND_ID = 3;
    public static final int SIZE = PlayState.ISLAND_SIZE;

    private BufferedImage islandBgImg;

    public static final int MAP3_WIDTH  = SIZE * PlayState.TILE_W;
    public static final int MAP3_HEIGHT = SIZE * PlayState.TILE_H;

    public EarthArea() {
        super(false);
        loadImages();
    }

    @Override
    public void loadImages() {
        islandBgImg = ImageHelper.loadImage("res/map3/map3_island.png");
        if (islandBgImg == null) islandBgImg = ImageHelper.loadImage("res/map3_earth/map3_island.png");
    }

    @Override
    public void drawTiles(Graphics2D g2) {
        int centerX = ImageHelper.getIslandCenterX(ISLAND_ID);
        int centerY = ImageHelper.getIslandCenterY(ISLAND_ID);

        if (islandBgImg != null) {
            ImageHelper.drawAreaBackground(g2, islandBgImg, centerX, centerY, MAP3_WIDTH, MAP3_HEIGHT);
        } else {
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    ImageHelper.drawIslandTile(g2, null, ISLAND_ID, r, c, new Color(120, 75, 40));
                }
            }
        }
    }

    @Override
    public void drawObjects(Graphics2D g2) {}
}