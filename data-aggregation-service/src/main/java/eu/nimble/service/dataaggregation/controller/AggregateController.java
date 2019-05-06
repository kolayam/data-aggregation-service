package eu.nimble.service.dataaggregation.controller;

import eu.nimble.service.dataaggregation.clients.BusinessProcessClient;
import eu.nimble.service.dataaggregation.clients.CatalogueClient;
import eu.nimble.service.dataaggregation.clients.IdentityClient;
import eu.nimble.service.dataaggregation.domain.BusinessProcessStatistics;
import eu.nimble.service.dataaggregation.domain.CatalogueStatistics;
import eu.nimble.service.dataaggregation.domain.PlatformStats;
import eu.nimble.service.dataaggregation.domain.IdentityStatistics;
import eu.nimble.service.dataaggregation.domain.TradingVolume;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;

import static eu.nimble.service.dataaggregation.clients.BusinessProcessClient.Role.*;
import static eu.nimble.service.dataaggregation.clients.BusinessProcessClient.Status.*;
import static eu.nimble.service.dataaggregation.clients.BusinessProcessClient.Type.*;


/**
 * REST Controller for managing data channels.
 *
 * @author Johannes Innerbichler
 */
@Controller
@RequestMapping(path = "/")
@Api("Data Aggregation API")
@SuppressWarnings("unused")
public class AggregateController {

    private static Logger logger = LoggerFactory.getLogger(AggregateController.class);

    @Autowired
    private IdentityClient identityClient;

    @Autowired
    private BusinessProcessClient businessProcessClient;

    @Autowired
    private CatalogueClient catalogueClient;

    @Autowired
    private Environment environment;

    @PostConstruct
    public void init() {
        logger.info("Using the following URLs: {}, {}", environment.getProperty("nimble.identity.url"), environment.getProperty("nimble.business-process.url"));
    }

    @ApiOperation(value = "Aggregate statistics of platform.", nickname = "getPlatformStats", response = PlatformStats.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Aggregated statistics of platform"),
            @ApiResponse(code = 400, message = "Error while aggregating statistics.")})
    @RequestMapping(value = "/", produces = {"application/json"}, method = RequestMethod.GET)
    public ResponseEntity<?> getPlatformStatistics(@ApiParam(value = "The Bearer token provided by the identity service") @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                   @ApiParam(value = "companyID (not yet supported") @RequestParam(required = false) String companyID) {

        logger.info("Start aggregating platform statistics...");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // collect statistics from Identity service
        IdentityStatistics identityStats = identityClient.getIdentityStatistics();

        // statistics from Business-Process service
        Integer totalBusinessProcesses = businessProcessClient.getTotalCountOfProcesses(bearerToken);
        Integer totalBusinessProcessesWaiting = businessProcessClient.getProcessCountByStatus(WAITINGRESPONSE, bearerToken);
        Integer totalBusinessProcessesApproved = businessProcessClient.getProcessCountByStatus(APPROVED, bearerToken);
        Integer totalBusinessProcessesDenied = businessProcessClient.getProcessCountByStatus(DENIED, bearerToken);
        Integer totalBusinessProcessesBuyer = businessProcessClient.getProcessCountByRole(BUYER, bearerToken);
        Integer totalBusinessProcessesSeller = businessProcessClient.getProcessCountByRole(SELLER, bearerToken);
        Integer totalBusinessProcessesInformationRequest = businessProcessClient.getProcessCountByType(ITEM_INFORMATION_REQUEST, bearerToken);
        Integer totalBusinessProcessesNegotiations = businessProcessClient.getProcessCountByType(NEGOTIATION, bearerToken);
        Integer totalBusinessProcessesOrder = businessProcessClient.getProcessCountByType(ORDER, bearerToken);

        BusinessProcessStatistics businessProcessStatistics = new BusinessProcessStatistics(totalBusinessProcesses, totalBusinessProcessesWaiting,
                totalBusinessProcessesApproved, totalBusinessProcessesDenied, totalBusinessProcessesBuyer, totalBusinessProcessesSeller,
                totalBusinessProcessesInformationRequest, totalBusinessProcessesNegotiations, totalBusinessProcessesOrder);

        // trading volume
        Double volumeWaiting = businessProcessClient.getTradingVolumeByStatus(WAITINGRESPONSE, bearerToken);
        Double volumeApproved = businessProcessClient.getTradingVolumeByStatus(APPROVED, bearerToken);
        Double volumeDenied = businessProcessClient.getTradingVolumeByStatus(DENIED, bearerToken);
        TradingVolume tradingVolume = new TradingVolume(volumeWaiting, volumeApproved, volumeDenied);

        CatalogueStatistics catStats = catalogueClient.getTotalProductsAndServices(bearerToken);

        // aggregate statistics
        PlatformStats platformStats = new PlatformStats();
        platformStats.setIdentity(identityStats);
        platformStats.setBusinessProcessCount(businessProcessStatistics);
        platformStats.setTradingVolume(tradingVolume);
        platformStats.setCatalogueStatistics(catStats);

        stopWatch.stop();
        logger.info("Finished aggregation of platform statistics in {} ms", stopWatch.getLastTaskTimeMillis());

        return ResponseEntity.ok(platformStats);
    }
}
