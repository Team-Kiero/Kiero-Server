package com.kiero.holiday;

import com.kiero.holiday.domain.Holiday;
import com.kiero.holiday.repository.HolidayRepository;
import com.kiero.holiday.service.HolidayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class HolidayServiceTest {

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private HolidayRepository holidayRepository;

    @Test
    @DisplayName("통합: 실제 공공데이터 API를 호출하여 XML 파싱 후 DB에 저장되는지 확인")
    void fetchAndSaveTest() {
        // when
        holidayService.fetchAndSaveHolidays();

        // then
        long count = holidayRepository.count();
        System.out.println("저장된 공휴일 개수: " + count);

        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("로직: DB에 있는 공휴일 데이터를 정확히 조회하는지 확인")
    void isHolidayTest() {
        // given
        LocalDate testDate = LocalDate.of(2099, 12, 25);
        Holiday testHoliday = Holiday.builder()
                .date(testDate)
                .name("미래의 크리스마스")
                .isHoliday(true)
                .build();

        holidayRepository.save(testHoliday);

        // when
        boolean isHoliday = holidayService.isHoliday(testDate);
        boolean isNotHoliday = holidayService.isHoliday(LocalDate.of(2099, 12, 26));

        // then
        assertThat(isHoliday).isTrue();
        assertThat(isNotHoliday).isFalse();
    }
}