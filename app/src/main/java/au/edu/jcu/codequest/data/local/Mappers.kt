package au.edu.jcu.codequest.data.local

import au.edu.jcu.codequest.domain.model.Lesson
import au.edu.jcu.codequest.domain.model.LessonSummary
import au.edu.jcu.codequest.domain.model.Question
import au.edu.jcu.codequest.domain.model.RecentSession
import au.edu.jcu.codequest.domain.model.TopicAccuracy
import au.edu.jcu.codequest.domain.quiz.TriviaQuestions

// Conversions between database rows and the domain models the rest of the app uses.

fun Lesson.toEntity() = LessonEntity(id, unitIndex, unitTitle, orderIndex, title, description)

fun LessonEntity.toDomain() = Lesson(id, unitIndex, unitTitle, orderIndex, title, description)

fun LessonWithProgress.toDomain() = LessonSummary(lesson.toDomain(), bestStars, timesCompleted)

fun Question.toEntity(orderIndex: Int) = QuestionEntity(
    id = id,
    lessonId = lessonId,
    orderIndex = orderIndex,
    topic = topic,
    type = type,
    prompt = prompt,
    code = code,
    options = options,
    correctIndex = correctIndex,
    explanation = explanation,
)

fun QuestionEntity.toDomain() = Question(
    id = id,
    lessonId = lessonId,
    topic = topic,
    type = type,
    prompt = prompt,
    code = code,
    options = options,
    correctIndex = correctIndex,
    explanation = explanation,
)

fun TriviaEntity.toDomain(): Question = TriviaQuestions.build(
    id = "${TriviaQuestions.LESSON_ID}-$id",
    question = question,
    correctAnswer = correctAnswer,
    incorrectAnswers = incorrectAnswers,
)

fun TopicAccuracyRow.toDomain() = TopicAccuracy(topic, correctCount, totalCount)

fun SessionEntity.toDomain() = RecentSession(
    id = id,
    lessonTitle = lessonTitle,
    stars = stars,
    xp = xp,
    firstTryCorrect = firstTryCorrect,
    total = total,
    finishedAt = finishedAt,
)
