package sample;

import javafx.application.Application;
import javafx.geometry.Point3D;
import javafx.scene.*;
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

    //ustawienie osi obrotu
    //wartość Z to tg(34), obrót o 34 stopnie osi obrotu
    private final Point3D axisUpDown = new Point3D(1, 0, -Math.tan(Math.toRadians(33.581)));
    private final Point3D axisOpenClose = new Point3D(1, 0, -Math.tan(Math.toRadians(123.581)));

    private final Rotate rotationUpDown = new Rotate(0, axisUpDown);
    private final Rotate rotationUpDownPivot2 = new Rotate(0, axisUpDown);
    private final Rotate rotationLeftRight = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotationCloseOpenL = new Rotate(0, axisOpenClose);
    private final Rotate rotationCloseOpenR = new Rotate(0, axisOpenClose);

    private final Box box = new Box(5, 5, 5);

    private boolean didCollide = false;

    @Override
    public void start(Stage primaryStage) {


        Box ground = new Box(500, 2, 500);

        //wczytanie modelu robota
        Group model = loadModel(getClass().getResource("OBJ_Robot.obj"));

        redMaterial.setSpecularColor(Color.ORANGE);
        redMaterial.setDiffuseColor(Color.RED);

        Group group = new Group();
        group.getChildren().add(model);
        group.getChildren().add(ground);
        group.getChildren().add(box);

        Camera camera = new PerspectiveCamera(true);
        Scene scene = new Scene(group,WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.SILVER);
        scene.setCamera(camera);

        //ustawienia pudełka (pozycja i kolor)
        box.translateXProperty().set(0);
        box.translateYProperty().set(GROUND_LEVEL-4);
        box.translateZProperty().set(0);
        box.setMaterial(redMaterial);
        box.setManaged(false);

        //pozycja poziomu ziemi
        ground.translateXProperty().set(0);
        ground.translateYProperty().set(GROUND_LEVEL);
        ground.translateZProperty().set(0);

        //ustawienia kamery
        camera.setNearClip(1);
        camera.setFarClip(1000);

        //ruch i rotacja kamery
        cameraControl(camera, scene);

        //wejścia z klawiatury
        keyboardInputHandler(scene, model);

        primaryStage.setTitle("Ramię robota");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Group loadModel(URL url) {
        Group modelRoot = new Group();

        ObjModelImporter importer = new ObjModelImporter();
        importer.read(url);

        for (MeshView view : importer.getImport()) {
            view.translateXProperty().set(0);
            view.translateYProperty().set(GROUND_LEVEL);
            view.translateZProperty().set(0);
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

    private void keyboardInputHandler(Scene scene, Group model) {

        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case LEFT:
                    rotateRobotLeftRight(model, -5);
                    moveBox(model);
                    break;
                case RIGHT:
                    rotateRobotLeftRight(model, 5);
                    moveBox(model);
                    break;
                case UP:
                    //if(rotationUpDown.getAngle()<20)
                    rotateRobotUpDown(model, 5);
                    moveBox(model);
                    break;
                case DOWN:
                    //if(rotationUpDown.getAngle()>-70)
                    rotateRobotUpDown(model, -5);
                    moveBox(model);
                    break;
                case W:
                    rotateRobotUpDownPivot2(model, 5);
                    moveBox(model);
                    break;
                case S:
                    rotateRobotUpDownPivot2(model, -5);
                    moveBox(model);
                    break;
                case A:
                    rotateGripperCloseOpenL(model, 5);
                    rotateGripperCloseOpenR(model, -5);
                    moveBox(model);
                    checkCollision(model);
                    break;
                case D:
                    rotateGripperCloseOpenL(model, -5);
                    rotateGripperCloseOpenR(model, 5);
                    moveBox(model);
                    checkCollision(model);
                    break;
            }
        });

    }

    private void rotateRobotLeftRight(Group model, int direction) {

        rotationLeftRight.pivotXProperty().set(16.3); //ustawienie wartości X osi obrotu
        rotationLeftRight.pivotYProperty().set(-25.8); //ustawienie wartości Y osi obrotu
        rotationLeftRight.pivotZProperty().set(-18.55); //ustawienie wartości Z osi obrotu

        model.getChildren()
                .stream()
                .filter(view -> view.getId().startsWith("Robot_Head__Axis_1_"))
                .forEach(view -> {
                    view.getTransforms().remove(rotationUpDown);
                    view.getTransforms().remove(rotationUpDownPivot2);
                    view.getTransforms().remove(rotationLeftRight);
                    view.getTransforms().remove(rotationCloseOpenL);
                    view.getTransforms().remove(rotationCloseOpenR);
                    Timeline timeline = new Timeline();
                    KeyValue kv = new KeyValue(rotationLeftRight.angleProperty(), rotationLeftRight.getAngle() + direction);
                    KeyFrame kf = new KeyFrame(Duration.seconds(0.1), kv);
                    timeline.getKeyFrames().add(kf);
                    timeline.play();
                    view.getTransforms().add(rotationLeftRight);
                    //zapobiega złemu obrotowi pozostałych części robota
                    if(view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_")) {
                        view.getTransforms().add(rotationUpDown);
                    }
                    if(view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_")) {
                        view.getTransforms().add(rotationUpDownPivot2);
                    }
                    if(view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper_L")) {
                        view.getTransforms().add(rotationCloseOpenL);
                    }
                    if(view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper_R")) {
                        view.getTransforms().add(rotationCloseOpenR);
                    }
                });
    }


    private void rotateRobotUpDown(Group model, int direction) {

        rotationUpDown.pivotXProperty().set(16.3); //ustawienie wartości X osi obrotu
        rotationUpDown.pivotYProperty().set(-25.8); //ustawienie wartości Y osi obrotu
        rotationUpDown.pivotZProperty().set(-18.55); //ustawienie wartości Z osi obrotu

        model.getChildren()
                .stream()
                .filter(view -> view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_"))
                .forEach(view -> {
                    view.getTransforms().remove(rotationUpDown);
                    view.getTransforms().remove(rotationUpDownPivot2);
                    view.getTransforms().remove(rotationCloseOpenL);
                    view.getTransforms().remove(rotationCloseOpenR);
                    Timeline timeline = new Timeline();
                    KeyValue kv = new KeyValue(rotationUpDown.angleProperty(), rotationUpDown.getAngle() + direction);
                    KeyFrame kf = new KeyFrame(Duration.seconds(0.1), kv);
                    timeline.getKeyFrames().add(kf);
                    timeline.play();
                    view.getTransforms().add(rotationUpDown);
                    //zapobiega złemu obrotowi pozostałych części robota
                    if(view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_")) {
                        view.getTransforms().add(rotationUpDownPivot2);
                    }
                    if(view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper_L")) {
                        view.getTransforms().add(rotationCloseOpenL);
                    }
                    if(view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper_R")) {
                        view.getTransforms().add(rotationCloseOpenR);
                    }
                });

    }

    private void rotateRobotUpDownPivot2(Group model, int direction) {

        rotationUpDownPivot2.pivotXProperty().set(30.1); //ustawienie wartości X osi obrotu
        rotationUpDownPivot2.pivotYProperty().set(-43.2); //ustawienie wartości Y osi obrotu
        rotationUpDownPivot2.pivotZProperty().set(-14.35); //ustawienie wartości Z osi obrotu

        model.getChildren()
                .stream()
                .filter(view -> view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_"))
                .forEach(view -> {
                    view.getTransforms().remove(rotationUpDownPivot2);
                    view.getTransforms().remove(rotationCloseOpenL);
                    view.getTransforms().remove(rotationCloseOpenR);
                    Timeline timeline = new Timeline();
                    KeyValue kv = new KeyValue(rotationUpDownPivot2.angleProperty(), rotationUpDownPivot2.getAngle() + direction);
                    KeyFrame kf = new KeyFrame(Duration.seconds(0.1), kv);
                    timeline.getKeyFrames().add(kf);
                    timeline.play();
                    view.getTransforms().add(rotationUpDownPivot2);
                    if(view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper_L")) {
                        view.getTransforms().add(rotationCloseOpenL);
                    }
                    if(view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper_R")) {
                        view.getTransforms().add(rotationCloseOpenR);
                    }
                });
    }

    private void rotateGripperCloseOpenL(Group model, int direction) {

        rotationCloseOpenL.pivotXProperty().set(2.659); //ustawienie wartości X osi obrotu
        rotationCloseOpenL.pivotYProperty().set(-18.02); //ustawienie wartości Y osi obrotu
        rotationCloseOpenL.pivotZProperty().set(-38.91); //ustawienie wartości Z osi obrotu

        model.getChildren()
                .stream()
                .filter(view -> view.getId().equals("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper_L"))
                .forEach(view -> {
                    view.getTransforms().remove(rotationCloseOpenL);
                    Timeline timeline = new Timeline();
                    KeyValue kv = new KeyValue(rotationCloseOpenL.angleProperty(), rotationCloseOpenL.getAngle() + direction);
                    KeyFrame kf = new KeyFrame(Duration.seconds(0.1), kv);
                    timeline.getKeyFrames().add(kf);
                    timeline.play();
                    view.getTransforms().add(rotationCloseOpenL);
                });
    }

    private void rotateGripperCloseOpenR(Group model, int direction) {

        rotationCloseOpenR.pivotXProperty().set(0.459); //ustawienie wartości X osi obrotu
        rotationCloseOpenR.pivotYProperty().set(-18.02); //ustawienie wartości Y osi obrotu
        rotationCloseOpenR.pivotZProperty().set(-38.91); //ustawienie wartości Z osi obrotu

        model.getChildren()
                .stream()
                .filter(view -> view.getId().equals("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper_R"))
                .forEach(view -> {
                    view.getTransforms().remove(rotationCloseOpenR);
                    Timeline timeline = new Timeline();
                    KeyValue kv = new KeyValue(rotationCloseOpenR.angleProperty(), rotationCloseOpenR.getAngle() + direction);
                    KeyFrame kf = new KeyFrame(Duration.seconds(0.1), kv);
                    timeline.getKeyFrames().add(kf);
                    timeline.play();
                    view.getTransforms().add(rotationCloseOpenR);
                });
    }

    private void checkCollision(Group model) {

        model.getChildren()
                .stream()
                .filter(view -> view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper"))
                .forEach(view -> {
                    if(box.getBoundsInParent().intersects(view.getBoundsInParent())) {
                       //System.out.println("YEP!");
                        didCollide = true;
                    }
                    else {
                        gravityAnimation(box);
                        didCollide = false;
                    }
                });
    }

    private void moveBox(Group model) {

        TranslateTransition boxMovement = new TranslateTransition(Duration.millis(100), box);


        if(didCollide) {
            model.getChildren()
                    .stream()
                    .filter(view -> view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper"))
                    .forEach(view -> {
                        boxMovement.setToX(view.getBoundsInParent().getCenterX());
                        boxMovement.setToY(view.getBoundsInParent().getCenterY());
                        boxMovement.setToZ(view.getBoundsInParent().getCenterZ());
                        boxMovement.play();
                    });
        }
    }


    private void gravityAnimation(Box box) {

        double movementDistance;
        double endPoint;
        double movementTime;

        endPoint = GROUND_LEVEL - 3;
        movementDistance = Math.abs(endPoint - box.getTranslateY());
        movementTime = Math.sqrt(2*movementDistance*0.1/9.81); //jedna jednostka 10 cm

        final Timeline timeline = new Timeline();
        final KeyValue kv = new KeyValue(box.translateYProperty(), endPoint,
                Interpolator.LINEAR);
        final KeyFrame kf = new KeyFrame(Duration.seconds(movementTime), kv);
        //timeline.setCycleCount(10); //test
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }


    public static void main(String[] args) {
        launch(args);
    }
}