package state;

import data.SaveData;
import data.SaveManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class SaveLoadState extends GameState {
    public static final int MODE_SAVE = 1;
    public static final int MODE_LOAD = 2;

    private int mode;
    private PlayState playStateRef; // Tham chiếu dùng khi lưu

    private Rectangle[] slotBoxes = new Rectangle[4];
    private Rectangle btnBack;
    private String statusMessage = "";

    public SaveLoadState(GameStateManager gsm, int mode, PlayState playStateRef) {
        super(gsm);
        this.mode = mode;
        this.playStateRef = playStateRef;
        init();
    }

    @Override
    public void init() {
        int boxW = 500, boxH = 90;
        int startX = (800 - boxW) / 2;

        slotBoxes[1] = new Rectangle(startX, 150, boxW, boxH);
        slotBoxes[2] = new Rectangle(startX, 260, boxW, boxH);
        slotBoxes[3] = new Rectangle(startX, 370, boxW, boxH);

        btnBack = new Rectangle(StartX(140), 490, 140, 45);
    }

    private int StartX(int width) {
        return (800 - width) / 2;
    }

    @Override
    public void update() {}

    @Override
    public void draw(Graphics2D g2) {
        g2.setColor(new Color(18, 24, 36));
        g2.fillRect(0, 0, 800, 600);

        // Tiêu đề
        g2.setColor(new Color(255, 215, 0));
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        String title = (mode == MODE_SAVE) ? "LƯU TRÒ CHƠI (CHỌN SLOT)" : "TẢI DỮ LIỆU ĐÃ LƯU";
        g2.drawString(title, (800 - g2.getFontMetrics().stringWidth(title)) / 2, 90);

        // Vẽ 3 slot
        for (int i = 1; i <= 3; i++) {
            Rectangle r = slotBoxes[i];
            boolean exists = SaveManager.hasSave(i);

            g2.setColor(exists ? new Color(35, 55, 80) : new Color(30, 35, 45));
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 15, 15);
            g2.setColor(exists ? new Color(70, 160, 240) : Color.DARK_GRAY);
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 15, 15);

            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.setColor(Color.WHITE);
            g2.drawString("SLOT " + i, r.x + 25, r.y + 40);

            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            if (exists) {
                SaveData preview = SaveManager.load(i);
                g2.setColor(new Color(180, 220, 255));
                g2.drawString("Thời gian: " + (preview != null ? preview.saveTime : "Không rõ"), r.x + 130, r.y + 35);
                int mapCount = 0;
                if (preview != null) {
                    for (int m = 1; m <= 4; m++) if (preview.mapUnlocked[m]) mapCount++;
                }
                g2.drawString("Khu vực đã mở: " + mapCount + "/4 Map", r.x + 130, r.y + 60);
            } else {
                g2.setColor(Color.GRAY);
                g2.drawString("[Trống - Chưa có dữ liệu]", r.x + 130, r.y + 50);
            }
        }

        // Nút Quay lại
        g2.setColor(new Color(180, 60, 60));
        g2.fillRoundRect(btnBack.x, btnBack.y, btnBack.width, btnBack.height, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(btnBack.x, btnBack.y, btnBack.width, btnBack.height, 10, 10);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("QUAY LẠI", btnBack.x + 30, btnBack.y + 28);

        // Thông báo trạng thái
        if (!statusMessage.isEmpty()) {
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(Color.YELLOW);
            g2.drawString(statusMessage, (800 - g2.getFontMetrics().stringWidth(statusMessage)) / 2, 470);
        }
    }

    @Override
    public void mousePressed(int x, int y) {
        if (btnBack.contains(x, y)) {
            if (mode == MODE_SAVE && playStateRef != null) {
                gsm.resumePlayState(playStateRef);
            } else {
                gsm.setState(GameStateManager.MENU);
            }
            return;
        }

        for (int i = 1; i <= 3; i++) {
            if (slotBoxes[i].contains(x, y)) {
                if (mode == MODE_SAVE) {
                    if (playStateRef != null) {
                        SaveData data = playStateRef.exportSaveData();
                        boolean success = SaveManager.save(data, i);
                        statusMessage = success ? "Lưu vào Slot " + i + " thành công!" : "Lỗi khi lưu!";
                    }
                } else { // MODE_LOAD
                    if (SaveManager.hasSave(i)) {
                        SaveData data = SaveManager.load(i);
                        if (data != null) {
                            gsm.loadGameFromSave(data);
                        }
                    } else {
                        statusMessage = "Slot " + i + " chưa có dữ liệu để tải!";
                    }
                }
                return;
            }
        }
    }
    @Override public void mouseMoved(int x, int y) {}
}