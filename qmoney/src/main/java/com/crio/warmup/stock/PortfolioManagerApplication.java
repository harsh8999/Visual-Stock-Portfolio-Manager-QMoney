
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;



public class PortfolioManagerApplication<mainCalculateSingleReturn> {
  
  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    List<String> symbol = new ArrayList<>();

    List<PortfolioTrade> trades = readTradesFromJson(args[0]);
    
    trades.forEach(trade -> symbol.add(trade.getSymbol()));
    return symbol;
    // return Collections.emptyList();
  }


  // TODO: CRIO_TASK_MODULE_REST_API
  //  Find out the closing price of each stock on the end_date and return the list
  //  of all symbols in ascending order by its close value on end date.

  // Note:
  // 1. You may have to register on Tiingo to get the api_token.
  // 2. Look at args parameter and the module instructions carefully.
  // 2. You can copy relevant code from #mainReadFile to parse the Json.
  // 3. Use RestTemplate#getForObject in order to call the API,
  //    and deserialize the results in List<Candle>

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
      List<String> result = new ArrayList<>();
      
      // PortfolioTrade[] portfolioTradeDtos = objectMapper.readValue(filePath, PortfolioTrade[].class);
      List<PortfolioTrade> trades = readTradesFromJson(args[0]);
      
      LocalDate endDate = LocalDate.parse(args[1]);

      List<TotalReturnsDto> totalReturnsDtoList = new ArrayList<>();

      for(PortfolioTrade trade: trades) {
        List<Candle> responses = fetchCandles(trade, endDate, getToken());
        Candle lastResponse = responses.get(responses.size()-1);
        TotalReturnsDto totalReturnsDto = new TotalReturnsDto(trade.getSymbol(), lastResponse.getClose());
        totalReturnsDtoList.add(totalReturnsDto);
      }

      Collections.sort(totalReturnsDtoList, (arg0, arg1) -> (int) (arg0.getClosingPrice() - arg1.getClosingPrice()));
      totalReturnsDtoList.forEach(e-> result.add(e.getSymbol()));
      return result;
  }

 


  // TODO:
  //  After refactor, make sure that the tests pass by using these two commands
  //  ./gradlew test --tests PortfolioManagerApplicationTest.readTradesFromJson
  //  ./gradlew test --tests PortfolioManagerApplicationTest.mainReadFile
  public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
    File filePath = resolveFileFromResources(filename);
    PortfolioTrade[] portfolioTradeDtos = getObjectMapper().readValue(filePath, PortfolioTrade[].class);
    return Arrays.asList(portfolioTradeDtos);
  }


  // TODO:
  //  Build the Url using given parameters and use this function in your code to cann the API.
  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
    String symbol = trade.getSymbol();
    LocalDate startDate = trade.getPurchaseDate();
    return "https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?startDate="+startDate.toString()+"&endDate="+endDate.toString()+"&token="+token;
  }


  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }



  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  //  for the stocks provided in the Json.
  //  Use the function you just wrote #calculateAnnualizedReturns.
  //  Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.




  // TODO:
  //  Ensure all tests are passing using below command
  //  ./gradlew test --tests ModuleThreeRefactorTest
  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
     return candles.get(0).getOpen();
  }


  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
     return candles.get(candles.size()-1).getClose();
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
    String url = prepareUrl(trade, endDate, getToken());
    Candle[] responses = getRestTemplate().getForObject(url, TiingoCandle[].class);
    return Arrays.asList(responses);
  }

  private static RestTemplate getRestTemplate() {
    return new RestTemplate();
  }


  private static Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }
  
  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args) throws IOException, URISyntaxException {
        List<AnnualizedReturn> annualizedReturnsList = new ArrayList<>();
        List<PortfolioTrade> trades = readTradesFromJson(args[0]);
        if (trades.isEmpty()) {
          return Collections.emptyList();
        }
        // get parsed endDate (yyyy-MM-dd)
        LocalDate endDate = LocalDate.parse(args[1]);

        for(PortfolioTrade trade: trades) {
          List<Candle> candles = fetchCandles(trade, endDate, getToken());

          double buyPrice = getOpeningPriceOnStartDate(candles);
          double sellPrice = getClosingPriceOnEndDate(candles);
          AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice);
          annualizedReturnsList.add(annualizedReturn);
        }

        // sort in decending
        // Collections.sort(annualizedReturnsList);
        Collections.sort(annualizedReturnsList, getComparator());
        

        return annualizedReturnsList;
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Return the populated list of AnnualizedReturn for all stocks.
  //  Annualized returns should be calculated in two steps:
  //   1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  //      1.1 Store the same as totalReturns
  //   2. Calculate extrapolated annualized returns by scaling the same in years span.
  //      The formula is:
  //      annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  //      2.1 Store the same as annualized_returns
  //  Test the same using below specified command. The build should be successful.
  //     ./gradlew test --tests PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
      double totalReturn = (sellPrice - buyPrice) / buyPrice;
      double totalNumYears = (double) trade.getPurchaseDate().until(endDate, ChronoUnit.DAYS)/365.24;
      double annualizedReturns = Math.pow((1 + totalReturn), (1 /totalNumYears)) - 1;
      return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturn);
  }



  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "/home/crio-user/workspace/harsh-kumar8999-ME_QMONEY_V2/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@7c6908d7";
    String functionNameFromTestFileInStackTrace = "mainReadFile";
    String lineNumberFromTestFileInStackTrace = "29";


   return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
       toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
       lineNumberFromTestFileInStackTrace});
 }


  // public static void main(String[] args) throws Exception {
  //   Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
  //   ThreadContext.put("runId", UUID.randomUUID().toString());
  //   // printJsonObject(mainReadQuotes(args));

  //   printJsonObject(mainCalculateSingleReturn(args));

  // }


  public static String getToken() {
    return "17291c16a70d8a68e47231a95690257b8c584c86";

  }




  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
        String file = args[0];
        LocalDate endDate = LocalDate.parse(args[1]);
        String contents = readFileAsString(file);
        ObjectMapper objectMapper = getObjectMapper();

        PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(getRestTemplate());
        
        PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);

        return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }


  private static String readFileAsString(String file) throws IOException {
    return new String(Files.readAllBytes(Paths.get(file)));
  }


  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());


    printJsonObject(mainCalculateReturnsAfterRefactor(args));
    

  }
}

