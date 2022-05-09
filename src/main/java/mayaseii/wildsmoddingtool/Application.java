package mayaseii.wildsmoddingtool;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class Application extends javafx.application.Application
{
    static MediaPlayer mediaPlayer = null;

    public static Locale locale;

    @Override
    public void start(Stage stage) throws IOException
    {
        // Resolves the locale.
        locale = new Locale("en");
        ResourceBundle bundle = ResourceBundle.getBundle("mayaseii.wildsmoddingtool.strings", locale);

        // Loads the custom Pokémon font.
        Font.loadFont(Objects.requireNonNull(getClass().getResource("PokeWilds-Regular.ttf")).toExternalForm(), 16);

        // Loads the scene from the FXML file.
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("main-menu.fxml"), bundle);
        Scene scene = new Scene(fxmlLoader.load());

        // Loads the CSS file to apply to the view.
        String css = Objects.requireNonNull(this.getClass().getResource("app.css")).toExternalForm();
        scene.getStylesheets().add(css);

        // Sets up the media player for the background music.
        this.setupMediaPlayer();

        // Prepares the app window.
        stage.getIcons().add(new Image("file:src/icon.png"));
        stage.setTitle(bundle.getString("Global.AppTitle"));
        stage.setResizable(false);

        // Shows the stage.
        stage.setScene(scene);
        stage.show();

        // Starts the media player.
        this.startMediaPlayer(scene);
    }

    public static void main(String[] args)
    {
        launch(args);
    }

    private void setupMediaPlayer()
    {
        Media media = null;

        // Loads the background music track.
        try { media = new Media(Objects.requireNonNull(getClass().getResource("audio/azaleaBG.mp3")).toURI().toString()); }
        catch (URISyntaxException e) { e.printStackTrace(); }

        assert media != null;
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(0.2);

        // Enables looping.
        mediaPlayer.setOnEndOfMedia(() -> {
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.play();
        });
    }

    private void startMediaPlayer(Scene scene)
    {
        // Plays the background track.
        mediaPlayer.setAutoPlay(true);

        // Adds the media player to the scene.
        MediaView mediaView = new MediaView(mediaPlayer);
        ((AnchorPane)scene.getRoot()).getChildren().add(mediaView);
    }
}