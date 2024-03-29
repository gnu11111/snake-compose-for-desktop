
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlin.random.Random

class SnakeGame {

    companion object {
        const val AREA_SIZE = 20
        const val MINIMUM_TAIL_LENGTH = 5
    }

    enum class Key(val code: Long) {
        None(0L), Esc(116500987904L), Up(163745628160L), Right(168040595456L), Down(172335562752L), Left(159450660864L);
        companion object {
            fun of(code: Long) = entries.firstOrNull { it.code == code } ?: None
        }
    }

    val gameObjects = mutableStateListOf<GameObject>()
    val snake = SnakeData(AREA_SIZE / 2, AREA_SIZE / 2)
    var score = 0
    var highScore = 0

    private val apple = AppleData(AREA_SIZE * 2 / 3, AREA_SIZE * 2 / 3)
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
            else -> let { lastKey = Key.of(event.key.keyCode); false }
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
        if ((apple.x == snake.x) && (apple.y == snake.y)) {
            snake.tailLength++
            apple.x = Random.nextInt(AREA_SIZE)
            apple.y = Random.nextInt(AREA_SIZE)
        }
    }

    private fun calculateScore() {
        if (snake.direction != Direction.None) {
            score = snake.tailLength - MINIMUM_TAIL_LENGTH
            highScore = score.coerceAtLeast(highScore)
        }
    }

    private fun updateGameObjects() {
        gameObjects.clear()
        gameObjects += apple
        gameObjects.addAll(snake.tiles)
    }
}

class SnakeData(var x: Int, var y: Int) {

    enum class Direction(val dx: Int, val dy: Int) { None(0, 0), Up(0, -1), Right(1, 0), Down(0, 1), Left(-1, 0) }

    val tiles = mutableListOf(SnakeTileData(x, y))
    var direction = Direction.None
    var tailLength = SnakeGame.MINIMUM_TAIL_LENGTH

    fun update() {
        moveHead()
        handleCollision()
        updateTiles()
    }

    private fun moveHead() {
        x += direction.dx
        y += direction.dy
        when {
            (x < 0) -> x = SnakeGame.AREA_SIZE - 1
            (x >= SnakeGame.AREA_SIZE) -> x = 0
            (y < 0) -> y = SnakeGame.AREA_SIZE - 1
            (y >= SnakeGame.AREA_SIZE) -> y = 0
        }
    }

    private fun handleCollision() =
        tiles.firstOrNull { (it.x == x) && (it.y == y) }?.let {
            direction = Direction.None
            tailLength = SnakeGame.MINIMUM_TAIL_LENGTH
        }

    private fun updateTiles() {
        tiles += SnakeTileData(x, y)
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
fun SnakeTile(snakeTileData: SnakeTileData, tileSize: Pair<Dp, Dp> = Pair(20.dp, 20.dp), color: Color = Color.Green) =
    Box(Modifier
        .offset(tileSize.first * snakeTileData.x, tileSize.second * snakeTileData.y)
        .size(width = tileSize.first - 2.dp, height = tileSize.second - 2.dp)
        .background(color)
    )

@Composable
fun Apple(appleData: AppleData, tileSize: Pair<Dp, Dp> = Pair(20.dp, 20.dp)) =
    Box(Modifier
        .offset(tileSize.first * appleData.x, tileSize.second * appleData.y)
        .size(width = tileSize.first - 2.dp, height = tileSize.second - 2.dp)
        .background(Color.Red)
    )

fun main() = application {

    val game = remember { SnakeGame() }
    val density = LocalDensity.current
    val refreshTimeNanos = 83333333L
    var lastUpdate by mutableStateOf(0L)
    var tileSize by mutableStateOf(Pair(20.dp, 20.dp))

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
                    with (density) {
                        tileSize = Pair(it.width.toDp() / SnakeGame.AREA_SIZE, it.height.toDp() / SnakeGame.AREA_SIZE)
                    }
                }) {
                    game.gameObjects.forEach {
                        when (it) {
                            is AppleData -> Apple(it, tileSize)
                            is SnakeTileData -> {
                                val color = when {
                                    (game.snake.tailLength < game.snake.tiles.size) -> Color.Gray
                                    (it === game.gameObjects.last()) -> Color.Yellow
                                    else -> Color.Green
                                }
                                SnakeTile(it, tileSize, color)
                            }
                        }
                    }
                }
            }
        }
    }
}
