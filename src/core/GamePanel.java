package core;

import state.GameStateManager;
import state.PlayState;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class GamePanel extends JPanel implements Runnable, KeyListener {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    private Thread gameThread;
    private boolean isRunning = false;
    private final int FPS = 60;

    private GameStateManager gsm;

    // Toạ độ góc nhìn camera và mức phóng to thu nhỏ
    public double cameraX = 0;
    public double cameraY = 0;
    public double zoom = 0.22;

    // Giới hạn zoom tối thiểu và tối đa
    public static final double MIN_ZOOM = 0.08;
    public static final double MAX_ZOOM = 1.5;
    private static final double ZOOM_FACTOR = 1.15;

    public GamePanel() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.setFocusable(true);

        this.gsm = new GameStateManager(this);
        setupMouseListeners();
        this.addKeyListener(this); // Đăng ký lắng nghe sự kiện bàn phím
        clampCamera();
    }

    // Căn camera vào đúng trọng tâm của khối đại hình thoi
    public void centerCameraOnMap() {
        cameraX = (WIDTH / (2.0 * zoom)) - PlayState.MAP_CENTER_X;
        cameraY = (HEIGHT / (2.0 * zoom)) - PlayState.MAP_CENTER_Y;
    }

    // Giới hạn camera trong phạm vi ranh giới bản đồ kèm khoảng đệm padding
    public void clampCamera() {
        double visibleWidth = WIDTH / zoom;
        double visibleHeight = HEIGHT / zoom;

        // Xử lý trục ngang X
        if (PlayState.MAP_TOTAL_WIDTH <= visibleWidth) {
            cameraX = (visibleWidth / 2.0) - PlayState.MAP_CENTER_X;
        } else {
            double maxCameraX = -PlayState.MAP_BOUND_LEFT;
            double minCameraX = visibleWidth - PlayState.MAP_BOUND_RIGHT;

            if (cameraX > maxCameraX) cameraX = maxCameraX;
            if (cameraX < minCameraX) cameraX = minCameraX;
        }

        // Xử lý trục dọc Y
        if (PlayState.MAP_TOTAL_HEIGHT <= visibleHeight) {
            cameraY = (visibleHeight / 2.0) - PlayState.MAP_CENTER_Y;
        } else {
            double maxCameraY = -PlayState.MAP_BOUND_TOP;
            double minCameraY = visibleHeight - PlayState.MAP_BOUND_BOTTOM;

            if (cameraY > maxCameraY) cameraY = maxCameraY;
            if (cameraY < minCameraY) cameraY = minCameraY;
        }
    }

    private void setupMouseListeners() {
        MouseAdapter ma = new MouseAdapter() {
            int lastX, lastY;

            @Override
            public void mousePressed(MouseEvent e) {
                // Tự động nhận lại focus bàn phím khi click vào game
                requestFocusInWindow();

                lastX = e.getX();
                lastY = e.getY();
                gsm.mousePressed(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // Di chuyển bản đồ theo thao tác kéo chuột
                cameraX += (e.getX() - lastX) / zoom;
                cameraY += (e.getY() - lastY) / zoom;
                lastX = e.getX();
                lastY = e.getY();

                clampCamera();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                gsm.mouseMoved(e.getX(), e.getY());
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double mouseX = e.getX();
                double mouseY = e.getY();

                // Toạ độ thế giới dưới trỏ chuột trước khi đổi tỉ lệ phóng
                double worldXBefore = (mouseX / zoom) - cameraX;
                double worldYBefore = (mouseY / zoom) - cameraY;

                if (e.getWheelRotation() < 0) {
                    zoom = Math.min(MAX_ZOOM, zoom * ZOOM_FACTOR);
                } else {
                    zoom = Math.max(MIN_ZOOM, zoom / ZOOM_FACTOR);
                }

                // Cập nhật lại góc nhìn bám sát vị trí con trỏ chuột
                cameraX = (mouseX / zoom) - worldXBefore;
                cameraY = (mouseY / zoom) - worldYBefore;

                clampCamera();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    // =========================================================================
    // XỬ LÝ PHÓNG TO / THU NHỎ QUA PHÍM BÀN PHÍM (+ VÀ -)
    // =========================================================================
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        char keyChar = e.getKeyChar();

        // Nhận diện phím CỘNG: dấu +, dấu = (cùng phím với +), hoặc phím + trên bàn phím số
        boolean isPlus = (keyCode == KeyEvent.VK_PLUS || 
                          keyCode == KeyEvent.VK_EQUALS || 
                          keyCode == KeyEvent.VK_ADD || 
                          keyChar == '+' || keyChar == '=');

        // Nhận diện phím TRỪ: dấu -, phím - trên bàn phím số
        boolean isMinus = (keyCode == KeyEvent.VK_MINUS || 
                           keyCode == KeyEvent.VK_SUBTRACT || 
                           keyChar == '-');

        if (isPlus) {
            zoomAtCenter(ZOOM_FACTOR); // Phóng to
        } else if (isMinus) {
            zoomAtCenter(1.0 / ZOOM_FACTOR); // Thu nhỏ
        }
    }

    /**
     * Phóng to / thu nhỏ lấy tâm màn hình làm điểm neo chuẩn
     */
    private void zoomAtCenter(double factor) {
        double screenCenterX = WIDTH / 2.0;
        double screenCenterY = HEIGHT / 2.0;

        // Vị trí World hiện tại ở tâm màn hình trước khi zoom
        double worldXBefore = (screenCenterX / zoom) - cameraX;
        double worldYBefore = (screenCenterY / zoom) - cameraY;

        double targetZoom = zoom * factor;
        if (targetZoom < MIN_ZOOM) targetZoom = MIN_ZOOM;
        if (targetZoom > MAX_ZOOM) targetZoom = MAX_ZOOM;

        if (targetZoom != zoom) {
            zoom = targetZoom;

            // Neo lại góc nhìn camera sao cho tâm màn hình giữ nguyên toạ độ World
            cameraX = (screenCenterX / zoom) - worldXBefore;
            cameraY = (screenCenterY / zoom) - worldYBefore;

            clampCamera();
            repaint();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    public void startGameThread() {
        gameThread = new Thread(this);
        isRunning = true;
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double nextDrawTime = System.nanoTime() + drawInterval;

        while (isRunning) {
            update();
            repaint();

            try {
                double remainingTime = nextDrawTime - System.nanoTime();
                remainingTime = remainingTime / 1000000.0;
                if (remainingTime < 0) remainingTime = 0;
                Thread.sleep((long) remainingTime);
                nextDrawTime += drawInterval;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        gsm.update();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        gsm.draw(g2);
        g2.dispose();
    }
}