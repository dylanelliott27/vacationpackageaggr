package ca.dylanelliott.tripfinder.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.dylanelliott.tripfinder.models.RunData;

public interface RunDataRepository extends JpaRepository<RunData, Long> {

}
