package data;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class SaveManager {
    private static final String DIR = "saves/";

    /**
     * Lưu game an toàn bằng kỹ thuật ghi ra file tạm .tmp rồi atomic rename/replace.
     * Tránh lỗi hỏng file save nếu game bị crash hoặc tắt đột ngột giữa chừng.
     */
    public static boolean save(SaveData data, int slot) {
        if (data == null) {
            System.err.println("Cannot save slot " + slot + ": save data is null");
            return false;
        }

        Path tempFile = null;
        try {
            Path directory = Paths.get(DIR);
            Files.createDirectories(directory);
            Path saveFile = directory.resolve("slot" + slot + ".dat");
            tempFile = Files.createTempFile(directory, "slot" + slot + "-", ".tmp");

            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(tempFile))) {
                output.writeObject(data);
            }

            try {
                Files.move(tempFile, saveFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, saveFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Cannot save slot " + slot + ": " + describe(e));
            return false;
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    System.err.println("Cannot remove temporary save file " + tempFile + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Tải dữ liệu lưu từ file slotX.dat
     */
    public static SaveData load(int slot) {
        File file = new File(DIR + "slot" + slot + ".dat");
        try {
            if (!file.exists()) return null;

            try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file.toPath()))) {
                Object saved = input.readObject();
                if (!(saved instanceof SaveData)) {
                    System.err.println("Cannot load save " + file + ": file does not contain SaveData");
                    return null;
                }
                return (SaveData) saved;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Cannot load save " + file + ": " + describe(e));
            return null;
        }
    }

    private static String describe(Exception e) {
        String message = e.getMessage();
        return message == null || message.isEmpty() ? e.getClass().getSimpleName() : message;
    }

    public static boolean hasSave(int slot) {
        return new File(DIR + "slot" + slot + ".dat").exists();
    }
}