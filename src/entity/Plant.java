package entity;

import map.ImageHelper;
import state.PlayState;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Plant {
    private int row;
    private int col;
    private int elementId; // 1: Fire, 2: Water, 3: Earth, 4: Wind
    private int level = 1;  // 1, 2, 3

    private BufferedImage imgLv1, imgLv2, imgLv3;

    public Plant(int row, int col, int elementId) {
        this.row = row;
        this.col = col;
        this.elementId = elementId;
        loadImages();
    }

    private void loadImages() {
        String folder = switch (elementId) {
            case 1 -> "map1";
            case 2 -> "map2";
            case 3 -> "map3";
            default -> "map4";
        };
        imgLv1 = ImageHelper.loadImage("res/" + folder + "/plant_lv1.png");
        imgLv2 = ImageHelper.loadImage("res/" + folder + "/plant_lv2.png");
        imgLv3 = ImageHelper.loadImage("res/" + folder + "/plant_lv3.png");
    }

    public double getBaseEnergyPerSecond() {
        if (level == 1) return 0.1;
        if (level == 2) return 0.2;
        return 0.5;
    }

    public int getUpgradeCost() {
        if (level == 1) return 3;
        if (level == 2) return 10;
        return 0;
    }

    public boolean upgrade() {
        if (level < 3) {
            level++;
            return true;
        }
        return false;
    }

    public void draw(Graphics2D g2) {
        // LẤY TOẠ ĐỘ ISOMETRIC CHUẨN XÁC THEO TỪNG ĐẢO TÁCH RỜI
        int isoX = ImageHelper.getIslandIsoX(elementId, row, col);
        int isoY = ImageHelper.getIslandIsoY(elementId, row, col);

        BufferedImage curImg = switch (level) {
            case 1 -> imgLv1;
            case 2 -> imgLv2;
            default -> imgLv3;
        };

        if (curImg != null) {
            int drawW = 100 + (level * 25);
            int drawH = 120 + (level * 30);
            
            // Neo chân gốc cây vào tâm hình thoi của đảo
            int drawX = isoX - (drawW / 2);
            int drawY = isoY - drawH + (PlayState.TILE_H / 2);
            g2.drawImage(curImg, drawX, drawY, drawW, drawH, null);
        } else {
            // Đồ hoạ dự phòng nếu chưa có ảnh
            int size = 30 + (level * 20);
            int drawY = isoY - size + (PlayState.TILE_H / 4);
            Color c = switch (elementId) {
                case 1 -> Color.RED;
                case 2 -> Color.CYAN;
                case 3 -> new Color(139, 69, 19);
                default -> Color.GREEN;
            };
            g2.setColor(c);
            g2.fillOval(isoX - size / 2, drawY, size, size);
            g2.setColor(Color.WHITE);
            g2.drawOval(isoX - size / 2, drawY, size, size);
        }

        // Nhãn Lv hiển thị phía trên đầu cây
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(Color.BLACK);
        g2.drawString("Lv." + level, isoX - 12, isoY - 70 - (level * 15) + 1);
        g2.setColor(Color.YELLOW);
        g2.drawString("Lv." + level, isoX - 13, isoY - 70 - (level * 15));
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getLevel() { return level; }
    public int getElementId() { return elementId; }
}