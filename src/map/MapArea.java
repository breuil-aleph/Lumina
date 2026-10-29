package map;

import java.awt.Graphics2D;

public abstract class MapArea {
    protected boolean isUnlocked;

    public MapArea(boolean isUnlocked) {
        this.isUnlocked = isUnlocked;
    }

    // Nạp ảnh
    public abstract void loadImages();

    // Vẽ toàn bộ ô đất nền của khu vực
    public abstract void drawTiles(Graphics2D g2);

    // Vẽ cây cối/vật trang trí trên các ô đất
    public abstract void drawObjects(Graphics2D g2);

    // Kiểm tra xem ô (r, c) có cho phép trồng cây và chọn ô hay không
    public boolean isPlantable(int r, int c) {
        return true; 
    }

    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) { this.isUnlocked = unlocked; }
}