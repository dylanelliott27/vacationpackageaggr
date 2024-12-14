package ca.dylanelliott.tripfinder.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import ca.dylanelliott.tripfinder.models.Run;

@Repository
public interface RunRepository extends JpaRepository<Run, Long>
{
    public List<Run> findByProcessedIsNull();

    public Run findTopByOrderByCreatedDesc();
}