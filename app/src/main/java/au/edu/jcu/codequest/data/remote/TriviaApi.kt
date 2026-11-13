package au.edu.jcu.codequest.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open Trivia DB (https://opentdb.com), a free question API that needs no key or account.
 * Category 18 is "Science: Computers".
 */
interface TriviaApi {

    @GET("api.php")
    suspend fun getQuestions(
        @Query("amount") amount: Int,
        @Query("difficulty") difficulty: String,
        @Query("category") category: Int = CATEGORY_COMPUTERS,
        @Query("type") type: String = TYPE_MULTIPLE_CHOICE,
        @Query("encode") encode: String = ENCODING_URL,
    ): TriviaResponseDto

    companion object {
        const val BASE_URL = "https://opentdb.com/"
        const val CATEGORY_COMPUTERS = 18
        const val TYPE_MULTIPLE_CHOICE = "multiple"
        const val ENCODING_URL = "url3986"

        /** response_code 0 means success; other codes mean no results or rate limiting. */
        const val RESPONSE_OK = 0
    }
}

@Serializable
data class TriviaResponseDto(
    @SerialName("response_code") val responseCode: Int,
    val results: List<TriviaQuestionDto> = emptyList(),
)

@Serializable
data class TriviaQuestionDto(
    val question: String,
    @SerialName("correct_answer") val correctAnswer: String,
    @SerialName("incorrect_answers") val incorrectAnswers: List<String>,
)
