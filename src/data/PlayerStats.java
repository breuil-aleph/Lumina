package data;

import java.io.Serializable;

/**
 * Lưu trữ thống kê tổng thể của người chơi.
 * Đồng bộ cùng SaveData qua Java Serialization.
 */
public class PlayerStats implements Serializable {
    private static final long serialVersionUID = 1L;

    // Thời gian chơi tích lũy (giây)
    private long playTimeSeconds = 0;

    // Cây cối
    private int totalPlantsPlanted = 0;
    private int totalPlantsUpgraded = 0;

    // Tài nguyên tích lũy (Index: 1 = Lửa, 2 = Nước, 3 = Đất, 4 = Gió)
    private double[] totalEnergyCollected = new double[5];
    private int[] totalSeedsCollected = new int[5];

    // Gacha & Khác
    private int totalGachaRolls = 0;

    public PlayerStats() {
        // Khởi tạo mặc định
    }

    // =========================
    // PLAYTIME
    // =========================
    public long getPlayTimeSeconds() {
        return playTimeSeconds;
    }

    public void addPlayTimeSeconds(long seconds) {
        if (seconds > 0) {
            playTimeSeconds += seconds;
        }
    }

    public String getFormattedPlayTime() {
        long hours = playTimeSeconds / 3600;
        long minutes = (playTimeSeconds % 3600) / 60;
        long seconds = playTimeSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // =========================
    // PLANTS
    // =========================
    public int getTotalPlantsPlanted() {
        return totalPlantsPlanted;
    }

    public void incrementPlantsPlanted() {
        totalPlantsPlanted++;
    }

    public int getTotalPlantsUpgraded() {
        return totalPlantsUpgraded;
    }

    public void incrementPlantsUpgraded() {
        totalPlantsUpgraded++;
    }

    // =========================
    // ENERGY & SEEDS
    // =========================
    public double getTotalEnergyCollected(int elementId) {
        return isValidElementId(elementId) ? totalEnergyCollected[elementId] : 0.0;
    }

    public void addEnergyCollected(int elementId, double amount) {
        if (isValidElementId(elementId) && amount > 0) {
            totalEnergyCollected[elementId] += amount;
        }
    }

    public int getTotalSeedsCollected(int elementId) {
        return isValidElementId(elementId) ? totalSeedsCollected[elementId] : 0;
    }

    public void addSeedsCollected(int elementId, int amount) {
        if (isValidElementId(elementId) && amount > 0) {
            totalSeedsCollected[elementId] += amount;
        }
    }

    public double getAllEnergyCollected() {
        double total = 0.0;
        for (int i = 1; i <= 4; i++) {
            total += totalEnergyCollected[i];
        }
        return total;
    }

    // =========================
    // GACHA
    // =========================
    public int getTotalGachaRolls() {
        return totalGachaRolls;
    }

    public void incrementGachaRolls() {
        totalGachaRolls++;
    }

    // =========================
    // UTILITIES
    // =========================
    private boolean isValidElementId(int elementId) {
        return elementId >= 1 && elementId <= 4;
    }
}