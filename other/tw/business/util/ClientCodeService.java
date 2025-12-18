package other.tw.business.util;

import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientCodeService {
    public static String generateUniqueFromName(String clientName) throws ApiEntityServiceException {
        String code = generateShortCode(clientName);
        return code;
    }

    private static List<String> toWords(String name) {
        List<String> words = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) return words;
        String[] tokens = name.split("[^A-Za-z0-9]+");
        for (String t : tokens) {
            if (t != null) {
                String s = t.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
                if (!s.isEmpty()) words.add(s);
            }
        }
        return words;
    }

    private static String generateShortCode(String clientName) {
        List<String> words = toWords(clientName);
        String w1 = words.size() >= 1 ? words.get(0).replaceAll("[^A-Z]", "") : "";
        String w2 = words.size() >= 2 ? words.get(1).replaceAll("[^A-Z]", "") : "";
        String p1 = w1.length() >= 3 ? w1.substring(0, 3) : w1;
        String p2 = w2.length() >= 2 ? w2.substring(0, 2) : w2;
        if (p1.length() < 3) {
            p1 = (p1 + "XXX").substring(0, 3);
        }
        if (p2.length() < 2) {
            p2 = (p2 + "XX").substring(0, 2);
        }
        return p1 + p2;
    }
        public static void main(String[] args) throws ApiEntityServiceException {
            String clientName = "LI JIN HUAN DE CE SHI KEHU4";
            String code = generateUniqueFromName(clientName);
            System.out.println(code);
        }
}
