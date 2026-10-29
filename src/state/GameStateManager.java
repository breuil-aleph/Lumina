package state;

import core.GamePanel;
import java.awt.Graphics2D;

public class GameStateManager {
    private GameState currentState;
    private GamePanel gamePanel;

    public static final int INTRO = 0;
    public static final int MENU = 1;
    public static final int CHARACTER_SELECT = 2;
    public static final int STORY = 3;
    public static final int PLAY = 4;

    public static final int AVATAR = 5;

    public GameStateManager(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        setState(INTRO);
    }

    public void setState(int state) {
        if (state == INTRO)
            currentState = new IntroState(this);
        else if (state == MENU)
            currentState = new MenuState(this);
        else if (state == CHARACTER_SELECT)
            currentState = new CharacterSelectState(this);
        else if (state == PLAY)
            currentState = new PlayState(this);
    }

    public void startStory(String gender, String difficulty) {
        currentState = new StoryState(this, gender, difficulty);
    }

    public void startPlay(String difficulty) {
        double mult = 1.0;
        if ("Normal".equalsIgnoreCase(difficulty))
            mult = 0.75;
        else if ("Hard".equalsIgnoreCase(difficulty))
            mult = 0.5;
        currentState = new PlayState(this, mult);
    }

    public void update() {
        if (currentState != null)
            currentState.update();
    }

    public void openAvatarMenu(PlayState currentPlay) {
        currentState = new AvatarState(this, currentPlay);
    }

    public void draw(Graphics2D g2) {
        if (currentState != null)
            currentState.draw(g2);
    }

    public void mousePressed(int x, int y) {
        if (currentState != null)
            currentState.mousePressed(x, y);
    }

    public void mouseMoved(int x, int y) {
        if (currentState != null)
            currentState.mouseMoved(x, y);
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    // Thêm các hàm sau vào GameStateManager.java:
    public void openSaveMenu(PlayState currentPlay) {
        currentState = new SaveLoadState(this, SaveLoadState.MODE_SAVE, currentPlay);
    }

    public void openLoadMenu() {
        currentState = new SaveLoadState(this, SaveLoadState.MODE_LOAD, null);
    }

    // tận dụng dùng cho avatar menu
    public void resumePlayState(PlayState playState) {
        currentState = playState;
    }

    public void loadGameFromSave(data.SaveData data) {
        PlayState loadedPlay = new PlayState(this, data.difficultyMultiplier);
        loadedPlay.importSaveData(data);
        currentState = loadedPlay;
    }
}