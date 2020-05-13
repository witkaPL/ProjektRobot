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
import javafx.animation.*;
import com.interactivemesh.jfx.importer.obj.ObjModelImporter;
import javafx.util.Duration;
import java.lang.Math;

import java.net.URL;


public class Main extends Application {

    private static final int WIDTH = 1400;
    private static final int HEIGHT = 800;
    private static final double GROUND_LEVEL = 50;

    final PhongMaterial redMaterial = new PhongMaterial();

    private double anchorX;
    private double anchorAngleY = 0;
    private final DoubleProperty angleY = new SimpleDoubleProperty(0);

    @Override
    public void start(Stage primaryStage) {

        Box box = new Box(5, 5, 5);
        Box ground = new Box(500, 2, 500);

        //wczytanie modelu robota
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

        //ustawienia pudełka (pozycja i kolor)
        box.translateXProperty().set(-10);
        box.translateYProperty().set(-10);
        box.translateZProperty().set(0);
        box.setMaterial(redMaterial);

        //pozycja poziomu ziemi
        ground.translateXProperty().set(0);
        ground.translateYProperty().set(GROUND_LEVEL);
        ground.translateZProperty().set(0);

        //ustawienia kamery
        camera.setNearClip(1);
        camera.setFarClip(1000);

        //ruch i rotacja kamery
        cameraControl(camera, scene);

        gravityAnimation(box);

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

    private void cameraControl(Camera camera, Scene scene) {

        Translate pivot = new Translate();
        Rotate yRotate = new Rotate(0, Rotate.Y_AXIS);

        camera.getTransforms().addAll(
                pivot,
                yRotate,
                new Rotate(-20, Rotate.X_AXIS),
                new Translate(0, 0, -500)
        );

        yRotate.angleProperty().bind(angleY);

        scene.setOnMousePressed(event -> {
            anchorX = event.getSceneX();
            anchorAngleY = angleY.get();
        });

        scene.setOnMouseDragged(event -> angleY.set(anchorAngleY + anchorX - event.getSceneX()));

    }

    private void gravityAnimation(Box box) {

        double movementDistance;
        double endPoint;
        double movementTime;

        endPoint = GROUND_LEVEL - 3;
        movementDistance = endPoint - box.getTranslateY();
        movementTime = Math.sqrt(2*movementDistance*0.1/9.81); //jedna jednostka 10 cm

        final Timeline timeline = new Timeline();
        final KeyValue kv = new KeyValue(box.translateYProperty(), endPoint,
                Interpolator.LINEAR);
        final KeyFrame kf = new KeyFrame(Duration.seconds(movementTime), kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();

    }

    public static void main(String[] args) {
        launch(args);
    }
}

