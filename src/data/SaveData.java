package data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SaveData implements Serializable {
    private static final long serialVersionUID = 1L;

    public String saveTime;
    public double difficultyMultiplier;

    // Kho hạt & năng lượng (index 1..4: 1=Lửa, 2=Nước, 3=Đất, 4=Gió)
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

    // ==========================================
    // DỮ LIỆU MỚI: AVATAR & PLAYER STATS
    // ==========================================
    
    // Thống kê chi tiết của người chơi
    public PlayerStats stats = new PlayerStats();

    // ID của avatar đang được trang bị (Mặc định: "lumi")
    public String activeAvatarId = "lumi";

    // Danh sách các ID avatar người chơi đã mở khóa sở hữu
    public List<String> unlockedAvatarIds = new ArrayList<>();

    public SaveData() {
        // Mặc định ban đầu luôn mở khóa nhân vật khởi đầu
        unlockedAvatarIds.add("lumi");
    }

    /**
     * Cơ chế của Java Serialization:
     * Được gọi tự động ngay sau khi deserialize file .dat cũ lên.
     * Đảm bảo các save cũ không bị null ở các trường mới thêm.
     */
    private Object readResolve() {
        if (stats == null) {
            stats = new PlayerStats();
        }
        if (activeAvatarId == null) {
            activeAvatarId = "lumi";
        }
        if (unlockedAvatarIds == null) {
            unlockedAvatarIds = new ArrayList<>();
        }
        if (!unlockedAvatarIds.contains("lumi")) {
            unlockedAvatarIds.add("lumi");
        }
        return this;
    }
}