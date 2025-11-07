package mobappdev.example.nback_cimpl.ui.viewmodels

import android.media.SoundPool
import android.media.tv.PesResponse
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import mobappdev.example.nback_cimpl.R

/**
 * This is the GameViewModel.
 *
 * It is good practice to first make an interface, which acts as the blueprint
 * for your implementation. With this interface we can create fake versions
 * of the viewmodel, which we can use to test other parts of our app that depend on the VM.
 *
 * Our viewmodel itself has functions to start a game, to specify a gametype,
 * and to check if we are having a match
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */


interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: Int

    fun setGameType(gameType: GameType)
    fun startGame()

    fun checkMatch()
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository
): GameViewModel, ViewModel() {
    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState>
        get() = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int>
        get() = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int>
        get() = _highscore

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()

    // nBack is currently hardcoded
    override val nBack: Int = 2

    private var job: Job? = null  // coroutine job for the game event
    private val eventInterval: Long = 2000L  // 2000 ms (2s)

    private val nBackHelper = NBackHelper()  // Helper that generate the event array
    private var events = emptyArray<Int>()  // Array with all events

    override fun setGameType(gameType: GameType) {
        // update the gametype in the gamestate
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        job?.cancel()  // Cancel any existing game loop
        _score.value=0
        // Get the events from our C-model (returns IntArray, so we need to convert to Array<Int>)
        events = nBackHelper.generateNBackString(10, 9, 30, nBack).toList().toTypedArray()  // Todo Higher Grade: currently the size etc. are hardcoded, make these based on user input
        Log.d("GameVM", "The following sequence was generated: ${events.contentToString()}")

        _gameState.value = gameState.value.copy(userResponses = mutableListOf())

        job = viewModelScope.launch {
            when (_gameState.value.gameType) {
                GameType.Audio -> runAudioGame()
                GameType.AudioVisual -> runAudioVisualGame()
                GameType.Visual -> runVisualGame(events)
            }
            Log.d("GameVM", "Game finished. Final score: ${_score.value}")
            if (_score.value > _highscore.value) userPreferencesRepository.saveHighScore(_score.value)
        }
    }

    override fun checkMatch() {
        val currentIndex = _gameState.value.eventIndex

        // Prevent registering same match twice
        if (currentIndex in _gameState.value.userResponses) return

        //Add responses
        val newInputs = _gameState.value.userResponses.toMutableList()
        newInputs.add(currentIndex)
        _gameState.value = _gameState.value.copy(userResponses = newInputs)

        // Check if it's actually a match (compare current with n-back)
        if (currentIndex >= nBack) {
            val currentValue = events[currentIndex]
            val nBackValue = events[currentIndex - nBack]

            if (currentValue == nBackValue) {
                _score.value += 1 // Correct match
                Log.d("GameVM", "Correct match at index $currentIndex")
            } else {
                _score.value = maxOf(0, _score.value - 1) // Decrease by one if score > 0
                Log.d("GameVM", "Incorrect match at index $currentIndex")
            }
        }
    }

    private suspend fun runAudioGame() {
        for ((index, value) in events.withIndex()) {
            _gameState.value = _gameState.value.copy(
                eventValue = value,
                eventIndex = index
            )
            playSound(value)
            delay(eventInterval)
        }
        _gameState.value = _gameState.value.copy(eventValue = -1, eventIndex = -1)
    }

    private suspend fun runVisualGame(events: Array<Int>) {
        for ((index, value) in events.withIndex()) {
            _gameState.value = _gameState.value.copy(
                eventValue = value,
                eventIndex = index,
                useAlternateColor = index % 2 == 1
            )
            delay(eventInterval)
        }
        _gameState.value = _gameState.value.copy(eventValue = -1, eventIndex = -1)
    }





    private fun runAudioVisualGame(){
        // Todo: Make work for Higher grade
    }

    fun initializeSounds(context: android.content.Context) {
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .build()

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            Log.d("GameVM", "Sound $sampleId loaded with status $status")
        }
        // Load sounds into memory from A to I
        soundMap[0] = soundPool?.load(context, R.raw.sound_0, 1) ?: 0
        soundMap[1] = soundPool?.load(context, R.raw.sound_1, 1) ?: 0
        soundMap[2] = soundPool?.load(context, R.raw.sound_2, 1) ?: 0
        soundMap[3] = soundPool?.load(context, R.raw.sound_3, 1) ?: 0
        soundMap[4] = soundPool?.load(context, R.raw.sound_4, 1) ?: 0
        soundMap[5] = soundPool?.load(context, R.raw.sound_5, 1) ?: 0
        soundMap[6] = soundPool?.load(context, R.raw.sound_6, 1) ?: 0
        soundMap[7] = soundPool?.load(context, R.raw.sound_7, 1) ?: 0
        soundMap[8] = soundPool?.load(context, R.raw.sound_8, 1) ?: 0
    }

    private fun playSound(position: Int) {
        soundMap[position]?.let { soundId ->
            if (soundId != 0) {
                soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundPool?.release()
        soundPool = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRespository)
            }
        }
    }

    init {
        // Code that runs during creation of the vm
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }
}

// Class with the different game types
enum class GameType{
    Audio,
    Visual,
    AudioVisual
}

data class GameState(
    // You can use this state to push values from the VM to your UI.
    val gameType: GameType = GameType.Visual,  // Type of the game
    val eventValue: Int = -1,  // The value of the array string
    val eventIndex: Int = -1, // Current pos in the sequence
    val useAlternateColor: Boolean = false,
    val userResponses: MutableList<Int> = mutableListOf() // List of where users click were correct
)

class FakeVM: GameViewModel{
    override val gameState: StateFlow<GameState>
        get() = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int>
        get() = MutableStateFlow(42).asStateFlow()
    override val nBack: Int
        get() = 2

    override fun setGameType(gameType: GameType) {
    }

    override fun startGame() {
    }

    override fun checkMatch() {
    }
}