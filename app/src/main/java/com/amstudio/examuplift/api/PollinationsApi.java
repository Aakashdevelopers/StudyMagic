package com.amstudio.examuplift.api;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface PollinationsApi {
    @Headers("Content-Type: application/json")
    @POST("/")
    Call<ResponseBody> getCompletion(@Body PollinationsRequest request);

    class PollinationsRequest {
        public List<Message> messages;
        public String model;

        public PollinationsRequest(List<Message> messages) {
            this.messages = messages;
            this.model = "openai";
        }

        public PollinationsRequest(String prompt) {
            this.messages = new ArrayList<>();
            this.messages.add(new Message("user", prompt));
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