package au.edu.jcu.codequest

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application class; its only job is to host the Hilt dependency graph. */
@HiltAndroidApp
class CodeQuestApp : Application()
