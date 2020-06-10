package my.games.netgame2.game;

import com.badlogic.gdx.math.Vector2;

public class Player {
    public Vector2 position;
    boolean ai;
    public int lives;
    public int playerNumber;

    boolean ai_moving;

    public Player(int playerNumber, boolean ai){
        this.playerNumber = playerNumber;
        this.ai = ai;
        this.ai_moving = false;
        this.lives = Constants.PLAYER_MAX_LIVES;

        if(playerNumber == 0){
            position = new Vector2(Constants.GAME_WIDTH * 0.1f, Constants.GAME_HEIGHT/2);
        } else {
            position = new Vector2(Constants.GAME_WIDTH * 0.9f, Constants.GAME_HEIGHT/2);
        }
    }
}
