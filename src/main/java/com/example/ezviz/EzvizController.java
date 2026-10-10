package com.example.ezviz;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ezviz")
public class EzvizController {

	private final EzvizService ezvizService;

	public EzvizController(EzvizService ezvizService) {
		this.ezvizService = ezvizService;
	}

	@GetMapping("/token")
	public Map<String, String> token() {
		return Map.of("accessToken", ezvizService.getAccessToken());
	}

	@GetMapping("/devices")
	public JsonNode devices(@RequestParam(defaultValue = "0") int pageStart,
							@RequestParam(defaultValue = "50") int pageSize) {
		return ezvizService.listCameras(pageStart, pageSize);
	}

	@GetMapping("/live")
	public JsonNode live(@RequestParam String deviceSerial,
						 @RequestParam(defaultValue = "1") int channelNo,
						 @RequestParam(defaultValue = "1") int quality) {
		return ezvizService.getLiveAddress(deviceSerial, channelNo, quality);
	}

	@PostMapping("/ptz")
	public JsonNode ptz(@RequestParam String deviceSerial,
						@RequestParam(defaultValue = "1") int channelNo,
						@RequestParam String action,
						@RequestParam(defaultValue = "5") int speed) {
		return ezvizService.startPtz(deviceSerial, channelNo, action, speed);
	}

	@PostMapping("/ptz/stop")
	public JsonNode ptzStop(@RequestParam String deviceSerial,
							@RequestParam(defaultValue = "1") int channelNo) {
		return ezvizService.stopPtz(deviceSerial, channelNo);
	}
}
