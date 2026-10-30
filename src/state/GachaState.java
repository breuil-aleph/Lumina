package state;

import java.awt.Graphics2D;

public class GachaState extends GameState {
    private PlayState playStateRef;
    private GachaSystem gachaSystem;

    public GachaState(GameStateManager gsm, PlayState playStateRef) {
        super(gsm);
        this.playStateRef = playStateRef;
        init();
    }

    @Override
    public void init() {
        if (playStateRef != null) {
            this.gachaSystem = new GachaSystem(
                    playStateRef.getSeedsArray(),
                    playStateRef.getEnergiesArray(),
                    playStateRef.getMapUnlockedArray(),
                    () -> playStateRef.unlockMagicBook(),
                    playStateRef.getPlayerStats()
            );
            this.gachaSystem.setOpen(true);
        }
    }

    @Override
    public void update() {
    }

    @Override
    public void draw(Graphics2D g2) {
        // Vẽ game bên dưới làm nền
        if (playStateRef != null) {
            playStateRef.draw(g2);
        }

        if (gachaSystem != null) {
            gachaSystem.drawModal(g2);
        }
    }

    @Override
    public void mousePressed(int x, int y) {
        if (gachaSystem != null) {
            boolean handled = gachaSystem.handleClick(x, y);
            // Khi người chơi bấm nút X đóng hoặc thoát gacha
            if (!gachaSystem.isOpen() || !handled) {
                closeGacha();
            }
        } else {
            closeGacha();
        }
    }

    private void closeGacha() {
        if (playStateRef != null) {
            playStateRef.resetStatsTimer();
            gsm.resumePlayState(playStateRef);
        } else {
            gsm.setState(GameStateManager.MENU);
        }
    }

    @Override
    public void mouseMoved(int x, int y) {
    }
}