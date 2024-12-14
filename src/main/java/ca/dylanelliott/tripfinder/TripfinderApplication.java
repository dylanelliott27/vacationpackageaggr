package ca.dylanelliott.tripfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TripfinderApplication {

	public static void main(String[] args) {
		SpringApplication.run(TripfinderApplication.class, args);
	}

}
