package data;

import java.io.Serializable;

public class SaveData implements Serializable {
    private static final long serialVersionUID = 1L;

    public String saveTime;
    public double difficultyMultiplier;

    // Kho hạt & năng lượng (index 1..4)
    public int[] seeds = new int[5];
    public double[] energies = new double[5];

    // Trạng thái mở khóa bản đồ
    public boolean[] mapUnlocked = new boolean[5];

    // Sách phép bí truyền
    public boolean hasMagicBook;

    // Dữ liệu cây trồng: [32][32]
    // plantElement[r][c] = 0 (trống), 1 (Lửa), 2 (Nước), 3 (Đất), 4 (Gió)
    public int[][] plantElement = new int[32][32];
    public int[][] plantLevel = new int[32][32];
}