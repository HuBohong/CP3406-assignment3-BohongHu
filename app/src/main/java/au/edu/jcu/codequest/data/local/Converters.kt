package au.edu.jcu.codequest.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Stores string lists (answer options, code lines) as a JSON array in a single column. */
class Converters {
    private val listSerializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(listSerializer, value)

    @TypeConverter
    fun toStringList(value: String): List<String> = Json.decodeFromString(listSerializer, value)
}
