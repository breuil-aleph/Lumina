package entity;

import map.ImageHelper;
import state.PlayState;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class Plant {
    private int row;
    private int col;
    private int elementId; // 1: Fire, 2: Water, 3: Earth, 4: Wind
    private int level = 1;

    private BufferedImage imgLv1, imgLv2, imgLv3;
    private long upgradeStartTime = 0;
    private static final double UPGRADE_DURATION = 1.4;

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
        return switch (level) {
            case 1 -> 0.1;
            case 2 -> 0.2;
            default -> 0.5;
        };
    }

    public int getUpgradeCost() {
        return switch (level) {
            case 1 -> 3;
            case 2 -> 10;
            default -> 0;
        };
    }

    public boolean upgrade() {
        if (level >= 3) {
            return false;
        }

        level++;
        upgradeStartTime = System.currentTimeMillis();
        return true;
    }

    public void draw(Graphics2D g2) {
        int isoX = ImageHelper.getIslandIsoX(elementId, row, col);
        int isoY = ImageHelper.getIslandIsoY(elementId, row, col);

        BufferedImage curImg = switch (level) {
            case 1 -> imgLv1;
            case 2 -> imgLv2;
            default -> imgLv3;
        };

        int drawW = 100 + level * 25;
        int drawH = 120 + level * 30;
        int drawX = isoX - drawW / 2;
        int drawY = isoY - drawH + PlayState.TILE_H / 2;

        Composite oldComp = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        AffineTransform oldTrans = g2.getTransform();

        double elapsed = (System.currentTimeMillis() - upgradeStartTime) / 1000.0;
        boolean isUpgrading = elapsed >= 0 && elapsed < UPGRADE_DURATION;

        // Tâm chân cây dùng chung cho aura và vòng elip.
        double baseY = isoY + PlayState.TILE_H / 2.0;

        if (isUpgrading) {
            drawUpgradeAuraBack(g2, isoX, baseY, baseY, drawW, drawH, elapsed);
        }

        // Cây trên đảo Gió đung đưa quanh gốc, không làm gốc cây trượt.
        if (elementId == 4) {
            double sec = System.currentTimeMillis() / 1000.0;
            double sway = Math.sin(sec * 2.5 + (row + col) * 0.5) * 0.12;

            g2.translate(isoX, baseY);
            g2.shear(sway, 0);
            g2.translate(-isoX, -baseY);
        }

        if (curImg != null) {
            g2.drawImage(curImg, drawX, drawY, drawW, drawH, null);
        } else {
            int size = 30 + level * 20;
            int fallbackY = isoY - size + PlayState.TILE_H / 4;

            g2.setColor(getThemeColor());
            g2.fillOval(isoX - size / 2, fallbackY, size, size);
            g2.setColor(Color.WHITE);
            g2.drawOval(isoX - size / 2, fallbackY, size, size);
        }

        if (isUpgrading) {
            drawUpgradeParticlesFront(g2, isoX, drawY, drawH, elapsed);
        }

        // Khôi phục transform trước khi vẽ nhãn cấp độ.
        g2.setTransform(oldTrans);
        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);

        drawLevelBadge(g2, isoX, drawY);
    }

    private void drawLevelBadge(Graphics2D g2, int isoX, int drawY) {
        String text = "Lv." + level;
        g2.setFont(new Font("Arial", Font.BOLD, 12));

        int textW = g2.getFontMetrics().stringWidth(text);
        int badgeW = textW + 14;
        int badgeH = 18;
        int badgeX = isoX - badgeW / 2;
        int badgeY = drawY - badgeH - 4;

        g2.setColor(new Color(15, 25, 35, 210));
        g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);

        Color borderCol = switch (level) {
            case 1 -> new Color(180, 220, 255);
            case 2 -> new Color(120, 255, 180);
            default -> new Color(255, 215, 0);
        };

        g2.setColor(borderCol);
        g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);

        g2.setColor(Color.YELLOW);
        g2.drawString(text, badgeX + 7, badgeY + 13);
    }

    private void drawUpgradeAuraBack(
            Graphics2D g2, double cx, double cy, double ringCy,
            int w, int h, double elapsed) {

        float t = (float) Math.max(0.0, Math.min(1.0, elapsed / UPGRADE_DURATION));
        float fade = (float) Math.pow(1.0 - t, 1.3);

        Color theme = getThemeColor();
        Color core = getCoreColor();

        float radius = (float) (w * (0.8 + 0.6 * Math.sin(t * Math.PI)));
        int alphaCore = Math.min(255, (int) (220 * fade));
        int alphaEdge = Math.min(255, (int) (130 * fade));

        RadialGradientPaint auraGlow = new RadialGradientPaint(
                new Point2D.Double(cx, cy),
                radius,
                new float[]{0f, 0.45f, 1f},
                new Color[]{
                        new Color(core.getRed(), core.getGreen(), core.getBlue(), alphaCore),
                        new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), alphaEdge),
                        new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 0)
                });

        g2.setPaint(auraGlow);
        g2.fill(new Ellipse2D.Double(
                cx - radius, cy - radius, radius * 2, radius * 2));

        // Tách tâm vòng elip khỏi tâm quầng sáng để dễ căn chỉnh độc lập.
        double ringW = w * (0.4 + 1.4 * t);
        double ringH = ringW * 0.5;

        g2.setStroke(new BasicStroke(
                Math.max(1.5f, 6f * fade),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));

        g2.setColor(new Color(
                core.getRed(), core.getGreen(), core.getBlue(),
                Math.max(0, Math.min(255, (int) (230 * fade)))));

        g2.draw(new Ellipse2D.Double(
                cx - ringW / 2.0,
                ringCy - 40 - ringH / 2.0,
                ringW,
                ringH));
    }

    private void drawUpgradeParticlesFront(
            Graphics2D g2, int cx, int topY, int h, double elapsed) {

        float t = (float) Math.max(0.0, Math.min(1.0, elapsed / UPGRADE_DURATION));
        float fade = 1.0f - t;
        Color core = getCoreColor();

        g2.setColor(new Color(
                core.getRed(), core.getGreen(), core.getBlue(),
                Math.max(0, Math.min(255, (int) (240 * fade)))));

        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0 + t * 2.0;
            double dist = 20 + 35 * Math.sin(i * 1.5 + t * 4.0);
            double px = cx + Math.cos(angle) * dist;
            double py = topY + h - t * (h + 30) + i * 10;
            double size = 4 + (i % 3) * 2;

            g2.fill(new Ellipse2D.Double(
                    px - size / 2.0, py - size / 2.0, size, size));
        }
    }

    private Color getThemeColor() {
        return switch (elementId) {
            case 1 -> new Color(255, 90, 30);
            case 2 -> new Color(50, 180, 255);
            case 3 -> new Color(185, 100, 255);
            default -> new Color(80, 245, 180);
        };
    }

    private Color getCoreColor() {
        return switch (elementId) {
            case 1 -> new Color(255, 235, 160);
            case 2 -> new Color(215, 245, 255);
            case 3 -> new Color(245, 225, 255);
            default -> new Color(220, 255, 240);
        };
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getLevel() {
        return level;
    }

    public int getElementId() {
        return elementId;
    }
}
