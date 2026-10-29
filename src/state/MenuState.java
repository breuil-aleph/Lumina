package state;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class MenuState extends GameState {
    private BufferedImage bgImage;

    // 4 nút bấm gốc
    private Rectangle btnNew, btnLoad, btnSetting, btnExit;

    // Trạng thái hover của từng nút (true = chuột đang trỏ vào)
    private boolean hoverNew = false;
    private boolean hoverLoad = false;
    private boolean hoverSetting = false;
    private boolean hoverExit = false;

    private int timer = 0; // Dùng tạo hiệu ứng lung lay bồng bềnh cho chữ LUMINA

    public MenuState(GameStateManager gsm) {
        super(gsm);
        init();
    }

    @Override
    public void init() {
        // Tải ảnh nền menu từ res/menu/bg_menu.png
        try {
            InputStream is = getClass().getResourceAsStream("/menu/bg_menu.png");
            if (is != null) {
                bgImage = ImageIO.read(is);
            }
        } catch (Exception e) {
            System.err.println("Dùng nền mặc định nếu chưa có bg_menu.png");
        }

        int btnWidth = 230;
        int btnHeight = 48;
        int startX = (800 - btnWidth) / 2; // Căn giữa màn hình

        btnNew = new Rectangle(startX, 220, btnWidth, btnHeight);
        btnLoad = new Rectangle(startX, 285, btnWidth, btnHeight);
        btnSetting = new Rectangle(startX, 350, btnWidth, btnHeight);
        btnExit = new Rectangle(startX, 415, btnWidth, btnHeight);
    }

    @Override
    public void update() {
        timer++;
    }

    @Override
    public void draw(Graphics2D g2) {
        // Bật khử răng cưa
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. VẼ NỀN (Ảnh nền hoặc nền gradient huyền bí)
        if (bgImage != null) {
            g2.drawImage(bgImage, 0, 0, 800, 600, null);
        } else {
            g2.setColor(new Color(12, 28, 22));
            g2.fillRect(0, 0, 800, 600);
        }

        // Phủ 1 lớp bóng tối nhẹ để chữ và nút nổi bật hơn ảnh nền
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRect(0, 0, 800, 600);

        // 2. VẼ TIÊU ĐỀ GAME: "LUMINA" (Phát sáng vàng kim + lơ lửng)
        double titleFloatY = Math.sin(timer * 0.05) * 5.0;
        drawLuminaTitle(g2, "LUMINA", (int) (125 + titleFloatY));

        // Dòng phụ đề nhỏ phía dưới
        drawCenteredText(g2, "A Magic Garden Adventure", new Font("Arial", Font.ITALIC, 14), 
                         new Color(200, 240, 220), (int) (155 + titleFloatY));

        // 3. VẼ 4 NÚT BẤM (CÓ HIỆU ỨNG PHÓNG TO KHI RÊ CHUỘT)
        drawInteractiveButton(g2, btnNew, "New Game", hoverNew);
        drawInteractiveButton(g2, btnLoad, "Load Game", hoverLoad);
        drawInteractiveButton(g2, btnSetting, "Settings", hoverSetting);
        drawInteractiveButton(g2, btnExit, "Exit", hoverExit);
    }

    // Vẽ tiêu đề "LUMINA" phong cách ma thuật phát sáng
    private void drawLuminaTitle(Graphics2D g2, String text, int y) {
        Font font = new Font("Serif", Font.BOLD, 64);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int x = (800 - fm.stringWidth(text)) / 2;

        // Lớp bóng đổ sâu
        g2.setColor(new Color(0, 0, 0, 220));
        g2.drawString(text, x + 5, y + 5);

        // Lớp viền phát sáng hoàng kim (Glow effect)
        g2.setColor(new Color(255, 180, 0, 150));
        g2.drawString(text, x - 2, y);
        g2.drawString(text, x + 2, y);
        g2.drawString(text, x, y - 2);
        g2.drawString(text, x, y + 2);

        // Lớp mặt chữ chính màu vàng kim sáng
        g2.setColor(new Color(255, 235, 140));
        g2.drawString(text, x, y);
    }

    // Hàm vẽ nút bấm: Phóng to từ tâm khi chuột rê vào
    private void drawInteractiveButton(Graphics2D g2, Rectangle rect, String text, boolean isHovered) {
        int x = rect.x;
        int y = rect.y;
        int w = rect.width;
        int h = rect.height;

        // Nếu rê chuột vào: nở rộng mỗi cạnh ra 8px từ tâm, đổi màu sang phong cách phát sáng
        if (isHovered) {
            int expand = 8;
            x -= expand;
            y -= expand / 2;
            w += expand * 2;
            h += expand;

            // Nền nút khi hover: màu sáng vàng ngọc sang trọng
            g2.setColor(new Color(255, 248, 220));
            g2.fillRoundRect(x, y, w, h, 20, 20);

            // Viền phát sáng dày màu cam vàng
            g2.setColor(new Color(255, 170, 0));
            g2.drawRoundRect(x, y, w, h, 20, 20);
            g2.drawRoundRect(x - 1, y - 1, w + 2, h + 2, 22, 22);

            // Chữ to hơn và in đậm nổi bật
            g2.setFont(new Font("Arial", Font.BOLD, 21));
            g2.setColor(new Color(60, 30, 0)); // Chữ màu nâu đậm cổ điển
        } else {
            // Nền nút bình thường: trong suốt nhẹ kiểu kính mờ (Glassmorphism)
            g2.setColor(new Color(20, 45, 35, 210));
            g2.fillRoundRect(x, y, w, h, 16, 16);

            // Viền mảnh màu xanh ngọc
            g2.setColor(new Color(90, 160, 130));
            g2.drawRoundRect(x, y, w, h, 16, 16);

            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(new Color(230, 245, 235)); // Chữ màu trắng ngọc
        }

        // Căn chữ luôn nằm chính giữa lòng nút
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (w - fm.stringWidth(text)) / 2;
        int textY = y + ((h - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(text, textX, textY);
    }

    private void drawCenteredText(Graphics2D g2, String text, Font font, Color color, int y) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        int x = (800 - fm.stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }

    // BẮT SỰ KIỆN RÊ CHUỘT: Tự động đổi cờ hover
    @Override
    public void mouseMoved(int x, int y) {
        hoverNew = btnNew.contains(x, y);
        hoverLoad = btnLoad.contains(x, y);
        hoverSetting = btnSetting.contains(x, y);
        hoverExit = btnExit.contains(x, y);
    }

    @Override
    public void mousePressed(int x, int y) {
        if (btnNew.contains(x, y)) {
            gsm.setState(GameStateManager.CHARACTER_SELECT);
        } else if (btnLoad.contains(x, y)) {
            gsm.openLoadMenu();
        } else if (btnSetting.contains(x, y)) {
            System.out.println("Settings clicked");
        } else if (btnExit.contains(x, y)) {
            System.exit(0);
        }
    }
}