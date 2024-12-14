package ca.dylanelliott.tripfinder.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class RunData {
    @Id
    @GeneratedValue
    private Long id;
    
    private LocalDateTime created;
    
    private String resortName;
    
    private String link;
    
    private String country;

    private BigDecimal price;

    private LocalDateTime departureDate;

    private Integer starRating;

    @ManyToOne
    @JoinColumn (
        name = "run_id",
        nullable = false
    )
    private Run run;

    public Long getId()
    {
        return this.id;
    }

    public LocalDateTime getCreated()
    {
        return this.created;
    }

    public String getResortName()
    {
        return this.resortName;
    }

    public String getLink()
    {
        return this.link;
    }

    public String getCountry()
    {
        return this.country;
    }

    public BigDecimal getPrice()
    {
        return this.price;
    }

    public LocalDateTime getDepartureDate()
    {
        return this.departureDate;
    }

    public Integer getStarRating()
    {
        return this.starRating;
    }

    public void setCreated(LocalDateTime timestamp)
    {
        this.created = timestamp;
    }

    public void setResortName(String resortName)
    {
        this.resortName = resortName;
    }

    public void setLink(String link)
    {
        this.link = link;
    }

    public void setCountry(String country)
    {
        this.country = country;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public void setDepartureDate(LocalDateTime timestamp) 
    {
        this.departureDate = timestamp;
    }

    public void setStarRating(Integer rating)
    {
        this.starRating = rating;
    }

    public void setRun(Run run)
    {
        this.run = run;
    }
}
