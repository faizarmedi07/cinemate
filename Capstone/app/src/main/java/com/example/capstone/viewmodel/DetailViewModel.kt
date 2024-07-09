package com.example.capstone.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone.data.local.Review
import com.example.capstone.data.remote.ApiService
import com.example.capstone.model.remote.AverageRatingResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetailViewModel(private val apiService: ApiService) : ViewModel() {

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    private val _averageRating = MutableStateFlow<AverageRatingResponse?>(null)
    val averageRating: StateFlow<AverageRatingResponse?> = _averageRating

    fun fetchUserReviews(movieId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getRatings(movieId)
                if (response.isSuccessful) {
                    val reviews = response.body()?.map { ratingResponse ->
                        Review(
                            userId = ratingResponse.userid,
                            movieId = ratingResponse.movieid,
                            rating = ratingResponse.rating,
                            timestamp = ratingResponse.timestamp,
                            username = ratingResponse.username
                        )
                    }?.filter { it.rating > 0 } ?: emptyList()
                    _reviews.value = reviews
                } else {
                    // Handle error
                }
            } catch (e: Exception) {
                // Handle exception
            }
        }
    }

    fun fetchAverageRating(movieId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getAverageRating(movieId)
                if (response.isSuccessful) {
                    _averageRating.value = response.body()
                } else {
                    // Handle error
                }
            } catch (e: Exception) {
                // Handle exception
            }
        }
    }
}
