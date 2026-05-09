package ch.uzh.ifi.seal.monolith2microservices.services.evaluation;

import ch.uzh.ifi.seal.monolith2microservices.models.evaluation.EvaluationMetrics;
import ch.uzh.ifi.seal.monolith2microservices.models.evaluation.MicroserviceMetrics;
import ch.uzh.ifi.seal.monolith2microservices.models.graph.Component;
import ch.uzh.ifi.seal.monolith2microservices.models.graph.Decomposition;
import ch.uzh.ifi.seal.monolith2microservices.persistence.DecompositionMetricsRepository;
import ch.uzh.ifi.seal.monolith2microservices.persistence.MicroserviceMetricsRepository;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Genc on 15.01.2017.
 */
@Service
public class EvaluationService {

    private Logger logger = LoggerFactory.getLogger(EvaluationService.class);

    @Autowired
    DecompositionEvaluationService decompositionEvaluationService;

    @Autowired
    MicroserviceEvaluationService microserviceEvaluationService;

    @Autowired
    MicroserviceMetricsRepository microserviceMetricsRepository;

    @Autowired
    DecompositionMetricsRepository decompositionMetricsRepository;

    @Async
    public void performEvaluation(Decomposition decomposition, Repository repo) {
        try {
            List<MicroserviceMetrics> microserviceMetrics = computeMicroserviceMetrics(decomposition, repo);
            microserviceMetricsRepository.saveAll(microserviceMetrics);

            EvaluationMetrics metrics = decompositionEvaluationService.computeMetrics(decomposition,
                    microserviceMetrics);
            decompositionMetricsRepository.save(metrics);
            logger.info("Saved Metrics to Database");
        } catch (IOException ioe) {
            logger.error(ioe.toString());
        } catch (GitAPIException e) {
            logger.error(e.toString());
        }
    }

    private List<MicroserviceMetrics> computeMicroserviceMetrics(Decomposition decomposition, Repository repo)
            throws IOException, GitAPIException {
        List<MicroserviceMetrics> microserviceMetrics = new ArrayList<>();
        for (Component microservice : decomposition.getServices()) {
            microserviceMetrics.add(microserviceEvaluationService.from(microservice, decomposition.getRepository(),
                    decomposition.getHistory(), decomposition.getParameters().getGranularity(),
                    decomposition.getParameters().isUsingTreeSitter(), repo));
        }
        return microserviceMetrics;
    }

}
