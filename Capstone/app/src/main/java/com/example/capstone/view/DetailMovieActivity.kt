package com.example.capstone.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capstone.R
import com.example.capstone.adapter.ReviewAdapter
import com.example.capstone.data.repo.UserPreferences
import com.example.capstone.data.local.Review
import com.example.capstone.data.remote.ApiService
import com.example.capstone.databinding.ActivityDetailMovieBinding
import com.example.capstone.model.remote.RatingRequest
import com.example.capstone.data.remote.ApiClient
import com.example.capstone.util.DatasetLoader
import com.example.capstone.viewmodel.DetailViewModel
import com.example.capstone.viewmodel.DetailViewModelFactory
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class DetailMovieActivity : AppCompatActivity(), RatingFragment.RatingDialogListener {
    private lateinit var binding: ActivityDetailMovieBinding
    private lateinit var reviewAdapter: ReviewAdapter
    private val apiService: ApiService by lazy {
        ApiClient.retrofit.create(ApiService::class.java)
    }
    private val viewModel: DetailViewModel by viewModels {
        DetailViewModelFactory(apiService)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail_movie)
        binding.fab.visibility = View.GONE
        val movieId = intent.getIntExtra("MOVIE_ID", -1)
        if (movieId != -1) {
            Log.d("DetailMovieActivity", "Received movieId: $movieId")
            val movies = DatasetLoader.loadMovies(this)
            val movie = movies.firstOrNull { it.movieId == movieId }
            movie?.let {
                lifecycleScope.launch {
                    val userId = getUserId()
                    if (userId != null) {
                        Log.d("DetailMovieActivity", "User ID: $userId")
                        binding.movieDetail = it
                        viewModel.fetchAverageRating(movieId)
                        viewModel.fetchUserReviews(movieId)
                        setupReviewsRecyclerView()
                        binding.fab.setOnClickListener {
                            showRatingDialog()
                        }
                        checkIfUserHasRated(movieId, userId.toInt())
                        val scrollView = findViewById<ScrollView>(R.id.scrollView)
                        scrollView.setOnScrollChangeListener { v: View, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
                            binding.swipeRefreshLayout.isEnabled = scrollY == 0
                        }
                        binding.swipeRefreshLayout.setOnRefreshListener {
                            refreshContent(movieId)
                        }
                        lifecycleScope.launch {
                            repeatOnLifecycle(Lifecycle.State.STARTED) {
                                viewModel.reviews.collect { reviews ->
                                    updateReviewUI(reviews)
                                }
                            }
                        }
                        lifecycleScope.launch {
                            repeatOnLifecycle(Lifecycle.State.STARTED) {
                                viewModel.averageRating.collect { averageRating ->
                                    val formattedRating = if (averageRating?.average_rating ?: 0.0f > 0) {
                                        String.format(Locale.US, "Rating: %.1f", averageRating?.average_rating ?: 0.0f)
                                    } else {
                                        "Rating: -"
                                    }
                                    binding.tvPredictedRating.text = formattedRating
                                }
                            }
                        }
                    } else {
                        Log.e("DetailMovieActivity", "User ID not found. Please log in.")
                    }
                }
            } ?: run {
                Log.e("DetailMovieActivity", "Movie detail not found")
            }
        } else {
            Log.e("DetailMovieActivity", "Movie ID is null")
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@DetailMovieActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this@DetailMovieActivity,
                    R.anim.slide_in_left, R.anim.slide_out_right
                )
                startActivity(intent, options.toBundle())
                finishAfterTransition()
            }
        })
    }
    private suspend fun getUserId(): String? {
        val userPreferences = UserPreferences.getInstance(this)
        val userId = userPreferences.userId.first()
        Log.d("DetailMovieActivity", "Fetched User ID: $userId")
        return userId
    }
    private fun setupReviewsRecyclerView() {
        reviewAdapter = ReviewAdapter()
        binding.reviewsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.reviewsRecyclerView.adapter = reviewAdapter
    }
    private fun updateReviewUI(reviews: List<Review>) {
        if (reviews.isEmpty()) {
            binding.noReviewsText.visibility = View.VISIBLE
            binding.reviewsRecyclerView.visibility = View.GONE
        } else {
            binding.noReviewsText.visibility = View.GONE
            binding.reviewsRecyclerView.visibility = View.VISIBLE
            reviewAdapter.submitList(reviews)
        }
    }
    private fun refreshContent(movieId: Int) {
        lifecycleScope.launch {
            viewModel.fetchUserReviews(movieId)
            viewModel.fetchAverageRating(movieId)
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }
    private fun showRatingDialog() {
        val dialog = RatingFragment()
        dialog.setRatingDialogListener(this)
        dialog.show(supportFragmentManager, "RatingDialogFragment")
    }
    override fun onSubmitRating(rating: Float) {
        lifecycleScope.launch {
            val userId = getUserId()
            val movieId = intent.getIntExtra("MOVIE_ID", -1)
            if (userId != null && movieId != -1) {
                submitUserReview(userId.toInt(), movieId, rating)
            } else {
                Log.e("DetailMovieActivity", "Error submitting rating. Please try again.")
            }
        }
    }
    private fun submitUserReview(userId: Int, movieId: Int, rating: Float) {
        lifecycleScope.launch {
            try {
                val ratingRequest = RatingRequest(userId, movieId, rating)
                val response = apiService.insertRating(ratingRequest)
                if (response.isSuccessful) {
                    refreshContent(movieId)
                    binding.fab.visibility = View.GONE
                } else {
                    Log.e("DetailMovieActivity", "Error submitting review")
                }
            } catch (e: Exception) {
                Log.e("DetailMovieActivity", "Error submitting review", e)
            }
        }
    }
    private fun checkIfUserHasRated(movieId: Int, userId: Int) {
        lifecycleScope.launch {
            try {
                val response = apiService.getRatings(movieId)
                if (response.isSuccessful) {
                    val userRatings = response.body()?.filter { it.userid == userId }
                    Log.d("DetailMovieActivity", "User ratings for user $userId: $userRatings")
                    if (userRatings.isNullOrEmpty()) {
                        binding.fab.visibility = View.VISIBLE
                        Log.d("DetailMovieActivity", "FAB set to visible")
                    } else {
                        binding.fab.visibility = View.GONE
                        Log.d("DetailMovieActivity", "FAB set to gone")
                    }
                } else {
                    Log.e("DetailMovieActivity", "Error checking user rating status: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("DetailMovieActivity", "Error checking user rating status", e)
            }
        }
    }
}