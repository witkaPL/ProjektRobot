package sample;

import javafx.application.Application;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.transform.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.stage.Stage;
import javafx.scene.paint.PhongMaterial;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.shape.MeshView;
import com.interactivemesh.jfx.importer.obj.ObjModelImporter;
import java.net.URL;


public class Main extends Application {

    private static final int WIDTH = 1400;
    private static final int HEIGHT = 800;

    final PhongMaterial redMaterial = new PhongMaterial();
    private final DoubleProperty angleY = new SimpleDoubleProperty(0);

    @Override
    public void start(Stage primaryStage) {

        Box box = new Box(5, 5, 5);
        Box ground = new Box(500, 2, 500);

        //wczytanie modelu
        Group model = loadModel(getClass().getResource("OBJ_Robot.obj"));

        redMaterial.setSpecularColor(Color.ORANGE);
        redMaterial.setDiffuseColor(Color.RED);

        Group group = new Group();
        group.getChildren().add(ground);
        group.getChildren().add(box);
        group.getChildren().add(model);

        model.translateYProperty().set(50);

        Camera camera = new PerspectiveCamera(true);
        Scene scene = new Scene(group,WIDTH, HEIGHT, true);
        scene.setFill(Color.SILVER);
        scene.setCamera(camera);

        Translate pivot = new Translate();
        Rotate yRotate = new Rotate(0, Rotate.Y_AXIS);

        box.translateXProperty().set(10);
        box.translateYProperty().set(45);
        box.translateZProperty().set(0);
        box.setMaterial(redMaterial);

        ground.translateXProperty().set(0);
        ground.translateYProperty().set(50);
        ground.translateZProperty().set(0);

        //ustawienia kamery
        camera.setNearClip(1);
        camera.setFarClip(1000);


        //ruch i rotacja kamery
        camera.getTransforms().addAll(
                pivot,
                yRotate,
                new Rotate(-20, Rotate.X_AXIS),
                new Translate(0, 0, -500)
        );

        yRotate.angleProperty().bind(angleY);

        scene.setOnMouseDragged(event -> angleY.set(event.getSceneX()));

        primaryStage.setTitle("Ramię robota");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Group loadModel(URL url) {
        Group modelRoot = new Group();

        ObjModelImporter importer = new ObjModelImporter();
        importer.read(url);

        for (MeshView view : importer.getImport()) {
            modelRoot.getChildren().add(view);
        }

        return modelRoot;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

