package data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SaveManager {
    private static final String DIR = "saves/";

    public static boolean save(SaveData data, int slot) {
        try {
            File dir = new File(DIR);
            if (!dir.exists()) dir.mkdirs();

            FileOutputStream fos = new FileOutputStream(DIR + "slot" + slot + ".dat");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(data);
            oos.close();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static SaveData load(int slot) {
        try {
            File file = new File(DIR + "slot" + slot + ".dat");
            if (!file.exists()) return null;

            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            SaveData data = (SaveData) ois.readObject();
            ois.close();
            fis.close();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean hasSave(int slot) {
        return new File(DIR + "slot" + slot + ".dat").exists();
    }
}