package my.games.netgame2.game;

import com.badlogic.gdx.math.Vector2;

import java.util.Random;

public class Ball {
    public Vector2 position;
    Vector2 direction;
    int bounces;

    public Ball(){
        position = new Vector2(Constants.GAME_WIDTH/2, Constants.GAME_HEIGHT/2);

        Random rand = new Random(System.currentTimeMillis());

        float dirX, dirY, y;
        if(rand.nextFloat() > 0.5f) dirX = 1; else dirX = -1;
        if(rand.nextFloat() > 0.5f) dirY = 1; else dirY = -1;

        y = 0.25f + rand.nextFloat() * (0.75f - 0.25f);

        direction = new Vector2(dirX, y*dirY);

        bounces = 0;
    }

}
