package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AvatarDialogueBubble {
    private static final long INITIAL_IDLE_MILLIS = 2_000;
    private static final long DISPLAY_MILLIS = 2_000;
    private static final long MIN_BREAK_MILLIS = 1_000;
    private static final long MAX_BREAK_MILLIS = 2_000; // FIXME: TEST BUBBLE
    private static final int MAX_TEXT_WIDTH = 280;
    private static final int PADDING_X = 12;
    private static final int PADDING_Y = 9;
    private static final Font TEXT_FONT = new Font("Arial", Font.PLAIN, 13);

    private static final String[] DEFAULT_DIALOGUES = {
            "Thoại số 1...",
            "Thoại số 2...",
            "Thoại số 3..."
    };

    private static final Map<String, String[]> DIALOGUES_BY_AVATAR = new HashMap<>();

    static {
        DIALOGUES_BY_AVATAR.put("lumi", new String[] {
                "Thoại số 1...",
                "Thoại số 2...",
                "Thoại số 3..."
        });
    }

    private final Random random = new Random();
    private final Map<String, String> previousDialogueByAvatar = new HashMap<>();

    private String currentAvatarId;
    private String currentDialogue;
    private long nextDialogueAt;
    private long hideDialogueAt;
    private boolean visible;

    public AvatarDialogueBubble() {
        resetIdleTimer(System.currentTimeMillis());
    }

    public synchronized void update(long now, String avatarId) {
        if (!avatarId.equals(currentAvatarId)) {
            currentAvatarId = avatarId;
            hideCurrentDialogue();
            nextDialogueAt = now + INITIAL_IDLE_MILLIS;
        }

        if (visible) {
            if (now >= hideDialogueAt) {
                hideCurrentDialogue();
                nextDialogueAt = now + randomBreakMillis();
            }
            return;
        }

        if (now >= nextDialogueAt) {
            currentDialogue = chooseDialogue(avatarId);
            visible = true;
            hideDialogueAt = now + DISPLAY_MILLIS;
        }
    }

    public synchronized void recordActivity(long now) {
        hideCurrentDialogue();
        nextDialogueAt = now + INITIAL_IDLE_MILLIS;
    }

    public synchronized void resetIdleTimer(long now) {
        currentAvatarId = null;
        recordActivity(now);
    }

    public synchronized void draw(Graphics2D g2, Rectangle avatarBounds, int screenWidth, int screenHeight) {
        if (!visible || currentDialogue == null) {
            return;
        }

        int bubbleX = avatarBounds.x + avatarBounds.width + 40;
        int maxTextWidth = Math.max(40,
                Math.min(MAX_TEXT_WIDTH, screenWidth - bubbleX - PADDING_X * 2 - 12));

        Graphics2D bubbleGraphics = (Graphics2D) g2.create();
        try {
            bubbleGraphics.setFont(TEXT_FONT);
            FontMetrics metrics = bubbleGraphics.getFontMetrics();
            String[] lines = wrapText(currentDialogue, metrics, maxTextWidth);

            int lineHeight = metrics.getHeight();
            int textWidth = 0;
            for (String line : lines) {
                textWidth = Math.max(textWidth, metrics.stringWidth(line));
            }

            int bubbleWidth = textWidth + PADDING_X * 2;
            int bubbleHeight = Math.max(36, lines.length * lineHeight + PADDING_Y * 2);
            int bubbleY = avatarBounds.y + (avatarBounds.height - bubbleHeight) / 2 - 8;
            bubbleY = Math.max(8, Math.min(bubbleY, screenHeight - bubbleHeight - 8));

            drawBubbleShape(bubbleGraphics, bubbleX, bubbleY, bubbleWidth, bubbleHeight);
            bubbleGraphics.setColor(new Color(35, 40, 52));

            int textY = bubbleY + PADDING_Y + metrics.getAscent();
            for (String line : lines) {
                bubbleGraphics.drawString(line, bubbleX + PADDING_X, textY);
                textY += lineHeight;
            }
        } finally {
            bubbleGraphics.dispose();
        }
    }

    private void drawBubbleShape(Graphics2D g2, int x, int y, int width, int height) {
        int radius = 12;

        // Vị trí phần đuôi trên cạnh trái
        double tailTop = y + height * 0.40;
        double tailBottom = y + height * 0.62;
        double tailTipX = x - 28;
        double tailTipY = y + height * 0.78;

        Path2D bubble = new Path2D.Double();

        // Góc trên trái
        bubble.moveTo(x + radius, y);

        // Cạnh trên và góc trên phải
        bubble.lineTo(x + width - radius, y);
        bubble.quadTo(x + width, y, x + width, y + radius);

        // Cạnh phải và góc dưới phải
        bubble.lineTo(x + width, y + height - radius);
        bubble.quadTo(x + width, y + height,
                x + width - radius, y + height);

        // Cạnh dưới và góc dưới trái
        bubble.lineTo(x + radius, y + height);
        bubble.quadTo(x, y + height, x, y + height - radius);

        // Cạnh trái + đuôi hướng về avatar
        bubble.lineTo(x, tailBottom);
        bubble.lineTo(tailTipX, tailTipY);
        bubble.lineTo(x, tailTop);

        // Góc trên trái
        bubble.lineTo(x, y + radius);
        bubble.quadTo(x, y, x + radius, y);

        bubble.closePath();

        // Vẽ nền trắng
        g2.setColor(Color.WHITE);
        g2.fill(bubble);

        // Viền đen
        g2.setColor(Color.BLACK);
        g2.setStroke(new java.awt.BasicStroke(
                2.0f,
                java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g2.draw(bubble);
    }

    private String[] wrapText(String text, FontMetrics metrics, int maxWidth) {
        String[] words = text.split("\\s+");
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && metrics.stringWidth(candidate) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                if (line.length() > 0)
                    line.append(' ');
                line.append(word);
            }
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines.toArray(new String[0]);
    }

    private String chooseDialogue(String avatarId) {
        String[] dialogues = DIALOGUES_BY_AVATAR.getOrDefault(avatarId, DEFAULT_DIALOGUES);
        String previousDialogue = previousDialogueByAvatar.get(avatarId);
        int selectedIndex = random.nextInt(dialogues.length);

        if (dialogues.length > 1 && dialogues[selectedIndex].equals(previousDialogue)) {
            selectedIndex = (selectedIndex + 1 + random.nextInt(dialogues.length - 1)) % dialogues.length;
        }

        String dialogue = dialogues[selectedIndex];
        previousDialogueByAvatar.put(avatarId, dialogue);
        return dialogue;
    }

    private long randomBreakMillis() {
        return MIN_BREAK_MILLIS + random.nextInt((int) (MAX_BREAK_MILLIS - MIN_BREAK_MILLIS + 1));
    }

    private void hideCurrentDialogue() {
        visible = false;
        currentDialogue = null;
    }
}
