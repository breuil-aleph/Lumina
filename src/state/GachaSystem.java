package state;

import core.GamePanel;
import map.ImageHelper;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hệ thống Gacha dùng ảnh. Toàn bộ ảnh nằm trong res/gachasystem/
 *
 *   button.png      nút GACHA ở HUD góc phải dưới
 *   bg.png          nền toàn màn hình gacha
 *   bar_1..4.png    4 thanh chọn banner (Lửa, Nước, Đất, Gió)
 *   banner_bg.png   khung banner hiện ra khi bấm thanh
 *   btn_x1.png      nút quay x1
 *   btn_x10.png     nút quay x10
 *   result_bg.png   khung kết quả (hình chữ nhật ngang)
 *   seed_1..4.png   hạt giống Lửa, Nước, Đất, Gió
 *   none.png        (tuỳ chọn) ô "hụt"
 *   book.png        (tuỳ chọn) Sách Phép
 *   close.png       (tuỳ chọn) nút đóng
 *   back.png        (tuỳ chọn) nút quay lại
 *
 * Ảnh nào chưa có thì tự vẽ hình tạm để vẫn chạy được.
 */
public class GachaSystem {
    private static final String DIR = "res/gachasystem/";

    // ---- Chi phí mỗi lượt quay (đơn vị NL của banner đang chọn) ----
    // Muốn mỗi banner một giá khác nhau thì sửa 2 mảng này (index 1..4)
    private static final int[] COST_X1  = { 0, 1, 1, 1, 1 };
    private static final int[] COST_X10 = { 0, 10, 10, 10, 10 };

    // ---- Bảng tỉ lệ: ngưỡng cộng dồn (trên 100) và vật phẩm tương ứng ----
    // Vật phẩm: 0 = hụt, 1..4 = hạt Lửa/Nước/Đất/Gió, 5 = Sách Phép
    // Phần tử cuối của mỗi hàng là phần thưởng hiếm (mở khoá đảo / sách)
    private static final int[][] CUT = {
            null,
            { 50, 99, 100 },
            { 70, 99, 100 },
            { 30, 70, 99, 100 },
            { 20, 40, 65, 99, 100 }
    };
    private static final int[][] DROP = {
            null,
            { 0, 1, 2 },
            { 1, 2, 3 },
            { 1, 2, 3, 4 },
            { 1, 2, 3, 4, 5 }
    };

    private static final String[] ELEMENT = { "", "Lửa", "Nước", "Đất", "Gió" };
    private static final String[] TITLES = {
            "",
            "Banner Lửa (50% Hụt | 49% Hạt Lửa | 1% Hạt Nước)",
            "Banner Nước (70% Hạt Lửa | 29% Hạt Nước | 1% Hạt Đất)",
            "Banner Đất (30% Lửa | 40% Nước | 29% Đất | 1% Hạt Gió)",
            "Banner Gió (20% Lửa | 20% Nước | 25% Đất | 34% Gió | 1% Sách)"
    };
    private static final Color[] THEME = {
            Color.GRAY, new Color(180, 50, 40), new Color(30, 100, 180),
            new Color(139, 85, 40), new Color(40, 140, 90)
    };

    private static final int SCREEN_BANNERS = 0; // chọn 1 trong 4 thanh
    private static final int SCREEN_BANNER = 1;  // khung banner đã chọn
    private static final int SCREEN_RESULT = 2;  // kết quả

    private final int[] seeds;
    private final double[] energies;
    private final boolean[] mapUnlocked;
    private final Runnable onGetMagicBook;

    private boolean open = false;
    private int screen = SCREEN_BANNERS;
    private int selected = 1;
    private String message = "";
    private final Random random = new Random();

    // Kết quả lần quay gần nhất
    private final List<Integer> lastResults = new ArrayList<>();
    private final List<String> lastNotes = new ArrayList<>();

    // ---- Vùng bấm (toạ độ màn hình 800x600) ----
    private final Rectangle btnOpen = new Rectangle(GamePanel.WIDTH - 120, GamePanel.HEIGHT - 100, 100, 80);
    private final Rectangle btnClose = new Rectangle(700, 30, 50, 50);
    private final Rectangle btnBack = new Rectangle(50, 30, 50, 50);
    private final Rectangle[] bars = new Rectangle[5];
    private final Rectangle bannerBox = new Rectangle(130, 110, 540, 380);
    private final Rectangle btnX1 = new Rectangle(190, 390, 180, 60);
    private final Rectangle btnX10 = new Rectangle(430, 390, 180, 60);
    private final Rectangle resultBox = new Rectangle(60, 220, 680, 170);

    public GachaSystem(int[] seeds, double[] energies, boolean[] mapUnlocked, Runnable onGetMagicBook) {
        this.seeds = seeds;
        this.energies = energies;
        this.mapUnlocked = mapUnlocked;
        this.onGetMagicBook = onGetMagicBook;

        for (int i = 1; i <= 4; i++) {
            bars[i] = new Rectangle(150, 130 + (i - 1) * 100, 500, 80);
        }
    }

    public boolean isOpen() {
        return open;
    }

    // =====================================================================
    // Xử lý click: trả về true nếu Gacha đã "ăn" cú click
    // =====================================================================
    public boolean handleClick(int x, int y) {
        if (!open) {
            if (btnOpen.contains(x, y)) {
                open = true;
                screen = SCREEN_BANNERS;
                message = "";
                return true;
            }
            return false;
        }

        if (btnClose.contains(x, y)) {
            open = false;
            return true;
        }

        switch (screen) {
            case SCREEN_BANNERS:
                for (int i = 1; i <= 4; i++) {
                    if (bars[i].contains(x, y)) {
                        selected = i;
                        screen = SCREEN_BANNER;
                        message = "Chọn số lượt quay.";
                        break;
                    }
                }
                break;

            case SCREEN_BANNER:
                if (btnBack.contains(x, y)) {
                    screen = SCREEN_BANNERS;
                } else if (btnX1.contains(x, y)) {
                    doRoll(1);
                } else if (btnX10.contains(x, y)) {
                    doRoll(10);
                }
                break;

            case SCREEN_RESULT:
                // Bấm bất kỳ chỗ nào để quay lại banner
                screen = SCREEN_BANNER;
                break;
        }
        return true; // modal đang mở thì chặn mọi click bên dưới
    }

    // =====================================================================
    // Vẽ
    // =====================================================================
    /** Nút GACHA ở HUD góc phải dưới */
    public void drawButton(Graphics2D g2) {
        BufferedImage img = img("button.png");
        if (img != null) {
            g2.drawImage(img, btnOpen.x, btnOpen.y, btnOpen.width, btnOpen.height, null);
        } else {
            g2.setColor(new Color(80, 50, 20, 230));
            g2.fillRoundRect(btnOpen.x, btnOpen.y, btnOpen.width, btnOpen.height, 15, 15);
            g2.setColor(new Color(255, 215, 0));
            g2.drawRoundRect(btnOpen.x, btnOpen.y, btnOpen.width, btnOpen.height, 15, 15);
            drawCentered(g2, "GACHA", btnOpen, new Font("Arial", Font.BOLD, 16), Color.YELLOW);
        }
    }

    /** Toàn bộ giao diện gacha, chỉ vẽ khi đang mở */
    public void drawModal(Graphics2D g2) {
        if (!open) return;

        // Nền
        BufferedImage bg = img("bg.jpg");
        if (bg != null) {
            g2.drawImage(bg, 0, 0, GamePanel.WIDTH, GamePanel.HEIGHT, null);
        } else {
            g2.setColor(new Color(20, 25, 40, 245));
            g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);
        }

        drawImageButton(g2, "close.png", btnClose, new Color(200, 40, 40), "X");

        switch (screen) {
            case SCREEN_BANNERS: drawBanners(g2); break;
            case SCREEN_BANNER:  drawBanner(g2); break;
            case SCREEN_RESULT:  drawResult(g2); break;
        }
    }

    private void drawBanners(Graphics2D g2) {
        drawCentered(g2, "VÒNG QUAY NGUYÊN TỐ", new Rectangle(0, 40, GamePanel.WIDTH, 40),
                new Font("Arial", Font.BOLD, 26), Color.YELLOW);

        for (int i = 1; i <= 4; i++) {
            drawImageButton(g2, "bar_" + i + ".jpg", bars[i], THEME[i], "Banner " + ELEMENT[i]);
        }
    }

    private void drawBanner(Graphics2D g2) {
        drawImageButton(g2, "back.png", btnBack, new Color(90, 90, 110), "<");

        // Khung banner
        BufferedImage bb = img("banner_bg.jpg");
        if (bb != null) {
            g2.drawImage(bb, bannerBox.x, bannerBox.y, bannerBox.width, bannerBox.height, null);
        } else {
            g2.setColor(new Color(30, 36, 55));
            g2.fillRoundRect(bannerBox.x, bannerBox.y, bannerBox.width, bannerBox.height, 20, 20);
            g2.setColor(THEME[selected]);
            g2.drawRoundRect(bannerBox.x, bannerBox.y, bannerBox.width, bannerBox.height, 20, 20);
        }

        drawCentered(g2, "BANNER " + ELEMENT[selected].toUpperCase(),
                new Rectangle(bannerBox.x, bannerBox.y + 20, bannerBox.width, 40),
                new Font("Arial", Font.BOLD, 24), Color.YELLOW);
        drawCentered(g2, TITLES[selected],
                new Rectangle(bannerBox.x, bannerBox.y + 70, bannerBox.width, 30),
                new Font("Arial", Font.PLAIN, 13), Color.WHITE);

        drawCentered(g2, "NL " + ELEMENT[selected] + " hiện có: " + String.format("%.1f", energies[selected]),
                new Rectangle(bannerBox.x, bannerBox.y + 120, bannerBox.width, 30),
                new Font("Arial", Font.BOLD, 16), new Color(200, 240, 255));

        // 2 nút quay
        drawImageButton(g2, "btn_x1.jpg", btnX1, THEME[selected], "QUAY x1");
        drawImageButton(g2, "btn_x10.jpg", btnX10, THEME[selected], "QUAY x10");

        // Giá dưới mỗi nút
        Font small = new Font("Arial", Font.BOLD, 14);
        drawCentered(g2, "Cần " + COST_X1[selected] + " NL", new Rectangle(btnX1.x, btnX1.y + 62, btnX1.width, 22), small, Color.WHITE);
        drawCentered(g2, "Cần " + COST_X10[selected] + " NL", new Rectangle(btnX10.x, btnX10.y + 62, btnX10.width, 22), small, Color.WHITE);

        if (!message.isEmpty()) {
            drawCentered(g2, message, new Rectangle(bannerBox.x, bannerBox.y + 335, bannerBox.width, 30),
                    new Font("Arial", Font.BOLD, 14), Color.ORANGE);
        }
    }

    private void drawResult(Graphics2D g2) {
        drawCentered(g2, "KẾT QUẢ", new Rectangle(0, 120, GamePanel.WIDTH, 40),
                new Font("Arial", Font.BOLD, 28), Color.YELLOW);

        // Khung ngang
        BufferedImage rb = img("result_bg.jpg");
        if (rb != null) {
            g2.drawImage(rb, resultBox.x, resultBox.y, resultBox.width, resultBox.height, null);
        } else {
            g2.setColor(new Color(30, 36, 55));
            g2.fillRoundRect(resultBox.x, resultBox.y, resultBox.width, resultBox.height, 20, 20);
            g2.setColor(new Color(255, 215, 0));
            g2.drawRoundRect(resultBox.x, resultBox.y, resultBox.width, resultBox.height, 20, 20);
        }

        // Các vật phẩm xếp thành hàng ngang trên khung
        int n = lastResults.size();
        int icon = 56, gap = 10;
        int totalW = n * icon + (n - 1) * gap;
        int startX = resultBox.x + (resultBox.width - totalW) / 2;
        int y = resultBox.y + (resultBox.height - icon) / 2;
        for (int i = 0; i < n; i++) {
            drawItem(g2, lastResults.get(i), startX + i * (icon + gap), y, icon);
        }

        // Ghi chú (mở khoá đảo, sách...)
        int ny = resultBox.y + resultBox.height + 30;
        for (String note : lastNotes) {
            drawCentered(g2, note, new Rectangle(0, ny, GamePanel.WIDTH, 24),
                    new Font("Arial", Font.BOLD, 16), new Color(255, 230, 120));
            ny += 26;
        }

        drawCentered(g2, "[Bấm vào bất kỳ đâu để tiếp tục]", new Rectangle(0, 540, GamePanel.WIDTH, 24),
                new Font("Arial", Font.ITALIC, 13), Color.LIGHT_GRAY);
    }

    /** Vẽ 1 vật phẩm: ảnh nếu có, không thì hình tròn màu + chữ */
    private void drawItem(Graphics2D g2, int item, int x, int y, int size) {
        String file = item >= 1 && item <= 4 ? "seed_" + item + ".png"
                : item == 5 ? "book.png" : "none.";
        BufferedImage im = img(file);
        if (im != null) {
            g2.drawImage(im, x, y, size, size, null);
            return;
        }
        Color c = item >= 1 && item <= 4 ? THEME[item] : item == 5 ? new Color(150, 90, 200) : Color.DARK_GRAY;
        g2.setColor(c);
        g2.fillOval(x, y, size, size);
        g2.setColor(Color.WHITE);
        g2.drawOval(x, y, size, size);
        String t = item >= 1 && item <= 4 ? ELEMENT[item] : item == 5 ? "Sách" : "Hụt";
        drawCentered(g2, t, new Rectangle(x, y, size, size), new Font("Arial", Font.BOLD, 12), Color.WHITE);
    }

    // =====================================================================
    // Logic quay
    // =====================================================================
    private void doRoll(int times) {
        int cost = (times == 1) ? COST_X1[selected] : COST_X10[selected];
        if (energies[selected] < cost) {
            message = "Không đủ Năng Lượng! Cần " + cost + " NL " + ELEMENT[selected] + ".";
            return;
        }
        energies[selected] -= cost;

        lastResults.clear();
        lastNotes.clear();
        for (int i = 0; i < times; i++) {
            lastResults.add(rollOnce(selected));
        }
        screen = SCREEN_RESULT;
    }

    /** Quay 1 lần, cập nhật kho và trả về mã vật phẩm */
    private int rollOnce(int element) {
        int r = random.nextInt(100);
        int[] cut = CUT[element];
        int[] drop = DROP[element];

        int idx = 0;
        while (idx < cut.length - 1 && r >= cut[idx]) idx++;
        int item = drop[idx];
        boolean rare = (idx == cut.length - 1);

        if (item >= 1 && item <= 4) {
            seeds[item]++;
            if (rare) {
                mapUnlocked[item] = true;
                lastNotes.add("MỞ KHÓA ĐẢO " + ELEMENT[item].toUpperCase() + "!");
            }
        } else if (item == 5) {
            onGetMagicBook.run();
            lastNotes.add("HUYỀN THOẠI! Nhận QUYỂN SÁCH PHÉP!");
        }
        return item;
    }

    // =====================================================================
    // Tiện ích vẽ
    // =====================================================================
    private BufferedImage img(String name) {
        return ImageHelper.getOrLoadImage(DIR + name);
    }

    /** Vẽ nút bằng ảnh; không có ảnh thì vẽ hình chữ nhật màu kèm chữ */
    private void drawImageButton(Graphics2D g2, String file, Rectangle r, Color fallback, String label) {
        BufferedImage im = img(file);
        if (im != null) {
            g2.drawImage(im, r.x, r.y, r.width, r.height, null);
            return;
        }
        g2.setColor(fallback);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        drawCentered(g2, label, r, new Font("Arial", Font.BOLD, 16), Color.WHITE);
    }

    private void drawCentered(Graphics2D g2, String text, Rectangle r, Font font, Color color) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(text)) / 2;
        int ty = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, tx, ty);
    }
}