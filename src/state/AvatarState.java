package state;

import core.GamePanel;
import data.PlayerStats;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class AvatarState extends GameState {

    private PlayState playState;

    public static class AvatarItem {
        public String id;
        public String name;
        public String description;
        public boolean unlocked;
        public String unlockType; // "DEFAULT", "ENERGY", "SEEDS", "STATS"
        public int elementId; // 1: Lửa, 2: Nước, 3: Đất, 4: Gió
        public double costValue;
        public int reqStatValue;

        public AvatarItem(String id, String name, String description, boolean unlocked,
                String unlockType, int elementId, double costValue, int reqStatValue) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.unlocked = unlocked;
            this.unlockType = unlockType;
            this.elementId = elementId;
            this.costValue = costValue;
            this.reqStatValue = reqStatValue;
        }
    }

    private List<AvatarItem> avatars = new ArrayList<>();
    private AvatarItem selectedAvatar;

    private Rectangle col1Rect, col2Rect, col3Rect;
    private Rectangle btnBack;
    private Rectangle btnAction;

    private String message = "Chọn một nhân vật để xem chi tiết hoặc trang bị.";
    private int hoveredIndex = -1;
    private long animStartTime;

    private BufferedImage[] avatarImages = new BufferedImage[6];
    private BufferedImage[] avatarChibiImages = new BufferedImage[6];

    public AvatarState(GameStateManager gsm, PlayState playState) {
        super(gsm);
        this.playState = playState;
        init();
        initAvatars();
    }

    @Override
    public void init() {
        int topY = 65;
        int colH = 460;

        col1Rect = new Rectangle(20, topY, 225, colH);
        col2Rect = new Rectangle(255, topY, 290, colH);
        col3Rect = new Rectangle(555, topY, 225, colH);
        btnBack = new Rectangle(20, 540, 110, 42);
        btnAction = new Rectangle(col2Rect.x + 35, col2Rect.y + colH - 55, 220, 42);

        loadAvatarAssets();
        loadChibiAvatarAssets();
    }

    private void initAvatars() {
        avatars.clear();
        avatars.add(new AvatarItem("lumi", "Lữ Khách Lumi", "Người gieo hạt đầu tiên đặt chân lên Đảo Lửa.", true, "DEFAULT", 1, 0, 0));
        avatars.add(new AvatarItem("fire_spirit", "Hỏa Linh Viêm Hộ", "Bảo hộ ngọn lửa nguyên sơ bất diệt của Đảo Lửa.", false, "ENERGY", 1, 10.0, 0));
        avatars.add(new AvatarItem("aqua_spirit", "Tinh Linh Nước", "Bảo hộ nguồn sống trong lành của Đảo Nước.", false, "ENERGY", 2, 10.0, 0));
        avatars.add(new AvatarItem("terra_guard", "Hộ Vệ Nham Thạch", "Chiến binh mang sức mạnh vững chãi từ Đảo Đất.", false, "SEEDS", 3, 5.0, 0));
        avatars.add(new AvatarItem("zephyr", "Zephyr Phong Kỵ", "Kỵ sĩ làm chủ luồng gió tự do trên tầng không.", false, "ENERGY", 4, 15.0, 0));
        avatars.add(new AvatarItem("sage_ancient", "Hiền Giả Cổ Đạo", "Bậc thầy nắm giữ tri thức phong ấn Đền Trung Tâm.", false, "STATS", 0, 0, 10));
        syncAvatarState();
        selectedAvatar = avatars.get(0);
        for (AvatarItem av : avatars) {
            if (av.id.equals(playState.getActiveAvatarId())) { selectedAvatar = av; break; }
        }
    }

    private void syncAvatarState() {
        if (playState == null) return;
        for (AvatarItem av : avatars) av.unlocked = playState.isAvatarUnlocked(av.id);
    }

    private void loadAvatarAssets() {
        animStartTime = System.currentTimeMillis();
        String[] ids = { "lumi", "fire_spirit", "aqua_spirit", "terra_guard", "zephyr", "sage_ancient" };
        for (int i = 0; i < ids.length; i++) {
            try {
                String path = "res/avt/" + ids[i] + ".png";
                BufferedImage img = ImageIO.read(new File(path));
                avatarImages[i] = img;
                if (img == null) System.err.println("Missing/invalid avatar artwork: " + path + " (fallback enabled)");
            } catch (IOException | SecurityException e) {
                avatarImages[i] = null;
                System.err.println("Cannot load avatar artwork for " + ids[i] + " (fallback enabled): " + e.getMessage());
            }
        }
    }

    private void loadChibiAvatarAssets() {
        String[] ids = { "lumi", "fire_spirit", "aqua_spirit", "terra_guard", "zephyr", "sage_ancient" };
        for (int i = 0; i < ids.length; i++) {
            try {
                String path = "res/avt/" + ids[i] + "_chibi.png";
                BufferedImage img = ImageIO.read(new File(path));
                avatarChibiImages[i] = img;
                if (img == null) System.err.println("Missing/invalid avatar chibi: " + path + " (fallback enabled)");
            } catch (IOException | SecurityException e) {
                avatarChibiImages[i] = null;
                System.err.println("Cannot load avatar chibi for " + ids[i] + " (fallback enabled): " + e.getMessage());
            }
        }
    }

    private void drawImageFit(Graphics2D g2, BufferedImage image, int x, int y, int width, int height) {
        double scale = Math.min((double) width / image.getWidth(), (double) height / image.getHeight());
        int drawWidth = (int) Math.round(image.getWidth() * scale);
        int drawHeight = (int) Math.round(image.getHeight() * scale);
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;
        g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
    }

    private int getAvatarIndex(String id) {
        for (int i = 0; i < avatars.size(); i++) {
            if (avatars.get(i).id.equals(id)) return i;
        }
        return 0;
    }

    @Override
    public void update() {}

    @Override
    public void draw(Graphics2D g2) {
        g2.setColor(new Color(15, 20, 32));
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setColor(new Color(255, 215, 0));
        String title = "HỒ SƠ & BỘ SƯU TẬP AVATAR";
        FontMetrics fmTitle = g2.getFontMetrics();
        g2.drawString(title, (GamePanel.WIDTH - fmTitle.stringWidth(title)) / 2, 42);

        syncAvatarState();
        String activeAvatarId = (playState == null) ? "lumi" : playState.getActiveAvatarId();

        // ==========================================
        // CỘT 1: DANH SÁCH AVATAR
        // ==========================================
        drawPanelBox(g2, col1Rect);
        int itemH = 65, itemGap = 10, itemY = col1Rect.y + 15, itemW = col1Rect.width - 20;

        for (int i = 0; i < avatars.size(); i++) {
            AvatarItem av = avatars.get(i);
            int currY = itemY + i * (itemH + itemGap);
            Rectangle rect = new Rectangle(col1Rect.x + 10, currY, itemW, itemH);

            if (av == selectedAvatar) g2.setColor(new Color(50, 75, 120));
            else if (i == hoveredIndex) g2.setColor(new Color(40, 55, 80));
            else g2.setColor(new Color(28, 36, 52));
            g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);

            if (av.id.equals(activeAvatarId)) g2.setColor(new Color(50, 220, 100));
            else if (av == selectedAvatar) g2.setColor(Color.CYAN);
            else g2.setColor(new Color(70, 90, 120));
            g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);

            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(Color.WHITE);
            g2.drawString(av.name, rect.x + 12, currY + 26);

            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            if (av.id.equals(activeAvatarId)) {
                g2.setColor(new Color(80, 255, 140));
                g2.drawString("[Đang trang bị]", rect.x + 12, currY + 48);
            } else if (av.unlocked) {
                g2.setColor(Color.CYAN);
                g2.drawString("[Đã mở khóa]", rect.x + 12, currY + 48);
            } else {
                g2.setColor(new Color(160, 160, 160));
                g2.drawString("[Chưa sở hữu]", rect.x + 12, currY + 48);
            }
        }

        // ==========================================
        // CỘT 2: CHI TIẾT & ARTWORK
        // ==========================================
        drawPanelBox(g2, col2Rect);

        if (selectedAvatar != null) {
            int artW = 230, artH = 175;
            int artX = col2Rect.x + (col2Rect.width - artW) / 2;
            int artY = col2Rect.y + 18;

            long now = System.currentTimeMillis();
            double timeSec = (now - animStartTime) / 1000.0;
            int bobOffset = (int) (Math.sin(timeSec * 2.5) * 6);

            int selectedIndex = getAvatarIndex(selectedAvatar.id);
            BufferedImage img = avatarImages[selectedIndex];

            if (img != null) {
                drawImageFit(g2, img, artX, artY + bobOffset, artW, artH);
                g2.setColor(new Color(255, 215, 0, 150));
                g2.drawRoundRect(artX - 2, artY + bobOffset - 2, artW + 4, artH + 4, 12, 12);
            } else {
                g2.setColor(new Color(36, 46, 68));
                g2.fillRoundRect(artX, artY, artW, artH, 10, 10);
                g2.setColor(new Color(255, 215, 0));
                g2.drawRoundRect(artX, artY, artW, artH, 10, 10);

                int circleSize = 80;
                int cx = artX + (artW - circleSize) / 2;
                int cy = artY + (artH - circleSize) / 2 + bobOffset;

                Color elemColor = Color.GRAY;
                if (selectedAvatar.elementId == 1) elemColor = new Color(255, 80, 80);
                else if (selectedAvatar.elementId == 2) elemColor = new Color(80, 180, 255);
                else if (selectedAvatar.elementId == 3) elemColor = new Color(210, 140, 80);
                else if (selectedAvatar.elementId == 4) elemColor = new Color(100, 255, 150);
                else if (selectedAvatar.id.equals("lumi")) elemColor = new Color(255, 255, 255);

                g2.setColor(elemColor);
                g2.fillOval(cx, cy, circleSize, circleSize);

                g2.setFont(new Font("Arial", Font.BOLD, 36));
                g2.setColor(new Color(30, 30, 40));
                String initial = selectedAvatar.name.substring(0, 1).toUpperCase();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initial, cx + (circleSize - fm.stringWidth(initial)) / 2, cy + (circleSize + fm.getAscent()) / 2 - 5);

                g2.setFont(new Font("Arial", Font.ITALIC, 12));
                g2.setColor(new Color(180, 190, 210));
                String ph = "[Chưa có asset: " + selectedAvatar.id + ".png]";
                g2.drawString(ph, artX + (artW - g2.getFontMetrics().stringWidth(ph)) / 2, artY + artH - 15);
            }

            g2.setFont(new Font("Arial", Font.BOLD, 17));
            g2.setColor(Color.WHITE);
            g2.drawString(selectedAvatar.name, col2Rect.x + 20, artY + artH + 32);

            g2.setFont(new Font("Arial", Font.ITALIC, 11));
            g2.setColor(new Color(200, 215, 235));
            g2.drawString(selectedAvatar.description, col2Rect.x + 20, artY + artH + 52);

            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            int condY = artY + artH + 85;
            if (selectedAvatar.unlocked) {
                g2.setColor(new Color(100, 240, 140));
                g2.drawString("Trạng thái: Đã có thể sử dụng.", col2Rect.x + 20, condY);
            } else {
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawString("Điều kiện mở khóa:", col2Rect.x + 20, condY);
                g2.setColor(new Color(255, 185, 70));
                String condStr = "";
                if ("ENERGY".equals(selectedAvatar.unlockType)) condStr = "- Cần " + (int) selectedAvatar.costValue + " NL nguyên tố #" + selectedAvatar.elementId;
                else if ("SEEDS".equals(selectedAvatar.unlockType)) condStr = "- Cần " + (int) selectedAvatar.costValue + " Hạt giống nguyên tố #" + selectedAvatar.elementId;
                else if ("STATS".equals(selectedAvatar.unlockType)) condStr = "- Cần trồng tích lũy " + selectedAvatar.reqStatValue + " cây trên các đảo";
                g2.drawString(condStr, col2Rect.x + 20, condY + 22);
            }

            boolean isEquipped = selectedAvatar.id.equals(activeAvatarId);
            if (!selectedAvatar.unlocked) g2.setColor(new Color(190, 115, 20));
            else if (isEquipped) g2.setColor(new Color(60, 75, 95));
            else g2.setColor(new Color(40, 140, 65));

            g2.fillRoundRect(btnAction.x, btnAction.y, btnAction.width, btnAction.height, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(btnAction.x, btnAction.y, btnAction.width, btnAction.height, 8, 8);

            g2.setFont(new Font("Arial", Font.BOLD, 13));
            String actText = "TRANG BỊ";
            if (!selectedAvatar.unlocked) actText = "MỞ KHÓA NGAY";
            else if (isEquipped) actText = "ĐANG TRANG BỊ";

            int tx = btnAction.x + (btnAction.width - g2.getFontMetrics().stringWidth(actText)) / 2;
            g2.drawString(actText, tx, btnAction.y + 26);
        }

        // ==========================================
        // CỘT 3: THỐNG KÊ NGƯỜI CHƠI
        // ==========================================
        drawPanelBox(g2, col3Rect);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(255, 215, 0));
        String statsTitle = "THỐNG KÊ NGƯỜI CHƠI";
        g2.drawString(statsTitle, col3Rect.x + (col3Rect.width - g2.getFontMetrics().stringWidth(statsTitle)) / 2, col3Rect.y + 30);

        if (playState != null) {
            PlayerStats stats = playState.getPlayerStats();
            if (stats == null) stats = new PlayerStats();
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.setColor(new Color(225, 230, 240));

            int statX = col3Rect.x + 14, statY = col3Rect.y + 65, gap = 34;
            g2.drawString("• Thời gian chơi: " + stats.getFormattedPlayTime(), statX, statY); statY += gap;
            g2.drawString("• Cây đã trồng: " + stats.getTotalPlantsPlanted(), statX, statY); statY += gap;
            g2.drawString("• Cây đã nâng cấp: " + stats.getTotalPlantsUpgraded(), statX, statY); statY += gap;
            g2.drawString("• Lượt quay Gacha: " + stats.getTotalGachaRolls(), statX, statY); statY += gap;
            g2.drawString("• NL Lửa thu: " + String.format("%.1f", stats.getTotalEnergyCollected(1)), statX, statY); statY += gap;
            g2.drawString("• NL Nước thu: " + String.format("%.1f", stats.getTotalEnergyCollected(2)), statX, statY); statY += gap;
            g2.drawString("• NL Đất thu: " + String.format("%.1f", stats.getTotalEnergyCollected(3)), statX, statY); statY += gap;
            g2.drawString("• NL Gió thu: " + String.format("%.1f", stats.getTotalEnergyCollected(4)), statX, statY);
        }

        // ==========================================
        // HÀNG DƯỚI CÙNG
        // ==========================================
        g2.setColor(new Color(150, 45, 45));
        g2.fillRoundRect(btnBack.x, btnBack.y, btnBack.width, btnBack.height, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(btnBack.x, btnBack.y, btnBack.width, btnBack.height, 8, 8);
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        String backText = "QUAY LẠI";
        g2.drawString(backText, btnBack.x + (btnBack.width - g2.getFontMetrics().stringWidth(backText)) / 2, btnBack.y + 26);

        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(new Color(255, 215, 80));
        g2.drawString("Thông báo: " + message, btnBack.x + btnBack.width + 16, btnBack.y + 26);
    }

    private void drawPanelBox(Graphics2D g2, Rectangle r) {
        g2.setColor(new Color(22, 28, 42));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setColor(new Color(55, 72, 100));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
    }

    @Override
    public void mousePressed(int x, int y) {
        if (btnBack.contains(x, y)) {
            playState.resetStatsTimer();
            gsm.resumePlayState(playState);
            return;
        }

        int itemH = 65, itemGap = 10, itemY = col1Rect.y + 15, itemW = col1Rect.width - 20;
        for (int i = 0; i < avatars.size(); i++) {
            int currY = itemY + i * (itemH + itemGap);
            Rectangle rect = new Rectangle(col1Rect.x + 10, currY, itemW, itemH);
            if (rect.contains(x, y)) {
                selectedAvatar = avatars.get(i);
                message = "Đã chọn: " + selectedAvatar.name;
                return;
            }
        }

        if (selectedAvatar != null && btnAction.contains(x, y)) {
            syncAvatarState();
            if (selectedAvatar.unlocked) {
                if (playState.equipAvatar(selectedAvatar.id)) {
                    message = "Đã trang bị avatar: " + selectedAvatar.name;
                } else {
                    message = "Không thể trang bị avatar này; đã khôi phục trạng thái mặc định.";
                }
                return;
            }

            boolean canUnlock = false;
            if ("ENERGY".equals(selectedAvatar.unlockType)) {
                if (playState.getEnergy(selectedAvatar.elementId) >= selectedAvatar.costValue) {
                    canUnlock = playState.consumeEnergy(selectedAvatar.elementId, selectedAvatar.costValue);
                } else message = "Không đủ Năng lượng nguyên tố để mở khóa!";
            } else if ("SEEDS".equals(selectedAvatar.unlockType)) {
                if (playState.getSeeds(selectedAvatar.elementId) >= (int) selectedAvatar.costValue) {
                    canUnlock = playState.consumeSeeds(selectedAvatar.elementId, (int) selectedAvatar.costValue);
                } else message = "Không đủ Hạt giống nguyên tố để mở khóa!";
            } else if ("STATS".equals(selectedAvatar.unlockType)) {
                PlayerStats stats = playState.getPlayerStats();
                if (stats != null && stats.getTotalPlantsPlanted() >= selectedAvatar.reqStatValue) canUnlock = true;
                else message = "Cần trồng đủ " + selectedAvatar.reqStatValue + " cây để mở khóa!";
            }

            if (canUnlock && playState.unlockAvatar(selectedAvatar.id)) {
                selectedAvatar.unlocked = true;
                playState.equipAvatar(selectedAvatar.id);
                message = "Mở khóa thành công và đã trang bị! Hãy lưu game để ghi ra file.";
            }
        }
    }

    @Override
    public void mouseMoved(int x, int y) {
        hoveredIndex = -1;
        int itemH = 65, itemGap = 10, itemY = col1Rect.y + 15, itemW = col1Rect.width - 20;
        for (int i = 0; i < avatars.size(); i++) {
            int currY = itemY + i * (itemH + itemGap);
            Rectangle rect = new Rectangle(col1Rect.x + 10, currY, itemW, itemH);
            if (rect.contains(x, y)) {
                hoveredIndex = i;
                break;
            }
        }
    }
}