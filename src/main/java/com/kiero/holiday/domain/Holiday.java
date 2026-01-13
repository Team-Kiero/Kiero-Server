package com.kiero.holiday.domain;

import com.kiero.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = HolidayTableConstants.TABLE_HOLIDAY)
public class Holiday extends BaseTimeEntity {
    @Id
    @Column(name = HolidayTableConstants.COLUMN_ID)
    private LocalDate date;

    @Column(name = HolidayTableConstants.COLUMN_NAME)
    private String name;

    @Column(name = HolidayTableConstants.COLUMN_IS_HOLIDAY)
    private boolean isHoliday;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}