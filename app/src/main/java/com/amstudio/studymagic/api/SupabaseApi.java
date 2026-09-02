package com.amstudio.studymagic.api;

import com.amstudio.studymagic.models.Category;
import com.amstudio.studymagic.models.Chapter;
import com.amstudio.studymagic.models.Note;
import com.amstudio.studymagic.models.Subject;
import com.amstudio.studymagic.models.SupabaseTest;
import com.amstudio.studymagic.models.Topic;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseApi {
    @GET("mockup_tests?select=*&is_featured=eq.true")
    Call<List<SupabaseTest>> getFeaturedTests();

    @GET("mockup_tests?select=*")
    Call<List<SupabaseTest>> getAllMockupTests();

    @GET("notes?select=*&order=created_at.desc")
    Call<List<Note>> getNotes();

    @POST("notes")
    Call<Void> insertNote(@Body Note note);

    @GET("subjects")
    Call<List<Subject>> getSubjects(@Query("category_id") String categoryId);

    @GET("chapters")
    Call<List<Chapter>> getChapters(@Query("subject_id") String subjectId);

    @GET("topics")
    Call<List<Topic>> getTopics(@Query("chapter_id") String chapterId);

    @GET("tests")
    Call<List<SupabaseTest>> getTestsByTopic(@Query("topic_id") String topicId);

    @GET("tests")
    Call<List<SupabaseTest>> getTestsByCategory(@Query("category_id") String categoryId);

    @GET("categories")
    Call<List<Category>> getCategories();
}
