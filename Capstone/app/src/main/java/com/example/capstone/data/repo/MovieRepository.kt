package com.example.capstone.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.capstone.data.remote.ApiService
import com.example.capstone.view.MoviePagingSource
import kotlinx.coroutines.flow.Flow

class MovieRepository(private val apiService: ApiService) {

    fun searchMovies(query: String): Flow<PagingData<com.example.capstone.model.remote.Movie>> {
        return Pager(
            config = PagingConfig(pageSize = 10, enablePlaceholders = false),
            pagingSourceFactory = { MoviePagingSource(apiService, query) }
        ).flow
    }

}
