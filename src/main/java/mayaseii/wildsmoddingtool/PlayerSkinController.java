package mayaseii.wildsmoddingtool;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PlayerSkinController implements Initializable
{
    @FXML private SplitPane splitPane;
    @FXML private AnchorPane leftPane;
    @FXML private AnchorPane rightPane;
    @FXML private AnchorPane errorPane;
    @FXML private AnchorPane previewPane;
    @FXML private TextField nameField;
    @FXML private TextField authorField;
    @FXML private Label nameLabel;
    @FXML private Label errorLabel;
    @FXML private ImageView sleepingView;
    @FXML private ImageView frontView;
    @FXML private ImageView backView;

    private ResourceBundle _bundle;

    private static java.awt.Color _toColour = new java.awt.Color(248, 56, 8);
    private static java.awt.Color _fromColour;

    public void initialize(URL location, @NotNull ResourceBundle bundle)
    {
        resizeUsingNearestNeighbour(leftPane);
        resizeUsingNearestNeighbour(rightPane);

        lockPaneDivider();
        limitNameFieldLength();

        // Sets the default character and author names.
        nameLabel.setText(nameField.getText());
        authorField.setText(bundle.getString("PlayerSkin.Author"));

        initialiseErrorPane();
        hidePreviewPane();

        _bundle = bundle;
    }

    private void initialiseErrorPane()
    {
        errorLabel.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        errorPane.setVisible(false);
    }

    private void limitNameFieldLength()
    {
        Pattern pattern = Pattern.compile(".{0,16}");
        TextFormatter<String> formatter = new TextFormatter<>(change -> pattern.matcher(change.getControlNewText()).matches() ? change : null);
        nameField.setTextFormatter(formatter);
    }

    private void lockPaneDivider()
    {
        SplitPane.Divider divider = splitPane.getDividers().get(0);
        double position = divider.getPosition();
        divider.positionProperty().addListener((observable, oldvalue, newvalue) -> divider.setPosition(position));
    }

    private void resizeUsingNearestNeighbour(@NotNull AnchorPane pane)
    {
        for (Node child : pane.getChildren())
        {
            if (child instanceof ImageView view)
            {
                Image newImage = new Image(view.getImage().getUrl(), view.getFitWidth(), view.getFitHeight(), true, false);
                view.setImage(newImage);
            }
        }
    }

    public void backToMenu(@NotNull ActionEvent e) throws IOException
    {
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();

        // Resolves the locale.
        ResourceBundle bundle = ResourceBundle.getBundle("mayaseii.wildsmoddingtool.strings", Application.locale);

        // Loads the scene.
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("main-menu.fxml")), bundle);
        Scene scene = new Scene(root);

        // Loads the CSS file to apply to the view.
        String css = Objects.requireNonNull(this.getClass().getResource("app.css")).toExternalForm();
        scene.getStylesheets().add(css);

        // Shows the stage.
        stage.setScene(scene);
        stage.show();
    }

    public void characterNameChanged(@NotNull InputEvent e)
    {
        // Updates the name label to reflect the new text.
        TextField textField = (TextField) e.getSource();
        nameLabel.setText(textField.getText());
    }

    public void uploadImage(@NotNull MouseEvent e)
    {
        ImageView imageView = (ImageView) e.getSource();

        File file = getFile();
        if (file == null) return; // Tests if image file is null.

        // Stores the chosen image.
        Image image = new Image(file.toURI().toString());
        boolean isSheet = isSpriteSheet(imageView, image);
        @NonNls String spriteType = imageView.getId().split("-")[0]; // E.g., walking, running.

        if (!isSheet && !imageFitsView(imageView, image)) displayDimensionsErrorPopup(imageView, spriteType);
        else showUploadedImage(imageView, file, image, isSheet, spriteType);
    }

    private void showUploadedImage(ImageView imageView, File file, Image image, boolean isSheet, String spriteType)
    {
        if (!isSheet) displayIndividualSprite(imageView, file);
        else displaySpriteSheet(file, image, spriteType);
    }

    private void displayDimensionsErrorPopup(ImageView imageView, String spriteType)
    {
        Vector2 dimensions = getSpriteSheetDimensions(spriteType);
        String popupText = getDimensionsErrorText(imageView, (int) dimensions.x, (int) dimensions.y);

        displayInfoPopup(popupText);
    }

    private void displaySpriteSheet(@NotNull File file, Image image, @NonNls String spriteType)
    {
        // Resizes the image.
        image = new Image(file.toURI().toString(), image.getWidth() * 2, image.getHeight() * 2, true, false);

        // Prepares the counter.
        int x = 0;

        // Loops through all image view nodes.
        for (Node child : rightPane.getChildren())
        {
            // Checks if the image view has a relevant ID.
            if (child instanceof ImageView view && view.getId() != null && view.getId().contains(spriteType))
            {
                // Crops the image accordingly.
                WritableImage newImage = cropImage(image, x, view);

                // Sets the image for each view.
                view.setImage(newImage);
                view.setOpacity(1);

                // Displays the image on the corresponding file-name image view.
                ImageView fileView = (ImageView) view.getScene().lookup('#' + view.getId() + "-1");
                fileView.setImage(newImage);
                fileView.setOpacity(1);

                // Increases the X coordinate for the next sprite.
                x += view.getFitWidth();
            }
        }
    }

    private @NotNull WritableImage cropImage(@NotNull Image image, int x, @NotNull ImageView view)
    {
        PixelReader reader = image.getPixelReader();
        return new WritableImage(reader, x, (int) (image.getHeight() - view.getFitHeight()), (int) view.getFitWidth(), (int) view.getFitHeight());
    }

    private void displayIndividualSprite(@NotNull ImageView imageView, @NotNull File file)
    {
        // Resizes the image.
        Image image = new Image(file.toURI().toString(), imageView.getFitWidth(), imageView.getFitHeight(), true, false);

        // Displays the image on the image view.
        imageView.setImage(image);
        imageView.setOpacity(1);

        // Displays the image on the corresponding file-name image view.
        ImageView fileView = (ImageView) imageView.getScene().lookup('#' + imageView.getId() + "-1");
        fileView.setImage(image);
        fileView.setOpacity(1);
    }

    private boolean imageFitsView(@NotNull ImageView imageView, @NotNull Image image)
    {
        return image.getWidth() == imageView.getFitWidth() / 2 && image.getHeight() == imageView.getFitHeight() / 2;
    }

    private Vector2 getSpriteSheetDimensions(@NonNls String spriteType)
    {
        Vector2 dimensions = Vector2.Zero;

        // Gets the sprite sheet dimensions.
        for (Node child : rightPane.getChildren())
        {
            if (child instanceof ImageView view && view.getId() != null && view.getId().contains(spriteType))
            {
                dimensions.x +=  view.getFitWidth() / 2;
                if (dimensions.y == 0) dimensions.y = view.getFitHeight() / 2;
            }
        }
        return dimensions;
    }

    private @NonNls @NotNull String getDimensionsErrorText(@NotNull ImageView imageView, int width, int height)
    {
        @NonNls String popupText = _bundle.getString("PlayerSkin.IncorrectDimensions") + " ";
        popupText += (int)(imageView.getFitWidth() / 2) + "x" + (int)(imageView.getFitHeight() / 2) + "px";
        popupText += " (" + _bundle.getString("PlayerSkin.Sprite") + ") ";
        popupText += _bundle.getString("PlayerSkin.Or") + " " + width + "x" + height + "px (" + _bundle.getString("PlayerSkin.SpriteSheet") + ").";
        return popupText;
    }

    private void displayInfoPopup(@NonNls String popupText)
    {
        errorLabel.setText(popupText);
        errorPane.setVisible(true);
    }

    private boolean isSpriteSheet(@NotNull ImageView imageView, Image image)
    {
        boolean isSheet = imageView.getId().contains("walking") && image.getWidth() == 128 && image.getHeight() == 16;
        isSheet |= imageView.getId().contains("running") && image.getWidth() == 128 && image.getHeight() == 16;
        isSheet |= imageView.getId().contains("sitting") && image.getWidth() == 48 && image.getHeight() == 16;
        isSheet |= imageView.getId().contains("fishing") && image.getWidth() == 56 && image.getHeight() == 24;
        return isSheet;
    }

    private File getFile()
    {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.PNG)", "*.PNG");
        FileChooser.ExtensionFilter extFilterpng = new FileChooser.ExtensionFilter("png files (*.png)", "*.png");
        fileChooser.getExtensionFilters().addAll(extFilterPNG, extFilterpng);

        // Opens the file chooser.
        return fileChooser.showOpenDialog(null);
    }

    public void closePopup()
    {
        // Hides the error popup.
        errorPane.setVisible(false);
    }

    public void downloadTemplate() throws IOException
    {
        String selectedDirPath = getUserChosenDirectory("PlayerSkin.SaveTemplate");
        if (selectedDirPath == null) return;

        Path to = Paths.get(selectedDirPath + "/Wilds_PlayerSkinTemplate.zip");
        InputStream from = getClass().getResourceAsStream("misc/Wilds_PlayerSkinTemplate.zip");

        Files.copy(Objects.requireNonNull(from), to, StandardCopyOption.REPLACE_EXISTING);

        displayTemplateSuccessPopup(selectedDirPath);
    }

    private @NonNls void displayTemplateSuccessPopup(@NotNull String selectedDirPath)
    {
        @NonNls String popupText = _bundle.getString("PlayerSkin.TemplateDownloadedTo") + "\n";
        popupText += selectedDirPath.replace('\\', '/');

        displayInfoPopup(popupText);
    }

    private @Nullable String getUserChosenDirectory(String key)
    {
        // Creates a directory choose for the download folder.
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle(_bundle.getString(key));

        // Gets the absolute path for the directory chosen.
        File selectedDir = dirChooser.showDialog(null);
        return selectedDir == null ? null : selectedDir.getAbsolutePath();
    }

    public void downloadMod() throws IOException
    {
        BufferedImage[] wSprites = new BufferedImage[8];
        BufferedImage[] rSprites = new BufferedImage[8];
        BufferedImage[] sSprites = new BufferedImage[3];
        BufferedImage[] fSprites = new BufferedImage[3];

        loadSpritesIntoArrays(wSprites, rSprites, sSprites, fSprites);

        // Concatenates all sprite arrays.
        BufferedImage walkingSprite = concatenateImages(wSprites);
        BufferedImage runningSprite = concatenateImages(rSprites);
        BufferedImage sittingSprite = concatenateImages(sSprites);
        BufferedImage fishingSprite = concatenateImages(fSprites);

        // Gets individual sprites.
        BufferedImage sleepingSprite = getSingularSprite(sleepingView);
        BufferedImage frontSprite = getSingularSprite(frontView);
        BufferedImage backSprite = getSingularSprite(backView);

        // Creates a directory choose for the download folder.
        String selectedDirPath = getUserChosenDirectory("PlayerSkin.SaveMod");
        if (selectedDirPath == null) return;

        // Creates the zip folder.
        File zip = new File(selectedDirPath + "/Wilds_PlayerSkin_" + nameField.getText() + ".zip");
        ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zip));

        // Adds all sprites to the zip folder.
        addSpriteToZip("walking.png", walkingSprite, outputStream);
        addSpriteToZip("running.png", runningSprite, outputStream);
        addSpriteToZip("sitting.png", sittingSprite, outputStream);
        addSpriteToZip("fishing.png", fishingSprite, outputStream);
        addSpriteToZip("front.png", frontSprite, outputStream);
        addSpriteToZip("back.png", backSprite, outputStream);
        addSpriteToZip("sleepingbag.png", sleepingSprite, outputStream);

        addCreditsFileToZip(outputStream);
        outputStream.close();

        displayModDownloadSuccessPopup(selectedDirPath);
    }

    private void displayModDownloadSuccessPopup(@NotNull String selectedDirPath)
    {
        // Creates the text for the success popup.
        @NonNls String popupText = _bundle.getString("PlayerSkin.ModCreatedIn") + "\n";
        popupText += selectedDirPath.replace('\\', '/');

        // Displays the success popup.
        displayInfoPopup(popupText);
    }

    private void addCreditsFileToZip(@NotNull ZipOutputStream outputStream) throws IOException
    {
        // Gets the bytes needed for the credits file.
        byte @NonNls [] data = ("Mod created by " + authorField.getText() + ".\nDo not remove this file from the mod folder.").getBytes();

        // Creates and saves the credits file.
        ZipEntry entry = new ZipEntry("credits.txt");
        outputStream.putNextEntry(entry);
        outputStream.write(data, 0, data.length);
        outputStream.closeEntry();
    }

    private void addSpriteToZip(String name, BufferedImage walkingSprite, @NotNull ZipOutputStream outputStream) throws IOException
    {
        ZipEntry entry = new ZipEntry(name);
        outputStream.putNextEntry(entry);
        ImageIO.write(walkingSprite, "png", outputStream);
        outputStream.closeEntry();
    }

    private BufferedImage getSingularSprite(@NotNull ImageView view)
    {
        Image baseImage = view.getImage();
        Image smallImage = scale(baseImage, (int) (baseImage.getWidth() / 2), (int) (baseImage.getHeight() / 2));
        return SwingFXUtils.fromFXImage(smallImage, null);
    }

    private void loadSpritesIntoArrays(BufferedImage[] w, BufferedImage[] r, BufferedImage[] s, BufferedImage[] f)
    {
        int cWalking = 0;
        int cRunning = 0;
        int cSitting = 0;
        int cFishing = 0;

        for (Node child : rightPane.getChildren())
        {
            // Checks if the image view has a relevant ID.
            if (child instanceof ImageView view && view.getId() != null)
            {
                Image baseImage = view.getImage();
                Image smallImage = scale(baseImage, (int) (baseImage.getWidth() / 2), (int) (baseImage.getHeight() / 2));

                if (view.getId().contains("walking"))
                {
                    w[cWalking] = SwingFXUtils.fromFXImage(smallImage, null);
                    cWalking++;
                }
                else if (view.getId().contains("running"))
                {
                    r[cRunning] = SwingFXUtils.fromFXImage(smallImage, null);
                    cRunning++;
                }
                else if (view.getId().contains("sitting"))
                {
                    s[cSitting] = SwingFXUtils.fromFXImage(smallImage, null);
                    cSitting++;
                }
                else if (view.getId().contains("fishing"))
                {
                    f[cFishing] = SwingFXUtils.fromFXImage(smallImage, null);
                    cFishing++;
                }
            }
        }
    }

    private @NotNull BufferedImage concatenateImages(BufferedImage @NotNull [] imageSet)
    {
        int widthTotal = 0;
        for (BufferedImage image : imageSet) widthTotal += image.getWidth();

        int widthCurr = 0;
        BufferedImage concatImage = new BufferedImage(widthTotal, imageSet[0].getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = concatImage.createGraphics();

        for (BufferedImage image : imageSet)
        {
            g2d.drawImage(image, widthCurr, concatImage.getHeight() - image.getHeight(), null);
            widthCurr += image.getWidth();
        }

        g2d.dispose();

        return concatImage;
    }

    private Image scale(Image source, int targetWidth, int targetHeight)
    {
        ImageView imageView = new ImageView(source);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(targetWidth);
        imageView.setFitHeight(targetHeight);

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);

        return imageView.snapshot(parameters, null);
    }

    public void showPreviewPane() throws Exception
    {
        createPreviewGIFs();
        loadGIFsIntoViews();

        Map<Integer, Integer> colourMap = getSpriteColourMap();
        createPalettePanel(colourMap);

        previewPane.setVisible(true);
    }

    private void createPalettePanel(@NotNull Map<Integer, Integer> colourMap)
    {
        for (Integer colour : colourMap.keySet())
        {
            Rectangle palettePane = new Rectangle();

            palettePane.setWidth(32);
            palettePane.setHeight(32);
            palettePane.setLayoutX(new Random().nextInt(1000));
            palettePane.setLayoutY(new Random().nextInt(600));

            Color newColor = Color.valueOf(String.format("#%06X", (0xFFFFFF & colour)));
            palettePane.setFill(newColor);

            // Adds the mouse click event handler.
            EventHandler<MouseEvent> eventHandler = this::palettePanelClicked;
            palettePane.addEventHandler(MouseEvent.MOUSE_CLICKED, eventHandler);

            previewPane.getChildren().add(palettePane);

        }
    }

    private void loadGIFsIntoViews()
    {
        for (Node node : previewPane.getChildren())
        {
            if (node instanceof ImageView view && view.getId() != null)
            {
                Image image = new Image(new File(view.getId() + ".gif").toURI().toString());
                view.setImage(image);
            }
        }
    }

    private @NotNull Map<Integer, Integer> getSpriteColourMap()
    {
        Map<Integer, Integer> colourMap = new HashMap<>();

        // Adds all pixel colours to the colour map.
        for (Node node: rightPane.getChildren())
        {
            if (node instanceof ImageView view && view.getId() != null && view.getImage() != null)
            {
                int[] data = ( (DataBufferInt) SwingFXUtils.fromFXImage(view.getImage(), null).getRaster().getDataBuffer() ).getData();
                for (int datum : data) putColourInMap(colourMap, datum);
            }
        }

        return colourMap;
    }

    private void putColourInMap(@NotNull Map<Integer, Integer> colourMap, int datum)
    {
        int rgba = new java.awt.Color(datum).getRGB();

        Integer colourCount = colourMap.get(rgba);
        colourCount = colourCount == null ? 1 : colourCount + 1;
        colourMap.put(rgba, colourCount);
    }

    private void createPreviewGIFs() throws Exception
    {
        int delay = 200;
        int runDelay = (int) (delay / 1.5);

        int[] delays = new int[] { delay, delay, delay, delay };
        int[] runDelays = new int[] { runDelay, runDelay, runDelay, runDelay };

        // Walking GIFs.
        createDirectionGIF(delays, "walking", "front", "anim-walk-front.gif", true);
        createDirectionGIF(delays, "walking", "back", "anim-walk-back.gif", true);
        createDirectionGIF(delays, "walking", "left", "anim-walk-left.gif", false);
        createDirectionGIF(delays, "walking", "right", "anim-walk-right.gif", false);

        // Running GIFs.
        createDirectionGIF(runDelays, "running", "front", "anim-run-front.gif", true);
        createDirectionGIF(runDelays, "running", "back", "anim-run-back.gif", true);
        createDirectionGIF(runDelays, "running", "left", "anim-run-left.gif", false);
        createDirectionGIF(runDelays, "running", "right", "anim-run-right.gif", false);
    }

    private @NonNls void createDirectionGIF(int[] delays, String action, String direction, String file, boolean mirror) throws Exception
    {
        BufferedImage[] sprites = new BufferedImage[mirror ? 4 : 2];
        populateSpriteArray(action, direction, mirror, sprites);

        AnimatedGIFWriter writer = new AnimatedGIFWriter(true);
        OutputStream os = new FileOutputStream(file);

        writer.writeAnimatedGIF(sprites, delays, os);
    }

    private void populateSpriteArray(String action, String direction, boolean mirror, BufferedImage[] sprites)
    {
        int i = 0;

        for (Node node : rightPane.getChildren())
        {
            if (node instanceof ImageView view && view.getId() != null && view.getId().contains(action) && view.getId().contains(direction))
            {
                BufferedImage sprite = SwingFXUtils.fromFXImage(view.getImage(), null);

                sprites[i] = sprite;

                if (mirror)
                {
                    sprite = mirrorImage(sprite);
                    sprites[i + 2] = sprite;
                }

                i++;
            }
        }
    }

    private BufferedImage mirrorImage(@NotNull BufferedImage img)
    {
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-img.getWidth(), 0);

        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

        return op.filter(img, null);
    }

    public void palettePanelClicked(@NotNull MouseEvent e)
    {
        Rectangle button = (Rectangle) e.getSource();
        _fromColour = fxToAWTColor((Color) button.getFill());

        changeColour();
    }

    private void changeColour()
    {
        ArrayList<Image> originalImages = changeTemporaryPaneColour(_fromColour, _toColour);

        try
        {
            createPreviewGIFs();
            loadGIFsIntoViews();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            revertTemporaryChanges(originalImages);
        }
    }

    private @NotNull ArrayList<Image> changeTemporaryPaneColour(java.awt.Color from, java.awt.Color to)
    {
        ArrayList<Image> oldImageList = new ArrayList<>();

        for (Node node : rightPane.getChildren())
        {
            if (node instanceof ImageView view && view.getId() != null)
            {
                Image oldImage = view.getImage();
                oldImageList.add(oldImage);

                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(oldImage, null);
                BufferedImageOp lookup = new LookupOp(new ColorMapper(from, to), null);
                BufferedImage convertedImage = lookup.filter(bufferedImage, null);

                view.setImage(SwingFXUtils.toFXImage(convertedImage, null));
            }
        }

        return oldImageList;
    }

    private void revertTemporaryChanges(ArrayList<Image> originalImages)
    {
        for (Node node : rightPane.getChildren())
        {
            if (node instanceof ImageView view && view.getId() != null)
            {
                view.setImage(originalImages.get(0));
                originalImages.remove(0);
            }
        }
    }

    public void setReplacementColour(@NotNull MouseEvent e)
    {
        Rectangle colourRect = (Rectangle) e.getSource();
        Color fxColour = (Color) colourRect.getFill();

        _toColour = new java.awt.Color((float) fxColour.getRed(), (float) fxColour.getGreen(), (float) fxColour.getBlue(), (float) fxColour.getOpacity());

        if (_fromColour == null) return;
        changeColour();
    }

    public void hidePreviewPane()
    {
        previewPane.setVisible(false);
    }

    @Contract("_ -> new")
    private java.awt.@NotNull Color fxToAWTColor(@NotNull Color fxColour)
    {
        return new java.awt.Color((float) fxColour.getRed(), (float) fxColour.getGreen(), (float) fxColour.getBlue(), (float) fxColour.getOpacity());
    }
}