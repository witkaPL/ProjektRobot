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

        Box box = new Box(1, 100, 1);
        Box ground = new Box(500, 2, 500);

        //wczytanie modelu robota
        Group model = loadModel(getClass().getResource("OBJ_Robot.obj"));

        redMaterial.setSpecularColor(Color.ORANGE);
        redMaterial.setDiffuseColor(Color.RED);

        Group group = new Group();
        group.getChildren().add(model);
        group.getChildren().add(ground);
        group.getChildren().add(box);

        model.translateXProperty().set(0);
        model.translateYProperty().set(50);
        model.translateZProperty().set(0);

        Camera camera = new PerspectiveCamera(true);
        Scene scene = new Scene(group,WIDTH, HEIGHT, true);
        scene.setFill(Color.SILVER);
        scene.setCamera(camera);

        //ustawienia pudełka (pozycja i kolor)
        box.translateXProperty().set(0);
        box.translateYProperty().set(40);
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

        animate(model);

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

    private void animate(Group model) {

        /*Przesunąłem model względem punktu 0 0, poprawiłem to w programie do obróbki 3d.
        Nowy model (ten już który był w 0 0), nie chciał się zaimportować, bo miał też jakieś domyślne tekstury
        w pliku .mtl, którego nie mogłem znaleźć. Poprawiłem to w .obj (usunąłem odwołania do tych tekstur),
        ale był problem z tymi częściami bo były inaczej oznaczone. Może później to ogarnę bo tekstury w sumie też
        się przydadzą.
        Teraz co jest poniżej (i działa) to jest po prostu przesunięcie osi obrotu, wartości te znalazłem w tym programie
        do 3d, przesuwając model żeby był na środku, co było upierdliwe bo nie jest symetryczny (probowałem
        żeby oś była w połowie szerokości i długości, ale nadal była przesunięta).
        Pewnie będzie problem z szukaniem następnych osi obrotu, więc jak to Ci się nie uda, to zrób żeby ten obrót
        co jest można było sterować klawiaturą np. A i D.

         */
        Rotate rotation = new Rotate(0, Rotate.Y_AXIS);
        rotation.pivotXProperty().set(16.212); //ustawienie wartości X osi obrotu
        rotation.pivotZProperty().set(-18.481); //ustawienie wartości Z osi obrotu

        model.getChildren()
                .stream()
                .filter(view -> view.getId().equals("Robot_Head__Axis_1_"))
                .forEach(view -> {

                    view.getTransforms().add(rotation);
                    Timeline timeline = new Timeline(
                            new KeyFrame(Duration.ZERO, new KeyValue(rotation.angleProperty(), 0)),
                            new KeyFrame(Duration.seconds(2), new KeyValue(rotation.angleProperty(), 360)));
                    timeline.setCycleCount(100);
                    timeline.play();

                });
    }

    public static void main(String[] args) {
        launch(args);
    }
}

