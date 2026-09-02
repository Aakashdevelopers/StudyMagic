package com.amstudio.studymagic.utils;

public class Constants {
    public static final boolean DEBUG = true; // Set to false for production
    public static final String SUPABASE_URL = "https://crafyflxsvgjvvuzdftm.supabase.co/rest/v1/";
    public static final String SUPABASE_KEY = "sb_publishable_NxzsXWLHPGkNi0jTVx5k1Q_QYoRwGFo";
    
    public static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/";
    // IMPORTANT: For production, do NOT hardcode secrets. Move this to a secure backend or environment variable.
    public static final String OPENROUTER_KEY = "";
    public static final String AI_MODEL = "nvidia/nemotron-3-super-120b-a12b:free";
}
