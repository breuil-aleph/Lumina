package entity;

import map.ImageHelper;
import state.PlayState;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class Plant {
    private int row;
    private int col;
    private int elementId; // 1: Fire, 2: Water, 3: Earth, 4: Wind
    private int level = 1;  // 1, 2, 3

    private BufferedImage imgLv1, imgLv2, imgLv3;

    // Biến điều khiển hoạt ảnh riêng cho cây Map 1
    private double animTick;
    private double spawnScale = 0.2;

    public Plant(int row, int col, int elementId) {
        this.row = row;
        this.col = col;
        this.elementId = elementId;
        this.animTick = (row * 1.7 + col * 2.3);
        loadImages();
    }

    private void loadImages() {
        String folder = switch (elementId) {
            case 1 -> "map1";
            case 2 -> "map2";
            case 3 -> "map3";
            default -> "map4";
        };
        imgLv1 = ImageHelper.getOrLoadImage("res/" + folder + "/plant_lv1.png");
        imgLv2 = ImageHelper.getOrLoadImage("res/" + folder + "/plant_lv2.png");
        imgLv3 = ImageHelper.getOrLoadImage("res/" + folder + "/plant_lv3.png");
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
            spawnScale = 0.55;
            if ((level == 2 && imgLv2 == null) || (level == 3 && imgLv3 == null)) {
                loadImages();
            }
            return true;
        }
        return false;
    }

    public void draw(Graphics2D g2) {
        int isoX = ImageHelper.getIslandIsoX(elementId, row, col);
        int isoY = ImageHelper.getIslandIsoY(elementId, row, col);

        BufferedImage curImg = switch (level) {
            case 1 -> imgLv1;
            case 2 -> imgLv2;
            default -> imgLv3;
        };

        // =====================================================================
        // NẾU KHÔNG PHẢI MAP 1 (LỬA): VẼ MẶC ĐỊNH NGUYÊN BẢN CỦA NHÓM
        // =====================================================================
        if (elementId != 1) {
            if (curImg != null) {
                int drawW = 100 + (level * 25);
                int drawH = 120 + (level * 30);
                int drawX = isoX - (drawW / 2);
                int drawY = isoY - drawH + (PlayState.TILE_H / 2);
                g2.drawImage(curImg, drawX, drawY, drawW, drawH, null);
            } else {
                int size = 30 + (level * 20);
                int drawY = isoY - size + (PlayState.TILE_H / 4);
                Color c = switch (elementId) {
                    case 2 -> Color.CYAN;
                    case 3 -> new Color(139, 69, 19);
                    default -> Color.GREEN;
                };
                g2.setColor(c);
                g2.fillOval(isoX - size / 2, drawY, size, size);
                g2.setColor(Color.WHITE);
                g2.drawOval(isoX - size / 2, drawY, size, size);
            }

            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(Color.BLACK);
            g2.drawString("Lv." + level, isoX - 12, isoY - 70 - (level * 15) + 1);
            g2.setColor(Color.YELLOW);
            g2.drawString("Lv." + level, isoX - 13, isoY - 70 - (level * 15));
            return;
        }

        // =====================================================================
        // CHỈ DÀNH RIÊNG CHO CÂY MAP 1 (elementId == 1 - ĐẢO LỬA)
        // =====================================================================
        animTick += 0.055;
        if (spawnScale < 1.0) {
            spawnScale = Math.min(1.0, spawnScale + 0.06);
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ĐỒNG BỘ TỌA ĐỘ GỐC CÂY: Điểm chạm đất nằm khớp 100% với đáy ảnh cây
        int footY = isoY + (PlayState.TILE_H / 2);
        Color fireColor = new Color(255, 120, 20);

        // 1. TÍNH KÍCH THƯỚC THEO ĐÚNG TỈ LỆ GỐC CỦA ẢNH (KHÔNG BAO GIỜ BỊ DẸT)
        int targetH = switch (level) {
            case 1 -> 135;
            case 2 -> 180;
            default -> 245; // Lv.3 cao lớn vượt trội
        };

        double breatheScale = 1.0 + Math.sin(animTick * 2.0) * 0.02;
        int baseH = (int) (targetH * spawnScale * breatheScale);
        int baseW;

        if (curImg != null && curImg.getHeight() > 0) {
            // Lấy đúng tỉ lệ Rộng / Cao của file ảnh PNG để không làm biến dạng cây
            double aspectRatio = (double) curImg.getWidth() / curImg.getHeight();
            baseW = (int) Math.round(baseH * aspectRatio);
        } else {
            baseW = (int) (baseH * 0.85);
        }

        // 2. Vẽ bóng đổ mềm ngay dưới đáy gốc cây (nâng lên 8px để ôm vừa khít rễ)
        drawSoftNaturalShadow(g2, isoX, footY - 8, baseW);

        // 3. Thân cây đung đưa nhẹ quanh đúng gốc (isoX, footY)
        double swayAngleDeg = Math.sin(animTick) * (1.8 + level * 0.3);

        AffineTransform oldTransform = g2.getTransform();
        g2.rotate(Math.toRadians(swayAngleDeg), isoX, footY);

        if (curImg != null) {
            int drawX = isoX - (baseW / 2);
            int drawY = footY - baseH;
            g2.drawImage(curImg, drawX, drawY, baseW, baseH, null);
        } else {
            int size = (int) ((40 + (level * 22)) * spawnScale);
            int drawY = footY - size;
            g2.setColor(fireColor);
            g2.fillOval(isoX - size / 2, drawY, size, size);
            g2.setColor(Color.WHITE);
            g2.drawOval(isoX - size / 2, drawY, size, size);
        }

        g2.setTransform(oldTransform);

        // 4. Đom đóm lửa bay quanh tán cây
        drawElementalFireflies(g2, isoX, footY, baseH, fireColor);

        // 5. Nhãn Level trên đầu cây
        drawLevelBadge(g2, isoX, footY - baseH - 12, fireColor);
    }

    /**
     * Vẽ bóng đổ mềm tự nhiên tỉ lệ theo bề ngang thực tế của cây
     */
    private void drawSoftNaturalShadow(Graphics2D g2, int shadowCenterX, int shadowCenterY, int treeWidth) {
        int radius = Math.max(10, (int) (treeWidth * 0.36));

        AffineTransform oldTrans = g2.getTransform();
        Paint oldPaint = g2.getPaint();

        // Nén trục Y tỉ lệ 2:1 chuẩn Isometric để bóng trải phẳng trên mặt sàn
        g2.translate(shadowCenterX, shadowCenterY);
        g2.scale(1.0, 0.5);

        RadialGradientPaint shadowGrad = new RadialGradientPaint(
            new Point2D.Float(0, 0),
            radius,
            new float[]{0.0f, 0.5f, 1.0f},
            new Color[]{
                new Color(0, 0, 0, 125), // Đậm vừa phải dưới đáy rễ
                new Color(0, 0, 0, 50),  // Mờ dần ra xung quanh
                new Color(0, 0, 0, 0)    // Tan biến hoàn toàn, không có viền
            }
        );

        g2.setPaint(shadowGrad);
        g2.fillOval(-radius, -radius, radius * 2, radius * 2);

        g2.setPaint(oldPaint);
        g2.setTransform(oldTrans);
    }

    private void drawElementalFireflies(Graphics2D g2, int isoX, int footY, int treeHeight, Color elemColor) {
        Composite oldComp = g2.getComposite();
        int particleCount = 2 + level * 2;

        for (int i = 0; i < particleCount; i++) {
            double pPhase = animTick * 1.3 + i * (Math.PI * 2 / particleCount);
            double heightRatio = ((animTick * 0.4 + i * 0.27) % 1.0);
            double orbitRadius = (28 + level * 12) * Math.sin(heightRatio * Math.PI);

            int px = (int) (isoX + Math.cos(pPhase) * orbitRadius);
            int py = (int) (footY - heightRatio * (treeHeight * 0.92));

            float alpha = (float) Math.sin(heightRatio * Math.PI);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));

            g2.setColor(elemColor);
            g2.fillOval(px - 6, py - 6, 12, 12);

            g2.setColor(new Color(255, 255, 220));
            g2.fillOval(px - 2, py - 2, 5, 5);
        }
        g2.setComposite(oldComp);
    }

    private void drawLevelBadge(Graphics2D g2, int centerX, int badgeY, Color elemColor) {
        String text = (level == 3) ? "★ MAX Lv.3" : "Lv." + level;
        Font font = new Font("Arial", Font.BOLD, 13);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(text);
        int boxW = textW + 16;
        int boxH = 22;
        int boxX = centerX - boxW / 2;
        int boxY = badgeY - boxH / 2 + (int) (Math.sin(animTick * 2.0) * 3);

        g2.setColor(new Color(15, 20, 30, 200));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);

        g2.setColor(level == 3 ? new Color(255, 215, 0) : elemColor);
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 10, 10);

        g2.setColor(level == 3 ? new Color(255, 240, 120) : Color.WHITE);
        g2.drawString(text, boxX + 8, boxY + 15);
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getLevel() { return level; }
    public int getElementId() { return elementId; }
}