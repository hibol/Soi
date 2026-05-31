package com.hibol.miette.soi.data.repository

import com.hibol.miette.soi.data.db.SoiDatabase
import com.hibol.miette.soi.data.entity.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(private val db: SoiDatabase) {

    fun getProfile(): Flow<Profile?> =
        db.profileDao().getProfile()

    suspend fun createProfile(name: String): Long =
        db.profileDao().insert(Profile(name = name))

    suspend fun updateProfile(profile: Profile) =
        db.profileDao().update(profile)

    fun hasProfile(): Flow<Boolean> =
        db.profileDao().getProfile().map { profile -> profile != null }
}