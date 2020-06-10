package my.games.netgame2.game;

import com.badlogic.gdx.math.Vector2;

public class GameClass {
    public Player player1;
    public Player player2;
    public Ball ball;

    public float pauseTime;

    public final int RUNNING = 0;
    public final int PAUSED = 1;
    public final int ENDED = 2;

    public int state;
    public int justScored;

    public int gameID;

    public GameClass(Player p1, Player p2){
        player1 = p1;
        player2 = p2;
        ball = new Ball();
        state = PAUSED;
        pauseTime = Constants.START_PAUSE_TIME;
        justScored = -1;
    }

    public void update(float deltaTime){
        this.justScored = -1;

        if(state == ENDED){
            return;
        }

        if(state == PAUSED){
            pauseTime -= deltaTime;

            if(pauseTime > 0){
                return;
            } else {
                ball = new Ball();
                state = RUNNING;
            }
        }

        if(player1.ai) aiMove(player1,deltaTime);
        if(player2.ai) aiMove(player2,deltaTime);


        float speed = (Constants.BALL_SPEED + ball.bounces * Constants.BALL_SPEED_INCREMENT) * deltaTime;

        Vector2 oldPosition = ball.position;
        ball.position = new Vector2(oldPosition.add(ball.direction.setLength(speed)));

        boolean hasBounced = false;

        //If hit top wall
        if((ball.position.y + Constants.BALL_RADIUS) > Constants.GAME_HEIGHT*(1-Constants.BORDER_RATIO)){
            ball.direction = new Vector2(ball.direction.x, -1f * ball.direction.y);

            ball.position.y = (Constants.GAME_HEIGHT*(1-Constants.BORDER_RATIO)-Constants.BALL_RADIUS);
            hasBounced = true;
        }

        //If hit bottom wall
        if((ball.position.y - Constants.BALL_RADIUS) < Constants.GAME_HEIGHT *Constants.BORDER_RATIO){
            ball.direction = new Vector2(ball.direction.x, -1f * ball.direction.y);

            ball.position.y = (Constants.GAME_HEIGHT*Constants.BORDER_RATIO) + Constants.BALL_RADIUS;
            hasBounced = true;
        }

        float player1height = Constants.PLAYER_MAX_HEIGHT - (Constants.PLAYER_MAX_LIVES - player1.lives)*Constants.PLAYER_HEIGHT_DECREMENT;
        float player2height = Constants.PLAYER_MAX_HEIGHT - (Constants.PLAYER_MAX_LIVES - player2.lives)*Constants.PLAYER_HEIGHT_DECREMENT;

        // Left player deflects ball
        if(ball.direction.x < 0 &&
                ball.position.x + Constants.BALL_RADIUS >= player1.position.x - (Constants.PLAYER_WIDTH/2f) &&
                ball.position.x - Constants.BALL_RADIUS < player1.position.x + (Constants.PLAYER_WIDTH/2f) &&
                ball.position.y > player1.position.y - (player1height/2f) &&
                ball.position.y < player1.position.y + (player1height/2f)
        ){
            ball.direction.x = -1f * ball.direction.x;
            hasBounced = true;
        }

        // Right player deflects ball
        if(ball.direction.x > 0 &&
                ball.position.x - Constants.BALL_RADIUS <= player2.position.x + (Constants.PLAYER_WIDTH/2f) &&
                ball.position.x + Constants.BALL_RADIUS > player2.position.x - (Constants.PLAYER_WIDTH/2f) &&
                ball.position.y > player2.position.y - (player2height/2f) &&
                ball.position.y < player2.position.y + (player2height/2f)
        ){
            ball.direction.x = -1f * ball.direction.x;
            hasBounced = true;
        }

        if(hasBounced){
            ball.bounces +=1;
        }

        // Right player scores
        if(ball.direction.x < 0 &&
                ball.position.x - Constants.BALL_RADIUS < Constants.GAME_WIDTH*Constants.BORDER_RATIO){
            player1.lives -=1;
            justScored = 1;

            if(player1.lives <= 0){
                state = ENDED;
            } else {
                state = PAUSED;
                pauseTime = Constants.SCORE_PAUSE_TIME;
            }
        }

        // Left player scores
        if(ball.direction.x > 0 &&
                ball.position.x + Constants.BALL_RADIUS > Constants.GAME_WIDTH*(1-Constants.BORDER_RATIO)){
            player2.lives -=1;
            justScored = 0;

            if(player2.lives <= 0){
                state = ENDED;
            }
            else {
                state = PAUSED;
                pauseTime = Constants.SCORE_PAUSE_TIME;
            }
        }
    }

    public void aiMove(Player player, float deltaTime){
        if(player.playerNumber == 0 && ball.direction.x > 0) return;
        if(player.playerNumber == 1 && ball.direction.x < 0) return;

        float speed = deltaTime*(Constants.PLAYER_SPEED);

        float playerHeight = Constants.PLAYER_MAX_HEIGHT - (Constants.PLAYER_MAX_LIVES - player.lives)*Constants.PLAYER_HEIGHT_DECREMENT;

        if((ball.position.y > player.position.y + playerHeight/3 ||
                player.ai_moving) &&
                player.position.y + (playerHeight /2.0f) != (Constants.GAME_HEIGHT * (1-Constants.BORDER_RATIO))){ //Already as up as can be

            //Move
            player.ai_moving = true;
            player.position = new Vector2(player.position.x, player.position.y + speed);

            //Correct if overshooting the playing field
            if(player.position.y + (playerHeight /2.0f) > (Constants.GAME_HEIGHT * (1-Constants.BORDER_RATIO))){
                player.ai_moving = false;
                player.position.y = (Constants.GAME_HEIGHT*(1-Constants.BORDER_RATIO))-(playerHeight/2.0f);
            }
        }
        else if ((ball.position.y < player.position.y - playerHeight/3 ||
                player.ai_moving)&&
                (player.position.y - (playerHeight /2.0f) != Constants.GAME_HEIGHT * Constants.BORDER_RATIO)) { //Already as down as can be

            //Move
            player.ai_moving = true;
            player.position = new Vector2(player.position.x, player.position.y - speed);

            //Correct if overshooting the playing field
            if(player.position.y - (playerHeight /2.0f) < (Constants.GAME_HEIGHT * Constants.BORDER_RATIO)){
                player.position.y = (Constants.GAME_HEIGHT*Constants.BORDER_RATIO)+(playerHeight/2.0f);
                player.ai_moving = false;
            }
        }

        if(ball.position.y > player.position.y + playerHeight/10 ||
                (ball.position.y < player.position.y -playerHeight/10)){
            player.ai_moving = false;
        }
    }

    public void movePlayer(Player player, int direction, float deltaTime){
        if(state == PAUSED) return;

        float speed = deltaTime*(Constants.PLAYER_SPEED);

        float playerHeight = Constants.PLAYER_MAX_HEIGHT - (Constants.PLAYER_MAX_LIVES - player.lives)*Constants.PLAYER_HEIGHT_DECREMENT;

        //UP
        if(direction == 0){
            player.position = new Vector2(player.position.x, player.position.y + speed);

            //Correct if overshooting the playing field
            if(player.position.y + (playerHeight /2.0f) > (Constants.GAME_HEIGHT * (1-Constants.BORDER_RATIO))){
                player.position.y = (Constants.GAME_HEIGHT*(1-Constants.BORDER_RATIO))-(playerHeight/2.0f);
            }
        }
        else{
            player.position = new Vector2(player.position.x, player.position.y - speed);

            //Correct if overshooting the playing field
            if(player.position.y - (playerHeight /2.0f) < (Constants.GAME_HEIGHT * Constants.BORDER_RATIO)){
                player.position.y = (Constants.GAME_HEIGHT*Constants.BORDER_RATIO)+(playerHeight/2.0f);
            }
        }
    }
}
