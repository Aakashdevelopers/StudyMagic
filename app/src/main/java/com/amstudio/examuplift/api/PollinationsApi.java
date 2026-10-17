package com.amstudio.examuplift.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface PollinationsApi {
    @Headers("Content-Type: application/json")
    @POST(".")
    Call<ResponseBody> getCompletion(@Body PollinationsRequest request);

    class PollinationsRequest {
        public Message[] messages;
        public String model;

        public PollinationsRequest(String prompt) {
            this.messages = new Message[]{ new Message("user", prompt) };
            this.model = "openai";
        }

        public static class Message {
            public String role;
            public String content;

            public Message(String role, String content) {
                this.role = role;
                this.content = content;
            }
        }
    }
}