package com.example.chillisauce.spaces.dto.request;

import com.example.chillisauce.spaces.dto.response.LocationDto;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class MultiBoxRequestDto {


    private String multiBoxName;

    private String x;

    private String y;



    public MultiBoxRequestDto(String locationName, String x, String y) {
        this.multiBoxName = locationName;
        this.x = x;
        this.y = y;
    }
}
