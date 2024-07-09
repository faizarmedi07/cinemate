package com.example.capstone.view

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isVisible
import com.example.capstone.R
import com.example.capstone.adapter.TicketAdapter
import com.example.capstone.data.repo.UserPreferences
import com.example.capstone.data.remote.ApiService
import com.example.capstone.model.remote.TicketRequest
import com.example.capstone.data.remote.ApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AccountFragment : Fragment() {
    private val apiService: ApiService by lazy {
        ApiClient.retrofit.create(ApiService::class.java)
    }
    private lateinit var userPreferences: UserPreferences
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var ticketRecyclerView: RecyclerView
    private lateinit var progressBar: View
    private lateinit var errorLayout: View
    private lateinit var errorMsg: View
    private lateinit var retryButton: View
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreferences = UserPreferences.getInstance(requireContext())
        val navigateToUpdateUserButton = view.findViewById<Button>(R.id.navigateToUpdateUserButton)
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        ticketRecyclerView = view.findViewById(R.id.ticketRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        errorLayout = view.findViewById(R.id.errorLayout)
        errorMsg = view.findViewById(R.id.errorMsg)
        retryButton = view.findViewById(R.id.retryButton)
        ticketRecyclerView.layoutManager = LinearLayoutManager(context)
        startFetchingTickets()
        navigateToUpdateUserButton.setOnClickListener {
            val intent = Intent(requireContext(), UpdateUserActivity::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                requireContext(), R.anim.slide_in_right, R.anim.slide_out_left
            )
            startActivity(intent, options.toBundle())
        }
        logoutButton.setOnClickListener {
            logout()
        }
        swipeRefreshLayout.setOnRefreshListener {
            fetchTickets()
        }
        retryButton.setOnClickListener {
            fetchTickets()
        }
    }
    private fun startFetchingTickets() {
        lifecycleScope.launch {
            while (isActive) {
                fetchTickets()
                delay(30000)
            }
        }
    }
    private fun fetchTickets() {
        lifecycleScope.launch {
            val userId = userPreferences.userId.firstOrNull()
            if (userId != null) {
                try {
                    showLoadingState()
                    val requestBody = mapOf("userid" to userId.toInt())
                    val response = apiService.getTickets(requestBody)

                    if (response.isSuccessful && response.body() != null) {
                        val tickets = response.body()!!
                        displayTickets(tickets)
                        showContentState()
                    } else {
                        Log.e(TAG, "Failed to fetch tickets: ${response.errorBody()?.string()}")
                        showErrorState()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching tickets", e)
                    showErrorState()
                } finally {
                    swipeRefreshLayout.isRefreshing = false
                }
            } else {
                Log.e(TAG, "User ID is null, unable to fetch tickets")
                showErrorState()
            }
        }
    }
    private fun showLoadingState() {
        ticketRecyclerView.isVisible = false
        errorLayout.isVisible = false
        progressBar.isVisible = true
    }
    private fun showContentState() {
        ticketRecyclerView.isVisible = true
        errorLayout.isVisible = false
        progressBar.isVisible = false
    }
    private fun showErrorState() {
        ticketRecyclerView.isVisible = false
        errorLayout.isVisible = true
        progressBar.isVisible = true
    }
    private fun logout() {
        lifecycleScope.launch {
            userPreferences.clearLoginStatus()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                requireContext(), R.anim.slide_in_left, R.anim.slide_out_right
            )
            startActivity(intent, options.toBundle())
            requireActivity().finish()
        }
    }
    private fun displayTickets(tickets: List<TicketRequest>) {
        ticketRecyclerView.adapter = TicketAdapter(tickets)
    }
}