package com.amstudio.examuplift.api;

import com.amstudio.examuplift.models.ai.ChatRequest;
import com.amstudio.examuplift.models.ai.ChatResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenRouterApi {
    @POST("chat/completions")
    Call<ChatResponse> getCompletion(
            @Header("Authorization") String auth,
            @Body ChatRequest request
    );
}