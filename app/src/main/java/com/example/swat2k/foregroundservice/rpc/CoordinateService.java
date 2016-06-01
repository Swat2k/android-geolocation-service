package com.example.swat2k.foregroundservice.rpc;

import com.example.swat2k.foregroundservice.dto.Coordinate;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface CoordinateService {
    @POST("geolocation")
    Call<Void> Send(@Body Coordinate body);
}
