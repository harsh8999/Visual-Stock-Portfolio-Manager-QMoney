
package com.crio.warmup.stock;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;


class TiingoServiceTestAfterException {
    

    @Test
    void testTiingoSerive() throws InterruptedException{
        RestTemplate restTemplate = new RestTemplate();
        PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager("", restTemplate);
        PortfolioTrade trade1 = new PortfolioTrade("AAPL", 50, LocalDate.parse("2019-01-02"));
        PortfolioTrade trade2 = new PortfolioTrade("GOOGL", 100, LocalDate.parse("2019-01-02"));
        PortfolioTrade trade3 = new PortfolioTrade("MSFT", 20, LocalDate.parse("2019-01-02"));
        List<PortfolioTrade> portfolioTrades = Arrays
            .asList(new PortfolioTrade[]{trade1, trade2, trade3});
        LocalDate endDate = LocalDate.now().minus(1, ChronoUnit.DAYS);
        List<AnnualizedReturn> calculateAnnualizedReturn;
        try {
            calculateAnnualizedReturn = portfolioManager.calculateAnnualizedReturnParallel(portfolioTrades, endDate, 5);
            calculateAnnualizedReturn.forEach(e->System.out.println(e.toString()));
        } catch (StockQuoteServiceException e) {
            e.printStackTrace();
        }
        
        
    }
}