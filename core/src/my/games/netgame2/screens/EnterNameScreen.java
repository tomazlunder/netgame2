package my.games.netgame2.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import my.games.netgame2.MainClass;
import my.games.netgame2.game.Constants;
import my.games.netgame2.server.NetworkConstants;
import my.games.netgame2.ui.Button;

public class EnterNameScreen implements Screen {
    private MainClass parent;
    Sprite spriteBackground;

    Button enterButton;

    String nameBuffer = "";

    InputProcessor oldInputProcessor;

    public EnterNameScreen(MainClass mainClass){
        parent = mainClass;
    }

    @Override
    public void show() {
        // TODO Auto-generated method stub
        spriteBackground = new Sprite(new Texture("images/white.png"));

        enterButton = new Button(0.40f, 0.4f, 0.2f, 0.10f, "images/plays_btn.png", "images/plays_btn_sel.png");

        oldInputProcessor = Gdx.input.getInputProcessor();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyUp (int keycode) {
                if ((keycode >= Input.Keys.A && keycode <= Input.Keys.Z) ||
                        (keycode >= Input.Keys.NUM_0 && keycode <= Input.Keys.NUM_9)) {

                    if(nameBuffer.length() >= NetworkConstants.MAX_NAME_LENGTH) return false;

                    String cStr = Input.Keys.toString(keycode);
                    nameBuffer += cStr;
                    System.out.println(nameBuffer);
                }
                return false;
            }
        });
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
        enterButton.draw(parent.batch);
        parent.fontBig.draw(parent.batch, "Enter your name:",width * 0.25f, height * 0.7f);
        parent.fontBig.draw(parent.batch,  nameBuffer,width * 0.25f, height * 0.6f);

        parent.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        parent.batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);

        enterButton.calculatePosAndDim();
        parent.generateFonts();
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

    public void handleInput(){
        enterButton.updateMouse();
        if(Gdx.input.justTouched() && enterButton.isActive){
            if(nameBuffer.length() >= NetworkConstants.MIN_NAME_LENGTH){
                parent.myUsername = nameBuffer;
                System.out.println("Name confirmed: "+nameBuffer);
                Gdx.input.setInputProcessor(oldInputProcessor);
                parent.changeScreen(parent.GAME_MATCHMAKING);
            }
            else {
                System.out.println("Name too short");
                //TODO: Print to screen
            }
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.DEL)){
            if(nameBuffer.length() >= 1){
                nameBuffer = nameBuffer.substring(0, nameBuffer.length() - 1);
                System.out.println(nameBuffer);
            }
        }
    }

}
