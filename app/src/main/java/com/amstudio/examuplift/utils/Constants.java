package com.amstudio.examuplift.utils;

import com.amstudio.examuplift.BuildConfig;

public class Constants {
    public static final boolean DEBUG = BuildConfig.DEBUG;
    public static final String SUPABASE_URL = "https://crafyflxsvgjvvuzdftm.supabase.co/rest/v1/";
    public static final String SUPABASE_KEY = "sb_publishable_NxzsXWLHPGkNi0jTVx5k1Q_QYoRwGFo";
    
    // OpenRouter AI configuration (Ultra Fast)
    public static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/";
    public static String OPENROUTER_KEY = BuildConfig.OPENROUTER_KEY; // Loaded securely from local.properties
    public static final String OPENROUTER_MODEL = "google/gemini-2.0-flash-lite-preview-02-05:free";
    public static final String OPENROUTER_FALLBACK_MODEL = "meta-llama/llama-3.3-70b-instruct:free";

    // Alternative AI configurations
    public static final String RAPIDAPI_URL = "https://chatgpt-42.p.rapidapi.com/";
    public static final String RAPIDAPI_HOST = "chatgpt-42.p.rapidapi.com";
    public static final String RAPIDAPI_KEY = BuildConfig.RAPIDAPI_KEY;
}