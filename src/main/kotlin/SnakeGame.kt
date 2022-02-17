
import SnakeData.Direction
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
        const val areaSize = 20
        const val minimumTailLength = 5
    }

    enum class Key(val code: Long) {
        None(0L), Esc(116500987904L), Up(163745628160L), Right(168040595456L), Down(172335562752L), Left(159450660864L);
        companion object {
            fun of(code: Long) = values().firstOrNull { it.code == code } ?: None
        }
    }

    val gameObjects = mutableStateListOf<GameObject>()
    var score = 0
    var highScore = 0

    private val snake = SnakeData(areaSize / 2, areaSize / 2)
    private val apple = AppleData(areaSize * 2 / 3, areaSize * 2 / 3)
    private var lastKey = Key.None

    fun update() {
        handleInput()
        snake.update()
        handleEatenApple()
        calculateScore()
        updateGameObjects()
    }

    fun registerKeyEvent(event: KeyEvent): Boolean = synchronized(this) {
        when {
            (Key.Esc.code == event.key.keyCode) -> true
            ((event.type != KeyDown) || (lastKey != Key.None)) -> false
            else -> { lastKey = Key.of(event.key.keyCode); false }
        }
    }

    private fun handleInput() = synchronized(this) {
        if (lastKey != Key.None) {
            when (lastKey) {
                Key.Up -> if (snake.direction != Direction.Down) snake.direction = Direction.Up
                Key.Right -> if (snake.direction != Direction.Left) snake.direction = Direction.Right
                Key.Down -> if (snake.direction != Direction.Up) snake.direction = Direction.Down
                Key.Left -> if (snake.direction != Direction.Right) snake.direction = Direction.Left
                else -> error("don't know how to handle $lastKey")
            }
            lastKey = Key.None
        }
    }

    private fun handleEatenApple() {
        if ((apple.x == snake.px) && (apple.y == snake.py)) {
            snake.tailLength++
            apple.x = Random.nextInt(areaSize)
            apple.y = Random.nextInt(areaSize)
        }
    }

    private fun calculateScore() {
        score = snake.tailLength - minimumTailLength
        highScore = max(score, highScore)
    }

    private fun updateGameObjects() {
        gameObjects.clear()
        gameObjects += apple
        gameObjects.addAll(snake.tiles)
    }
}

class SnakeData(x: Int, y: Int) {

    enum class Direction(val x: Int, val y: Int) { None(0, 0), Up(0, -1), Right(1, 0), Down(0, 1), Left(-1, 0) }

    val tiles = mutableListOf(SnakeTileData(x, y))
    var direction = Direction.None
    var tailLength = SnakeGame.minimumTailLength
    var px = x
    var py = y

    fun update() {
        moveHead()
        handleCollision()
        updateTiles()
    }

    private fun moveHead() {
        px += direction.x
        py += direction.y
        when {
            (px < 0) -> px = SnakeGame.areaSize - 1
            (px >= SnakeGame.areaSize) -> px = 0
            (py < 0) -> py = SnakeGame.areaSize - 1
            (py >= SnakeGame.areaSize) -> py = 0
        }
    }

    private fun handleCollision() =
        tiles.firstOrNull { (it.x == px) && (it.y == py) }?.let {
            direction = Direction.None
            tailLength = SnakeGame.minimumTailLength
        }

    private fun updateTiles() {
        tiles += SnakeTileData(px, py)
        repeat(2) {
            if (tiles.size > tailLength)
                tiles.removeFirst()
        }
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
        onKeyEvent = { if (game.registerKeyEvent(it)) { this.exitApplication(); true } else false }
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
                    tileSize = Pair(it.width.toFloat() / SnakeGame.areaSize, it.height.toFloat() / SnakeGame.areaSize)
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
