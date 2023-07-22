
package com.crio.warmup.stock.quotes;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

  private RestTemplate restTemplate;


  public AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }



  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement the StockQuoteService interface as per the contracts. Call Alphavantage service
  //  to fetch daily adjusted data for last 20 years.
  //  Refer to documentation here: https://www.alphavantage.co/documentation/
  //  --
  //  The implementation of this functions will be doing following tasks:
  //    1. Build the appropriate url to communicate with third-party.
  //       The url should consider startDate and endDate if it is supported by the provider.
  private String buildUrl(String symbol) {
    String token = "1KGFB1U6TNTSUK3G";
    return "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol="+symbol+"&outputsize=full&apikey="+token;
  }

  //    2. Perform third-party communication with the url prepared in step#1
  //    3. Map the response and convert the same to List<Candle>
  //    4. If the provider does not support startDate and endDate, then the implementation
  //       should also filter the dates based on startDate and endDate. Make sure that
  //       result contains the records for for startDate and endDate after filtering.
  //    5. Return a sorted List<Candle> sorted ascending based on Candle#getDate
  //  IMP: Do remember to write readable and maintainable code, There will be few functions like
  //    Checking if given date falls within provided date range, etc.
  //    Make sure that you write Unit tests for all such functions.
  //  Note:
  //  1. Make sure you use {RestTemplate#getForObject(URI, String)} else the test will fail.
  //  2. Run the tests using command below and make sure it passes:
  //    ./gradlew test --tests AlphavantageServiceTest
  //CHECKSTYLE:OFF
    //CHECKSTYLE:ON
  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  1. Write a method to create appropriate url to call Alphavantage service. The method should
  //     be using configurations provided in the {@link @application.properties}.
  //  2. Use this method in #getStockQuote.


  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
  throws JsonProcessingException, StockQuoteServiceException {

    // String response = this.restTemplate.getForObject(buildUrl(symbol), String.class);
    // ObjectMapper objectMapper = new ObjectMapper();
    // objectMapper.registerModule(new JavaTimeModule());

    // try {
    //   AlphavantageDailyResponse candles = objectMapper.readValue(response, AlphavantageDailyResponse.class);
    //   TreeMap<LocalDate, AlphavantageCandle> sortedCandles = new TreeMap<>(candles.getCandles());
      
    //   return sortedCandles.subMap(from, true, to, true)
    //                       .entrySet()
    //                       .stream()
    //                       .map(entry -> {
    //                         entry.getValue().setDate(entry.getKey());
    //                         return entry.getValue();
    //                       })
    //                       .collect(Collectors.toList());
    // } catch(Exception e) {
    //   throw new StockQuoteServiceException("Cannot receive response from Alphaventage Service!!!");
    // }

    try {
      String url = buildUrl(symbol);
      String response = Optional.ofNullable(this.restTemplate.getForObject(url, String.class))
                                .map(s -> s.isEmpty() ? null : s)
                                .orElseThrow(() -> new StockQuoteServiceException("Cannot receive response from Alphaventage Service!!!"));
                                
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      AlphavantageDailyResponse alphavantageDailyResponse = objectMapper.readValue(response, AlphavantageDailyResponse.class);
      Map<LocalDate, AlphavantageCandle> candles = Optional.ofNullable(alphavantageDailyResponse.getCandles())
                                                            .orElseThrow(() -> new StockQuoteServiceException("Cannot receive response from Alphaventage Service!!!"));
      TreeMap<LocalDate, AlphavantageCandle> sortedCandles = new TreeMap<>(candles);
      
      return sortedCandles.subMap(from, true,to, true)
                          .entrySet()
                          .stream()
                          .map(entry -> {
                            entry.getValue().setDate(entry.getKey());
                            return entry.getValue();
                          })
                          .collect(Collectors.toList());
    } catch(JsonProcessingException | StockQuoteServiceException e ) {
      throw e;
    } catch(Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

}

