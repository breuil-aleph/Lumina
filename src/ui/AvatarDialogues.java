package ui;

import java.util.HashMap;
import java.util.Map;

public final class AvatarDialogues {

    private AvatarDialogues() {}

    // =====================================================
    // DEFAULT DIALOGUES
    // =====================================================

    public static final String[] DEFAULT = {
        "Hôm nay thật yên bình.",
        "Không biết phía bên kia có gì nhỉ?",
        "Thế giới Lumina thật kỳ diệu.",
        "Mong rằng ngày mai sẽ tốt đẹp hơn.",
        "Chúng ta đã đi được một chặng đường dài.",
        "Mình cảm nhận được nguồn năng lượng mới!"
    };

    // =====================================================
    // AVATAR DIALOGUES
    // =====================================================

    private static final Map<String, String[]> DIALOGUES = new HashMap<>();

    static {

        // LUMI - LIGHT SPIRIT
        DIALOGUES.put("lumi", new String[] {
            "Hôm nay chúng ta sẽ làm gì nhỉ?",
            "Lumina đẹp quá phải không?",
            "Mình tin chúng ta sẽ làm được!",
            "Bạn có nghe thấy tiếng ánh sáng không?",
            "Đừng quên chăm sóc những mầm cây nhé!",
            "Mình thích những ngày bình yên thế này.",
            "Có bạn ở đây, mình thấy vui lắm!",
            "Ước gì khoảnh khắc này kéo dài mãi."
        });

        // FIRE SPIRIT
        DIALOGUES.put("fire", new String[] {
            "Nào! Đốt cháy mọi thử thách!",
            "Lửa trong tim không bao giờ tắt!",
            "Hôm nay yên bình quá nhỉ?",
            "Mình thích những thứ rực rỡ!",
            "Đừng để ngọn lửa của bạn lụi tàn!",
            "Ước gì có thêm chút náo nhiệt!",
            "Nóng một chút mới thú vị chứ!",
            "Sức mạnh luôn đến từ bên trong."
        });

        // WATER SPIRIT
        DIALOGUES.put("water", new String[] {
            "Mọi chuyện rồi sẽ ổn thôi.",
            "Hãy để tâm hồn bình yên như mặt hồ.",
            "Bạn có muốn nghỉ ngơi một chút không?",
            "Nước luôn tìm được đường đi.",
            "Mình thích tiếng suối chảy.",
            "Đôi khi chậm lại cũng là một cách tiến lên.",
            "Thế giới này thật đẹp.",
            "Mong rằng mọi mầm sống đều được bình an."
        });

        // TERRA - EARTH SPIRIT
        DIALOGUES.put("terra", new String[] {
            "Cây cối hôm nay có vẻ rất vui.",
            "Kiên nhẫn nhé, mọi thứ đều cần thời gian.",
            "Bạn có thấy tinh thể kia lấp lánh không?",
            "Đất mẹ luôn che chở chúng ta.",
            "Một khu vườn đẹp cần tình yêu thương.",
            "Hihi, mình vừa tìm thấy một viên đá đẹp!",
            "Rễ càng sâu, cây càng vững.",
            "Thiên nhiên luôn có cách chữa lành."
        });

        // ZEPHYR - WIND SPIRIT
        DIALOGUES.put("zephyr", new String[] {
            "Gió hôm nay thật tuyệt!",
            "Ước gì mình có thể bay mãi!",
            "Đi khám phá một chút không?",
            "Bạn có cảm nhận được làn gió không?",
            "Tự do là điều tuyệt vời nhất!",
            "Đoán xem gió sẽ đưa mình đi đâu?",
            "Hihi, bắt được mình thì giỏi đấy!",
            "Đừng đứng yên mãi nhé!"
        });

        // SAGE ANCIENT
        DIALOGUES.put("sage", new String[] {
            "Ánh sáng luôn tồn tại trong bóng tối.",
            "Tri thức là khởi đầu của sức mạnh.",
            "Vạn vật đều có quy luật riêng.",
            "Thời gian sẽ trả lời mọi câu hỏi.",
            "Đừng bao giờ ngừng học hỏi.",
            "Ta đã chứng kiến nơi này đổi thay qua hàng nghìn năm.",
            "Một hạt giống nhỏ cũng có thể tạo nên kỳ tích.",
            "Sức mạnh lớn nhất đôi khi là lòng kiên nhẫn.",
            "Hãy lắng nghe, thế giới luôn có điều muốn kể."
        });
    }

    // =====================================================
    // PUBLIC ACCESS
    // =====================================================

    public static String[] getDialogues(String avatarId) {
        return DIALOGUES.getOrDefault(avatarId, DEFAULT);
    }
}