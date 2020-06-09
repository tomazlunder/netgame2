package my.games.netgame2.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import my.games.netgame2.MainClass;
import my.games.netgame2.game.Constants;
import my.games.netgame2.game.GameClass;
import my.games.netgame2.game.Player;
import my.games.netgame2.ui.Button;

public class GameScreen implements Screen {

    protected MainClass parent;

    GameClass gameClass;

    Sprite spriteBackgound;
    Texture texturePlayer, textureBall;

    Button buttonMainMenu;


    public GameScreen(MainClass mainClass) {
        parent = mainClass;

        gameClass = new GameClass(new Player(0, false), new Player(1, true));

        buttonMainMenu = new Button(0.40f, 0.4f, 0.2f, 0.10f, "images/plays_btn.png", "images/plays_btn_sel.png");
    }

    @Override
    public void show() {
        spriteBackgound = new Sprite(new Texture("images/white.png"));
        texturePlayer = new Texture("images/black.png");
        textureBall = new Texture("images/ball.png");
    }

    @Override
    public void render(float deltaTime) {
        handleInput(deltaTime);
        gameClass.update(deltaTime);
        draw(deltaTime);

        if(gameClass.state == gameClass.ENDED){
            updateButton();
        }
    }

    @Override
    public void resize(int width, int height) {
        parent.batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub
    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub
    }

    @Override
    public void hide() {
        // TODO Auto-generated method stub
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
    }

    public void draw(float deltaTime){
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        //DRAWING VARIABLES
        Vector2 p1position = gameClass.player1.position.cpy();
        Vector2 p2position = gameClass.player2.position.cpy();
        p1position.x = (p1position.x / Constants.GAME_WIDTH) * width;
        p2position.x = (p2position.x / Constants.GAME_WIDTH) * width;
        p1position.y = (p1position.y / Constants.GAME_HEIGHT) * height;
        p2position.y = (p2position.y / Constants.GAME_HEIGHT) * height;

        float player1height = Constants.PLAYER_MAX_HEIGHT - (Constants.PLAYER_MAX_LIVES - gameClass.player1.lives) * Constants.PLAYER_HEIGHT_DECREMENT;
        float player2height = Constants.PLAYER_MAX_HEIGHT - (Constants.PLAYER_MAX_LIVES - gameClass.player2.lives) * Constants.PLAYER_HEIGHT_DECREMENT;
        player1height = (player1height / Constants.GAME_HEIGHT) * height;
        player2height = (player2height / Constants.GAME_HEIGHT) * height;

        float playerWidth = (Constants.PLAYER_WIDTH / Constants.GAME_WIDTH) * width;

        Vector2 ballPosition = gameClass.ball.position.cpy();
        ballPosition.x = (ballPosition.x / Constants.GAME_WIDTH) * width;
        ballPosition.y = (ballPosition.y / Constants.GAME_HEIGHT) * height;

        float ballRadius = (Constants.BALL_RADIUS / Constants.GAME_HEIGHT) * height;

        //Actual drawing

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        parent.batch.begin();
        parent.batch.draw(spriteBackgound, width * Constants.BORDER_RATIO, height * Constants.BORDER_RATIO, width * (1 - 2 * Constants.BORDER_RATIO), height * (1 - 2 * Constants.BORDER_RATIO));

        parent.batch.draw(texturePlayer, p1position.x - (playerWidth / 2.0f), p1position.y - (player1height / 2.0f), playerWidth, player1height);
        parent.batch.draw(texturePlayer, p2position.x - (playerWidth / 2.0f), p2position.y - (player2height / 2.0f), playerWidth, player2height);
        parent.batch.draw(textureBall, ballPosition.x - ballRadius, ballPosition.y - ballRadius, ballRadius * 2, ballRadius * 2);

        //Drawing text
        parent.font.draw(parent.batch, Integer.toString(gameClass.player1.lives), (2*Constants.BORDER_RATIO)*width, height - (2*Constants.BORDER_RATIO)*height);
        parent.font.draw(parent.batch, Integer.toString(gameClass.player2.lives), (1-(3*Constants.BORDER_RATIO))*width, height - (2*Constants.BORDER_RATIO)*height);
        if(gameClass.state == gameClass.PAUSED){
            parent.fontBig.draw(parent.batch, Integer.toString( (int) gameClass.pauseTime + 1),width * 0.49f, height * 0.6f);
        }

        if(gameClass.state == gameClass.ENDED){
            buttonMainMenu.draw(parent.batch);
        }

        parent.batch.end();
    }

    public void handleInput(float deltaTime) {
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            gameClass.movePlayer(gameClass.player1, 0, deltaTime);
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            gameClass.movePlayer(gameClass.player1, 1, deltaTime);
        }
    }

    public void updateButton(){
        buttonMainMenu.updateMouse();
        if(Gdx.input.justTouched() && buttonMainMenu.isActive){
            parent.changeScreen(parent.MAINMENU);
        }
    }
}