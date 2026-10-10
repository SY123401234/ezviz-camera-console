package com.example.ezviz;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class EzvizService {

	private static final String SUCCESS_CODE = "200";
	private static final Duration TOKEN_TTL = Duration.ofDays(6);
	private static final Map<String, String> DIRECTIONS = Map.of(
			"up", "0",
			"down", "1",
			"left", "2",
			"right", "3"
	);

	private final RestTemplate restTemplate = new RestTemplate();
	private final String baseUrl;
	private final String appKey;
	private final String appSecret;

	private String cachedToken;
	private Instant tokenExpiresAt = Instant.EPOCH;

	public EzvizService(@Value("${ezviz.base-url}") String baseUrl,
						@Value("${ezviz.app-key}") String appKey,
						@Value("${ezviz.app-secret}") String appSecret) {
		this.baseUrl = baseUrl;
		this.appKey = appKey;
		this.appSecret = appSecret;
	}

	public synchronized String getAccessToken() {
		if (cachedToken == null || Instant.now().isAfter(tokenExpiresAt)) {
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("appKey", appKey);
			form.add("appSecret", appSecret);
			JsonNode body = postForm("/api/lapp/token/get", form);
			cachedToken = body.path("data").path("accessToken").asText();
			tokenExpiresAt = Instant.now().plus(TOKEN_TTL);
		}
		return cachedToken;
	}

	public JsonNode listCameras(int pageStart, int pageSize) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("pageStart", String.valueOf(pageStart));
		form.add("pageSize", String.valueOf(pageSize));
		return callAuthorized("/api/lapp/camera/list", form);
	}

	public JsonNode getLiveAddress(String deviceSerial, int channelNo, int quality) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("deviceSerial", deviceSerial);
		form.add("channelNo", String.valueOf(channelNo));
		form.add("quality", String.valueOf(quality));
		return callAuthorized("/api/lapp/v2/live/address/get", form);
	}

	public JsonNode startPtz(String deviceSerial, int channelNo, String action, int speed) {
		String direction = DIRECTIONS.get(action);
		if (direction == null) {
			throw new IllegalArgumentException("不支持的云台方向: " + action);
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("deviceSerial", deviceSerial);
		form.add("channelNo", String.valueOf(channelNo));
		form.add("direction", direction);
		form.add("speed", String.valueOf(speed));
		return callAuthorized("/api/lapp/device/ptz/start", form);
	}

	public JsonNode stopPtz(String deviceSerial, int channelNo) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("deviceSerial", deviceSerial);
		form.add("channelNo", String.valueOf(channelNo));
		form.add("direction", "0");
		return callAuthorized("/api/lapp/device/ptz/stop", form);
	}

	private JsonNode callAuthorized(String path, MultiValueMap<String, String> form) {
		form.add("accessToken", getAccessToken());
		return postForm(path, form);
	}

	private JsonNode postForm(String path, MultiValueMap<String, String> form) {
		ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + path, form, JsonNode.class);
		JsonNode body = response.getBody();
		if (body == null) {
			throw new IllegalStateException("萤石云接口返回空响应: " + path);
		}
		String code = body.path("code").asText();
		if (!code.isEmpty() && !SUCCESS_CODE.equals(code)) {
			throw new IllegalStateException("萤石云接口错误 [" + code + "]: " + body.path("msg").asText() + " (" + path + ")");
		}
		return body;
	}
}
