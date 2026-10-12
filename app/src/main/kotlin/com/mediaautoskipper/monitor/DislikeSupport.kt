package com.mediaautoskipper.monitor

/** A custom action a player publishes in its playback state. */
data class CustomActionInfo(val id: String, val name: String)

/** The user's rating of the current track as the player reports it in the session metadata. */
enum class RatingState {
    /** The player does not publish a user rating, so a dislike could toggle an existing one off. */
    UNKNOWN,
    UNRATED,
    LIKED,
    DISLIKED,
    ;

    /** A dislike is only sent when it cannot undo an existing dislike. */
    val allowsDislike: Boolean get() = this == UNRATED || this == LIKED

    companion object {
        /** [published]: the metadata carries a user rating; [rated]: it is set; [thumbUp]: the set rating is a thumbs-up. */
        fun of(published: Boolean, rated: Boolean, thumbUp: Boolean): RatingState = when {
            !published -> UNKNOWN
            !rated -> UNRATED
            thumbUp -> LIKED
            else -> DISLIKED
        }
    }
}

object DislikeSupport {

    private const val KNOWN_ID = "thumbs_down_action"

    /** Ids that undo or clear a rating, such as `remove_dislike` or `undislike`, are never the dislike action. */
    private val UNDO = Regex("(remove|clear|undo|cancel|(^|[^a-z])un)", RegexOption.IGNORE_CASE)

    /**
     * The id of the player's dislike action: the known id `thumbs_down_action` first, then the first action
     * whose id contains `thumbs_down` or `dislike` and is not an undo action, otherwise the first whose name
     * is "Dislike" (all case-insensitive); null when none.
     */
    fun findActionId(actions: List<CustomActionInfo>): String? {
        actions.firstOrNull { it.id.equals(KNOWN_ID, ignoreCase = true) }?.let { return it.id }
        actions.firstOrNull { a ->
            (a.id.contains("thumbs_down", ignoreCase = true) || a.id.contains("dislike", ignoreCase = true)) &&
                !UNDO.containsMatchIn(a.id)
        }?.let { return it.id }
        return actions.firstOrNull { it.name.equals("Dislike", ignoreCase = true) }?.id
    }
}
