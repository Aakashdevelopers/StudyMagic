package com.amstudio.examuplift.api;

import com.amstudio.examuplift.models.ai.RapidApiRequest;
import com.amstudio.examuplift.models.ai.RapidApiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface RapidApi {
    @POST("conversationgpt4-2")
    Call<RapidApiResponse> getCompletion(
            @Header("x-rapidapi-host") String host,
            @Header("x-rapidapi-key") String apiKey,
            @Body RapidApiRequest request
    );
}