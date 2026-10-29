package state;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class StoryState extends GameState {
    private Rectangle btnSkip;
    private String gender;
    private String difficulty;

    private String[][] dialogues;
    private int currentDialogueIndex = 0;

    public StoryState(GameStateManager gsm, String gender, String difficulty) {
        super(gsm);
        this.gender = gender;
        this.difficulty = difficulty;
        init();
    }

    @Override
    public void init() {
        btnSkip = new Rectangle(680, 20, 90, 35);

        dialogues = new String[][] {
            {"Lời dẫn", "Khu vườn Phép Thuật từ xa xưa vốn được nuôi dưỡng bởi 4 nguyên tố thiêng liêng..."},
            {"Lời dẫn", "Tuy nhiên, phong ấn ở trung tâm đã khiến 3 khu vực chìm vào giấc ngủ."},
            {"Người bảo hộ", "Chào " + (gender.equals("Nam") ? "chàng trai" : "cô gái") + " dũng cảm! Bạn đã đến đây."},
            {"Người bảo hộ", "Hiện tại chỉ có Khu vườn Lửa còn giữ được sinh khí. Hãy bắt đầu từ đây."},
            {"Lời dẫn", "Bạn được tặng 5 Hạt Giống Lửa đầu tiên. Hãy gieo trồng và hồi sinh khu vườn!"}
        };
    }

    @Override
    public void update() {}

    @Override
    public void draw(Graphics2D g2) {
        g2.setColor(new Color(15, 20, 30));
        g2.fillRect(0, 0, 800, 600);

        // Nút Skip
        g2.setColor(new Color(180, 50, 50));
        g2.fill(btnSkip);
        g2.setColor(Color.WHITE);
        g2.drawRect(btnSkip.x, btnSkip.y, btnSkip.width, btnSkip.height);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString("SKIP >>", btnSkip.x + 18, btnSkip.y + 23);

        // Khung thoại
        int boxX = 50, boxY = 400, boxWidth = 700, boxHeight = 150;
        g2.setColor(new Color(30, 40, 55, 220));
        g2.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 15, 15);
        g2.setColor(new Color(255, 215, 0));
        g2.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 15, 15);

        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString(dialogues[currentDialogueIndex][0], boxX + 25, boxY + 35);

        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.setColor(Color.WHITE);
        g2.drawString(dialogues[currentDialogueIndex][1], boxX + 25, boxY + 75);

        g2.setFont(new Font("Arial", Font.ITALIC, 12));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("[Click chuột bất kỳ để tiếp tục (" + (currentDialogueIndex + 1) + "/" + dialogues.length + ")]", boxX + 440, boxY + 130);
    }

    private void enterGame() {
        gsm.startPlay(difficulty);
    }

    @Override
    public void mousePressed(int x, int y) {
        if (btnSkip.contains(x, y)) {
            enterGame();
            return;
        }

        currentDialogueIndex++;
        if (currentDialogueIndex >= dialogues.length) {
            enterGame();
        }
    }
    @Override public void mouseMoved(int x, int y) {}
}