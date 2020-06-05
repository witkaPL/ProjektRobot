package sample;

import javafx.application.Application;
import javafx.geometry.Orientation;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.control.*;
import javafx.scene.text.Text;


public class Main extends Application {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final double GROUND_LEVEL = 50;

    final PhongMaterial redMaterial = new PhongMaterial();
    final PhongMaterial greyMaterial = new PhongMaterial();

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

    private final double[] savedPositionArray = {0.001, 0.001, 0.001, 0.001, 0.001};

    private final Box box = new Box(5, 5, 5);
    private final Box ground = new Box(500, 2, 500);
    //private final Box collisionBox1 = new Box(1, 5, 1);

    private boolean didCollideWithBox = false;
    private boolean didCollideWithGround = false;

    @Override
    public void start(Stage primaryStage) {

        //wczytanie modelu robota
        Group model = loadModel(getClass().getResource("OBJ_Robot.obj"));

        redMaterial.setSpecularColor(Color.ORANGE);
        redMaterial.setDiffuseColor(Color.RED);

        greyMaterial.setSpecularColor(Color.DARKGRAY);
        greyMaterial.setDiffuseColor(Color.GRAY);

        Group group = new Group();
        group.getChildren().add(model);
        group.getChildren().add(ground);
        group.getChildren().add(box);

        //światło
        PointLight light1 = new PointLight();
        light1.setColor(Color.ORANGERED);
        light1.getTransforms().add(new Translate(500,-200,500));

        PointLight light2 = new PointLight();
        light2.setColor(Color.LIGHTSKYBLUE);
        light2.getTransforms().add(new Translate(-500,-200,-500));

        group.getChildren().add(light1);
        group.getChildren().add(light2);


        //kamera
        Camera camera = new PerspectiveCamera(true);
        SubScene scene= new SubScene(group, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.SILVER);
        scene.setCamera(camera);

        //ustawienia pudełka (pozycja i kolor)
        box.translateXProperty().set(0);
        box.translateYProperty().set(GROUND_LEVEL-4);
        box.translateZProperty().set(-30);
        box.setMaterial(redMaterial);
        box.setManaged(false);

        //pozycja poziomu ziemi
        ground.translateXProperty().set(0);
        ground.translateYProperty().set(GROUND_LEVEL);
        ground.setMaterial(greyMaterial);
        ground.translateZProperty().set(0);

        //2D GUI
        BorderPane pane = new BorderPane();
        pane.setCenter(scene);
        Text instructionText = new Text();
        instructionText.setText("Sterowanie: \n ← →: obór lewo prawo \n ↑ ↓: obrót góra dół \n W S: obrót góra dół \n A D: otwieranie zamykanie chwytaka");

        Button saveButton = new Button("Zapisz pozycję");
        Button moveButton = new Button("Odtwórz pozycję");

        Text savedPositionText = new Text();
        savedPositionText.setText("\n Zapisana pozycja:" +
                            "\n Lewo prawo: " +  String.format("%.2f", savedPositionArray[0]) +
                            "\n Góra dół (punkt1): " + String.format("%.2f", savedPositionArray[1]) +
                            "\n Góra dół (punkt1): "+ String.format("%.2f", savedPositionArray[2]) +
                            "\n Chwytak: " + String.format("%.2f", savedPositionArray[3]));

        Text currentPositionText = new Text();
        updateText(currentPositionText);

        ToolBar toolBar = new ToolBar(instructionText, saveButton, moveButton, savedPositionText, currentPositionText);
        toolBar.setOrientation(Orientation.VERTICAL);
        toolBar.setMinWidth(200);
        pane.setRight(toolBar);
        pane.setPrefSize(HEIGHT,WIDTH+200);
        Scene sceneGUI = new Scene(pane);
        group.requestFocus();

        //ustawienia kamery
        camera.setNearClip(1);
        camera.setFarClip(1000);

        //ruch i rotacja kamery
        cameraControl(camera, scene, group);

        //wejścia z klawiatury
        keyboardInputHandler(scene, model, currentPositionText);

        //obsługa przycisków
        buttonHandler(saveButton, moveButton, model, savedPositionText, currentPositionText);

        //dokładna kolizja
        /*
        collisionBox1.getTransforms().setAll(model.getTransforms());
        collisionBox1.setMaterial(redMaterial);
        group.getChildren().add(collisionBox1);
        setUpCollisionBoxes(model);

         */

        primaryStage.setTitle("Ramię robota");
        primaryStage.setScene(sceneGUI);
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
            view.setMaterial(greyMaterial);
            modelRoot.getChildren().add(view);
        }

        return modelRoot;
    }

    private void cameraControl(Camera camera, SubScene scene, Group group) {

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

        scene.setOnMouseDragged(event -> {
            group.requestFocus();
            angleY.set(anchorAngleY + anchorX - event.getSceneX());
        });

    }

    /*
    private void setUpCollisionBoxes(Group model) {
        model.getChildren()
                .stream()
                .filter(view -> view.getId().equals("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper_R"))
                .forEach(view -> {
                    collisionBox1.relocate(view.getBoundsInParent().getCenterX(), view.getBoundsInParent().getCenterY());

                    //collisionBox1.translateXProperty().set(view.getBoundsInParent().getCenterX());
                    //collisionBox1.translateYProperty().set(view.getBoundsInParent().getCenterY());
                    //collisionBox1.translateZProperty().set(view.getBoundsInParent().getCenterZ());

                });
    }

     */

    private void buttonHandler(Button saveButton, Button moveButton, Group model, Text savedPositionText,Text currentPositionText) {

        saveButton.setOnAction(event -> {
            savePosition();
            savedPositionText.setText("\n Zapisana pozycja:" +
                    "\n Lewo prawo: " +  String.format("%.2f", savedPositionArray[0]) +
                    "\n Góra dół (punkt1): " + String.format("%.2f", savedPositionArray[1]) +
                    "\n Góra dół (punkt1): "+ String.format("%.2f", savedPositionArray[2]) +
                    "\n Chwytak: " + String.format("%.2f", savedPositionArray[3]));
        });
        moveButton.setOnAction(event -> {
            moveToSavedPosition(model);
            updateText(currentPositionText);
        });

    }

    private void keyboardInputHandler(SubScene scene, Group model, Text currentPositionText) {

        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case LEFT:
                    rotateRobotLeftRight(model, -5);
                    moveBox(model);
                    updateText(currentPositionText);
                    break;
                case RIGHT:
                    rotateRobotLeftRight(model, 5);
                    moveBox(model);
                    updateText(currentPositionText);
                    break;
                case UP:
                    if(rotationUpDown.getAngle()<105 && !didCollideWithGround) {
                        rotateRobotUpDown(model, 5);
                        moveBox(model);
                        checkCollisionWithGround(model);
                        updateText(currentPositionText);
                    }
                    break;
                case DOWN:
                    if(rotationUpDown.getAngle()>-70) {
                        rotateRobotUpDown(model, -5);
                        moveBox(model);
                        checkCollisionWithGround(model);
                    }
                    break;
                case W:
                    if(rotationUpDownPivot2.getAngle()<5 && !didCollideWithGround) {
                        rotateRobotUpDownPivot2(model, 5);
                        moveBox(model);
                        checkCollisionWithGround(model);
                        updateText(currentPositionText);
                    }
                    break;
                case S:
                    if(rotationUpDownPivot2.getAngle()>-108) {
                        rotateRobotUpDownPivot2(model, -5);
                        moveBox(model);
                        checkCollisionWithGround(model);
                        updateText(currentPositionText);
                    }
                    break;
                case A:
                    if(rotationCloseOpenL.getAngle()<5) {
                        rotateGripperCloseOpenL(model, 5);
                        rotateGripperCloseOpenR(model, -5);
                        moveBox(model);
                        checkCollision(model);
                        updateText(currentPositionText);
                    }
                    break;
                case D:
                    if(rotationCloseOpenL.getAngle()>-75) {
                        rotateGripperCloseOpenL(model, -5);
                        rotateGripperCloseOpenR(model, 5);
                        moveBox(model);
                        checkCollision(model);
                        updateText(currentPositionText);
                    }
                    break;
            }
        });

    }

    private void rotateRobotLeftRight(Group model, int direction) {

        rotationLeftRight.pivotXProperty().set(16.3); //ustawienie wartości X osi obrotu
        rotationLeftRight.pivotYProperty().set(-25.8); //ustawienie wartości Y osi obrotu
        rotationLeftRight.pivotZProperty().set(-18.55); //ustawienie wartości Z osi obrotu

        //wartość końcowa rotacji
        double endRotationValue;
        double time;
        //sprawdzanie czy funkcja została wywołana przez klawisze do obrotu, czy aby odtworzyć zachowaną pozycje (direction = 0)
        if(direction!=0) {
            endRotationValue = rotationLeftRight.getAngle() + direction;
            time = 0.1;
        }
        else {
            endRotationValue = savedPositionArray[0];
            time = Math.abs(savedPositionArray[0])/5*0.1; //obliczenie czasu na obrót w przypadku odtworzenia zachowanej pozycji (to samo tempo)
        }

        model.getChildren()
                .stream()
                .filter(view -> view.getId().startsWith("Robot_Head__Axis_1_"))
                .forEach(view -> {
                    view.getTransforms().remove(rotationUpDown);
                    view.getTransforms().remove(rotationUpDownPivot2);
                    view.getTransforms().remove(rotationLeftRight);
                    view.getTransforms().remove(rotationCloseOpenL);
                    view.getTransforms().remove(rotationCloseOpenR);
                    //animacja ruchu
                    Timeline timeline = new Timeline();
                    KeyValue kv = new KeyValue(rotationLeftRight.angleProperty(), endRotationValue);
                    KeyFrame kf = new KeyFrame(Duration.seconds(time), kv);
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

        //wartość końcowa rotacji
        double endRotationValue;
        double time; //czas trwania animacji
        //sprawdzanie czy funkcja została wywołana przez klawisze do obrotu, czy aby odtworzyć zachowaną pozycje (direction = 0)
        if(direction!=0) {
            endRotationValue = rotationUpDown.getAngle() + direction;
            time  = 0.1;
        }
        else {
            endRotationValue = savedPositionArray[1];
            time = Math.abs(savedPositionArray[1])/5*0.1; //obliczenie czasu na obrót w przypadku odtworzenia zachowanej pozycji (to samo tempo)
        }

        model.getChildren()
                .stream()
                .filter(view -> view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_"))
                .forEach(view -> {
                    view.getTransforms().remove(rotationUpDown);
                    view.getTransforms().remove(rotationUpDownPivot2);
                    view.getTransforms().remove(rotationCloseOpenL);
                    view.getTransforms().remove(rotationCloseOpenR);
                    Timeline timeline = new Timeline();
                    KeyValue kv = new KeyValue(rotationUpDown.angleProperty(), endRotationValue);
                    KeyFrame kf = new KeyFrame(Duration.seconds(time), kv);
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

        //wartość końcowa rotacji
        double endRotationValue;
        double time;
        //sprawdzanie czy funkcja została wywołana przez klawisze do obrotu, czy aby odtworzyć zachowaną pozycje (direction = 0)
        if(direction!=0) {
            endRotationValue = rotationUpDownPivot2.getAngle() + direction;
            time = 0.1;
        }
        else {
            endRotationValue = savedPositionArray[2];
            time = Math.abs(savedPositionArray[2])/5*0.1; //obliczenie czasu na obrót w przypadku odtworzenia zachowanej pozycji (to samo tempo)
        }

        model.getChildren()
                .stream()
                .filter(view -> view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_"))
                .forEach(view -> {
                    view.getTransforms().remove(rotationUpDownPivot2);
                    view.getTransforms().remove(rotationCloseOpenL);
                    view.getTransforms().remove(rotationCloseOpenR);
                    Timeline timeline = new Timeline();
                    KeyValue kv = new KeyValue(rotationUpDownPivot2.angleProperty(), endRotationValue);
                    KeyFrame kf = new KeyFrame(Duration.seconds(time), kv);
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

        //wartość końcowa rotacji
        double endRotationValue;
        double time;
        //sprawdzanie czy funkcja została wywołana przez klawisze do obrotu, czy aby odtworzyć zachowaną pozycje (direction = 0)
        if(direction!=0) {
            endRotationValue = rotationCloseOpenL.getAngle() + direction;
            time = 0.1;
        }
        else {
            endRotationValue = savedPositionArray[3];
            time = Math.abs(savedPositionArray[3])/5*0.1; //obliczenie czasu na obrót w przypadku odtworzenia zachowanej pozycji (to samo tempo)
        }

        model.getChildren()
                .stream()
                .filter(view -> view.getId().equals("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper_L"))
                .forEach(view -> {
                    view.getTransforms().remove(rotationCloseOpenL);
                    Timeline timeline = new Timeline();
                    KeyValue kv = new KeyValue(rotationCloseOpenL.angleProperty(), endRotationValue);
                    KeyFrame kf = new KeyFrame(Duration.seconds(time), kv);
                    timeline.getKeyFrames().add(kf);
                    timeline.play();
                    view.getTransforms().add(rotationCloseOpenL);
                });
    }

    private void rotateGripperCloseOpenR(Group model, int direction) {

        rotationCloseOpenR.pivotXProperty().set(0.459); //ustawienie wartości X osi obrotu
        rotationCloseOpenR.pivotYProperty().set(-18.02); //ustawienie wartości Y osi obrotu
        rotationCloseOpenR.pivotZProperty().set(-38.91); //ustawienie wartości Z osi obrotu

        //wartość końcowa rotacji
        double endRotationValue;
        double time;
        //sprawdzanie czy funkcja została wywołana przez klawisze do obrotu, czy aby odtworzyć zachowaną pozycje (direction = 0)
        if(direction!=0) {
            endRotationValue = rotationCloseOpenR.getAngle() + direction;
            time = 0.1;
        }
        else {
            endRotationValue = savedPositionArray[4];
            time = Math.abs(savedPositionArray[4])/5*0.1; //obliczenie czasu na obrót w przypadku odtworzenia zachowanej pozycji (to samo tempo)
        }

        model.getChildren()
                .stream()
                .filter(view -> view.getId().equals("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper_R"))
                .forEach(view -> {
                    view.getTransforms().remove(rotationCloseOpenR);
                    Timeline timeline = new Timeline();
                    KeyValue kv = new KeyValue(rotationCloseOpenR.angleProperty(), endRotationValue);
                    KeyFrame kf = new KeyFrame(Duration.seconds(time), kv);
                    timeline.getKeyFrames().add(kf);
                    timeline.play();
                    view.getTransforms().add(rotationCloseOpenR);
                });
    }

    private void checkCollision(Group model) {

        //sprawdzanie kolizji z pudełkiem
        model.getChildren()
                .stream()
                .filter(view -> view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper"))
                .forEach(view -> {
                    if(box.getBoundsInParent().intersects(view.getBoundsInParent())) {
                       //System.out.println("YEP!");
                        didCollideWithBox = true;
                    }
                    else {
                        gravityAnimation(box);
                        didCollideWithBox = false;
                    }
                });
    }

    private void checkCollisionWithGround(Group model) {

        //sprawdzanie kolizji z ziemią
        model.getChildren()
                .stream()
                .filter(view -> view.getId().startsWith("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_basegrasper"))
                .forEach(view -> {
                    //System.out.println("YEP!");
                    didCollideWithGround = view.getBoundsInParent().getMaxY() > GROUND_LEVEL - 5;
                });
    }

    private void moveBox(Group model) {

        TranslateTransition boxMovement = new TranslateTransition(Duration.millis(100), box);

        if(didCollideWithBox) {
            model.getChildren()
                    .stream()
                    .filter(view -> view.getId().equals("Robot_Head__Axis_1_Arm_1__Axis_2_Arm_2__Axis_3_Arm_3__Axis_4_Joint__Axis_5_Grasper_base"))
                    .forEach(view -> {
                        boxMovement.setToX(view.getBoundsInParent().getCenterX());
                        boxMovement.setToY(view.getBoundsInParent().getCenterY()+1.5*box.getHeight());
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

    private void savePosition() {
        //Przypisanie kolejnym elementom tablicy wartości poszczególnych rotacji
        savedPositionArray[0] = rotationLeftRight.getAngle();
        savedPositionArray[1] = rotationUpDown.getAngle();
        savedPositionArray[2] = rotationUpDownPivot2.getAngle();
        savedPositionArray[3] = rotationCloseOpenL.getAngle();
        savedPositionArray[4] = rotationCloseOpenR.getAngle();

    }

    private void moveToSavedPosition(Group model) {
        //Przesunięcie robota do zapisanej pozycji
        rotateRobotLeftRight(model, 0);
        rotateRobotUpDown(model, 0);
        rotateRobotUpDownPivot2(model, 0);
        rotateGripperCloseOpenL(model, 0);
        rotateGripperCloseOpenR(model, 0);
    }

    private void updateText(Text currentPositionText) {
        currentPositionText.setText("\n Obecna pozycja:" +
                "\n Lewo prawo: " +  String.format("%.2f", rotationLeftRight.getAngle()) +
                "\n Góra dół (punkt1): " + String.format("%.2f", rotationUpDown.getAngle()) +
                "\n Góra dół (punkt1): "+ String.format("%.2f", rotationUpDownPivot2.getAngle()) +
                "\n Chwytak: " + String.format("%.2f", rotationCloseOpenL.getAngle()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}