package com.aura.football.data.remote

import com.aura.football.BuildConfig

object SupabaseConfig {
    const val BASE_URL = BuildConfig.SUPABASE_URL
    const val ANON_KEY = BuildConfig.SUPABASE_ANON_KEY
    const val REST_ENDPOINT = "/rest/v1/"
}
