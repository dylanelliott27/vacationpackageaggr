package ca.dylanelliott.tripfinder.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

@Entity
public class Run {
    @Id
    @GeneratedValue
    private Long id;

    private LocalDateTime created;

    @Column(nullable = true)
    private Boolean processed;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RunData> runs;

    public List<RunData> getTrips()
    {
        return this.runs;
    }

    public Long getId()
    {
        return this.id;
    }

    public LocalDateTime getCreated()
    {
        return this.created;
    }

    public void setCreated(LocalDateTime timestamp)
    {
        this.created = timestamp;
    }

    public void setRuns(List<RunData> runs) {
        this.runs = runs;
    }

    public void setProcessed(boolean newState)
    {
        this.processed = newState;
    }

    public void removeTrip(RunData trip)
    {
        this.runs.removeIf(runData -> runData.getId().equals(trip.getId()));
    }
}
