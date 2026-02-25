package com.yuz.toplinks.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuz.toplinks.dto.UspsRateRequest;
import com.yuz.toplinks.dto.UspsRateResponse;
import com.yuz.toplinks.dto.UspsRateResponse.RateDetail;

/**
 * USPS 运费查询服务。
 * <p>
 * 使用 USPS Web Tools API v3（RESTful）查询国内邮寄价格。
 * <ul>
 *   <li>OAuth2 令牌端点: POST {baseUrl}/oauth2/v3/token</li>
 *   <li>价格查询端点 : POST {baseUrl}/prices/v3/total-rates/search</li>
 * </ul>
 * 需要在 USPS 开发者门户 (https://developer.usps.com) 注册应用获取 Consumer Key 和 Consumer Secret。
 * </p>
 *
 * <h3>所需配置参数（application.properties）</h3>
 * <pre>
 * usps.api.base-url=https://api.usps.com          # 生产环境
 * usps.api.consumer-key=${USPS_CONSUMER_KEY:}
 * usps.api.consumer-secret=${USPS_CONSUMER_SECRET:}
 * </pre>
 *
 * <h3>请求必需参数</h3>
 * <ul>
 *   <li>originZIPCode      — 寄件人邮编（5 位）</li>
 *   <li>destinationZIPCode — 收件人邮编（5 位）</li>
 *   <li>weight             — 包裹重量（磅）</li>
 *   <li>mailClass          — 邮寄类别（可选，为空则查询所有可用类别）</li>
 *   <li>processingCategory — 处理类别（默认 PARCELS）</li>
 *   <li>rateIndicator      — 费率指示器（默认 SP）</li>
 *   <li>priceType          — 价格类型（默认 RETAIL）</li>
 *   <li>length / width / height — 包裹尺寸（英寸，可选）</li>
 * </ul>
 */
@Service
public class UspsService {

    private static final Logger logger = Logger.getLogger(UspsService.class.getName());

    private final String baseUrl;
    private final String consumerKey;
    private final String consumerSecret;
    private final boolean enabled;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /** 缓存的 OAuth2 access token */
    private volatile String accessToken;
    /** Token 到期时间 */
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public UspsService(
            @Value("${usps.api.base-url:https://api.usps.com}") String baseUrl,
            @Value("${usps.api.consumer-key:}") String consumerKey,
            @Value("${usps.api.consumer-secret:}") String consumerSecret) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.enabled = !consumerKey.isBlank() && !consumerSecret.isBlank();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * USPS 功能是否已启用（凭证已配置）。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 根据请求参数查询 USPS 运费。
     *
     * @param request 运费请求
     * @return 运费结果
     */
    public UspsRateResponse calculateRate(UspsRateRequest request) {
        if (!enabled) {
            return UspsRateResponse.error("USPS API is not configured. Set usps.api.consumer-key and usps.api.consumer-secret.");
        }

        try {
            String token = getAccessToken();
            String body = buildRateRequestBody(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/prices/v3/total-rates/search"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseRateResponse(response.body());
            } else {
                logger.warning("USPS rate API returned status " + response.statusCode() + ": " + response.body());
                return UspsRateResponse.error("USPS API error (HTTP " + response.statusCode() + "): " + extractErrorMessage(response.body()));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to call USPS rate API", e);
            return UspsRateResponse.error("Failed to call USPS API: " + e.getMessage());
        }
    }

    // ---- OAuth2 token management ----

    String getAccessToken() throws Exception {
        if (accessToken != null && Instant.now().isBefore(tokenExpiry)) {
            return accessToken;
        }
        return refreshAccessToken();
    }

    private synchronized String refreshAccessToken() throws Exception {
        // Double-check after acquiring lock
        if (accessToken != null && Instant.now().isBefore(tokenExpiry)) {
            return accessToken;
        }

        String tokenUrl = baseUrl + "/oauth2/v3/token";
        String formBody = "grant_type=client_credentials"
                + "&client_id=" + consumerKey
                + "&client_secret=" + consumerSecret;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to obtain USPS access token (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        this.accessToken = json.get("access_token").asText();
        int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 3600;
        // Expire 60 seconds early to avoid edge cases
        this.tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);
        return this.accessToken;
    }

    // ---- Request / Response helpers ----

    String buildRateRequestBody(UspsRateRequest req) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("originZIPCode", req.getOriginZIPCode());
        root.put("destinationZIPCode", req.getDestinationZIPCode());
        root.put("weight", req.getWeight());

        root.put("mailClass", defaultIfBlank(req.getMailClass(), "ALL"));
        root.put("processingCategory", defaultIfBlank(req.getProcessingCategory(), "PARCELS"));
        root.put("rateIndicator", defaultIfBlank(req.getRateIndicator(), "SP"));
        root.put("destinationEntryFacilityType", "NONE");
        root.put("priceType", defaultIfBlank(req.getPriceType(), "RETAIL"));

        if (req.getLength() > 0) root.put("length", req.getLength());
        if (req.getWidth() > 0) root.put("width", req.getWidth());
        if (req.getHeight() > 0) root.put("height", req.getHeight());

        return objectMapper.writeValueAsString(root);
    }

    UspsRateResponse parseRateResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        List<RateDetail> rates = new ArrayList<>();

        // The USPS v3 total-rates response contains a "totalBasePrice" or "rates" array
        JsonNode ratesNode = root.has("rates") ? root.get("rates") : null;
        if (ratesNode != null && ratesNode.isArray()) {
            for (JsonNode rate : ratesNode) {
                RateDetail detail = new RateDetail();
                detail.setMailClass(textOrEmpty(rate, "mailClass"));
                detail.setProductName(textOrEmpty(rate, "productName"));
                detail.setTotalPrice(rate.has("totalPrice") ? rate.get("totalPrice").asDouble() : 0);
                detail.setDeliveryDays(textOrEmpty(rate, "deliveryDays"));
                detail.setZone(textOrEmpty(rate, "zone"));
                rates.add(detail);
            }
        } else {
            // Single rate response
            RateDetail detail = new RateDetail();
            detail.setMailClass(textOrEmpty(root, "mailClass"));
            detail.setProductName(textOrEmpty(root, "productName"));
            detail.setTotalPrice(root.has("totalPrice") ? root.get("totalPrice").asDouble() : 0);
            detail.setDeliveryDays(textOrEmpty(root, "deliveryDays"));
            detail.setZone(textOrEmpty(root, "zone"));
            if (detail.getTotalPrice() > 0) {
                rates.add(detail);
            }
        }

        return new UspsRateResponse(true, "OK", rates);
    }

    private String extractErrorMessage(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("message")) return root.get("message").asText();
            if (root.has("error") && root.get("error").has("message")) return root.get("error").get("message").asText();
        } catch (Exception ignored) {}
        return body.length() > 200 ? body.substring(0, 200) : body;
    }

    private static String textOrEmpty(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "";
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
