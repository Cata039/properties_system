//class D
package org.example;
import java.util.Date;
import java.io.Serializable;

public class Lease implements IContract, Serializable {
    private String tenantName;
    private Date startDate;
    private Date endDate;
    private double monthlyRent;
    private String propertyAddress;
    private boolean isTerminated = false;


    public Lease(String tenantName, Date startDate, Date endDate, double monthlyRent, String propertyAddress) {
        this.tenantName = tenantName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlyRent = monthlyRent;
        this.propertyAddress = propertyAddress;
    }

    public String getTenantName() {
        return tenantName;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public double getMonthlyRent() {
        return monthlyRent;
    }

    public String getPropertyAddress() {
        return propertyAddress;
    }

    @Override
    public void terminateContract() {
        this.isTerminated = true;  // Mark as terminated
        System.out.println("Terminating lease for tenant: " + tenantName);
    }

    @Override
    public String toString() {
        return "Lease{" +
                "tenantName='" + tenantName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", monthlyRent=" + monthlyRent +
                '}';
    }
}
