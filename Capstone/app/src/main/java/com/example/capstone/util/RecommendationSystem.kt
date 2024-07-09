package com.example.capstone.util

import android.content.Context
import com.example.capstone.data.local.Movie
import com.example.capstone.data.remote.ApiService
import kotlinx.coroutines.runBlocking
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class RecommendationSystem(context: Context, modelPath: String, private val apiService: ApiService) {
    private val tflite: Interpreter

    init {
        tflite = Interpreter(loadModelFile(context, modelPath))
    }

    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(userId: Int, movieId: Int): Float {
        val movieInput = arrayOf(floatArrayOf(movieId.toFloat()))
        val userInput = arrayOf(floatArrayOf(userId.toFloat()))
        val output = Array(1) { FloatArray(1) }

        tflite.runForMultipleInputsOutputs(arrayOf(movieInput, userInput), mapOf(0 to output))

        var predictedRating = denormalizeRating(output[0][0])

        predictedRating = max(1.0f, min(predictedRating, 5.0f))

        val formattedRating = String.format(Locale.US, "%.1f", predictedRating).toFloat()

        return formattedRating
    }

    private fun denormalizeRating(rating: Float, minRating: Float = 0f, maxRating: Float = 5f): Float {
        return rating * (maxRating - minRating) + minRating
    }

    fun getTop5Recommendations(userId: Int, movies: List<Movie>): List<Movie> {
        val recommendations = movies.map { movie ->
            val predictedRating = predict(userId, movie.movieId)
            movie.predictedRating = predictedRating
            movie
        }.sortedByDescending { it.predictedRating }

        return recommendations.take(5)
    }

    fun getRecommendations(userId: Int, movies: List<Movie>): List<Movie> {
        val preferredGenres = getUserPreferredGenres(userId)
        return if (preferredGenres.isNotEmpty()) {
            val filteredMovies = movies.filter { movie ->
                movie.genres.split(" | ").any { genre -> preferredGenres.contains(genre) }
            }
            getTop5Recommendations(userId, filteredMovies)
        } else {
            getTop5Recommendations(userId, movies)
        }
    }

    private fun getUserPreferredGenres(userId: Int): List<String> {
        return runBlocking {
            try {
                val response = apiService.getUserPreferredGenres(userId)
                if (response.isSuccessful) {
                    response.body()?.genres ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
