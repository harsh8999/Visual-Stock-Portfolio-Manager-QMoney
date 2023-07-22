
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {


  private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  
  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest
  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws StockQuoteServiceException, JsonProcessingException {

      String url = buildUri(symbol, from, to);

      try {
        String response = Optional.ofNullable(this.restTemplate.getForObject(url, String.class))
                                  .map(s -> s.isEmpty() ? null : s)
                                  .orElseThrow(() -> new StockQuoteServiceException("Cannot receive response from Tiingo Service!!!"));
        // String response = this.restTemplate.getForObject(url, String.class);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        Candle[] candles = mapper.readValue(response, TiingoCandle[].class);
        if(candles == null) {
          return new ArrayList<>();
        }
        return Arrays.asList(candles);
      } catch(StockQuoteServiceException | JsonProcessingException e) {
        throw e;
      } catch(Exception e) {
        throw new RuntimeException(e.getMessage());
      } 
        /*
    try {
      String url = buildUri(symbol, from, to);
      
      String response = Optional.ofNullable(this.restTemplate.getForObject(url, String.class))
                                  .map(s -> s.isEmpty() ? null : s)
                                  .orElseThrow(() -> new StockQuoteServiceException("Cannot receive response from Alphaventage Service!!!"));
        
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());
      Candle[] candles = mapper.readValue(response, TiingoCandle[].class);
      if(candles == null) {
        return new ArrayList<>();
      }
      return Arrays.asList(candles);
    } catch(StockQuoteServiceException | JsonProcessingException e) {
      throw e;
    } catch(Exception e) {
      throw new RuntimeException(e.getMessage());
    } 
    */
  }


  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.
  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    //  String uriTemplate = "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
    //       + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    String token = "17291c16a70d8a68e47231a95690257b8c584c86";
    return "https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?startDate="+startDate.toString()
                        +"&endDate="+endDate.toString()+"&token="+token;
  }
}
