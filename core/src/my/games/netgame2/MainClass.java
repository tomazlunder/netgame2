package my.games.netgame2;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import my.games.netgame2.screens.*;

import java.io.IOException;

public class MainClass extends Game {
	//Game screens
	public final static int MAINMENU = 0;
	public final static int GAME = 1;
	public final static int SETTINGS = 2;
	public final static int GAME_MATCHMAKING = 3;
	public final static int ENTER_NAME = 4;

	Screen screenMainMenu;
	Screen screenGame;
	Screen screenSettings;
	Screen screenMatchmaking;
	Screen screenName;

	public BitmapFont font;
	public BitmapFont fontBig;
	String FONT_PATH = "fonts/OpenSans-Bold.ttf";

	public String myUsername = "";

	//Batch
	public SpriteBatch batch;

	@Override
	public void create () {
		batch = new SpriteBatch();
		generateFonts();

		changeScreen(MAINMENU);
	}

	@Override
	public void render () {
		super.render();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
	}

	public void changeScreen(int screen){
		switch(screen){
			case MAINMENU:
				if(screenMainMenu == null) screenMainMenu = new MainMenuScreen(this);
				this.setScreen(screenMainMenu);
				break;
			case GAME:
				screenGame = new GameScreen(this);
				this.setScreen(screenGame);
				break;
			case SETTINGS:
				if(screenSettings == null) screenSettings = new SettingsScreen(this);
				this.setScreen(screenSettings);
				break;
			case GAME_MATCHMAKING:
				try {
					screenMatchmaking = new ClientGameScreen(this);
					this.setScreen(screenMatchmaking);
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case ENTER_NAME:
				screenName = new EnterNameScreen(this);
				this.setScreen(screenName);
				break;
		}
	}


	public void generateFonts(){
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(FONT_PATH));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = Gdx.graphics.getHeight()/15;
		parameter.color = Color.BLACK;
		font = generator.generateFont(parameter); // font size 12 pixels
		generator.dispose(); // don't forget to dispose to avoid memory leaks!

		generator = new FreeTypeFontGenerator(Gdx.files.internal(FONT_PATH));
		parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = Gdx.graphics.getHeight()/12;
		parameter.color = Color.BLACK;
		fontBig = generator.generateFont(parameter); // font size 12 pixels
		generator.dispose(); // don't forget to dispose to avoid memory leaks!
	}


}
