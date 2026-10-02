package vn.ptit.one.shared.mail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/** Đường Brevo API, chạy với một HTTP server giả thay cho api.brevo.com. Không cần mạng hay DB. */
class MailerTest {

    private record Captured(String method, String apiKey, String body) {
    }

    private HttpServer server;
    private final BlockingQueue<Captured> requests = new ArrayBlockingQueue<>(4);

    @BeforeEach
    void startFakeBrevo() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/smtp/email", exchange -> {
            requests.add(new Captured(exchange.getRequestMethod(), exchange.getRequestHeaders().getFirst("api-key"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            byte[] reply = "{\"messageId\":\"<x@brevo>\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void coApiKeyThiGoiBrevoApi() throws Exception {
        Mailer mailer = new Mailer(properties("xkeysib-test", null));

        mailer.sendAfterCommit("an@gmail.com", "Mã kích hoạt", "Mã: 1234");

        Captured request = requests.poll(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.apiKey()).isEqualTo("xkeysib-test");
        assertThat(request.body()).contains("\"email\":\"an@gmail.com\"", "\"email\":\"noreply@ptit.test\"",
                "\"subject\":\"Mã kích hoạt\"", "\"textContent\":\"Mã: 1234\"");
        mailer.destroy();
    }

    @Test
    void thieuNguoiGuiHoacThieuCaKeyLanHostThiTat() {
        assertThat(new Mailer(properties(null, null)).enabled()).isFalse();
        assertThat(new Mailer(new MailProperties("xkeysib-test", url(), null, 587, null, null, "", "PTIT One"))
                .enabled()).isFalse();
        assertThat(new Mailer(properties(null, "smtp-relay.brevo.com")).enabled()).isTrue();
    }

    private MailProperties properties(String apiKey, String host) {
        return new MailProperties(apiKey, url(), host, 587, null, null, "noreply@ptit.test", "PTIT One");
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v3/smtp/email";
    }
}
