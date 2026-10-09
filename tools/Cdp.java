import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Electron 앱(Blockbench)을 --remote-debugging-port=9222 로 띄운 뒤 DevTools 프로토콜로 제어하는 도구.
 *   java tools/Cdp.java eval "<js 식>"        식을 실행하고 결과를 출력 (Blockbench 의 JS API 사용 가능)
 *   java tools/Cdp.java eval @파일.js         파일 내용을 실행
 *   java tools/Cdp.java shot 출력.png          창 화면을 PNG 로 저장
 */
public class Cdp {
    static String json(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }

    public static void main(String[] a) throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        String list = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:9222/json")).build(), HttpResponse.BodyHandlers.ofString()).body();
        Matcher m = Pattern.compile("\"type\":\\s*\"page\"[^}]*?\"webSocketDebuggerUrl\":\\s*\"([^\"]+)\"", Pattern.DOTALL).matcher(list);
        if (!m.find()) { System.err.println("page 대상이 없습니다:\n" + list); System.exit(2); }

        CompletableFuture<String> reply = new CompletableFuture<>();
        StringBuilder buf = new StringBuilder();
        WebSocket ws = http.newWebSocketBuilder().buildAsync(URI.create(m.group(1)), new WebSocket.Listener() {
            @Override public CompletionStage<?> onText(WebSocket w, CharSequence data, boolean last) {
                buf.append(data);
                if (last) {
                    String msg = buf.toString(); buf.setLength(0);
                    if (msg.startsWith("{\"id\":1,") || msg.contains("\"id\":1,\"")) reply.complete(msg);
                }
                w.request(1);
                return null;
            }
            @Override public void onOpen(WebSocket w) { w.request(1); }
        }).get(10, TimeUnit.SECONDS);

        String cmd = a[0];
        String req;
        if (cmd.equals("shot")) {
            req = "{\"id\":1,\"method\":\"Page.captureScreenshot\",\"params\":{\"format\":\"png\"}}";
        } else {
            String js = a[1].startsWith("@") ? Files.readString(Path.of(a[1].substring(1))) : a[1];
            req = "{\"id\":1,\"method\":\"Runtime.evaluate\",\"params\":{\"expression\":" + json(js)
                    + ",\"awaitPromise\":true,\"returnByValue\":true,\"timeout\":60000}}";
        }
        ws.sendText(req, true);
        String out = reply.get(90, TimeUnit.SECONDS);
        if (cmd.equals("shot")) {
            Matcher d = Pattern.compile("\"data\":\"([^\"]+)\"").matcher(out);
            if (!d.find()) { System.err.println(out); System.exit(3); }
            Files.write(Path.of(a[1]), Base64.getDecoder().decode(d.group(1)));
            System.out.println("saved " + a[1]);
        } else {
            System.out.println(out);
        }
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        System.exit(0);
    }
}
