import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends Application {
    private XYChart.Series<Number, Number> sensorSeries = new XYChart.Series<>();
    private XYChart.Series<Number, Number> generatedSeries = new XYChart.Series<>();
    private int xSensorData = 0;
    private int xGeneratedData = 0;
    private ScheduledExecutorService scheduledExecutorService;
    private SerialPort serialPort;
    private InputStream inputStream;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        // Setting up axes for the sensor data chart
        NumberAxis xAxis1 = new NumberAxis();
        NumberAxis yAxis1 = new NumberAxis(0, 100, 10);
        xAxis1.setLabel("Time (seconds)");
        yAxis1.setLabel("Sensor Value");

        LineChart<Number, Number> sensorChart = new LineChart<>(xAxis1, yAxis1);
        sensorChart.setTitle("Data from Raspberry Pi Pico");
        sensorSeries.setName("Sensor Data");
        sensorChart.getData().add(sensorSeries);

        // Setting up axes for the generated data chart
        NumberAxis xAxis2 = new NumberAxis();
        NumberAxis yAxis2 = new NumberAxis(0, 100, 10);
        xAxis2.setLabel("Time (seconds)");
        yAxis2.setLabel("Generated Value");

        LineChart<Number, Number> generatedChart = new LineChart<>(xAxis2, yAxis2);
        generatedChart.setTitle("Generated Data");
        generatedSeries.setName("Generated Values");
        generatedChart.getData().add(generatedSeries);

        Button startButton = new Button("Start Graph");
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll("Sin", "Cos");
        comboBox.setValue("Sin");

        startButton.setOnAction(event -> startDataFetching(comboBox.getValue()));

        VBox root = new VBox(10, comboBox, startButton, sensorChart, generatedChart);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Raspberry Pi Pico Graph");
        primaryStage.show();

        startGeneratingData();
    }

    private void startDataFetching(String functionType) {
        try {
            Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
            while (portList.hasMoreElements()) {
                CommPortIdentifier portId = portList.nextElement();
                if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                    serialPort = (SerialPort) portId.open("SerialPort", 2000);
                    serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                    inputStream = serialPort.getInputStream();
                    System.out.println("Serial port opened: " + portId.getName());
                    break;
                }
            }

            if (serialPort == null) {
                System.out.println("No serial ports found.");
                return;
            }

            // Start reading data from the serial port
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    if (inputStream.available() > 0) {
                        byte[] buffer = new byte[inputStream.available()];
                        inputStream.read(buffer);
                        String data = new String(buffer, StandardCharsets.UTF_8).trim();
                        System.out.println("Raw data received: " + data); // Виводить отримані дані одразу після отримання
                        processSensorData(data);
                    }
                } catch (Exception e) {
                    System.out.println("Serial port read error: " + e.getMessage());
                }
            }, 0, 100, TimeUnit.MILLISECONDS); // Change to 100ms for more frequent reads

        } catch (Exception e) {
            System.out.println("Failed to open serial port: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processSensorData(String data) {
        try {
            // Ensure the data can be parsed as a number
            int rawValue = Integer.parseInt(data);
            System.out.println("Parsed sensor value: " + rawValue); // Виводить значення, отримане з мікроконтролера

            Platform.runLater(() -> {
                sensorSeries.getData().add(new XYChart.Data<>(xSensorData++, rawValue));
                if (sensorSeries.getData().size() > 30) {
                    sensorSeries.getData().remove(0);  // Keep only the last 30 values
                }
            });
        } catch (NumberFormatException e) {
            System.out.println("Received non-numeric data: " + data);
        }
    }

    private void startGeneratingData() {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            double generatedValue = Math.sin(Math.toRadians(xGeneratedData * 10)) * 100; // Generate data
            Platform.runLater(() -> {
                generatedSeries.getData().add(new XYChart.Data<>(xGeneratedData++, generatedValue));
                if (generatedSeries.getData().size() > 30) {
                    generatedSeries.getData().remove(0);  // Keep only the last 30 values
                }
            });
        }, 0, 1, TimeUnit.SECONDS); // Generate new data every second
    }

    @Override
    public void stop() {
        System.out.println("Stopping application...");
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
        if (serialPort != null) {
            try {
                inputStream.close();
                serialPort.close();
                System.out.println("Serial port closed.");
            } catch (Exception e) {
                System.out.println("Error closing serial port: " + e.getMessage());
            }
        }
    }
}
