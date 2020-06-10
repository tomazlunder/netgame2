package my.games.netgame2.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import my.games.netgame2.MainClass;
import my.games.netgame2.game.Constants;
import my.games.netgame2.ui.Button;

import java.util.ArrayList;

public class MainMenuScreen implements Screen {
    private MainClass parent;

    Sprite spriteBackground;

    Button buttonPlaySolo;
    Button buttonPlayMatchmaking;

    ArrayList<Button> buttons;

    public MainMenuScreen(MainClass mainClass){
        parent = mainClass;
    }

    @Override
    public void show() {
        // TODO Auto-generated method stub
        spriteBackground = new Sprite(new Texture("images/white.png"));

        buttons = new ArrayList<>();
        buttonPlaySolo = new Button(0.1f, 0.7f, 0.2f, 0.10f, "images/plays_btn.png", "images/plays_btn_sel.png");
        buttonPlayMatchmaking = new Button(0.1f, 0.6f, 0.2f, 0.10f, "images/plays_btn.png", "images/plays_btn_sel.png");

        buttons.add(buttonPlaySolo);
        buttons.add(buttonPlayMatchmaking);
    }

    @Override
    public void render(float delta) {
        handleInput();

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        parent.batch.begin();
        parent.batch.draw(spriteBackground, width * Constants.BORDER_RATIO, height * Constants.BORDER_RATIO, width*(1-2*Constants.BORDER_RATIO), height*(1-2*Constants.BORDER_RATIO));
        for(Button b : buttons){
            b.draw(parent.batch);
        }
        parent.batch.end();
    }

    @Override
    public void resize(int width, int height) {

        parent.batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        for(Button b: buttons){
            b.calculatePosAndDim();
        }
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

    //INPUT
    public void handleInput(){
        for(Button b : buttons){
            b.updateMouse();
        }

        if(Gdx.input.justTouched()){
            for(int i = 0; i < buttons.size(); i++){
                if(buttons.get(i).isActive){
                    switch(i){
                        case 0: buttonPlaySoloClicked(); break;
                        case 1: buttonPlayMatchmakingClicked(); break;
                        //case 2: btnSettingsClicked(); break;
                        //case 3: btnExitClicked(); break;
                    }
                }
            }
        }
    }

    public void buttonPlaySoloClicked(){
        parent.changeScreen(parent.GAME);
    }
    public void buttonPlayMatchmakingClicked(){
        if(parent.myUsername.length() == 0){
            parent.changeScreen(parent.ENTER_NAME);
        }
        else {
            parent.changeScreen(parent.GAME_MATCHMAKING);
        }
    }

}