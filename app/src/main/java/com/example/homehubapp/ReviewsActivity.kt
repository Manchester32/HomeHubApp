package com.example.homehubapp

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// Stores and reads service reviews in Firestore (NoSQL database)
class ReviewsActivity : AppCompatActivity() {

    private lateinit var reviewsContainer: LinearLayout
    private lateinit var tvNoReviews: TextView
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reviews)

        reviewsContainer = findViewById(R.id.reviewsContainer)
        tvNoReviews = findViewById(R.id.tvNoReviews)
        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val etComment = findViewById<EditText>(R.id.etReviewComment)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitReview)

        findViewById<TextView>(R.id.tvBackReviews).setOnClickListener { finish() }

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val comment = etComment.text.toString().trim()
            if (rating == 0 || comment.isBlank()) {
                Toast.makeText(this, getString(R.string.msg_review_invalid), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false

            // Make sure we have a Firebase session first, then WRITE the review
            FirebaseHelper.ensureSignedIn(
                onReady = {
                    // WRITE: add one document to the "reviews" collection
                    val review = hashMapOf(
                        "userId" to SessionManager.userId(this),
                        "userName" to SessionManager.name(this),
                        "rating" to rating,
                        "comment" to comment,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("reviews").add(review)
                        .addOnSuccessListener {
                            btnSubmit.isEnabled = true
                            Log.i("HomeHub", "Review saved to Firestore")
                            Toast.makeText(this, getString(R.string.msg_review_saved), Toast.LENGTH_SHORT).show()
                            etComment.text.clear()
                            ratingBar.rating = 0f
                            loadReviews()
                        }
                        .addOnFailureListener {
                            btnSubmit.isEnabled = true
                            Log.e("HomeHub", "Saving review failed", it)
                            // The real error text is shown so we can see what is wrong
                            showError(it)
                        }
                },
                onError = {
                    btnSubmit.isEnabled = true
                    showError(it)
                }
            )
        }

        FirebaseHelper.ensureSignedIn(onReady = { loadReviews() }, onError = { showError(it) })
    }

    // READ: get the 20 newest reviews from Firestore
    private fun loadReviews() {
        db.collection("reviews")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { snapshot ->
                reviewsContainer.removeAllViews()
                tvNoReviews.text = getString(R.string.no_reviews)
                tvNoReviews.visibility = if (snapshot.isEmpty) View.VISIBLE else View.GONE
                for (doc in snapshot.documents) {
                    val rating = (doc.getLong("rating") ?: 0L).toInt().coerceIn(0, 5)
                    addReviewCard(
                        doc.getString("userName") ?: "User",
                        rating,
                        doc.getString("comment") ?: ""
                    )
                }
                Log.d("HomeHub", "Loaded ${snapshot.size()} reviews")
            }
            .addOnFailureListener {
                Log.e("HomeHub", "Reading reviews failed", it)
                tvNoReviews.visibility = View.VISIBLE
                tvNoReviews.text = getString(R.string.msg_review_failed)
                showError(it)
            }
    }

    private fun showError(e: Exception) {
        Toast.makeText(this, getString(R.string.msg_review_failed) + "\n" + (e.message ?: ""), Toast.LENGTH_LONG).show()
    }

    private fun addReviewCard(name: String, rating: Int, comment: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(12)) }
        }
        card.addView(label(name, 17f, "#0B3273", true))
        card.addView(label("★".repeat(rating) + "☆".repeat(5 - rating), 18f, "#F7941D", false))
        card.addView(label(comment, 14f, "#333333", false))
        reviewsContainer.addView(card)
    }

    private fun label(text: String, size: Float, color: String, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(5), 0, 0)
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}