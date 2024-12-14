package ca.dylanelliott.tripfinder;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

import ca.dylanelliott.tripfinder.models.Run;
import ca.dylanelliott.tripfinder.models.RunData;
import ca.dylanelliott.tripfinder.repositories.RunDataRepository;
import ca.dylanelliott.tripfinder.repositories.RunRepository;
import jakarta.transaction.Transactional;

@Controller
public class IndexController {
    @Autowired
    private RunRepository runRepository;

    @Autowired
    private Environment environment;

    @Autowired
    private RunDataRepository runDataRepository;

    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

    @GetMapping("/runs/{runId}")
    public String getIndex(@PathVariable("runId") Long runId, Model model)
    {
        Run latestRun = runRepository.findById(runId).orElseThrow();

        model.addAttribute("latestRun", latestRun);

        return "index";
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?")
    @GetMapping("/run")
    @ResponseStatus(HttpStatus.OK)
    public void fetchTrips() throws URISyntaxException, IOException, InterruptedException
    {
        logger.info("Running fetch action");
        List<RunData> results = new ArrayList<>();
        Run run = new Run();
        run.setCreated(LocalDateTime.now());

        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Host");
        HttpClient client = HttpClient.newBuilder().build();
        Builder requestBuilder = HttpRequest.newBuilder().uri(new URI("https://www.sunwing.ca/page-data/en/promotion/packages/last-minute-vacations/page-data.json"));

        for (Map.Entry<String, String> entry : getHeaders().entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }

        HttpRequest request = requestBuilder.build();

        // https://book.sunwing.ca/cgi-bin/resultspackage-plus.cgi?page=1&sorted_by=grand_total&section=exact&language=en&sid=bff29018187a14fdfbf812bb0f08a333&search_id=&code_ag=rds&alias=btd&query_timestamp=&flex=N&action=results

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        JsonObject sunwingLastMinuteDeals = JsonParser.parseString(response.body()).getAsJsonObject();

        JsonArray merchandising = sunwingLastMinuteDeals
                                .getAsJsonObject("result")
                                .getAsJsonObject("data")
                                .getAsJsonObject("contentfulFluidLayout")
                                .getAsJsonObject("pageSections")
                                .getAsJsonArray("pageSections")
                                .get(2) // Assuming the 3rd section contains the "ContentfulPromotionRule"
                                .getAsJsonObject()
                                .getAsJsonObject("wrapperSections")
                                .getAsJsonArray("pageSections")
                                .get(0) // Assuming the first page section has "merchandising"
                                .getAsJsonObject()
                                .getAsJsonArray("merchandising");

        for (JsonElement airport : merchandising) {
            String airportName = airport.getAsJsonObject().getAsJsonObject("Gateway").get("Code").getAsString();

            if (airportName.equals("YYZ")) {
                JsonArray packagesForAirport = airport.getAsJsonObject().getAsJsonArray("PromotionGroups");

                for (JsonElement tripPackage : packagesForAirport) {
                    JsonObject trip = tripPackage.getAsJsonObject();
                    String title = trip.get("title").toString();

                    JsonArray tripsForDestination = trip.getAsJsonArray("Offers");

                    for (JsonElement packageForDestination : tripsForDestination) {
                        JsonObject accomodationData = packageForDestination.getAsJsonObject().getAsJsonObject("AccommodationInfo");
                        String resortName = accomodationData.get("AccommodationName").getAsString();
                        Integer starRating = accomodationData.get("StarRating").getAsInt();
                        String departureDate = packageForDestination.getAsJsonObject().get("DepartureDate").getAsString();
                        Integer price = packageForDestination.getAsJsonObject().get("Price").getAsInt();
                        String link = packageForDestination.getAsJsonObject().get("DeepLink").getAsString();
                        String country = packageForDestination.getAsJsonObject().getAsJsonObject("Destination").get("CountryName").getAsString();

                        RunData tripRunData = new RunData();
                        tripRunData.setCountry(country);
                        tripRunData.setCreated(LocalDateTime.now());
                        tripRunData.setLink(link);
                        tripRunData.setPrice(new BigDecimal(price));
                        tripRunData.setResortName(resortName);
                        tripRunData.setDepartureDate(LocalDateTime.parse(departureDate));
                        tripRunData.setStarRating(starRating);
                        tripRunData.setRun(run);

                        results.add(tripRunData);
                    }
                }
            }
        }

        run.setRuns(results);

        runRepository.save(run);

        process();
    }

    @GetMapping("process")
    public void process()
    {
        List<Run> runsAwaitingProcessing = runRepository.findByProcessedIsNull();

        for (Run run : runsAwaitingProcessing) {
            run.getTrips().removeIf(runData -> !tripMeetsCriteria(runData));
            run.setProcessed(true);

            runRepository.save(run);

            if (run.getTrips().size() > 0) {
                alert(run.getId());
            }
        }
    }

    private Boolean tripMeetsCriteria(RunData trip)
    {
        if (trip.getPrice().compareTo(new BigDecimal(1500)) > 0) {
            return false;
        }

        if (trip.getDepartureDate().isBefore(LocalDateTime.parse("2024-12-25T00:00:00")) || trip.getDepartureDate().isAfter(LocalDateTime.parse("2024-12-29T00:00:00"))) {
            return false;
        }

        if (trip.getStarRating() < 3) {
            return false;
        }

        return true;
    }

    @GetMapping("alert")
    public void alert(Long runId)
    {
        Twilio.init(environment.getProperty("twilio.username"), environment.getProperty("twilio.password"));

        Message.creator(new com.twilio.type.PhoneNumber(environment.getProperty("phone.first")), new com.twilio.type.PhoneNumber(environment.getProperty("twilio.originator.phone")), "Trips matching criteria found. View here: trips.dylanelliott.ca/runs/" + runId).create();
        Message.creator(new com.twilio.type.PhoneNumber(environment.getProperty("phone.second")), new com.twilio.type.PhoneNumber(environment.getProperty("twilio.originator.phone")), "Trips matching criteria found. View here: trips.dylanelliott.ca/runs/" + runId).create();
    }

    public Map<String, String> getHeaders()
    {
        Map<String, String> headerMap = new HashMap<>();

        headerMap.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headerMap.put("Accept-Language", "en-US,en;q=0.9");
        headerMap.put("Cache-Control", "max-age=0");
        headerMap.put("Host", "book.sunwing.ca");
        headerMap.put("Sec-Fetch-Dest", "document");
        headerMap.put("Sec-Fetch-Mode", "navigate");
        headerMap.put("Sec-Fetch-Site", "none");
        headerMap.put("Sec-Fetch-User", "?1");
        headerMap.put("Upgrade-Insecure-Requests", "1");
        headerMap.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        headerMap.put("Sec-Ch-Device-Memory", "8");
        headerMap.put("Sec-Ch-Ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"");
        headerMap.put("Sec-Ch-Ua-Arch", "\"arm\"");
        headerMap.put("Sec-Ch-Ua-Full-Version-List", "\"Google Chrome\";v=\"131.0.6778.109\", \"Chromium\";v=\"131.0.6778.109\", \"Not_A Brand\";v=\"24.0.0.0\"");
        headerMap.put("Sec-Ch-Ua-Mobile", "?0");
        headerMap.put("Sec-Ch-Ua-Model", "\"\"");
        headerMap.put("Sec-Ch-Ua-Platform", "\"macOS\"");

        return headerMap;
    }
}
