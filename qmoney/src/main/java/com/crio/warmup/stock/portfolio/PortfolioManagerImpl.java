
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.JsonMappingException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Collections;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {


  private RestTemplate restTemplate;

  private StockQuotesService stockQuotesService;

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  @Deprecated
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  protected PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }



  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws StockQuoteServiceException, JsonProcessingException {
    // String uri = buildUri(symbol, from, to);
    // Candle[] responses = this.restTemplate.getForObject(uri, TiingoCandle[].class);
    // return Arrays.asList(responses);
    String uri = buildUri(symbol, from, to);
    String responses = restTemplate.getForObject(uri, String.class);
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    Candle[] candles = mapper.readValue(responses, TiingoCandle[].class);
    // List<TiingoCandle> candles = mapper.readValue(responses, new TypeReference<List<TiingoCandle>>(){});
    
    if(candles == null) {
      return new ArrayList<>();
    }
    return Arrays.asList(candles);
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
      //  String uriTemplate = "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
      //       + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    return "https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?startDate="+startDate.toString()+"&endDate="+endDate.toString()+"&token="+getToken();
  }

  private String getToken() {
    return "17291c16a70d8a68e47231a95690257b8c584c86";

  }


  private Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.get(0).getOpen();
  } 


  private Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.get(candles.size()-1).getClose();
  }

  private AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
    PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    double totalReturn = (sellPrice - buyPrice) / buyPrice;
    double totalNumYears = (double) trade.getPurchaseDate().until(endDate, ChronoUnit.DAYS)/365.24;
    double annualizedReturns = Math.pow((1 + totalReturn), (1 /totalNumYears)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturn);
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) throws StockQuoteServiceException {
    
        List<AnnualizedReturn> annualizedReturnsList = new ArrayList<>();
        if (portfolioTrades.isEmpty()) {
          return new ArrayList<>();
        }

        for(PortfolioTrade trade: portfolioTrades) {
          // get candles from any of the service
          try {
            List<Candle> candles = stockQuotesService.getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
            double buyPrice = getOpeningPriceOnStartDate(candles);
            double sellPrice = getClosingPriceOnEndDate(candles);
            AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice);
            annualizedReturnsList.add(annualizedReturn);
          } catch(JsonProcessingException e) {
            e.printStackTrace();
          }
          
        }

        // sort in decending
        Collections.sort(annualizedReturnsList, getComparator());

        return annualizedReturnsList;
    
  }




  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
      throws InterruptedException, StockQuoteServiceException {
    

    if (portfolioTrades.isEmpty()) {
      return new ArrayList<>();
    }
    // Parallize this part
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    List<Future<AnnualizedReturn>> futures = new ArrayList<>();
    for(PortfolioTrade trade: portfolioTrades) {
      Future<AnnualizedReturn> future = executor.submit(() -> {
                                              List<Candle> candles = stockQuotesService.getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
                                              double buyPrice = getOpeningPriceOnStartDate(candles);
                                              double sellPrice = getClosingPriceOnEndDate(candles);
                                              AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice);
                                              // annualizedReturnsList.add(annualizedReturn);
                                              return annualizedReturn;
                                            });
      futures.add(future);
    }
    
    List<AnnualizedReturn> annualizedReturnsList = new ArrayList<>(); 
    for(Future<AnnualizedReturn> future: futures) {
      try {
        annualizedReturnsList.add(future.get());
      } catch (InterruptedException | ExecutionException e) {
        throw new StockQuoteServiceException(e.getMessage());
      } catch (Exception e) {
        throw new StockQuoteServiceException("Exception!!!");
      }
    }

    // shutdown the executor
    executor.shutdown();
    try {
        if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
            executor.shutdownNow();
        } 
    } catch (InterruptedException e) {
        executor.shutdownNow();
    }

    // sort in decending
    Collections.sort(annualizedReturnsList, getComparator());

    return annualizedReturnsList;
  }

}
