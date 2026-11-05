package org.example.bookweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.bookweb.dto.IsbnBookResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * ISBN 开放数据代理（v0.7，PRD 2.1 锁定）：
 * - 外部 HTTP 只用 Spring RestClient（禁止 OkHttp/WebClient/RestTemplate）；
 * - 数据源顺序锁定：OpenLibrary → Google Books（禁止换豆瓣等书源）；
 * - 超时 ISBN_PROXY_TIMEOUT 默认 5000ms（连接与读取同值）；
 * - 外部调用失败/超时不得抛 500：抛 UpstreamUnavailableException，由全局异常处理回
 *   200 + code 0 + data:null + 明确 message；干净未命中返回 null（「未查到该书目，请手动录入」）。
 */
@Slf4j
@Service
public class IsbnService {

    /** 外部书源整体不可用 → 200 + data:null + 明确 message（禁止 500） */
    public static class UpstreamUnavailableException extends RuntimeException {
        public UpstreamUnavailableException(String message) { super(message); }
    }

    private static final String OPENLIBRARY_URL =
            "https://openlibrary.org/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data";
    private static final String GOOGLE_BOOKS_URL =
            "https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IsbnService(@Value("${ISBN_PROXY_TIMEOUT:5000}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** 依次尝试 OpenLibrary → Google Books；干净未命中返回 null，外部失败抛 UpstreamUnavailableException */
    public IsbnBookResponse lookup(String isbn) {
        try {
            IsbnBookResponse fromOpenLibrary = fromOpenLibrary(isbn);
            if (fromOpenLibrary != null) {
                return fromOpenLibrary;
            }
            return fromGoogleBooks(isbn);
        } catch (RestClientException e) {
            // 只记概要，不落堆栈/URL 之外的敏感信息（PRD 3.3）
            log.warn("ISBN 外部书源调用失败: {}", e.getMessage());
            throw new UpstreamUnavailableException("外部书源暂时不可用，请稍后重试或手动录入");
        } catch (UpstreamUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("ISBN 书源响应解析失败: {}", e.getMessage());
            throw new UpstreamUnavailableException("外部书源响应异常，请稍后重试或手动录入");
        }
    }

    /** OpenLibrary jscmd=data：{ "ISBN:xxx": { title, authors[], publishers[], cover{}, notes } } */
    private IsbnBookResponse fromOpenLibrary(String isbn) throws Exception {
        String body = restClient.get().uri(OPENLIBRARY_URL, isbn).retrieve().body(String.class);
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonNode root = objectMapper.readTree(body);
        JsonNode entry = root.path("ISBN:" + isbn);
        String title = entry.path("title").asText("");
        if (title.isBlank()) {
            return null;
        }
        IsbnBookResponse r = new IsbnBookResponse();
        r.setTitle(title);
        JsonNode authors = entry.path("authors");
        if (authors.isArray() && !authors.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (JsonNode a : authors) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(a.path("name").asText());
            }
            r.setAuthor(names.toString());
        }
        JsonNode publishers = entry.path("publishers");
        if (publishers.isArray() && !publishers.isEmpty()) {
            r.setPublisher(publishers.get(0).path("name").asText(null));
        }
        JsonNode cover = entry.path("cover");
        if (cover.isObject()) {
            String coverUrl = firstText(cover, "large", "medium", "small");
            r.setCoverUrl(coverUrl);
        }
        String notes = entry.path("notes").asText("");
        if (!notes.isBlank()) {
            r.setDescription(notes);
        }
        return r;
    }

    /** Google Books：{ totalItems, items[]: { volumeInfo: { title, authors[], publisher, description, imageLinks{} } } } */
    private IsbnBookResponse fromGoogleBooks(String isbn) throws Exception {
        String body = restClient.get().uri(GOOGLE_BOOKS_URL, isbn).retrieve().body(String.class);
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonNode root = objectMapper.readTree(body);
        JsonNode items = root.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return null;
        }
        JsonNode info = items.get(0).path("volumeInfo");
        String title = info.path("title").asText("");
        if (title.isBlank()) {
            return null;
        }
        IsbnBookResponse r = new IsbnBookResponse();
        r.setTitle(title);
        JsonNode authors = info.path("authors");
        if (authors.isArray() && !authors.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (JsonNode a : authors) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(a.asText());
            }
            r.setAuthor(names.toString());
        }
        r.setPublisher(info.path("publisher").asText(null));
        r.setDescription(info.path("description").asText(null));
        JsonNode imageLinks = info.path("imageLinks");
        if (imageLinks.isObject()) {
            r.setCoverUrl(firstText(imageLinks, "thumbnail", "smallThumbnail"));
        }
        return r;
    }

    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            String v = node.path(key).asText("");
            if (!v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
