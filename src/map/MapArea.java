package map;

import java.awt.Graphics2D;

public abstract class MapArea {
    protected boolean isUnlocked;

    public MapArea(boolean isUnlocked) {
        this.isUnlocked = isUnlocked;
    }

    public abstract void loadImages();
    public abstract void drawTiles(Graphics2D g2);
    public abstract void drawObjects(Graphics2D g2);

    /**
     * Kiểm tra ô (r, c) có cho phép trồng cây hay không
     * Mặc định là cho trồng nếu không override
     */
    public boolean isPlantable(int r, int c) {
        return true;
    }

    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) { this.isUnlocked = unlocked; }
}