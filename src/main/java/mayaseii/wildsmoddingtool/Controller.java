package mayaseii.wildsmoddingtool;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;

import java.awt.*;
import java.util.ResourceBundle;

public class Controller
{
    public void volumeControl(MouseEvent e) throws URISyntaxException
    {
        // Gets the volume control image view.
        ImageView image = (ImageView)e.getSource();

        // Checks whether the music is on.
        if (Application.mediaPlayer.getVolume() == .2)
        {
            // Disables the music.
            Application.mediaPlayer.setVolume(0);
            image.setImage(new Image(Objects.requireNonNull(this.getClass().getResource("img/noVolume.png")).toURI().toString()));
        }
        else
        {
            // Enables the music.
            Application.mediaPlayer.setVolume(.2);
            image.setImage(new Image(Objects.requireNonNull(this.getClass().getResource("img/volume.png")).toURI().toString()));
        }
    }

    public void openSeiiLink(ActionEvent e) throws URISyntaxException, IOException
    {
        // Opens Seiiccubus' Twitter link on the client's browser.
        Desktop.getDesktop().browse(new URI("https://twitter.com/mayaseii"));
    }

    public void playerSkinScene(ActionEvent e) throws IOException
    {
        switchScene((Stage)((Node)e.getSource()).getScene().getWindow(), "player-skin.fxml", "player-skin.css");
    }

    private void switchScene(Stage stage, String sceneName, String cssFile) throws IOException
    {
        // Resolves the locale.
        ResourceBundle bundle = ResourceBundle.getBundle("mayaseii.wildsmoddingtool.strings", Application.locale);

        // Loads the scene.
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(sceneName)), bundle);
        Scene scene = new Scene(root);

        // Loads the CSS file to apply to the view.
        String css = Objects.requireNonNull(this.getClass().getResource(cssFile)).toExternalForm();
        scene.getStylesheets().add(css);

        // Shows the stage.
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}