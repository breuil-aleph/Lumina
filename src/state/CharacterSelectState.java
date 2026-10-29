package state;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class CharacterSelectState extends GameState {
    private Rectangle btnMale, btnFemale;
    private String selectedGender = "Nam";

    private Rectangle btnEasy, btnNormal, btnHard;
    private String selectedDifficulty = "Normal";

    private Rectangle btnStart, btnBack;

    public CharacterSelectState(GameStateManager gsm) {
        super(gsm);
        init();
    }

    @Override
    public void init() {
        btnMale = new Rectangle(250, 180, 130, 45);
        btnFemale = new Rectangle(420, 180, 130, 45);

        btnEasy = new Rectangle(180, 310, 120, 40);
        btnNormal = new Rectangle(340, 310, 120, 40);
        btnHard = new Rectangle(500, 310, 120, 40);

        btnBack = new Rectangle(220, 470, 160, 50);
        btnStart = new Rectangle(420, 470, 160, 50);
    }

    @Override
    public void update() {}

    @Override
    public void draw(Graphics2D g2) {
        g2.setColor(new Color(20, 30, 45));
        g2.fillRect(0, 0, 800, 600);

        g2.setColor(new Color(255, 215, 0));
        g2.setFont(new Font("Arial", Font.BOLD, 32));
        g2.drawString("TÙY CHỌN BẮT ĐẦU", 230, 80);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("1. Chọn Nhân Vật:", 250, 150);
        drawOptionButton(g2, btnMale, "Nam", selectedGender.equals("Nam"));
        drawOptionButton(g2, btnFemale, "Nữ", selectedGender.equals("Nữ"));

        g2.drawString("2. Chọn Độ Khó:", 180, 280);
        drawOptionButton(g2, btnEasy, "Easy", selectedDifficulty.equals("Easy"));
        drawOptionButton(g2, btnNormal, "Normal", selectedDifficulty.equals("Normal"));
        drawOptionButton(g2, btnHard, "Hard", selectedDifficulty.equals("Hard"));

        drawActionButton(g2, btnBack, "Quay Lại", new Color(180, 60, 60));
        drawActionButton(g2, btnStart, "VÀO GAME", new Color(46, 139, 87));
    }

    private void drawOptionButton(Graphics2D g2, Rectangle rect, String text, boolean isSelected) {
        g2.setColor(isSelected ? new Color(60, 160, 240) : new Color(70, 80, 95));
        g2.fill(rect);
        g2.setColor(isSelected ? Color.YELLOW : Color.LIGHT_GRAY);
        g2.drawRect(rect.x, rect.y, rect.width, rect.height);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(Color.WHITE);
        g2.drawString(text, rect.x + 35, rect.y + 26);
    }

    private void drawActionButton(Graphics2D g2, Rectangle rect, String text, Color bgColor) {
        g2.setColor(bgColor);
        g2.fill(rect);
        g2.setColor(Color.WHITE);
        g2.drawRect(rect.x, rect.y, rect.width, rect.height);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString(text, rect.x + 30, rect.y + 32);
    }

    @Override
    public void mousePressed(int x, int y) {
        if (btnMale.contains(x, y)) selectedGender = "Nam";
        else if (btnFemale.contains(x, y)) selectedGender = "Nữ";

        if (btnEasy.contains(x, y)) selectedDifficulty = "Easy";
        else if (btnNormal.contains(x, y)) selectedDifficulty = "Normal";
        else if (btnHard.contains(x, y)) selectedDifficulty = "Hard";

        if (btnBack.contains(x, y)) gsm.setState(GameStateManager.MENU);
        if (btnStart.contains(x, y)) gsm.startStory(selectedGender, selectedDifficulty);
    }
    @Override public void mouseMoved(int x, int y) {}
}