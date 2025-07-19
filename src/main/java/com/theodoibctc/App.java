package com.theodoibctc;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.List;

public class App {

    private static final String CONFIG_FILE = "websites.txt";
    private static final String HASH_DIR = "hashes";
    private static final String TOPIC = "bctc-updates";

    public static void main(String[] args) throws Exception {
        System.out.println("🟢 Bắt đầu chạy chương trình...");
        List<String> lines = Files.readAllLines(Paths.get(CONFIG_FILE));
        Files.createDirectories(Paths.get(HASH_DIR));

        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("#"))
                continue;

            String[] parts = line.split("\\|");
            if (parts.length != 3) {
                System.err.println("⚠️ Bỏ qua dòng sai định dạng: " + line);
                continue;
            }

            String stockCode = parts[0].trim();
            String url = parts[1].trim();
            String selector = parts[2].trim();

            System.out.println("📡 Kiểm tra: " + stockCode + " (" + url + "), selector: " + selector);

            try {
                String content = fetchContent(url, selector);
                String currentHash = Integer.toString(content.hashCode());

                Path hashPath = Paths.get(HASH_DIR, stockCode + ".hash");
                String lastHash = Files.exists(hashPath) ? Files.readString(hashPath) : null;

                System.out.println("📂 Hash cũ đọc được: " + lastHash);
                System.out.println("📝 Hash mới: " + currentHash);

                if (!currentHash.equals(lastHash)) {
                    System.out.println("🔔 CÓ CẬP NHẬT cho " + stockCode);
                    sendNotification(stockCode, url);
                } else {
                    System.out.println("✅ Không thay đổi.");
                }

                Files.writeString(hashPath, currentHash);
                System.out.println("📁 Ghi hash file tại: " + hashPath.toAbsolutePath());

            } catch (Exception e) {
                System.err.println("❌ Lỗi với " + stockCode + ": " + e.getMessage());
            }
        }
    }

    private static String fetchContent(String urlStr, String selector) throws IOException {
        Document doc = Jsoup.connect(urlStr).get();

        if ("ALL".equalsIgnoreCase(selector)) {
            return doc.html();
        } else {
            Element element = doc.selectFirst(selector);
            if (element == null) {
                throw new IOException("Không tìm thấy selector: " + selector);
            }
            return element.html();
        }
    }

    private static void sendNotification(String stockCode, String url) {
        try {
            URL apiUrl = new URL("https://ntfy.sh/" + TOPIC);
            HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Title", stockCode + " cập nhật BCTC");
            conn.setRequestProperty("Content-Type", "text/plain");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(url.getBytes());
            }

            System.out.println("📬 Gửi noti ntfy: " + conn.getResponseCode());

        } catch (Exception e) {
            System.err.println("⚠️ Gửi thông báo lỗi: " + e.getMessage());
        }
    }
}
