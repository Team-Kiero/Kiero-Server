package com.kiero.terms.domain;

import com.kiero.terms.domain.enums.LinkType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = LinkTableConstants.TABLE_LINK)
public class Link {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = LinkTableConstants.COLUMN_ID)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = LinkTableConstants.COLUMN_TYPE, nullable = false)
	private LinkType type;

	@Column(name = LinkTableConstants.COLUMN_URL, nullable = false)
	private String url;
}
