package state;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class IntroState extends GameState {
    // 1. Tài nguyên hình ảnh
    private Image bgGif;
    private BufferedImage[] cowImages = new BufferedImage[4];

    // Màu sắc dự phòng cho 4 chú bò nếu chưa kịp thêm ảnh vào thư mục res
    private final Color[] memberColors = {
        new Color(255, 175, 190), // Hồng phấn
        new Color(130, 210, 255), // Xanh trời
        new Color(255, 225, 115), // Vàng ấm
        new Color(150, 245, 175)  // Xanh mint
    };

    // 2. Biến điều khiển hoạt họa và độ mờ
    private int timer = 0;
    private double cowOrbitAngle = 0;

    private float textAlpha = 0.0f;   // Độ mờ của chữ (bắt đầu từ 0 - tàng hình)
    private float globalAlpha = 1.0f; // Độ mờ chung (khi fade-out toàn bộ về màu đen)
    private boolean isFadingOut = false;

    // Cấu hình thời gian (tính theo số khung hình ở 60 FPS)
    private final float FADE_IN_FRAMES = 600.0f;  // 10 giây (60 fps * 10)
    private final float FADE_OUT_FRAMES = 300.0f; // 5 giây (60 fps * 5)

    // Tâm xoay và bán kính quỹ đạo bay rộng thoáng
    private final double centerX = 400;
    private final double centerY = 285;
    private final double radiusX = 280; // Bán kính ngang rộng
    private final double radiusY = 135; // Bán kính dọc rộng

    // Đốm sáng ma thuật phụ họa
    private static class MagicSparkle {
        double x, y, speed, size;
        float opacity;
    }
    private ArrayList<MagicSparkle> sparkles = new ArrayList<>();
    private Random rand = new Random();

    public IntroState(GameStateManager gsm) {
        super(gsm);
        init();
    }

    @Override
    public void init() {
        // Tải ảnh nền GIF từ thư mục res/intro/
        try {
            URL gifUrl = getClass().getResource("/intro/bg_intro.gif");
            if (gifUrl != null) {
                bgGif = new ImageIcon(gifUrl).getImage();
            }
        } catch (Exception e) {
            System.err.println("Dùng nền mặc định nếu chưa có bg_intro.gif");
        }

        // Tải 4 ảnh bò riêng biệt: res/intro/cow1.png -> cow4.png
        for (int i = 0; i < 4; i++) {
            String path = "/intro/cow" + (i + 1) + ".png";
            try {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    cowImages[i] = ImageIO.read(is);
                }
            } catch (Exception e) {
                System.err.println("Chưa tìm thấy ảnh: " + path);
            }
        }

        // Tạo 35 hạt đốm sáng lấp lánh nền
        for (int i = 0; i < 35; i++) {
            MagicSparkle s = new MagicSparkle();
            s.x = rand.nextInt(800);
            s.y = rand.nextInt(600);
            s.speed = 0.4 + rand.nextDouble() * 1.2;
            s.size = 2 + rand.nextInt(4);
            s.opacity = 0.2f + rand.nextFloat() * 0.7f;
            sparkles.add(s);
        }
    }

    @Override
    public void update() {
        timer++;
        cowOrbitAngle += 0.032;

        // Cập nhật vị trí hạt đốm sáng bay từ dưới lên
        for (MagicSparkle s : sparkles) {
            s.y -= s.speed;
            if (s.y < 0) {
                s.y = 600;
                s.x = rand.nextInt(800);
            }
        }

        // GIAI ĐOẠN 1: Chữ ban đầu chưa có -> hiện rõ dần trong đúng 10 GIÂY (600 frames)
        if (!isFadingOut) {
            textAlpha += (1.0f / FADE_IN_FRAMES);
            if (textAlpha >= 1.0f) {
                textAlpha = 1.0f;
                isFadingOut = true; // Chữ vừa hiện rõ hoàn toàn -> kích hoạt đen dần ngay lập tức
            }
        }
        // GIAI ĐOẠN 2: Đen dần toàn bộ (cả nền, bò, chữ) trong đúng 5 GIÂY (300 frames)
        else {
            globalAlpha -= (1.0f / FADE_OUT_FRAMES);
            if (globalAlpha <= 0.0f) {
                globalAlpha = 0.0f;
                gsm.setState(GameStateManager.MENU); // Chuyển cảnh vào Menu
            }
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        // Bật khử răng cưa
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Nền đen tuyệt đối lót đáy (khi toàn bộ mờ dần sẽ chìm vào màu đen này)
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, 800, 600);

        // ÁP DỤNG ĐỘ MỜ CHUNG (Sẽ giảm dần từ 1 về 0 trong 5 giây cuối)
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, globalAlpha));

        // 1. VẼ NỀN ĐỘNG (Ảnh GIF hoặc nền màu đêm ma thuật)
        if (bgGif != null) {
            g2.drawImage(bgGif, 0, 0, 800, 600, gsm.getGamePanel());
        } else {
            g2.setColor(new Color(15, 12, 32));
            g2.fillRect(0, 0, 800, 600);
        }

        // 2. VẼ CÁC HẠT ĐỐM SÁNG PHỤ HỌA
        g2.setColor(new Color(255, 245, 180));
        for (MagicSparkle s : sparkles) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, s.opacity * globalAlpha));
            g2.fillOval((int) s.x, (int) s.y, (int) s.size, (int) s.size);
        }

        // 3. VẼ 4 CHÚ BÒ BAY VÒNG QUANH (Bò bay tự do ngay từ đầu)
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, globalAlpha));
        for (int i = 0; i < 4; i++) {
            double currentAngle = cowOrbitAngle + (i * Math.PI / 2.0);

            double cowX = centerX + Math.cos(currentAngle) * radiusX;
            double cowY = centerY + Math.sin(currentAngle) * radiusY + Math.sin(timer * 0.14 + i) * 7;

            drawFlyingCow(g2, (int) cowX, (int) cowY, i);
        }

        // 4. VẼ CỤM CHỮ (Áp dụng riêng textAlpha: rõ dần trong 10 giây đầu)
        float combinedTextAlpha = textAlpha * globalAlpha;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, combinedTextAlpha));

        double textFloatY = Math.sin(timer * 0.07) * 8.0;

        // Tên nhóm nổi bật có hiệu ứng 3D và lung lay
        drawGlowingTitle(g2, "BÒ BIẾT BAY", (int) (centerY + textFloatY));

        // Dòng chữ nhỏ bên dưới
        drawCenteredText(g2, "Nhóm 21", new Font("Arial", Font.BOLD, 18), 
                         new Color(215, 235, 255), (int) (centerY + 45 + textFloatY));

        // 5. GỢI Ý BẤM CHUỘT ĐỂ BỎ QUA NHANH
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f * globalAlpha));
        drawCenteredText(g2, "Click chuột để tiếp tục...", new Font("Arial", Font.PLAIN, 13), 
                         Color.LIGHT_GRAY, 565);

        // Khôi phục composite chuẩn
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    // Vẽ chữ "BÒ BIẾT BAY" với đổ bóng 3D và viền sáng xanh ma thuật
    private void drawGlowingTitle(Graphics2D g2, String text, int y) {
        Font font = new Font("Arial", Font.BOLD, 52);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int x = (800 - fm.stringWidth(text)) / 2;

        // Lớp bóng đổ phía sau
        g2.setColor(new Color(10, 5, 25, 200));
        g2.drawString(text, x + 4, y + 4);

        // Lớp viền phát sáng xanh ngọc
        g2.setColor(new Color(0, 200, 240));
        g2.drawString(text, x - 1, y - 1);
        g2.drawString(text, x + 1, y - 1);
        g2.drawString(text, x - 1, y + 1);
        g2.drawString(text, x + 1, y + 1);

        // Lớp mặt chữ chính màu trắng sáng
        g2.setColor(new Color(255, 255, 255));
        g2.drawString(text, x, y);
    }

    // Vẽ từng chú bò bay lượn (chỉ vẽ hình ảnh, không vẽ chữ tên)
    private void drawFlyingCow(Graphics2D g2, int x, int y, int index) {
        int cowSize = 58;
        int drawX = x - cowSize / 2;
        int drawY = y - cowSize / 2;

        if (cowImages[index] != null) {
            g2.drawImage(cowImages[index], drawX, drawY, cowSize, cowSize, null);
        } else {
            // Đồ hoạ dự phòng nếu chưa có ảnh
            g2.setColor(memberColors[index]);
            g2.fillOval(drawX, drawY, cowSize, cowSize);

            int wingOffset = (int) (Math.sin(timer * 0.35 + index) * 4);
            g2.setColor(new Color(255, 255, 255, 220));
            g2.fillOval(drawX - 7, drawY + 12 + wingOffset, 14, 10);
            g2.fillOval(drawX + cowSize - 7, drawY + 12 - wingOffset, 14, 10);
        }
    }

    // Hàm tiện ích căn giữa văn bản
    private void drawCenteredText(Graphics2D g2, String text, Font font, Color color, int y) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        int x = (800 - fm.stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }

    @Override
    public void mousePressed(int x, int y) {
        // Bấm chuột bất kỳ lúc nào để bỏ qua và vào Menu ngay
        gsm.setState(GameStateManager.MENU);
    }
    @Override public void mouseMoved(int x, int y) {}
}