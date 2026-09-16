package com.coinreward.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseApi {
    // IMPORTANT: Replace these with your actual Supabase project URL and anon key.
    private const val SUPABASE_URL = "https://YOUR_PROJECT_ID.supabase.co"
    private const val SUPABASE_KEY = "YOUR_SUPABASE_ANON_KEY"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}
