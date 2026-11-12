package com.devsam.urbantrend.Dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank
    private String name;

    private String description;
}