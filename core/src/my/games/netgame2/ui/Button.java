package my.games.netgame2.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
//import com.mygdx.zegame.java.sound.SoundSingleton;

public class Button {

    private Texture texture;
    private Texture textureActive;

    private float x,y;
    private float width;
    private float height;

    private float xRatio, yRatio, wRatio, hRatio;

    public boolean isActive;
    private boolean wasActive;

    public Button(float xRatio, float yRatio, float wRatio, float hRatio, String path, String pathActive){
        this.xRatio = xRatio;
        this.yRatio = yRatio;
        this.wRatio = wRatio;
        this.hRatio = hRatio;

        calculatePosAndDim();

        this.wasActive = false;
        this.isActive = false;

        texture = new Texture(path);
        textureActive = new Texture(pathActive);
    }

    public void calculatePosAndDim(){
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        this.x = width * xRatio;
        this.y = height * yRatio;
        this.width = width * wRatio;
        this.height = height * hRatio;
    }

    public void draw(SpriteBatch batch){
        if(this.isActive){
            batch.draw(textureActive, x, y, width, height);
            this.wasActive = true;
        }
        else{
            batch.draw(texture, x, y, width, height);
            this.wasActive = false;
        }
    }

    public boolean updateMouse(){
        float x_mouse = Gdx.input.getX();
        float y_mouse = Gdx.graphics.getHeight() - Gdx.input.getY();

        this.isActive = false;
        if(x_mouse > x && x_mouse < x + width){
            if(y_mouse > y && y_mouse < y + height){
                this.isActive = true;
                if(!this.wasActive){
                    //long sid = SoundSingleton.getInstance().menuSelect.play();
                }
                if(Gdx.input.justTouched()){
                    return true;
                }
            }
        }
        return false;
    }

    public void dispose(){
        texture.dispose();
        textureActive.dispose();
    }

}

