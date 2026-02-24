package com.cobak.carcatalog.model.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarResponseDTO {

    private UUID id;
    private String brand;
    private String model;
    private Integer year;
    private BigDecimal price;
    private String color;
    private String description;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
