
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlin.math.max
import kotlin.random.Random

class SnakeGame {

    companion object {
        const val tiles = 20
        const val minimumTailLength = 5
    }

    val gameObjects = mutableStateListOf<GameObject>()
    var score = 0
    var highScore = 0

    private val snake = SnakeData(tiles / 2, tiles / 2)
    private val apple = AppleData(tiles * 2 / 3, tiles * 2 / 3)

    fun update() {
        snake.update()
        handleEatenApple()
        calculateScore()
        updateGameObjects()
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.type != KeyDown)
            return false
        when (event.key.keyCode) {
            116500987904 -> return true                     // Esc
            163745628160 -> { snake.xv = 0; snake.yv = -1 } // Up
            168040595456 -> { snake.xv = 1; snake.yv = 0 }  // Right
            172335562752 -> { snake.xv = 0; snake.yv = 1 }  // Down
            159450660864 -> { snake.xv = -1; snake.yv = 0 } // Left
        }
        return false
    }

    private fun handleEatenApple() {
        if ((apple.x == snake.px) && (apple.y == snake.py)) {
            snake.tailLength++
            apple.x = Random.nextInt(tiles)
            apple.y = Random.nextInt(tiles)
        }
    }

    private fun calculateScore() {
        score = snake.tailLength - minimumTailLength
        highScore = max(score, highScore)
    }

    private fun updateGameObjects() {
        gameObjects.clear()
        gameObjects += apple
        gameObjects.addAll(snake.snakeTiles)
    }
}

class SnakeData(x: Int, y: Int) {

    var px = x
    var py = y
    var xv = 0
    var yv = 0
    var tailLength = SnakeGame.minimumTailLength
    var snakeTiles = mutableListOf(SnakeTileData(px, py))

    fun update() {
        moveHead()
        handleCollision()
        updateSnakeTiles()
    }

    private fun moveHead() {
        px += xv
        py += yv
        when {
            (px < 0) -> px = SnakeGame.tiles - 1
            (px >= SnakeGame.tiles) -> px = 0
            (py < 0) -> py = SnakeGame.tiles - 1
            (py >= SnakeGame.tiles) -> py = 0
        }
    }

    private fun handleCollision() {
        snakeTiles.firstOrNull { (it.x == px) && (it.y == py) }?.let { tailLength = SnakeGame.minimumTailLength }
    }

    private fun updateSnakeTiles() {
        snakeTiles += SnakeTileData(px, py)
        while (snakeTiles.size > tailLength)
            snakeTiles.removeFirst()
    }
}

sealed class GameObject(x: Int, y: Int) {
    var x by mutableStateOf(x)
    var y by mutableStateOf(y)
}
class SnakeTileData(x: Int, y: Int) : GameObject(x, y)
class AppleData(x: Int, y: Int) : GameObject(x, y)

@Composable
fun SnakeTile(snakeTileData: SnakeTileData, tileSize: Pair<Float, Float> = Pair(20f, 20f)) =
    Box(Modifier
        .offset((snakeTileData.x * tileSize.first).dp, (snakeTileData.y * tileSize.second).dp)
        .size(width = (tileSize.first - 2).dp, height = (tileSize.second - 2).dp)
        .background(Color.Green)
    )

@Composable
fun Apple(appleData: AppleData, tileSize: Pair<Float, Float> = Pair(20f, 20f)) =
    Box(Modifier
        .offset((appleData.x * tileSize.first).dp, (appleData.y * tileSize.second).dp)
        .size(width = (tileSize.first - 2).dp, height = (tileSize.second - 2).dp)
        .background(Color.Red)
    )

fun main() = application {

    val game = remember { SnakeGame() }
    val refreshTimeNanos = 83333333L
    var lastUpdate by mutableStateOf(0L)
    var tileSize by mutableStateOf(Pair(20f, 20f))

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                if ((it - lastUpdate) > refreshTimeNanos) {
                    game.update()
                    lastUpdate = it
                }
            }
        }
    }

    Window(
        title = "SnakeGame",
        state = WindowState(width = 416.dp, height = 473.dp),
        onCloseRequest = ::exitApplication,
        onKeyEvent = { if (game.handleKeyEvent(it)) { this.exitApplication(); true } else false }
    ) {
        MaterialTheme {
            Column {
                Box(modifier = Modifier.fillMaxWidth().background(Color.Blue)) {
                    Text(
                        "Score = ${game.score}, Highscore = ${game.highScore}",
                        color = Color.Cyan, modifier = Modifier.padding(8.dp)
                    )
                }
                Box(modifier = Modifier.fillMaxSize().background(Color.Black).clipToBounds().onSizeChanged {
                    tileSize = Pair(it.width.toFloat() / SnakeGame.tiles, it.height.toFloat() / SnakeGame.tiles)
                }) {
                    game.gameObjects.forEach {
                        when (it) {
                            is AppleData -> Apple(it, tileSize)
                            is SnakeTileData -> SnakeTile(it, tileSize)
                        }
                    }
                }
            }
        }
    }
}
