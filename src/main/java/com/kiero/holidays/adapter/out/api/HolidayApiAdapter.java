package com.kiero.holidays.adapter.out.api;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.kiero.holidays.application.dto.HolidayApiResponse;
import com.kiero.holidays.application.port.out.HolidayApiPort;
import com.kiero.holidays.domain.Holiday;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class HolidayApiAdapter implements HolidayApiPort {

	@Value("${open-api.service-key}")
	private String serviceKey;

	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public List<Holiday> fetchHolidays(int year, int month) {

		try {
			URI uri = UriComponentsBuilder
				.fromHttpUrl("https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo")
				.queryParam("serviceKey", serviceKey)
				.queryParam("solYear", year)
				.queryParam("solMonth", String.format("%02d", month))
				.build(true)
				.toUri();

			HolidayApiResponse response =
				restTemplate.getForObject(uri, HolidayApiResponse.class);

			if (!isValid(response)) return List.of();

			return response.body().items().itemList().stream()
				.filter(i -> "Y".equals(i.isHoliday()))
				.map(this::toEntity)
				.toList();

		} catch (Exception e) {
			log.error("Failed to fetch holidays {}-{}", year, month, e);
			return List.of();
		}
	}

	private boolean isValid(HolidayApiResponse res) {
		return res != null
			&& res.body() != null
			&& res.body().items() != null
			&& res.body().items().itemList() != null
			&& !res.body().items().itemList().isEmpty();
	}

	private Holiday toEntity(HolidayApiResponse.Item item) {
		return Holiday.builder()
			.date(parseDate(item.locdate()))
			.name(item.dateName())
			.build();
	}

	private LocalDate parseDate(Integer locdate) {
		return LocalDate.parse(
			String.valueOf(locdate),
			DateTimeFormatter.BASIC_ISO_DATE
		);
	}
}
