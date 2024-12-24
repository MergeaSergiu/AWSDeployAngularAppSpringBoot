package AWS.API.repository;

import AWS.API.entity.Distribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CloudFrontRepository extends JpaRepository<Distribution, Long> {

    List<Distribution> findAllByUserId(Long id);

    @Query("SELECT d FROM Distribution d WHERE d.distributionIdentifier = :distributionIdentifier")
    Distribution findByDistributionIdentifier(String distributionIdentifier);
}
