package com.examgrading.app.core.di

import com.examgrading.app.data.auth.AuthRepositoryImpl
import com.examgrading.app.data.exams.ExamRepositoryImpl
import com.examgrading.app.domain.repositories.AuthRepository
import com.examgrading.app.domain.repositories.ExamRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindExamRepository(impl: ExamRepositoryImpl): ExamRepository
}
