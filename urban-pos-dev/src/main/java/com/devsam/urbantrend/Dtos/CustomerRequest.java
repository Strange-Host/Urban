package com.devsam.urbantrend.Dtos;

import lombok.Data;

@Data
public class CustomerRequest {
    private String  name;
    private String phone;
    private String email;
    private String city_town;
}
