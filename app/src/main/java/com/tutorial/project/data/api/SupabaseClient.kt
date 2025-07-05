package com.tutorial.project.data.api

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object SupabaseClientProvider {
  @OptIn(SupabaseInternal::class)
  val client = createSupabaseClient(
    supabaseUrl = "https://nmdoanqxvmzlnjqobduy.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5tZG9hbnF4dm16bG5qcW9iZHV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDc2NDI1MDgsImV4cCI6MjA2MzIxODUwOH0.zmF6bPtuANmy1J7XmYzJpU8nmrBWBcnA9OROjhpFH-g"
  ) {
    install(Auth) {
      this@createSupabaseClient.httpConfig {
        install(ContentNegotiation) {
          json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
          })
        }
      }
    }
    install(Postgrest)
    install(Realtime)
    install(Storage)
    install(Functions)
  }
}