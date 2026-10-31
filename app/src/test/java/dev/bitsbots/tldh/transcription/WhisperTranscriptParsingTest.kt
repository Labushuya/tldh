package dev.bitsbots.tldh.transcription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperTranscriptParsingTest {
    // Shape and formatting as written by whisper-cli --output-json-full (examples/cli/cli.cpp, v1.9.5).
    private val json = """
{
	"systeminfo": "CPU : NEON = 1 | ARM_FMA = 1 |",
	"model": {
		"type": "base",
		"multilingual": true,
		"vocab": 51865,
		"ftype": 9
	},
	"params": {
		"model": "/data/user/0/dev.bitsbots.tldh/files/whisper-models/ggml-base-q5_1.bin",
		"language": "de",
		"translate": false
	},
	"result": {
		"language": "de"
	},
	"transcription": [
		{
			"timestamps": {
				"from": "00:00:00,000",
				"to": "00:00:02,400"
			},
			"offsets": {
				"from": 0,
				"to": 2400
			},
			"text": " Ich komme morgen nicht, sorry.",
			"tokens": [
				{
					"text": "[_BEG_]",
					"timestamps": {
						"from": "00:00:00,000",
						"to": "00:00:00,000"
					},
					"offsets": {
						"from": 0,
						"to": 0
					},
					"id": 50364,
					"p": 0.912,
					"t_dtw": -1
				},
				{
					"text": " Ich",
					"offsets": {
						"from": 0,
						"to": 320
					},
					"id": 3141,
					"p": 0.991,
					"t_dtw": -1
				},
				{
					"text": " kom",
					"offsets": {
						"from": 320,
						"to": 560
					},
					"id": 5207,
					"p": 0.95,
					"t_dtw": -1
				},
				{
					"text": "me",
					"offsets": {
						"from": 560,
						"to": 700
					},
					"id": 1398,
					"p": 0.88,
					"t_dtw": -1
				},
				{
					"text": " morgen",
					"offsets": {
						"from": 700,
						"to": 1100
					},
					"id": 18338,
					"p": 0.97,
					"t_dtw": -1
				},
				{
					"text": " nicht",
					"offsets": {
						"from": 1100,
						"to": 1500
					},
					"id": 1979,
					"p": 0.58,
					"t_dtw": -1
				},
				{
					"text": ",",
					"offsets": {
						"from": 1500,
						"to": 1560
					},
					"id": 11,
					"p": 0.21,
					"t_dtw": -1
				},
				{
					"text": " sorry",
					"offsets": {
						"from": 1560,
						"to": 2100
					},
					"id": 23388,
					"p": 0.31,
					"t_dtw": -1
				},
				{
					"text": ".",
					"offsets": {
						"from": 2100,
						"to": 2200
					},
					"id": 13,
					"p": 0.99,
					"t_dtw": -1
				},
				{
					"text": "[_TT_120]",
					"id": 50484,
					"p": 0.5,
					"t_dtw": -1
				}
			]
		},
		{
			"timestamps": {
				"from": "00:00:02,400",
				"to": "00:00:04,000"
			},
			"offsets": {
				"from": 2400,
				"to": 4000
			},
			"text": " Bis Samstag!",
			"tokens": [
				{
					"text": " Bis",
					"id": 15599,
					"p": 0.93,
					"t_dtw": -1
				},
				{
					"text": " Samstag",
					"id": 31240,
					"p": 0.9,
					"t_dtw": -1
				},
				{
					"text": "!",
					"id": 0,
					"p": 0.97,
					"t_dtw": -1
				}
			]
		}
	]
}
""".trimIndent()

    @Test fun parsesSegmentsAndTokens() {
        val segments = WhisperJsonParser.parse(json)
        assertEquals(2, segments.size)
        assertEquals("Ich komme morgen nicht, sorry.", segments[0].text)
        assertEquals(0L, segments[0].startMs)
        assertEquals(2400L, segments[0].endMs)
        assertEquals(10, segments[0].tokens.size)
        assertEquals(0.58f, segments[0].tokens[5].probability, 0.0001f)
        assertNull(segments[1].tokens[0].startMs)
    }

    @Test fun assemblesWordsAndDropsSpecialTokens() {
        val words = WordAssembler.assemble(WhisperJsonParser.parse(json))
        assertEquals(listOf("Ich", "komme", "morgen", "nicht,", "sorry.", "Bis", "Samstag!"), words.map { it.text })
    }

    @Test fun wordProbabilityIsLowestLetterTokenAndIgnoresPunctuation() {
        val words = WordAssembler.assemble(WhisperJsonParser.parse(json)).associateBy { it.text }
        assertEquals(0.88f, words.getValue("komme").probability, 0.0001f)
        // The comma token (p = 0.21) must not drag "nicht," down.
        assertEquals(0.58f, words.getValue("nicht,").probability, 0.0001f)
        assertEquals(0.31f, words.getValue("sorry.").probability, 0.0001f)
    }

    @Test fun wordTimesSpanItsTokens() {
        val komme = WordAssembler.assemble(WhisperJsonParser.parse(json)).first { it.text == "komme" }
        assertEquals(320L, komme.startMs)
        assertEquals(700L, komme.endMs)
    }

    @Test fun confidenceLevels() {
        val words = WordAssembler.assemble(WhisperJsonParser.parse(json)).associateBy { it.text }
        assertEquals(ConfidenceLevel.SICHER, words.getValue("morgen").confidence)
        assertEquals(ConfidenceLevel.UNSICHER, words.getValue("nicht,").confidence)
        assertEquals(ConfidenceLevel.SEHR_UNSICHER, words.getValue("sorry.").confidence)
    }

    @Test fun emptyTranscriptionYieldsNoWords() {
        assertTrue(WordAssembler.assemble(WhisperJsonParser.parse("""{"transcription": []}""")).isEmpty())
    }

    @Test
    fun parsesLiveSegmentLinesFromStdout() {
        assertEquals("Hey, ich wollte nur kurz fragen.", WhisperStdout.segmentText("[00:00:00.000 --> 00:00:04.200]   Hey, ich wollte nur kurz fragen."))
        assertEquals(null, WhisperStdout.segmentText(""))
        assertEquals(null, WhisperStdout.segmentText("whisper_init_from_file: loading model"))
        assertEquals(null, WhisperStdout.segmentText("[00:00:04.200 --> 00:00:05.000]   "))
    }
}
