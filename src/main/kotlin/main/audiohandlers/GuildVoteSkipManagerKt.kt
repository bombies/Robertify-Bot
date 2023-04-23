package main.audiohandlers

class GuildVoteSkipManagerKt {
    var isVoteSkipActive        = false
    var voteSkipCount           = 0
    var voteSkipMessage: Pair<Long, Long>?  = null
    var startedBy: Long?        = null
    private val voters                  = mutableSetOf<Long>()

    fun userAlreadyVoted(userId: Long): Boolean =
        voters.any { it == userId }

    fun addVoter(userId: Long) =
        voters.add(userId)

    fun removeVoter(userId: Long) =
        voters.remove(userId)

    fun clearVoters() =
        voters.clear()
}