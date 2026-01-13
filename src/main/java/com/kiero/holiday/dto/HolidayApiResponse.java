package com.kiero.holiday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

// <response> 태그
@JacksonXmlRootElement(localName = "response")
@JsonIgnoreProperties(ignoreUnknown = true)
public record HolidayApiResponse(
        Header header,
        Body body
) {
    // <header> 태그
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
            String resultCode,
            String resultMsg
    ) {}

    // <body> 태그
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            Items items,
            Integer numOfRows,
            Integer pageNo,
            Integer totalCount
    ) {}

    // <items> 태그
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "item")
            List<Item> itemList
    ) {}

    // <item> 태그
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String dateKind,
            String dateName,
            String isHoliday,
            Integer locdate,
            Integer seq
    ) {}
}