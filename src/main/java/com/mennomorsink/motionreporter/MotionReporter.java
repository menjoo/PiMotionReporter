package com.mennomorsink.motionreporter;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MotionReporter {

    private final static Logger logger = Logger.getLogger(MotionReporter.class);

    public static void main(String args[]) throws InterruptedException {
        logger.info("Motion reporter initializing...");

        if(args.length == 0) {
            logger.info("please pass notification url as 1st argument!");
        } else {
            final String notificationUrl = args[0];

            logger.info("notificationUrl: " + notificationUrl);

            // create gpio controller
            final GpioController gpio = GpioFactory.getInstance();

            // provision gpio pin #02 as an input pin with its internal pull down resistor enabled
            final GpioPinDigitalInput pirSensor = gpio.provisionDigitalInputPin(RaspiPin.GPIO_07, PinPullResistance.PULL_DOWN);

            // set shutdown state for this input pin
            //pirSensor1.setShutdownOptions(true);
            GpioPinListenerDigital listener = new GpioPinListenerDigital() {

                public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                    // display pin state on console
                    logger.info(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());

                    if(event.getState().toString().equals("HIGH")) {
                        try {
                            logger.info("Sending notifcation!");
                            sendNotification(notificationUrl);
                        }
                        catch (Exception ex) {
                            logger.error("Exception: " + ex.getMessage());
                        }
                    }
                }
            };

            // create and register gpio pin listener
            pirSensor.addListener(listener);

            logger.info(" ... running");

            // keep program running until user aborts (CTRL-C)
            while(true) {
                Thread.sleep(500);
            }

            // stop all GPIO activity/threads by shutting down the GPIO controller
            // (this method will forcefully shutdown all GPIO monitoring threads and scheduled tasks)
            // gpio.shutdown();   <--- implement this method call if you wish to terminate the Pi4J GPIO controller
        }
    }

    // HTTP GET request
    private static void sendNotification(String notificationUrl) throws Exception {

        String USER_AGENT = "Mozilla/5.0";

        URL obj = new URL(notificationUrl);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = con.getResponseCode();
        logger.info("\nSending 'GET' request to URL : " + notificationUrl);
        logger.info("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        logger.info(response.toString());
    }
}
