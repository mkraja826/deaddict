package com.deaddict.app.auth

import io.github.jan.supabase.SupabaseClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseClientProvider @Inject constructor() {
    val client: SupabaseClient? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createDeAddictSupabaseClient()
    }
}
