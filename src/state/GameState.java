package state;

import java.awt.Graphics2D;

public abstract class GameState {
    protected GameStateManager gsm;

    public GameState(GameStateManager gsm) {
        this.gsm = gsm;
    }

    public abstract void init();
    public abstract void update();
    public abstract void draw(Graphics2D g2);
    public abstract void mousePressed(int x, int y);
    public abstract void mouseMoved(int x, int y);
}