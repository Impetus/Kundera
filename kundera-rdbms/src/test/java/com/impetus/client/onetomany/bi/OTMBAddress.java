package com.impetus.client.onetomany.bi;

//~--- JDK imports ------------------------------------------------------------

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

//import com.impetus.client.entity.Person;
@Entity
@Table(
    name = "ADDRESS",
    schema = "KunderaKeyspace@kcassandra"
)
public class OTMBAddress
{
    @Id
    @Column(name = "ADDRESS_ID")
    private String addressId;
    @OneToMany(mappedBy = "address")

    // pointing Person's address field
    @Column(name = "PERSON_ID")

    // inverse=true
    private Set<OTMBNPerson> people;
    @Column(name = "STREET")
    private String street;

    public OTMBAddress()
    {
    }

    public String getAddressId()
    {
        return addressId;
    }

    public void setAddressId(String addressId)
    {
        this.addressId = addressId;
    }

    public String getStreet()
    {
        return street;
    }

    public void setStreet(String street)
    {
        this.street = street;
    }

    /**
     * @return the people
     */
    public Set<OTMBNPerson> getPeople()
    {
        return people;
    }

    /**
     * @param people the people to set
     */
    public void setPeople(Set<OTMBNPerson> people)
    {
        this.people = people;
    }
}
