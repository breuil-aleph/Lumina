package map;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class WinterEffectsManager {
    private List<Snowflake> snowflakes = new ArrayList<>();
    private List<Snowman> snowmen = new ArrayList<>();
    private ReindeerSleigh sleigh;

    private int islandCenterX, islandCenterY, islandWidth, islandHeight;

    public WinterEffectsManager(int centerX, int centerY, int width, int height) {
        this.islandCenterX = centerX;
        this.islandCenterY = centerY;
        this.islandWidth = width;
        this.islandHeight = height;

        // Khởi tạo 35 bông tuyết rơi và xoay tròn trong phạm vi đảo
        for (int i = 0; i < 35; i++) {
            snowflakes.add(new Snowflake(centerX, centerY, width, height));
        }
        
        snowmen.add(new Snowman(centerX - 120, centerY - 50));
        snowmen.add(new Snowman(centerX + 80, centerY + 30));

        sleigh = new ReindeerSleigh(centerX - width / 2, centerY - height / 2 - 40, width);
    }

    public void update() {
        for (Snowflake sf : snowflakes) {
            sf.update(islandCenterX, islandCenterY, islandWidth, islandHeight);
        }
        for (Snowman sm : snowmen) {
            sm.update();
        }
        sleigh.update(islandCenterX, islandWidth);
    }

    public void draw(Graphics2D g2) {
        sleigh.draw(g2);
        for (Snowman sm : snowmen) {
            sm.draw(g2);
        }
        for (Snowflake sf : snowflakes) {
            sf.draw(g2);
        }
    }

    // --- LỚP BÔNG TUYẾT RƠI & XOAY TRÒN ---
    private static class Snowflake {
        double x, y;
        double speedY, speedX;
        double angle = 0;       // Góc xoay hiện tại của bông tuyết
        double rotSpeed;        // Tốc độ xoay nhanh/chậm
        int size;               // Kích thước hiển thị bông tuyết
        int cX, cY, w, h;
        private static BufferedImage snowImg;

        public Snowflake(int centerX, int centerY, int width, int height) {
            this.cX = centerX;
            this.cY = centerY;
            this.w = width;
            this.h = height;
            
            // Nạp ảnh bông tuyết (chỉ load 1 lần dùng chung)
            if (snowImg == null) {
                snowImg = ImageHelper.loadImage("res/map2/bongtuyet.png");
            }
            reset();
        }

        public void reset() {
            x = cX - (w / 2.0) + Math.random() * w;
            y = cY - (h / 2.0) - Math.random() * 200;
            speedY = 0.8 + Math.random() * 1.5; // Tốc độ rơi nhẹ nhàng
            speedX = -0.2 + Math.random() * 0.4; // Gió thổi ngang lất phất
            rotSpeed = -0.03 + Math.random() * 0.06; // Tốc độ và hướng xoay ngẫu nhiên
            size = 16 + (int)(Math.random() * 16);     // Kích thước từ 16 đến 32 pixel
        }

        public void update(int centerX, int centerY, int width, int height) {
            y += speedY;
            x += speedX + Math.sin(y * 0.04) * 0.3; // Hiệu ứng lượn sóng theo gió
            angle += rotSpeed;                      // Tăng góc xoay theo thời gian

            // Khi rơi qua khỏi đáy đảo thì tái sinh lại ở phía trên
            if (y > centerY + (height / 2.0)) {
                x = centerX - (width / 2.0) + Math.random() * width;
                y = centerY - (height / 2.0) - 50;
            }
        }

        public void draw(Graphics2D g2) {
            if (snowImg != null) {
                // Lưu trạng thái cọ vẽ cũ
                AffineTransform oldTransform = g2.getTransform();

                // Dịch chuyển tâm cọ vẽ đến vị trí bông tuyết và thực hiện xoay góc (angle)
                g2.translate(x + size / 2.0, y + size / 2.0);
                g2.rotate(angle);

                // Vẽ ảnh bông tuyết (căn giữa tâm xoay)
                g2.drawImage(snowImg, -size / 2, -size / 2, size, size, null);

                // Khôi phục trạng thái cọ vẽ
                g2.setTransform(oldTransform);
            } else {
                // Dự phòng nếu chưa tìm thấy ảnh
                g2.setColor(Color.WHITE);
                g2.fillOval((int)x, (int)y, size / 2, size / 2);
            }
        }
    }

    // --- NGƯỜI TUYẾT ---
    private static class Snowman {
        double x, y;
        double startX;
        int direction = 1;

        public Snowman(double x, double y) {
            this.x = x;
            this.y = y;
            this.startX = x;
        }

        public void update() {
            x += 0.3 * direction;
            if (x > startX + 70) direction = -1;
            if (x < startX - 70) direction = 1;
        }

        public void draw(Graphics2D g2) {
            g2.setColor(Color.WHITE);
            g2.fillOval((int)x, (int)y + 12, 22, 22);
            g2.fillOval((int)x + 3, (int)y + 4, 16, 16);
            g2.fillOval((int)x + 5, (int)y, 12, 12);
            
            g2.setColor(Color.BLACK);
            g2.fillRect((int)x + 7, (int)y + 3, 2, 2);
            g2.fillRect((int)x + 11, (int)y + 3, 2, 2);
        }
    }

    // --- XE TUẦN LỘC ---
    private static class ReindeerSleigh {
        double x, y;
        double angle = 0;
        private BufferedImage sleighImg;

        public ReindeerSleigh(double startX, double startY, int width) {
            this.x = startX;
            this.y = startY;
            sleighImg = ImageHelper.loadImage("res/map2/tuanloc.png");
        }

        public void update(int centerX, int width) {
            x += 1.0;
            angle += 0.04;
            y += Math.sin(angle) * 0.3;

            if (x > centerX + (width / 2.0)) {
                x = centerX - (width / 2.0);
            }
        }

        public void draw(Graphics2D g2) {
            if (sleighImg != null) {
                int drawW = 90;
                int drawH = (int) (drawW * ((double) sleighImg.getHeight() / sleighImg.getWidth()));
                g2.drawImage(sleighImg, (int)x, (int)y, drawW, drawH, null);
            } else {
                g2.setColor(new Color(180, 120, 70));
                g2.fillRect((int)x, (int)y, 35, 12);
                g2.setColor(Color.YELLOW);
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                g2.drawString("🛷 Tuần Lộc", (int)x - 10, (int)y - 4);
            }
        }
    }
}
