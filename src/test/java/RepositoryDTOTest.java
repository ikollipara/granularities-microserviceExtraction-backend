import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.seal.monolith2microservices.dtos.RepositoryDTO;

/**
 * Created by gmazlami on 12/7/16.
 */
public class RepositoryDTOTest {

    private RepositoryDTO repositoryDTO;

    @BeforeEach
    public void setUp() {
        repositoryDTO = new RepositoryDTO();
        repositoryDTO.setUri("https://github.com/feincms/feincms.git");
    }

    @Test
    public void testGetName() {
        assertEquals("feincms", repositoryDTO.getName());
    }
}
