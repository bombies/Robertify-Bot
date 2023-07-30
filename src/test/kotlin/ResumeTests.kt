//import com.fasterxml.jackson.core.JsonProcessingException
//import dev.minn.jda.ktx.util.SLF4J
//import main.audiohandlers.models.Requester
//import main.utils.resume.GuildResumeCache
//import main.utils.resume.ResumableTrack
//import main.utils.resume.ResumableTrack.Companion.toResumableTracks
//import main.utils.resume.ResumeData
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertNotNull
//
//class ResumeTests {
//
//    val logger by SLF4J
//
//    @Test
//    fun testResumableDataToJson() {
//        val tracks = mockAudioTracks(10)
//        val resumableTracks = tracks.map { ResumableTrack(it, Requester("userid", it.identifier)) }
//        val resumeData = ResumeData("vcId", resumableTracks)
//        val json = resumeData.toString()
//        logger.info("Json: $json")
//        assertNotNull(json, "Checking if Json is valid.")
//    }
//
//    @Test
//    fun testResumeDataJsonToObject() {
//        val tracks = mockAudioTracksWithRequester(10)
//        val resumableTracks = tracks.toResumableTracks()
//        val resumeData = ResumeData("vcId", resumableTracks)
//        val json = resumeData.toString()
//
//        try {
//            val revertedData = ResumeData.fromJSON(json)
//
//            assertNotNull(revertedData, "Checking if data is null")
//            assertNotNull(revertedData.channel_id, "Checking if voice channel ID is null")
//            assertEquals(10, revertedData.tracks.size, "Checking track list length")
//
//            revertedData.tracks.forEach { track ->
//                assertNotNull(track, "Checking if track ${track.info.identifier} is null")
//            }
//
//        } catch (e: JsonProcessingException) {
//            logger.error("Couldn't load tracks for a test!", e)
//        }
//    }
//
//    @Test
//    fun testSingularResumeCacheLoadAndSave() {
//        val resumeCache = GuildResumeCache("1")
//        val tracks = mockAudioTracksWithRequester(10)
//        val resumableTracks = tracks.toResumableTracks()
//        val resumeData = ResumeData("vcId", resumableTracks)
//
//        try {
//            resumeCache.data = resumeData
//            val loadedData = resumeCache.data
//
//            assertNotNull(loadedData, "Checking if the loaded data is null")
//            assertEquals(resumeData.toString(), loadedData.toString(), "Testing equivalence of the cache")
//        } catch (e: JsonProcessingException) {
//            logger.error("Couldn't load tracks for a test!", e)
//        }
//    }
//
//    @Test
//    fun testManyResumeCachesLoadAndSave() {
//        val resumeCaches = (0 until 10).map { GuildResumeCache(it.toString()) }
//        val tracks = mockAudioTracksWithRequester(500)
//        val resumableTracks = tracks.toResumableTracks()
//        val resumeData = ResumeData("vcId", resumableTracks)
//
//        resumeCaches.forEach { resumeCache ->
//            try {
//                resumeCache.data = resumeData
//                val loadedData = resumeCache.data
//
//                assertNotNull(loadedData, "Checking if the loaded data is null")
//                assertEquals(resumeData.toString(), loadedData.toString(), "Testing equivalence of the cache")
//            } catch (e: JsonProcessingException) {
//                logger.error("Couldn't load tracks for a test!", e)
//            }
//        }
//    }
//}